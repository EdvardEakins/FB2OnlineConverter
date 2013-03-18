package net.flibusta.servlet;

import net.flibusta.mailer.BookSender;
import net.flibusta.persistence.dao.BookDao;
import net.flibusta.persistence.dao.UrlDao;
import net.flibusta.persistence.dao.UrlInfo;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

@Controller
public class MailController {
    Logger logger = Logger.getLogger(MailController.class);

    public static final String PARAM_URL = "url";
    public static final String PARAM_MD5 = "md5";
    public static final String PARAM_OUT_FORMAT = "out";
    public static final String DEFAULT_OUT_FORMAT = "mobi";
    public static final String PARAM_TARGET_ADDRESS = "to";

    @Autowired
    SingleUrlConverter  singleConverterController;

    @Autowired
    UrlDao urlDao;

    @Autowired
    BookDao bookDao;
    private Properties mailSessionProperties;
    private String fromAddress;

    @RequestMapping(value = "/mail", method = RequestMethod.GET)
    public void convert(
            @RequestParam(value = PARAM_MD5, required = false) String sourceMd5,
                        @RequestParam(value = PARAM_OUT_FORMAT, required = false, defaultValue = DEFAULT_OUT_FORMAT) String outputFormat,
                        @RequestParam(value = PARAM_URL, required = false) String sourceUrl,
                        @RequestParam(PARAM_TARGET_ADDRESS) String targetAddress,
                        HttpServletResponse response) throws Exception {

        if (sourceMd5 == null && sourceUrl == null) {
            throw new Exception("One of parameters " + PARAM_URL + " or " + PARAM_MD5 + " required");
        }

        if (sourceUrl == null || sourceUrl.length() == 0) {
            throw new Exception("Parameter " + PARAM_URL + " required");
        }

        singleConverterController.convert(sourceUrl, sourceMd5, outputFormat, null, null);

        UrlInfo urlInfo = urlDao.findUrlInfo(sourceUrl);
        if (urlInfo != null) {
            File book = bookDao.findBook(urlInfo.getBookId(), outputFormat);
            if (book != null) {
                BookSender bookSender = new BookSender(book, urlInfo.getBookId(), outputFormat, targetAddress, mailSessionProperties, fromAddress, fromAddress);
                bookSender.run();
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                ServletOutputStream out = response.getOutputStream();
                out.print("Book conversion failed");
                out.close();
            }
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            ServletOutputStream out = response.getOutputStream();
            out.print("Book download failed");
            out.close();
        }
    }

    @ExceptionHandler(Exception.class)
    public void handleException(Exception e, HttpServletResponse response) {
        logger.error("Exception sent by MailController: " + e.getMessage(), e);
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.setContentType("text/plain");
        try {
            PrintWriter writer = response.getWriter();
            writer.println("Internal server error: " + e.getMessage());
        } catch (IOException e1) {
            logger.error(e);
        }
    }


    public void setMailSessionProperties(Properties mailSessionProperties) {
        this.mailSessionProperties = mailSessionProperties;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }
}
