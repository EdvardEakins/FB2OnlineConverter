package net.flibusta.converter.impl;

import net.flibusta.converter.ConversionException;
import net.flibusta.converter.ConversionService;
import net.flibusta.converter.Converter;
import net.flibusta.persistence.dao.BookDao;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;

public class ToEpubConversionService implements ConversionService{
    private Logger logger = Logger.getLogger(ToEpubConversionService.class);

    private BookDao bookDao;
    private Converter fb2epub;
    private Converter rtfepub;
    private Converter docxepub;

    public void setBookDao(BookDao bookDao) {
        this.bookDao = bookDao;
    }

    public void setFb2epub(Converter fb2epub) {
        this.fb2epub = fb2epub;
    }

    public void setRtfepub(Converter rtfepub) {
        this.rtfepub = rtfepub;
    }

    public void setDocxepub(Converter docxepub) {
        this.docxepub = docxepub;
    }

    @Override
    public File convert(String bookId) throws ConversionException {
        File epub;

        File sourceFile = bookDao.findBook(bookId, "fb2");
        if (sourceFile != null) {
            epub = fb2epub.convert(sourceFile);
        } else {
            sourceFile = bookDao.findBook(bookId, "rtf");
            if (sourceFile != null) {
                epub = rtfepub.convert(sourceFile);
            } else {
                sourceFile = bookDao.findBook(bookId, "docx");
                if (sourceFile != null) {
                    epub = docxepub.convert(sourceFile);
                } else {
                    throw new ConversionException("Compatible source format not found for book " + bookId);
                }
            }
        }

        if (epub == null || !epub.exists()) {
            throw new ConversionException("Conversion to epub failed for bookId = " + bookId);
        }
        File epubStorageFile = new File(epub.getParentFile(), FilenameUtils.getBaseName(sourceFile.getName()) + ".epub");
        if (!epubStorageFile.exists()) {
            try {
                FileUtils.moveFile(epub, epubStorageFile);
            } catch (IOException e) {
                throw new ConversionException("Can't move file " + epub.getName() + " to " + epubStorageFile.getName(), e);
            }
        } else {
            logger.warn("ToEubConvertService: file already exists: bookId=" + bookId + " file=" + epubStorageFile);
        }


        return bookDao.addBook(bookId, "epub", epubStorageFile);
    }

}
