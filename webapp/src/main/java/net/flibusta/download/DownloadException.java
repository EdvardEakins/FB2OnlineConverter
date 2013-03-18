package net.flibusta.download;

import java.io.IOException;

public class DownloadException extends IOException {
    public DownloadException() {
    }

    public DownloadException(String message) {
        super(message);
    }

    public DownloadException(String message, Throwable cause) {
        super(message, cause);
    }

    public DownloadException(Throwable cause) {
        super(cause);
    }
}
