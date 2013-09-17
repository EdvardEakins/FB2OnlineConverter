package net.flibusta.download.impl;

import net.flibusta.download.DownloadException;
import net.flibusta.download.DownloadService;
import net.flibusta.util.TempFileUtil;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.util.concurrent.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class TimedDownloadService implements DownloadService {
    Logger logger = Logger.getLogger(TimedDownloadService.class);

    private ExecutorService fetchExecutor;
    private int fetchPoolSize = 2;
    private long fetchTimeoutSeconds = 60;
    private String onionProxyHost;
    private int onionProxyPort;

    public void setOnionProxyHost(String onionProxyHost) {
        this.onionProxyHost = onionProxyHost;
    }

    public void setOnionProxyPort(int onionProxyPort) {
        this.onionProxyPort = onionProxyPort;
    }

    public void setI2pProxyHost(String i2pProxyHost) {
        this.i2pProxyHost = i2pProxyHost;
    }

    public void setI2pProxyPort(int i2pProxyPort) {
        this.i2pProxyPort = i2pProxyPort;
    }

    private String i2pProxyHost;
    private int i2pProxyPort;

    public TimedDownloadService() {
    }

    public void init() {
        fetchExecutor = Executors.newFixedThreadPool(fetchPoolSize);
    }

    @Override
    public File fetch(final URL url) throws Exception {
        logger.debug("Fetch " + url);
        Callable<File> task = new Callable<File>() {
            @Override
            public File call() throws Exception {
                return executeFetch(url);
            }
        };
        Future<File> submit;
        try {
            submit = fetchExecutor.submit(task);
        } catch (RejectedExecutionException e) {
            logger.warn("Fetch Pool overloaded. url=" + url);
            throw new RejectedExecutionException("Server overloaded. Please try late.");
        }
        try {
            return submit.get(fetchTimeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new TimeoutException("Book download timed out for " + url);
        }
    }

    public File executeFetch(URL url) throws Exception {
        HttpClient httpClient = getHttpClient();
        HostConfiguration hostConfiguration = getHostConfiguration(url);

        String uri = url.toString();
        GetMethod method = new GetMethod(uri);
        method.getParams().setParameter(HttpMethodParams.USER_AGENT, "Mobipocket/ePub Converter");
//        method.getParams().setParameter(HttpMethodParams.USER_AGENT, "Mozilla/5.0 (X11; Linux i686) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.46 Safari/536.5");
        method.getParams().setParameter(HttpMethodParams.SO_TIMEOUT, (int) TimeUnit.MILLISECONDS.convert(fetchTimeoutSeconds, TimeUnit.SECONDS));
        int retryCount = 10;
        int code;
        String fileName;
        File file;
        try {
            try {

                do {
                    logger.debug("Start download " + url + " try=" + (10 - retryCount));
                    code = httpClient.executeMethod(hostConfiguration, method);
                    if (code == 503) {
                        Thread.sleep(500);
                    }
                } while (code == 503 && retryCount-- > 0);

            } catch (IOException e) {
                logger.error("Download from " + uri + " failed with exception " + e.getMessage(), e);
                method.releaseConnection();
                throw new DownloadException("Download from " + uri + " failed with exception " + e.getMessage(), e);
            }
            if (code != 200) {
                method.releaseConnection();
                throw new DownloadException("File download failed with code " + code + " from url " + uri);
            }

            InputStream sourceStream = method.getResponseBodyAsStream();

            fileName = method.getPath();

            Header responseHeader = method.getResponseHeader("Content-Disposition");
            if (responseHeader != null) {
                String value = responseHeader.getValue();
                if (value.contains("attachment; filename=\"")) {
                    fileName = value.substring("attachment; filename=\"".length(), value.length() - 1);
                }
            }

            file = createTempFile(fileName);

            OutputStream targetStream = new FileOutputStream(file);

            try {
                IOUtils.copy(sourceStream, targetStream);
            } catch (Exception e) {
                IOUtils.closeQuietly(targetStream);
                FileUtils.deleteQuietly(file);
                throw e;
            } finally {
                IOUtils.closeQuietly(sourceStream);
                IOUtils.closeQuietly(targetStream);
                method.releaseConnection();
            }
        } finally {
            method.releaseConnection();
            httpClient.getHttpConnectionManager().closeIdleConnections(0);
        }

        Header contentType = method.getResponseHeader("Content-Type");
        if ((contentType != null && "application/zip".equals(contentType.getValue())) || fileName.endsWith(".zip")) {
            file = unzip(file);
        }
        logger.debug("Downloaded " + file.getName());
        return file;
    }

    private HostConfiguration getHostConfiguration(URL url) {
        HostConfiguration hostConfiguration = null;
        if (url.getHost().endsWith(".i2p")) {
            hostConfiguration = new HostConfiguration();
            hostConfiguration.setProxy(i2pProxyHost, i2pProxyPort);
        }
        if (url.getHost().endsWith(".onion")) {
            hostConfiguration = new HostConfiguration();
            hostConfiguration.setProxy(onionProxyHost, onionProxyPort);
        }
        return hostConfiguration;
    }

    private HttpClient getHttpClient() {
        HttpConnectionManager httpConnectionManager = new SimpleHttpConnectionManager(false);
        HttpConnectionManagerParams params = httpConnectionManager.getParams();
        params.setDefaultMaxConnectionsPerHost(20);
        params.setMaxTotalConnections(30);
        params.setLinger(10);

        return new HttpClient(httpConnectionManager);

    }

    private File unzip(File file) throws Exception {
        File tempDir = TempFileUtil.createTempDir();
        File tempFile = null;
        ZipInputStream inputStream = new ZipInputStream(new FileInputStream(file));
        try {
            ZipEntry zipEntry;
            int archiveFileCount = 0;
            while ((zipEntry = inputStream.getNextEntry()) != null) {
                if (zipEntry.getName().endsWith(".fbd")) {
                    continue;
                }
                tempFile = new File(tempDir, zipEntry.getName());

                OutputStream outputStream = new FileOutputStream(tempFile);
                try {
                    IOUtils.copy(inputStream, outputStream);
                } finally {
                    IOUtils.closeQuietly(outputStream);
                }
                if (++archiveFileCount > 1) {
                    throw new Exception("Multifile archives not supported");
                }
            }
        } finally {
            IOUtils.closeQuietly(inputStream);
            file.delete();
        }

        if (tempFile == null) {
            FileUtils.deleteDirectory(tempDir);
            return null;
        }

        // move unpacked file to upper level and remove temporary directory
        File systemTempDir = new File(System.getProperty("java.io.tmpdir"));
        String tempFileName = tempFile.getName();
        boolean isFileMoved = false;
        int tryCount = 20;
        File targetFile;
        do {
            targetFile = new File(systemTempDir, tempFileName);
            try {
                FileUtils.moveFile(tempFile, targetFile);
                isFileMoved = true;
            } catch (IOException e) {
                tempFileName = tempFileName.substring(0, 1) + tempFileName;
            }
        } while (!isFileMoved && tryCount-- > 0);

        FileUtils.deleteDirectory(tempDir);

        if (!isFileMoved) {
            throw new IOException("Can't move file " + tempFile.getName() + " to " + systemTempDir.getName());
        }

        return targetFile;
    }

    private File createTempFile(String path) throws IOException {
        String name = FilenameUtils.getName(path);
        if (name.length() == 0) {
            return File.createTempFile("tmp", null);
        }
        File systemTempDir = new File(System.getProperty("java.io.tmpdir"));
        return new File(systemTempDir, name);
    }


    public void shutdown() {
        fetchExecutor.shutdownNow();
    }

    public void setFetchPoolSize(int fetchPoolSize) {
        this.fetchPoolSize = fetchPoolSize;
    }

    public void setFetchTimeoutSeconds(long fetchTimeoutSeconds) {
        this.fetchTimeoutSeconds = fetchTimeoutSeconds;
    }
}
