package server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.stream.Stream;

public class WebAppServer {


    static final String API_ROUT = "/api";
    static final String ROUT_SEPARATOR = "/";
    private final String BACK_END_GREETING = "Hello from BackEnd.";

    private final Timer timer = new Timer();

    private final int threadNumber;
    private final String frontEndResources;
    private final int port;
    private final long maxInactiveSession;
    private final Semaphore rateLimiter;


    private final Class<? extends Session> sessionClass;



    private static final Map<String, Session> sessions = Collections.synchronizedMap(new HashMap<>());



    public WebAppServer(int threadNumber, int maxConnections, String frontEndResources, int port, long maxInactiveSession, Class<? extends Session> sessionClass) {
        rateLimiter = new Semaphore(maxConnections);
        this.threadNumber = threadNumber;
        this.frontEndResources = frontEndResources;
        this.port = port;
        this.maxInactiveSession = maxInactiveSession;
        this.sessionClass = sessionClass;
    }

    public void start() throws IOException {

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        initBackend(server);
        initFrontend(server);

        ExecutorService executor = Executors.newFixedThreadPool(threadNumber);
        server.setExecutor(executor);
        server.start();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                removeOldSessions();
            }
        }, 0,  maxInactiveSession * 1000L);

        System.out.println("Server is listening on port 8080");

    }

    // Front end //

    private void initFrontend(HttpServer server) {
        try {
            Path resourceDir = Paths.get(Objects.requireNonNull(WebAppServer.class.getResource(frontEndResources)).toURI());
            try (Stream<Path> paths = Files.walk(resourceDir)) {
                paths.filter(Files::isRegularFile).forEach(path -> {
                    String contextPath = ROUT_SEPARATOR + resourceDir.relativize(path).toString().replace(File.separator, ROUT_SEPARATOR);
                    server.createContext(FilenameUtils.removeExtension(contextPath), exchange -> {
                        if (rateLimiter.tryAcquire()) {
                            serveResource(exchange, path);
                            rateLimiter.release();
                        } else {
                            exchange.sendResponseHeaders(429, -1);
                        }

                    });
                });
            } catch (IOException e) {
                System.err.println("Error loading frontend file: " + e.getMessage());
            }
        } catch (URISyntaxException | NullPointerException e) {
            System.err.println("Error loading frontend resources: " + e.getMessage());
        }
    }


    // Back end //

    private void initBackend(HttpServer server) {
        server.createContext(API_ROUT, exchange -> {
            if (rateLimiter.tryAcquire()) {
                try {
                    switch (exchange.getRequestMethod()) {
                        case "GET":
                            get(exchange, BACK_END_GREETING);
                            break;
                        case "POST":
                            post(exchange);
                            break;
                        default:
                            exchange.sendResponseHeaders(405, -1); // Method not allowed
                            break;
                    }
                } finally {
                    rateLimiter.release();
                }
            } else {
                exchange.sendResponseHeaders(429, -1); // Too many requests
            }
        });
    }

    private void serveResource(HttpExchange exchange, Path path) throws IOException {

        try (InputStream resourceStream = Files.newInputStream(path)) {
            byte[] response = resourceStream.readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        }

    }


    private void get(HttpExchange exchange, String response) throws IOException {
        exchange.sendResponseHeaders(200, response.length());
        exchange.getResponseBody().write(response.getBytes());
        exchange.getResponseBody().close();
    }


    private void post(HttpExchange exchange) throws IOException {

        InputStream requestBody = exchange.getRequestBody();
        JsonObject data = JsonParser.parseString(new String(requestBody.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();

        Response response = update(data);
        String responseData = response.data.toString();

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(response.rCode, responseData.length());
        exchange.getResponseBody().write(responseData.getBytes());
        exchange.getResponseBody().close();
    }


    private Response update(JsonObject data) {
        try {
            String methodName = data.get("methodName").getAsString();
            if ("newSession".equals(methodName)) return newSession();

            String sessionId = data.get("sessionId").getAsString();
            Session session = sessions.get(sessionId);

            if (session != null) {
                return session.runMethod(methodName, data);
            } else {
                return new Response("This session does not exist.", true, 404);
            }
        } catch (NullPointerException e) {
            return new Response("Session ID or method name missing!", 400);
        } catch (Exception e) {
            return new Response("Unable to process request: " + e.getMessage(), 500);
        }
    }


    private void removeOldSessions() {
        LocalTime now = LocalTime.now();
        sessions.entrySet().removeIf(session -> Duration.between(session.getValue().getSessionTime(), now).getSeconds() > maxInactiveSession);
    }

    private Response newSession() {

        String sessionId = Long.toString(new SecureRandom().nextLong(), 36);

        try {
            sessions.put(sessionId, sessionClass.getDeclaredConstructor().newInstance());

            JsonObject data = new JsonObject();
            data.addProperty("sessionId", sessionId);

            return new Response(data, 200);
        } catch (Exception e) {
            return new Response("Error generating session ID.", 500);
        }
    }


}
