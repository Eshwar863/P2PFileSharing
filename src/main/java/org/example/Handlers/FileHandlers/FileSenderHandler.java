package org.example.Handlers.FileHandlers;

import java.io.*;
import java.net.Socket;

public class FileSenderHandler implements Runnable{

        private final Socket ClientSocket;
        private final String filePath;

        public FileSenderHandler(Socket ClientSocket,String filePath){
            this.ClientSocket = ClientSocket;
            this.filePath = filePath;
        }
        @Override
        public void run() {
            try(FileInputStream fileInputStream = new FileInputStream(filePath)){
                OutputStream oos = ClientSocket.getOutputStream();
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

