package net.flibusta.download;

import java.io.File;
import java.net.URL;

public interface DownloadService {
    File fetch(URL url) throws Exception;
}
