package net.flibusta.servlet;

import net.flibusta.persistence.dao.BatchDao;
import net.flibusta.persistence.dao.BookDao;
import net.flibusta.persistence.dao.UrlDao;
import net.flibusta.persistence.dao.UrlInfo;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Controller
public class MassConvertController {

    public static final String PARAM_URL = "url";
    public static final String PARAM_URL_TEMPLATE = "urltemplate";
    public static final String PARAM_OUT_FORMAT = "out";
    public static final String PARAM_SOURCE_FORMAT = "src";
    public static final String DEFAULT_OUT_FORMAT = "mobi";
    public static final String DEFAULT_URL_TEMPLATE = "http://flibusta.net/b/{0}/download";

    private String staticRedirectUrlPrefix = null;
    private Boolean useXAccelRerirect = false;
    private int convertersPoolSize = 10;


    ExecutorService converterExecutor = Executors.newFixedThreadPool(convertersPoolSize);

    @Autowired
    SingleUrlConverter  singleConverterController;

    @Autowired
    BatchDao batchDao;

    @Autowired
    UrlDao urlDao;

    @Autowired
    BookDao bookDao;

    @RequestMapping(value = "/batch", method = RequestMethod.GET)
    public void convert(@RequestParam(PARAM_URL) String sourceUrlParams,
                        @RequestParam(value = PARAM_URL_TEMPLATE, required = false, defaultValue = DEFAULT_URL_TEMPLATE) String sourceUrlTemplate,
                        @RequestParam(value = PARAM_OUT_FORMAT, required = false, defaultValue = DEFAULT_OUT_FORMAT) String outputFormat,
                        HttpServletResponse response) throws Exception {

        if (sourceUrlParams == null || sourceUrlParams.length() == 0) {
            throw new Exception("Required parameter missing: " + PARAM_URL);
        }


        String[] sourceUrls = getSourceUrls(sourceUrlTemplate, sourceUrlParams);
        Arrays.sort(sourceUrls);

        String batchSignature = calculateBatchSignature(sourceUrls);

        String batchFilePath = batchDao.findBatchPath(batchSignature, outputFormat);

        if (batchFilePath != null) { // already packed
            sendRedirect(response, batchFilePath);
            return;
        }


        List<Callable<Object>> tasks = new ArrayList<Callable<Object>>(sourceUrls.length);

        for (final String sourceUrl : sourceUrls) {
            tasks.add(createSingleConversionTask(sourceUrl, outputFormat));
        }

        converterExecutor.invokeAll(tasks); // make sure all files are converted


        File batchFile = zipFiles(sourceUrls, outputFormat, batchSignature);

        batchDao.addBatch(batchSignature, outputFormat, batchFile);
        batchFilePath = batchDao.findBatchPath(batchSignature, outputFormat);
        sendRedirect(response, batchFilePath);
    }


    private void sendRedirect(HttpServletResponse response, String batchFilePath) throws IOException {
        if (!useXAccelRerirect) {
            response.sendRedirect(staticRedirectUrlPrefix + batchFilePath);
        } else {
            response.setHeader("X-Accel-Redirect", staticRedirectUrlPrefix + batchFilePath);
            String fileName = FilenameUtils.getName(batchFilePath);
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
            response.setStatus(HttpStatus.OK.value());
        }

    }

    private String[] getSourceUrls(String sourceUrlTemplate, String sourceUrlParams) {

        String[] params = sourceUrlParams.split("_");
        String[] urls = new String[params.length];

        MessageFormat template = new MessageFormat(sourceUrlTemplate);
        for (int i = 0; i < params.length; i++) {
            String param = params[i];
            String[] subParams = param.split(";");
            StringBuffer url = template.format(subParams, new StringBuffer(), null);
            urls[i] = url.toString();
        }

        return urls;
    }

    private File zipFiles(String[] sourceUrls, String outputFormat, String batchSignature) throws IOException {
        File batchFile = File.createTempFile("b_" + batchSignature, ".zip");
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(batchFile));
        zipOutputStream.setLevel(ZipOutputStream.STORED);

        try {
            byte[] buffer = new byte[8 * 1024];
            for (String sourceUrl : sourceUrls) {
                UrlInfo urlInfo = urlDao.findUrlInfo(sourceUrl);
                if (urlInfo != null) {
                    File book = bookDao.findBook(urlInfo.getBookId(), outputFormat);
                    if (book != null) {
                        ZipEntry entry = new ZipEntry(book.getName());
                        zipOutputStream.putNextEntry(entry);
                        FileInputStream fileInputStream = new FileInputStream(book);
                        int read;
                        while ((read = fileInputStream.read(buffer)) > 0) {
                            zipOutputStream.write(buffer, 0, read);
                        }
                        IOUtils.closeQuietly(fileInputStream);
                    }
                }
            }
        } catch (Exception e) {
            batchFile.delete();
            throw new IOException(e);
        } finally {
            IOUtils.closeQuietly(zipOutputStream);
        }
        return batchFile;
    }

    private String calculateBatchSignature(String[] sourceUrls) {
        StringBuilder buffer = new StringBuilder();
        for (String sourceUrl : sourceUrls) {
            buffer.append(sourceUrl);
        }
        return DigestUtils.md5Hex(buffer.toString());
    }

    private Callable<Object> createSingleConversionTask(final String sourceUrl, final String outputFormat) {
        return Executors.callable(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            singleConverterController.convert(sourceUrl, null, outputFormat, null, null);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
        );
    }

    public void shutdown() {
        converterExecutor.shutdown();
    }

    public void setStaticRedirectUrlPrefix(String staticRedirectUrlPrefix) {
        this.staticRedirectUrlPrefix = staticRedirectUrlPrefix;
    }

    public void setConvertersPoolSize(int convertersPoolSize) {
        this.convertersPoolSize = convertersPoolSize;
    }

    public void setUseXAccelRerirect(Boolean useXAccelRerirect) {
        this.useXAccelRerirect = useXAccelRerirect;
    }
}
