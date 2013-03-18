package net.flibusta.converter.impl;

import net.flibusta.converter.ConversionException;
import net.flibusta.converter.Converter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class EpubToMobiConverter implements Converter, ApplicationContextAware {
    final Logger logger = Logger.getLogger(EpubToMobiConverter.class);

    private String path2kindlegen;

    @Override
    public File convert(File epub) throws ConversionException {

        String[] cmd = new String[2];
        cmd[0] = path2kindlegen;
        cmd[1] = epub.getAbsolutePath();

        try {
            int exitCode = execProcess(cmd);

            if (exitCode != 0 && exitCode != 1) { // ok or warning
                throw new ConversionException("Epub to mobi conversion failed with error code " + exitCode);
            }
        } catch (IOException e) {
            throw new ConversionException("Epub to mobi conversion failed: " + e.getMessage(), e);
        }

        String name = FilenameUtils.getBaseName(epub.getName());


        return new File(epub.getParent(), name + ".mobi");
    }

    private int execProcess(String[] cmd) throws IOException {
        final Process process = Runtime.getRuntime().exec(cmd);
        StreamConsumer stdout = new StreamConsumer(process.getInputStream());
        Thread inputReader = new Thread(stdout);
        inputReader.start();
        StreamConsumer stderr = new StreamConsumer(process.getErrorStream());
        Thread stdErrReader = new Thread(stderr);
        stdErrReader.start();
        int exitCode = -1;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            process.destroy();
            inputReader.interrupt();
            stdErrReader.interrupt();
        } finally {
            try {
                inputReader.join();
            } catch (InterruptedException e) {
                // exit
            }
            try {
                stdErrReader.join();
            } catch (InterruptedException e) {
                // exit
            }
        }

        logger.debug("kindlegen stdout: " + stdout.getResult());
        String stderrResult = stderr.getResult();
        if (stderrResult.length() > 0) {
            logger.warn("kindlegen stderr: " + stderrResult);
        }

        return exitCode;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Resource resource = applicationContext.getResource("WEB-INF/bin/kindlegen");
        if (!resource.exists()) {
            throw new FatalBeanException("Required conversion utility not found: WEB-INF/bin/kindlegen");
        }
        try {
            this.path2kindlegen = resource.getFile().getAbsolutePath();
            String[] cmd = new String[] {"chmod", "a+x", this.path2kindlegen};
            int exitCode = execProcess(cmd);
            if (exitCode != 0) {
                throw new FatalBeanException("Can not set executable bin on conversion utility " + this.path2kindlegen);
            }
        } catch (IOException e) {
            throw new FatalBeanException("Required conversion utility not found: WEB-INF/bin/kindlegen: " + e.getMessage());
        }
    }


    private static class StreamConsumer implements Runnable {
        private final InputStream stream;
        private StringBuilder logMessage = new StringBuilder(4096);

        public StreamConsumer(InputStream stream) {
            this.stream = stream;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[4096];
            int count;
            try {
                while ((count = stream.read(buffer)) > 0) {
                    logMessage.append(new String(buffer, 0, count));
                    if (Thread.interrupted()) {
                        return;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Can't read stdin from kindlegen");
            } finally {
                IOUtils.closeQuietly(stream);
            }

        }

        public String getResult() {
            return logMessage.toString();
        }
    }

}
