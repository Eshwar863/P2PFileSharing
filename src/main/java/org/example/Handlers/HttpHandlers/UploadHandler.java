package org.example.Handlers.HttpHandlers;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.commons.io.IOUtils;
import org.example.Service.FileSharer;
import org.example.Service.MultipartParser;

import java.io.*;
import java.util.UUID;

public class UploadHandler  implements HttpHandler {
    private final File uploadDir;
    private final FileSharer  fileSharer;
    public UploadHandler(File uploadDir,FileSharer  fileSharer){
        this.uploadDir = uploadDir;
        this.fileSharer = fileSharer;
    }
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            String response = "Method Not Allowed";
            exchange.sendResponseHeaders(405, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
            return;
        }

        Headers requestHeaders = exchange.getRequestHeaders();
        String contentType = requestHeaders.getFirst("Content-Type");

        if (
                contentType == null ||
                        !contentType.startsWith("multipart/form-data")
        ) {
            String response =
                    "Bad Request: Content-Type must be multipart/form-data";
            exchange.sendResponseHeaders(400, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
            return;
        }

        try {
            String boundary = contentType.substring(
                    contentType.indexOf("boundary=") + 9
            );

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(exchange.getRequestBody(), baos);
            byte[] requestData = baos.toByteArray();

            MultipartParser parser = new MultipartParser(
                    requestData,
                    boundary
            );
            MultipartParser.ParseResult result = parser.parse();

            if (result == null) {
                String response =
                        "Bad Request: Could not parse file content";
                exchange.sendResponseHeaders(
                        400,
                        response.getBytes().length
                );
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }

            String filename = result.filename;
            if (filename == null || filename.trim().isEmpty()) {
                filename = "unnamed-file";
            }

            String uniqueFilename =
                    UUID.randomUUID() +
                            "_" +
                            new File(filename).getName();
            String filePath = uploadDir + File.separator + uniqueFilename;

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(result.fileContent);
            }

            int port = FileSharer.offerFile(filePath);

            new Thread(() -> fileSharer.startFileServer(port)).start();

            String jsonResponse = "{\"port\": " + port + "}";
            headers.add("Content-Type", "application/json");
            exchange.sendResponseHeaders(
                    200,
                    jsonResponse.getBytes().length
            );
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(jsonResponse.getBytes());
            }
        } catch (Exception e) {
            System.err.println(
                    "Error processing file upload: " + e.getMessage()
            );
            String response = "Server error: " + e.getMessage();
            exchange.sendResponseHeaders(500, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
}

