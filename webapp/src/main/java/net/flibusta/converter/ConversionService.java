package net.flibusta.converter;

import java.io.File;

public interface ConversionService {
    File convert(String bookId) throws ConversionException;
}
