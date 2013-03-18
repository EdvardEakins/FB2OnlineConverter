package net.flibusta.converter.impl;

import com.adobe.dp.epub.conv.ConversionClient;
import com.adobe.dp.epub.conv.ConversionService;
import net.flibusta.converter.ConversionException;
import net.flibusta.converter.Converter;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class AdobeConverterAdapter implements Converter{
    Logger logger = Logger.getLogger(AdobeConverterAdapter.class);

    private ConversionService conversionService;


    @Override
    public File convert(File sourceFile) throws ConversionException {
        return conversionService.convert(sourceFile, null, new MyConversionClient(sourceFile), new PrintWriter(new StringWriter()));
    }


    private class MyConversionClient implements ConversionClient {

        private File fb2;

        public MyConversionClient(File fb2) {
            this.fb2 = fb2;
        }

        @Override
        public void reportProgress(float progress) {

        }

        @Override
        public void reportIssue(String errorCode) {

        }

        @Override
        public File makeFile(String baseName) {
            try {
                return File.createTempFile(FilenameUtils.getBaseName(fb2.getName()), ".epub");
            } catch (IOException e) {
                logger.error("Can't create temp file for epub converter", e);
                throw new RuntimeException(e);
            }
        }
    }

    public void setConversionService(ConversionService conversionService) {
        this.conversionService = conversionService;
    }
}
