package net.flibusta.converter;

import java.io.File;

public interface Converter {
    File convert(File sourceFile) throws ConversionException;
}
