package main.communication;

import java.io.*;
import java.net.*;

import main.voteServer.MessageHandler;

public class Communication {
  private final MessageHandler messageHandler;
  private ServerSocket serverSocket;

  public Communication(MessageHandler messageHandler) {
    this.messageHandler = messageHandler;
  }

  public ServerSocket getServerSocket() {
    return this.serverSocket;
  }

  public void closeServerSocket() {
    if (serverSocket == null || serverSocket.isClosed()) {
      return;
    }
    try {
      serverSocket.close();
      System.out.println("Election done. Server socket closed.");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void startServer(int port, String memberId) {
    try {
      serverSocket = new ServerSocket(port);
      new Thread(new Runnable() {
        @Override
        public void run() {
          while (!serverSocket.isClosed()) {
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
    if (socket == null || socket.isClosed()) {
      return;
    }
    try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
      String message;
      while ((message = in.readLine()) != null) {
        messageHandler.handleMessage(message, memberId);
      }
    } catch (IOException e) {

    } finally {
      try {
        socket.close(); // Close the socket
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public synchronized void sendMessage(String memberId, String message) {
    try {
      Socket socket = new Socket("localhost", messageHandler.getPortMap().get(memberId));
      if (socket != null && !socket.isClosed()) { // Check if the socket exists and is open
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println(message);
      }
      socket.close();
    } catch (UnknownHostException e) {
      e.printStackTrace();
      System.out.println("Unknown host exception occurred while sending message.");
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println("IO exception occurred while sending message.");
    }
  }

}
