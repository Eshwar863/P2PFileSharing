package org.example.Controller;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.example.Service.FileSharer;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileController {
    private final FileSharer fileSharer;
    private final HttpServer httpServer;
    private final String uploadPath;
    private final ExecutorService executorService;

    public FileController(int port) throws IOException {
        this.fileSharer = new FileSharer();
        this.httpServer = HttpServer.create(new InetSocketAddress(port),0);
        this.uploadPath = System.getProperty("java.io,tmpdir")+ File.separator+"/peerlink-uploads";
        this.executorService = Executors.newFixedThreadPool(10);

        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()){
            uploadDir.mkdirs();
        }

        httpServer.createContext("/upload",new UploadHandler());
        httpServer.createContext("/download",new DownloadHandler());
        httpServer.createContext("/private",new CROSHandler());
        httpServer.setExecutor(executorService);
    }


    public void start(){
        httpServer.start();
        System.out.println("Server Started on port: "+httpServer.getAddress().getPort());
    }

    public void stop(){
        executorService.shutdown();
        httpServer.stop(0);
        executorService.shutdownNow();
        System.out.println("Server Stopped");
    }

    public class CROSHandler implements HttpHandler {
        @Override
        public void handle(com.sun.net.httpserver.HttpExchange httpExchange) throws IOException {
            httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin","*");
            httpExchange.sendResponseHeaders(200,0);
        }
    }

}
