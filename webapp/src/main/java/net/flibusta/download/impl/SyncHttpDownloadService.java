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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SyncHttpDownloadService implements DownloadService {
    Logger logger = Logger.getLogger(SyncHttpDownloadService.class);


    private HttpClient httpClient = null;

    @Override
    public File fetch(URL url) throws Exception {
        HttpClient httpClient = getHttpClient();
        String uri = url.toString();
        GetMethod method = new GetMethod(uri);
        method.getParams().setParameter(HttpMethodParams.USER_AGENT, "Mobipocket/ePub Converter");
//        method.getParams().setParameter(HttpMethodParams.USER_AGENT, "Mozilla/5.0 (X11; Linux i686) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.46 Safari/536.5");
        int retryCount = 10;
        int code;
        String fileName;
        File file;
        try {
            try {

                do {
                    logger.debug("Start download " + url + " try=" + (10 - retryCount));
                    code = httpClient.executeMethod(method);
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

    private File createTempFile(String path) throws IOException {
        String name = FilenameUtils.getName(path);
        if (name.length() == 0) {
            return File.createTempFile("tmp", null);
        }
        File systemTempDir = new File(System.getProperty("java.io.tmpdir"));
        return new File(systemTempDir, name);
    }

    private HttpClient getHttpClient() {
        HttpConnectionManager httpConnectionManager = new SimpleHttpConnectionManager(false);
        HttpConnectionManagerParams params = httpConnectionManager.getParams();
        params.setDefaultMaxConnectionsPerHost(20);
        params.setMaxTotalConnections(30);
        params.setLinger(10);

        HostConfiguration hostConfiguration;
        hostConfiguration = new HostConfiguration();
        hostConfiguration.setHost("static.flibusta.net");
        params.setMaxConnectionsPerHost(hostConfiguration, 6);
        httpClient = new HttpClient(httpConnectionManager);
        return httpClient;

    }
//    private synchronized HttpClient getHttpClient() {
//        if (httpClient == null) {
//            HttpConnectionManager httpConnectionManager = new SimpleHttpConnectionManager(false);
//            HttpConnectionManagerParams params = httpConnectionManager.getParams();
//            params.setDefaultMaxConnectionsPerHost(20);
//            params.setMaxTotalConnections(30);
//            params.setLinger(10);
//
//            HostConfiguration hostConfiguration;
//            hostConfiguration = new HostConfiguration();
//            hostConfiguration.setHost("static.flibusta.net");
//            params.setMaxConnectionsPerHost(hostConfiguration, 6);
//            httpClient = new HttpClient(httpConnectionManager);
//
//        }
//        return httpClient;
//    }

    public void shutdown() {
        HttpConnectionManager connectionManager = getHttpClient().getHttpConnectionManager();
        if (connectionManager instanceof MultiThreadedHttpConnectionManager) {
            MultiThreadedHttpConnectionManager manager = (MultiThreadedHttpConnectionManager) connectionManager;
            manager.shutdown();
        }

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

}
