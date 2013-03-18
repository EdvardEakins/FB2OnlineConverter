package net.flibusta.converter.impl;

import net.flibusta.converter.ConversionService;
import net.flibusta.converter.ConversionServiceFactory;

import java.util.Map;

public class ConversionServiceFactoryImpl implements ConversionServiceFactory {

    private Map<String, ConversionService> serviceMap;

    @Override
    public ConversionService getConversionService(String targetFormat) {
        if (!serviceMap.containsKey(targetFormat)) {
            throw new RuntimeException("Unsupported format: " + targetFormat);
        }
        return serviceMap.get(targetFormat);
    }

    public void setServiceMap(Map<String, ConversionService> serviceMap) {
        this.serviceMap = serviceMap;
    }
}
