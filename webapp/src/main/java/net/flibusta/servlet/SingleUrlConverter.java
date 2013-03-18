package net.flibusta.servlet;

import javax.servlet.http.HttpServletResponse;

public interface SingleUrlConverter {
    void convert(String sourceUrl,
                 String sourceMd5, String outputFormat,
                 String sourceFormat,
                 HttpServletResponse response) throws Exception;
}
