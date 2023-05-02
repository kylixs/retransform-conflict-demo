package com.example.demo;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;

@Controller
public class TestController {

    @RequestMapping("/test")
    @ResponseBody
    public String test() {
        return "test";
    }

    @RequestMapping("/test/{id}")
    @ResponseBody
    public String test(@PathVariable("id") String id) {
        return "test-" + id;
    }

    @RequestMapping("/http")
    @ResponseBody
    public String testJdkHttpClient() {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("https://httpbin.org/headers");
            conn = (HttpURLConnection) url.openConnection();
            InputStream inputStream = conn.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            IOUtils.copy(inputStream, outputStream);
            return outputStream.toString();
        } catch (IOException e) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(outputStream));
            return outputStream.toString();
        } finally {
            if (conn != null) {
                conn.disconnect();

            }
        }
    }
}
