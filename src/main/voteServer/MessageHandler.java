package main.voteServer;

import java.net.Socket;
import java.util.Map;

public interface MessageHandler {
  void handleMessage(String message, String memberId);

  public Map<String, Socket> getSocketMap();
  public Map<String, Integer> getPortMap();
}
