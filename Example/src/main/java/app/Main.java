package app;



import server.WebAppServer;

import java.io.IOException;

public class Main {
    static final int THREAD_NUMBER = 100;

    static final int PORT = 8080;
    static final String FRONT_END_RESOURCES = "/public";

    public static void main(String[] args) throws IOException {

        WebAppServer webServer = new WebAppServer(THREAD_NUMBER,100, FRONT_END_RESOURCES, PORT, 20, UserSession.class);
        webServer.start();

    }
}