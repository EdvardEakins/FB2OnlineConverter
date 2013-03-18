package net.flibusta.converter.impl;

import net.flibusta.converter.ConversionException;
import net.flibusta.converter.ConversionService;
import net.flibusta.converter.ConversionServiceFactory;
import net.flibusta.converter.Converter;
import net.flibusta.persistence.dao.BookDao;
import org.apache.log4j.Logger;

import java.io.File;

public class ToMobiConversionService implements ConversionService {
    private Logger logger = Logger.getLogger(ToMobiConversionService.class);

    private BookDao bookDao;
    private Converter epub2mobi;
//    private Converter fb2epub;
    private ConversionServiceFactory conversionServiceFactory;

    public void setBookDao(BookDao bookDao) {
        this.bookDao = bookDao;
    }

    public void setEpub2mobi(Converter epub2mobi) {
        this.epub2mobi = epub2mobi;
    }

    public void setConversionServiceFactory(ConversionServiceFactory conversionServiceFactory) {
        this.conversionServiceFactory = conversionServiceFactory;
    }

    @Override
    public File convert(String bookId) throws ConversionException {
        File epub = bookDao.findBook(bookId, "epub");
        if (epub == null) {
            ConversionService conversionService = conversionServiceFactory.getConversionService("epub");
            epub = conversionService.convert(bookId);
        }
        File mobi = epub2mobi.convert(epub);
        return bookDao.addBook(bookId, "mobi", mobi);
    }
}
