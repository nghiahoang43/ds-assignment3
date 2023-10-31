package main;

import main.communication.Communication;
import main.member.Member;
import main.voteServer.VoteServer;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Main {
  public static void main(String[] args) {
    // Initialize the vote server
    VoteServer voteServer = new VoteServer();

    // Initialize communication
    Communication serverCommunication = new Communication(voteServer);
    voteServer.setCommunication(serverCommunication);

    // Create some members with unique IDs
    List<Member> members = new ArrayList<>();
    List<Integer> ports = new ArrayList<>();
    int basePort = 6000;
    for (int i = 1; i <= 9; i++) {
      String memberId = "" + i;
      Communication communication = new Communication(voteServer);
      Member member = new Member(memberId, communication, voteServer);
      int port = basePort + i;
      ports.add(port);
      member.getCommunication().startServer(port, member.getMemberId());
      members.add(member);
    }

    // Attach members to the vote server
    voteServer.setMembers(members, ports);

    // Start Paxos algorithm by sending Prepare request from one of the members
    members.get(0).sendPrepareRequest();
  }
}
