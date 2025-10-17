package org.example.Handlers.HttpHandlers;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class CROSHandler implements HttpHandler {
    @Override
    public void handle(com.sun.net.httpserver.HttpExchange httpExchange) throws IOException {
        Headers headers = httpExchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin","*");
        headers.add("Access-Control-Allow-Methods","GET,POST,OPTIONS");
        headers.add("Access-Control-Allow-Headers","Content-Type,Authorization");

        if(httpExchange.getRequestHeaders().equals("OPTIONS")){
            httpExchange.sendResponseHeaders(204,-1);
            return;
        }

        String response = "NOT FOUND";
        httpExchange.sendResponseHeaders(404,response.getBytes().length);
        if(httpExchange.getRequestHeaders().equals("POST")){}
        if(httpExchange.getRequestHeaders().equals("OPTIONS")){}


    }
}