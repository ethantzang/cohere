package org.example;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ReverseProxy implements Runnable {
  static class TenantConfig {
    String targetHost;
    int targetPort;

    public TenantConfig(String targetHost, int targetPort) {
      this.targetHost = targetHost;
      this.targetPort = targetPort;
    }
  }
  private Map<String, TenantConfig> tenantConfigs;

  private final int proxyPort;
  private final ExecutorService executor;
  private ServerSocket serverSocket;
  private boolean running = true;

  public ReverseProxy(int proxyPort, Map<String, TenantConfig> tenantConfigs) {
    this.proxyPort = proxyPort;
    this.tenantConfigs = tenantConfigs;
    this.executor = Executors.newFixedThreadPool(10);
  }

  @Override
  public void run() {
    try {
      serverSocket = new ServerSocket(proxyPort);
      System.out.println("Reverse proxy started on port " + proxyPort);

      while (running) {
        try {
          Socket clientSocket = serverSocket.accept();
          executor.submit(() -> handleRequest(clientSocket));
        } catch (IOException e) {
          if (running) {
            System.err.println("Error accepting connection: " + e.getMessage());
          }
        }
      }
    } catch (IOException e) {
      System.err.println("Error starting server: " + e.getMessage());
    } finally {
      try {
        if (serverSocket != null && !serverSocket.isClosed())
          serverSocket.close();
      } catch (IOException e) {
        System.err.println("Error closing server socket " + e.getMessage());
      }
    }
  }

  public void stop() throws IOException {
    running = false;
    if (serverSocket != null && !serverSocket.isClosed()) {
      serverSocket.close();
    }
    executor.shutdown(); // Initiates an orderly shutdown
    try {
      if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
        executor.shutdownNow(); // Force shutdown if not terminated
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt(); // Preserve interrupt status
      executor.shutdownNow();
    }
  }

  private void handleRequest(Socket clientSocket) {
    try (
        InputStream clientIn = clientSocket.getInputStream();
        OutputStream clientOut = clientSocket.getOutputStream();
    ) {
      // Read the request and extract tenant ID from the headers
      BufferedReader reader = new BufferedReader(new InputStreamReader(clientIn));
      String line;
      String tenantId = null;
      while ((line = reader.readLine()) != null && !line.isBlank()) {
        if (line.startsWith("X-Tenant-ID: ")) {
          tenantId = line.substring("X-Tenant-ID: ".length());
        }
      }

      if (tenantId == null || !tenantConfigs.containsKey(tenantId)) {
        clientOut.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
        clientOut.flush();
        return;
      }

      TenantConfig config = tenantConfigs.get(tenantId);

      try (
          Socket serverSocket = new Socket(config.targetHost, config.targetPort);
          InputStream serverIn = serverSocket.getInputStream();
          OutputStream serverOut = serverSocket.getOutputStream()
      ) {
        // Forward client request to server
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = clientIn.read(buffer)) != -1) {
          serverOut.write(buffer, 0, bytesRead);
        }
        serverOut.flush();

        // Forward server response to client
        while ((bytesRead = serverIn.read(buffer)) != -1) {
          clientOut.write(buffer, 0, bytesRead);
        }
        clientOut.flush();
      } catch (IOException e) {
        System.err.println("Error handling request: " + e.getMessage());
      }
    } catch (IOException e) {
      System.err.println("Error handling socket: " + e.getMessage());
    } finally {
      try {
        clientSocket.close();
      } catch (IOException e) {
        System.err.println("Error closing client socket: " + e.getMessage());
      }
    }
  }

  public static void main(String[] args) {
    // Example configuration
    Map<String, TenantConfig> configs = new HashMap<>();
    configs.put("tenant1", new TenantConfig("example1.com", 80));
    configs.put("tenant2", new TenantConfig("example2.com", 80));

    ReverseProxy proxy = new ReverseProxy(8080, configs);
    proxy.run();
  }
}