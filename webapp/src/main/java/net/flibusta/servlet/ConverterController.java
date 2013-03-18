package net.flibusta.servlet;

import net.flibusta.concurrent.LockManager;
import net.flibusta.converter.ConversionService;
import net.flibusta.converter.ConversionServiceFactory;
import net.flibusta.download.DownloadService;
import net.flibusta.persistence.dao.BookDao;
import net.flibusta.persistence.dao.UrlDao;
import net.flibusta.persistence.dao.UrlInfo;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URI;
import java.net.URL;

@Controller
public class ConverterController implements SingleUrlConverter {

    Logger logger = Logger.getLogger(ConverterController.class);

    public static final String PARAM_URL = "url";
    public static final String PARAM_SOURCE_MD5 = "md5";
    public static final String PARAM_OUT_FORMAT = "out";
    public static final String PARAM_SOURCE_FORMAT = "src";
    public static final String DEFAULT_OUT_FORMAT = "mobi";
    public static final String DEFAULT_SRC_FORMAT = "fb2";
    @Autowired
    private UrlDao urlDao;

    @Autowired
    private BookDao bookDao;

    @Autowired
    private ConversionServiceFactory conversionServiceFactory;

    @Autowired
    private DownloadService downloadService;

    @Autowired
    private LockManager lockManager;

    private String staticRedirectUrlPrefix = null;
    private Boolean useXAccelRerirect = false;

    @Override
    @RequestMapping(value = "/convert", method = {RequestMethod.GET, RequestMethod.HEAD})
    public void convert(@RequestParam(PARAM_URL) String sourceUrl,
                        @RequestParam(value = PARAM_SOURCE_MD5, required = false) String sourceMd5,
                        @RequestParam(value = PARAM_OUT_FORMAT, required = false) String outputFormat,
                        @RequestParam(value = PARAM_SOURCE_FORMAT, required = false) String sourceFormat,
                        HttpServletResponse response) throws Exception {

        if (sourceUrl == null || sourceUrl.length() == 0) {
            throw new Exception("Required parameter missing: " + PARAM_URL);
        }

        if (outputFormat == null) {
            outputFormat = DEFAULT_OUT_FORMAT;
        }

        String bookId;
        lockManager.lock(sourceUrl);
        try {
//            System.out.println("locked " + Thread.currentThread().getName() + " " + sourceUrl);
            UrlInfo urlInfo = urlDao.findUrlInfo(sourceUrl);
            if (urlInfo == null) {

                if (sourceMd5 != null && sourceMd5.length() == 32) {
                    urlInfo = new UrlInfo();
                    urlInfo.setBookId(sourceMd5);
                    urlInfo.setSourceFormat(sourceFormat);
                } else if (sourceUrl.contains("flibusta.net/b/")) { // guess book id and source format from url
                    URI uri = new URI(sourceUrl);
                    String path = uri.getPath();
                    String[] pathElements = path.split("/");
                    if (pathElements.length == 4 && pathElements[2].length() == 32) {
                        urlInfo = new UrlInfo();
                        urlInfo.setBookId(pathElements[2]);
                        String format = "download".equals(pathElements[3]) ? "fb2" : pathElements[3];
                        urlInfo.setSourceFormat(format);
                        urlDao.addUrlReference(sourceUrl, urlInfo.getBookId(), urlInfo.getSourceFormat());
                        logger.debug("guessed bookId=" + urlInfo.getBookId() + " format=" + urlInfo.getSourceFormat() + " from url=" + sourceUrl);
                    }
                }
            }

            if (urlInfo != null) {
                // source book already downloaded
                bookId = urlInfo.getBookId();
                File book = bookDao.findBook(bookId, outputFormat);
                if (book != null) {
                    // book already converted
                    redirectToFile(bookId, book, outputFormat, response);
                    return;
                }

                // book downloaded but not converted yet
                if (bookDao.findBook(bookId, urlInfo.getSourceFormat()) == null) { // just safety check for lost files
                    urlDao.removeUrlReference(sourceUrl);
                    bookId = downloadBook(sourceUrl, sourceFormat);
                }
            } else {
                // source book not downloaded yet
                bookId = downloadBook(sourceUrl, sourceFormat);
            }
        } finally {
            lockManager.unlock(sourceUrl);
//            System.out.println("unlocked " + Thread.currentThread().getName() + " " + sourceUrl);

        }

        logger.info("Start conversion url=" + sourceUrl + " bookId=" + bookId + " format=" + outputFormat);
        makeConversion(bookId, outputFormat, response);
    }

    @RequestMapping(value = "/clean", method = RequestMethod.GET)
    public void clean(@RequestParam(PARAM_URL) String sourceUrl, HttpServletResponse response) throws Exception {

        if (sourceUrl == null || sourceUrl.length() == 0) {
            throw new Exception("Required parameter missing: " + PARAM_URL);
        }

        lockManager.lock(sourceUrl);
        try {
            UrlInfo urlInfo = urlDao.findUrlInfo(sourceUrl);
            if (urlInfo != null) {
                String bookId = urlInfo.getBookId();
                lockManager.lock(bookId);
                try {
                    bookDao.deleteBook(bookId);
                } finally {
                    lockManager.unlock(bookId);
                }
                urlDao.removeUrlReference(sourceUrl);
            }
        } finally {
            lockManager.unlock(sourceUrl);
        }
        response.setStatus(HttpStatus.OK.value());
        response.setHeader("Content-Type", "text/plain");
        PrintWriter writer = response.getWriter();
        writer.write("OK");
    }


