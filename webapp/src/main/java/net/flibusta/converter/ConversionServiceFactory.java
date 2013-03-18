package net.flibusta.converter;


public interface ConversionServiceFactory {
    ConversionService getConversionService(String targetFormat);
}
