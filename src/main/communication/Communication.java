package main.communication;

import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import main.voteServer.MessageHandler;

public class Communication {
  private final MessageHandler messageHandler;
  private ServerSocket serverSocket;

  public Communication(MessageHandler messageHandler) {
    this.messageHandler = messageHandler;
  }

  ServerSocket getServerSocket() {
    return this.serverSocket;
  }

  public void startServer(int port, String memberId) {
    try {
      serverSocket = new ServerSocket(port);
      System.out.println("Server started on port " + port);
      new Thread(new Runnable() {
        @Override
        public void run() {
          while (true) {
            try {
              Socket clientSocket = serverSocket.accept();

              addMember(memberId, clientSocket);
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        }
      }).start();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void addMember(String memberId, Socket socket) {
    messageHandler.getSocketMap().put(memberId, socket);
    new Thread(() -> handleSocket(memberId, socket)).start();
  }

  private void handleSocket(String memberId, Socket socket) {
    try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
      String message;
      while ((message = in.readLine()) != null) {
        messageHandler.setCurrentMember(memberId);
        messageHandler.handleMessage(message);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        socket.close(); // Close the socket
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void sendMessage(Map<String, Socket> socketMap, String memberId, String message) {
    // Surround the socket creation and message sending code with try-catch to
    // handle exceptions
    try {
      Socket socket = new Socket("localhost", messageHandler.getPortMap().get(memberId));

      if (socket != null && !socket.isClosed()) { // Check if the socket exists and is open
        try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
          out.println(message);
        } // This will automatically close the PrintWriter
      }
    } catch (UnknownHostException e) {
      e.printStackTrace();
      System.out.println("Unknown host exception occurred while sending message.");
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println("IO exception occurred while sending message.");
    }
  }

}
