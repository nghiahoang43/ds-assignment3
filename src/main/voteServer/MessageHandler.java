package main.voteServer;

import java.net.Socket;
import java.util.Map;

import main.member.Member;

public interface MessageHandler {
  void setCurrentMember(String memberId);

  void handleMessage(String message);

  public Map<String, Socket> getSocketMap();
  public Map<String, Integer> getPortMap();
}
