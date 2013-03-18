package net.flibusta.download.impl;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;

import static org.junit.Assert.assertTrue;

public class TestSyncHttpDownloadService {

    private static HttpServer server;

    @BeforeClass
    public static void setupServer() throws IOException {
        server = HttpServer.create();
        server.bind(new InetSocketAddress("127.0.0.2", 8080), 1);
        server.createContext("/test", new HttpHandler() {
            @Override
            public void handle(HttpExchange t) throws IOException {
                URI uri = t.getRequestURI();
//                read(is); // .. read the request body
                String response = "This is the response";
                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();

            }
        });
          server.setExecutor(null); // creates a default executor
          server.start();
    }


    @AfterClass
    public static void stopServer() {
        server.stop(0);
    }

    @Test
    public void test() throws Exception {
        SyncHttpDownloadService service = new SyncHttpDownloadService();
        File file = service.fetch(new URL("http://127.0.0.2:8080/test/d.epub"));
//        File file = service.fetch(new URL("http://coronet2.iicm.tugraz.at/wbtmaster/kindle/zip_done/k/Kazenin_Tihie_konfliktyi_na_Severnom_Kavkaze__Adyigeya_Kabardino-Balkariya_Karachaevo-Cherkesiya_282954.epub"));
        assertTrue(file.exists());
        file.delete();
    }

}
