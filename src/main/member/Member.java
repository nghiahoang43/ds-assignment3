package main.member;

import java.net.Socket;
import java.util.Map;

import main.communication.Communication;
import main.voteServer.VoteServer;

public class Member {
  private final String memberId;
  private final Communication communication;
  private String highestSeenProposalNumber = null;
  private int count = 0;
  private AcceptedProposalPair acceptedProposalPair;
  private final VoteServer voteServer;
  private final String memberProposalValue;

  public Member(String memberId, Communication communication, VoteServer voteServer) {
    this.memberId = memberId;
    this.communication = communication;
    this.voteServer = voteServer; // Initialize voteServer here
    this.memberProposalValue = "Value of member " + memberId;
  }

  public String getMemberId() {
    return this.memberId;
  }

  public Communication getCommunication() {
    return this.communication;
  }

  public String generateProposalNumber() {
    count++;
    return memberId + ":" + count;
  }

  public void sendPrepareRequest() {
    System.out.println("Sending prepare request from member " + memberId);
    // Initialize proposal number of the prepare request
    String proposalNumber = generateProposalNumber();
    // Send prepare request to all members
    voteServer.broadcast("PREPARE " + proposalNumber);
  }

  public void setHighestSeenProposalNumber(String proposalNumber) {
    this.highestSeenProposalNumber = proposalNumber;
  }

  public String getHighestSeenProposalNumber() {
    return this.highestSeenProposalNumber;
  }

  public String getMemberProposalValue() {
    return this.memberProposalValue;
  }

  public AcceptedProposalPair getAcceptedProposalPair() {
    return this.acceptedProposalPair;
  }

  public void setAcceptedProposalPair(String currentProposalNumber, AcceptedProposalPair acceptedProposalPair) {
    if (this.acceptedProposalPair == null) {
      this.acceptedProposalPair = acceptedProposalPair;
    } else {
      if (voteServer.compareProposalNumbers(acceptedProposalPair.getProposalNumber(), currentProposalNumber)) {
        this.acceptedProposalPair = acceptedProposalPair;
      }
    }
  }

  public void sendPromise(String proposerId, String proposalNumber, AcceptedProposalPair acceptedProposalPair) {
    Map<String, Socket> socketMap = voteServer.getSocketMap();
    System.out.println(memberId + " send promise to " + proposerId);
    if (acceptedProposalPair == null) {
      communication.sendMessage(socketMap, proposerId, "PROMISE " + proposalNumber);
    } else {
      communication.sendMessage(socketMap, proposerId,
          "PROMISE " + proposalNumber + " " + acceptedProposalPair.getProposalNumber() + " "
              + acceptedProposalPair.getProposalValue());
    }
  }

  public void sendAcceptRequest(String proposalNumber, String proposalValue) {
    if (proposalValue == null) {
      proposalValue = this.memberProposalValue;
    }
    voteServer.broadcast("ACCEPT " + proposalNumber + " " + proposalValue);
  }

  public void sendReject(String proposalNumber) {
    Map<String, Socket> socketMap = voteServer.getSocketMap();
    communication.sendMessage(socketMap, memberId, "REJECT " + proposalNumber);
  }

  public void sendAccepted(String proposalNumber, String proposalValue) {
    voteServer.broadcast("ACCEPTED " + proposalNumber + " " + proposalValue);
  }

  public void sendResult(String result) {
    voteServer.broadcast("RESULT " + result); // Broadcasting result to all members
  }
}
