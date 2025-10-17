package org.example.Service;

import org.example.Handlers.FileHandlers.FileSenderHandler;
import org.example.Utils.UploadUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class FileSharer {

    private HashMap<Integer,String> avilableFiles;

    public FileSharer() {
        avilableFiles = new HashMap<>();
    }

    public int offerFile(String fileName){
        int port;
        while(true){
            port = UploadUtils.generatePort();
            if(!avilableFiles.containsKey(port)){
                avilableFiles.put(port,fileName);
                return port;
            }
        }
    }

    public void startFileServer(int port){
        String filePath = avilableFiles.get(port);
        if (filePath == null){
            System.out.println("File not found on port: "+port);
            return;
        }
        try (ServerSocket serverSocket = new ServerSocket(port)){
            System.out.println("Serving File"+new File(filePath) +" on port: "+port);
            Socket ClientSocket = serverSocket.accept();
            System.out.println("Client connected on port: "+ClientSocket.getInetAddress() );
            new Thread(new FileSenderHandler(ClientSocket,filePath)).start();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

