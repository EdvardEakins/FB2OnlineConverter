package net.flibusta.servlet;

import net.flibusta.persistence.dao.BookDao;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Controller
public class DownloadController {
    Logger logger = Logger.getLogger(DownloadController.class);

    @Autowired
    private BookDao bookDao;

    @RequestMapping(value = "/download/{bookid}/{format}/{name}", method = RequestMethod.GET)
    public void download(@PathVariable("bookid") String bookId, @PathVariable("format") String format, @PathVariable("name") String fileName,
                         HttpServletResponse response) throws IOException {
        File file = bookDao.findBook(bookId, format);
        if (file == null) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            logger.warn("File for download not found: bookId=" + bookId + " format=" + format + " name=" + fileName);
            return;
        }

        logger.debug("Send file:  bookId=" + bookId + " format=" + format + " name=" + fileName);
        streamFile(file, format, response);
    }

    private void streamFile(File book, String outputFormat, HttpServletResponse response) throws IOException {
        response.setContentType(getContentType(outputFormat));
        response.setHeader("Content-Length", Long.toString(book.length()));
        response.setHeader("Content-Disposition", "attachment; filename=" + book.getName());

        ServletOutputStream outputStream = response.getOutputStream();
        FileInputStream inputStream = new FileInputStream(book);
        try {
            IOUtils.copy(inputStream, outputStream);
        } finally {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(outputStream);
        }
    }

    private String getContentType(String outputFormat) {
        String mimeType;
        if ("epub".equals(outputFormat)) {
            mimeType = "application/epub+zip";
        } else if ("mobi".equals(outputFormat)) {
            mimeType = "application/x-mobipocket-ebook";
        } else {
            mimeType = "application/octetstream";
        }
        return mimeType;
    }


}
