package org.example.Service;

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



    public static class FileSenderHandler implements Runnable{

        private final Socket ClientSocket;
        private final String filePath;

        public FileSenderHandler(Socket ClientSocket,String filePath){
            this.ClientSocket = ClientSocket;
            this.filePath = filePath;
        }
        @Override
        public void run() {
            try(FileInputStream fileInputStream = new FileInputStream(filePath)){
                OutputStream  oos = ClientSocket.getOutputStream();
                String fileName = new File(filePath).getName();
                String header = "File Name: "+fileName+"\n";
                oos.write(header.getBytes());
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fileInputStream.read(buffer)) != -1){
                    oos.write(buffer,0,bytesRead);
                }
                System.out.println("File "+fileName+" sent to client: "+ClientSocket.getInetAddress());
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            finally {
                try{
                    ClientSocket.close();
                }catch (IOException e){
                    System.err.println("Error closing client socket"+e.getMessage());
                }
            }
        }
    }
}

