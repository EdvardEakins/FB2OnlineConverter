package net.flibusta.servlet;

import net.flibusta.persistence.dao.BatchDao;
import net.flibusta.persistence.dao.BookDao;
import net.flibusta.persistence.dao.UrlDao;
import net.flibusta.persistence.dao.UrlInfo;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestMassConvertController {

    @Test
    public void testZipFile() throws Exception {
        MassConvertController controller = new MassConvertController();
        BatchDao batchDao = mock(BatchDao.class);
//        when(batchDao.findBatchPath(anyString(), anyString())).thenAnswer(new Answer<String>() {
//            @Override
//            public String answer(InvocationOnMock invocation) throws Throwable {
//                return createTestFile((String) invocation.getArguments()[0], (String) invocation.getArguments()[1]);
//            }
//        });
        when(batchDao.findBatchPath(anyString(), anyString())).thenReturn(null);

        controller.batchDao = batchDao;

        UrlDao urlDao = mock(UrlDao.class);
        when(urlDao.findUrlInfo("http://flibusta.net/b/123/fb2")).thenAnswer(new Answer<UrlInfo>() {
            @Override
            public UrlInfo answer(InvocationOnMock invocation) throws Throwable {
                UrlInfo info = new UrlInfo();
                info.setBookId("123");
                info.setSourceFormat("epub");
                return info;
            }
        });
        when(urlDao.findUrlInfo("http://flibusta.net/b/345/fb2?t=def")).thenAnswer(new Answer<UrlInfo>() {
            @Override
            public UrlInfo answer(InvocationOnMock invocation) throws Throwable {
                UrlInfo info = new UrlInfo();
                info.setBookId("345");
                info.setSourceFormat("epub");
                return info;
            }
        });

        controller.urlDao = urlDao;


        BookDao bookDao = mock(BookDao.class);
        when(bookDao.findBook("123", "epub")).thenReturn(null);
        File testFile = createTestFile("345", "epub");
        when(bookDao.findBook("345", "epub")).thenReturn(testFile);

        controller.bookDao = bookDao;

        controller.singleConverterController = new SingleUrlConverter() {
            @Override
            public void convert(String sourceUrl, String sourceMd5, String outputFormat, String sourceFormat, HttpServletResponse response) throws Exception {

            }
        };

        controller.convert("123;abc;dd_345;def", "http://flibusta.net/b/{0}/fb2?t={1}", "epub",
                new HttpServletResponse() {
            @Override
            public void addCookie(Cookie cookie) {

            }

            @Override
            public boolean containsHeader(String name) {
                return false;
            }

            @Override
            public String encodeURL(String url) {
                return null;
            }

            @Override
            public String encodeRedirectURL(String url) {
                return null;
            }

            @Override
            public String encodeUrl(String url) {
                return null;
            }

            @Override
            public String encodeRedirectUrl(String url) {
                return null;
            }

            @Override
            public void sendError(int sc, String msg) throws IOException {

            }

            @Override
            public void sendError(int sc) throws IOException {

            }

            @Override
            public void sendRedirect(String location) throws IOException {
                System.out.println("location = " + location);
            }

            @Override
            public void setDateHeader(String name, long date) {

            }

            @Override
            public void addDateHeader(String name, long date) {

            }

            @Override
            public void setHeader(String name, String value) {

            }

            @Override
            public void addHeader(String name, String value) {

            }

            @Override
            public void setIntHeader(String name, int value) {

            }

            @Override
            public void addIntHeader(String name, int value) {

            }

            @Override
            public void setStatus(int sc) {

            }

            @Override
            public void setStatus(int sc, String sm) {

            }

            @Override
            public String getCharacterEncoding() {
                return null;
            }

            @Override
            public String getContentType() {
                return null;
            }

            @Override
            public ServletOutputStream getOutputStream() throws IOException {
                return null;
            }

            @Override
            public PrintWriter getWriter() throws IOException {
                return null;
            }

            @Override
            public void setCharacterEncoding(String charset) {

            }

            @Override
            public void setContentLength(int len) {

            }

            @Override
            public void setContentType(String type) {

            }

            @Override
            public void setBufferSize(int size) {

            }

            @Override
            public int getBufferSize() {
                return 0;
            }

            @Override
            public void flushBuffer() throws IOException {

            }

            @Override
            public void resetBuffer() {

            }

            @Override
            public boolean isCommitted() {
                return false;
            }

            @Override
            public void reset() {

            }

            @Override
            public void setLocale(Locale loc) {

            }

            @Override
            public Locale getLocale() {
                return null;
            }
        });


    }

    private File createTestFile(String batchId, String batchFormat) {
        try {
            File tempFile = File.createTempFile(batchId, "." + batchFormat);
            tempFile.deleteOnExit();
            return tempFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



}
