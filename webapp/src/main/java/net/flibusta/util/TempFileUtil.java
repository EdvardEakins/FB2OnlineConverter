package net.flibusta.util;

import java.io.File;
import java.io.IOException;

public class TempFileUtil {

    public static File createTempDir() throws IOException {
        File temp = File.createTempFile("temp", Long.toString(System.nanoTime()));

        if (!(temp.delete())) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }

        if (!(temp.mkdir())) {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }

        return temp;
    }
}