    @ExceptionHandler(Exception.class)
    public void handleException(Exception e, HttpServletResponse response) {
        logger.error("Exception sent by ConverterController: " + e.getMessage(), e);
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.setContentType("text/plain");
        try {
            PrintWriter writer = response.getWriter();
            writer.println("Internal server error: " + e.getMessage());
        } catch (IOException e1) {
            logger.error(e);
        }
    }

    private String downloadBook(String sourceUrl, String sourceFormat) throws Exception {

        File sourceFile = downloadService.fetch(new URL(sourceUrl));
        if (sourceFile == null) {
            throw new Exception("Can't download book from url " + sourceUrl);
        }

        String bookId = calculateBookId(sourceFile);

        if (sourceFormat == null) {
            try {
                sourceFormat = resolveSourceFormat(sourceFile);
            } catch (Exception e) {
                logger.error("book " + bookId + " Error: " + e.getMessage());
                throw e;
            }
        }

        urlDao.addUrlReference(sourceUrl, bookId, sourceFormat);
        if (bookDao.findBook(bookId, sourceFormat) == null) {
            bookDao.addBook(bookId, sourceFormat, sourceFile);
        } else {
            // this book already downloaded from other source
            sourceFile.delete();
        }
        return bookId;
    }

    private void makeConversion(String bookId, String outputFormat, HttpServletResponse response) throws Exception {
        File convertedFile;
        lockManager.lock(bookId);
        try {
            convertedFile = bookDao.findBook(bookId, outputFormat);
            if (convertedFile == null) {
                ConversionService conversionService = conversionServiceFactory.getConversionService(outputFormat);
                convertedFile = conversionService.convert(bookId);
            }
        } finally {
            lockManager.unlock(bookId);
        }
        if (convertedFile != null && convertedFile.exists()) {
            logger.debug("converted bookId=" + bookId + " to format=" + outputFormat);
            redirectToFile(bookId, convertedFile, outputFormat, response);
        } else {
            throw new Exception("Conversion failed. bookId=" + bookId + " to format=" + outputFormat);
        }
    }

    private void redirectToFile(String bookId, File convertedFile, String outputFormat, HttpServletResponse response) throws IOException {
        if (response == null) {
            return; // do nothing
        }
        String redirectLocation;
        if (staticRedirectUrlPrefix == null || staticRedirectUrlPrefix.length() == 0) { // redirect to download controller
            redirectLocation = response.encodeRedirectURL("/converter/get/download/" + bookId + "/" + outputFormat + "/" + convertedFile.getName());
        } else {
            redirectLocation = staticRedirectUrlPrefix + bookDao.findBookPath(bookId, outputFormat);
        }
        if (!useXAccelRerirect) {
            response.sendRedirect(redirectLocation);
        } else {
            response.setHeader("X-Accel-Redirect", redirectLocation);
            response.setHeader("Content-Disposition", "attachment; filename=" + convertedFile.getName());
            response.setStatus(HttpStatus.OK.value());
        }
    }


    private String calculateBookId(File sourceFile) throws Exception {
        FileInputStream source = new FileInputStream(sourceFile);
        try {
            return DigestUtils.md5Hex(source);
        } finally {
            IOUtils.closeQuietly(source);
        }
    }

    private String resolveSourceFormat(File sourceFile) throws Exception {
        String sourceFileName = sourceFile.getName();
        String extension = FilenameUtils.getExtension(sourceFileName);
        if (extension.length() > 1) {
            return extension;
        }

        FileReader fileReader = new FileReader(sourceFile);
        BufferedReader reader = new BufferedReader(fileReader, 1024);
        String line;

        line = getNextLine(reader);

        if (line != null) {
            if (line.startsWith("<?xml")) {
                line = getNextLine(reader);
                if (line != null && line.toLowerCase().contains("<fictionbook")) {
                    return "fb2";
                }
            } else {
                if (line.contains("mimetypeapplication/epub+zip")) {
                    return "epub";
                }
            }
        }
        throw new Exception("Can't determine source format. Please set url parameter '" + PARAM_SOURCE_FORMAT + "'");
    }

    private String getNextLine(BufferedReader reader) throws IOException {
        String line;
        do {
            line = reader.readLine();
        } while (line != null && line.trim().length() == 0);
        return line;
    }

    public void setStaticRedirectUrlPrefix(String staticRedirectUrlPrefix) {
        if (!staticRedirectUrlPrefix.endsWith("/")) {
            staticRedirectUrlPrefix = staticRedirectUrlPrefix + "/";
        }
        this.staticRedirectUrlPrefix = staticRedirectUrlPrefix;
    }

    public void setUseXAccelRerirect(Boolean useXAccelRerirect) {
        this.useXAccelRerirect = useXAccelRerirect;
    }
}
