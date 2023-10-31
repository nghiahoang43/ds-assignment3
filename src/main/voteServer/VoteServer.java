package main.voteServer;

import java.util.*;
import java.io.IOException;
import java.net.*;
import java.util.concurrent.*;

import main.communication.Communication;
import main.member.AcceptedProposalPair;
import main.member.Member;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class VoteServer implements MessageHandler {
  private List<Member> members;
  private Map<String, Integer> promiseCount;
  private Map<String, Integer> acceptedCount;
  Communication communication;
  Map<String, Socket> socketMap = new HashMap<>();
  Map<String, Integer> portMap = new HashMap<>();
  private final Object lock = new Object();
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private final Map<String, ScheduledFuture<?>> timeouts = new ConcurrentHashMap<>();
  private String president = null;

  public VoteServer() {
    this.promiseCount = new HashMap<>();
    this.acceptedCount = new HashMap<>();
  }

  public void setMembers(List<Member> members, List<Integer> port) {
    this.members = members;
    for (int i = 0; i < members.size(); i++) {
      portMap.put(members.get(i).getMemberId(), port.get(i));
    }
  }

  public void setCommunication(Communication communication) {
    this.communication = communication;
  }

  @Override
  public Map<String, Integer> getPortMap() {
    return this.portMap;
  }

  @Override
  public void handleMessage(String message, String memberId) {
    if (president != null) {
      closeAllSockets();
      return;
    }
    String[] parts = message.split(" ");
    String messageType = parts[0].trim();
    if (messageType.equals("PREPARE")) {
      handlePrepareRequest(message, memberId);
    } else if (messageType.equals("PROMISE")) {
      handlePromise(message, memberId);
    } else if (messageType.equals("ACCEPT")) {
      handleAcceptRequest(message, memberId);
    } else if (messageType.equals("ACCEPTED")) {
      handleAccepted(message, memberId);
    } else {
      handleReject(message, memberId);
    }
  }

  public void broadcast(String message) {
    for (Member member : members) {
      communication.sendMessage(member.getMemberId(), message);
    }
  }

  @Override
  public Map<String, Socket> getSocketMap() {
    return this.socketMap;
  }

  public void addMember(String memberId, Socket socket) {
    socketMap.put(memberId, socket);
  }

  private void handlePrepareRequest(String message, String memberId) {
    String[] parts = message.split(" ");
    String proposalNumber = parts[1];
    String proposerId = parts[1].split(":")[0];
    LocalDateTime now = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    String formattedDateTime = now.format(formatter);
    System.out
        .println("[" + formattedDateTime + "]" + " Member " + memberId + " received prepareRequest from proposer "
            + proposerId
            + " with proposalNumber " + proposalNumber);
    Member currentMember = members.stream().filter(m -> m.getMemberId().equals(memberId)).findFirst().get();
    if (currentMember.getHighestSeenProposalNumber() == null
        || compareProposalNumbers(proposalNumber, currentMember.getHighestSeenProposalNumber())) {
      currentMember.setHighestSeenProposalNumber(proposalNumber);
      // Send a promise to accept this proposal number
      if (currentMember.getAcceptedProposalPair() == null) { // Or however you check for null
        currentMember.sendPromise(proposerId, proposalNumber, null);
      } else {
        currentMember.sendPromise(proposerId, proposalNumber, currentMember.getAcceptedProposalPair());
      }
    } else {
      // Send a reject message
      currentMember.sendReject(proposalNumber);
    }
    scheduleTimeoutForProposal(proposalNumber);
  }

  private void handlePromise(String message, String memberId) {
    synchronized (lock) {
      Member currentMember = members.stream().filter(m -> m.getMemberId().equals(memberId)).findFirst().get();
      LocalDateTime now = LocalDateTime.now();
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
      String formattedDateTime = now.format(formatter);
      System.out.println(
          "[" + formattedDateTime + "]" + " Proposer " + memberId + " received promise from member " + memberId);
      String[] parts = message.split(" ");
      String proposalNumber = parts[1];
      promiseCount.put(proposalNumber, promiseCount.getOrDefault(proposalNumber, 0) + 1);

      if (parts.length > 2) {
        AcceptedProposalPair newPair = new AcceptedProposalPair();
        newPair.setAcceptedProposalPair(parts[2], parts[3]);
        currentMember.setAcceptedProposalPair(parts[1], newPair);
      }

      if (promiseCount.get(proposalNumber) == 5) {
        now = LocalDateTime.now();
        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        formattedDateTime = now.format(formatter);
        System.out
            .println("[" + formattedDateTime + "]" + " Proposal " + proposalNumber + " IS PROMISED BY THE MAJORITY");

        cancelTimeout("proposal:" + proposalNumber);
        String proposalValue;
        if (currentMember.getAcceptedProposalPair() != null) {
          proposalValue = currentMember.getAcceptedProposalPair().getProposalValue();
        } else {
          proposalValue = null;
        }
        currentMember.sendAcceptRequest(proposalNumber, proposalValue, currentMember.getMemberId());
      }
    }
  }

  private void handleAcceptRequest(String message, String memberId) {
    Member currentMember = members.stream().filter(m -> m.getMemberId().equals(memberId)).findFirst().get();
    String[] parts = message.split(" ");
    String proposalNumber = parts[1];
    String proposerId = parts[1].split(":")[0];
    LocalDateTime now = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    String formattedDateTime = now.format(formatter);
    System.out.println(
        "[" + formattedDateTime + "]" + " Member " + memberId + " received acceptRequest from proposer " + proposerId
            + " with proposalNumber " + proposalNumber);
    String proposalValue = parts[2];
    if (currentMember.getHighestSeenProposalNumber() == null
        || compareProposalNumbers(proposalNumber,
            currentMember.getHighestSeenProposalNumber())) {
      currentMember.setHighestSeenProposalNumber(proposalNumber);
      // Send a promise to accept this proposal number
      currentMember.sendAccepted(proposerId, proposalNumber, proposalValue);
    } else {
      // Send a reject message
      currentMember.sendReject(proposalNumber);
    }
    scheduleTimeoutForAcceptRequest(proposalNumber);
  }

  private void handleAccepted(String message, String memberId) {
    synchronized (lock) {
      String[] parts = message.split(" ");
      String proposalNumber = parts[1];
      LocalDateTime now = LocalDateTime.now();
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
      String formattedDateTime = now.format(formatter);
      System.out.println(
          "[" + formattedDateTime + "]" + " Proposer " + memberId + " received accepted from member " + memberId);
      String proposerValue = parts[2];
      acceptedCount.put(proposalNumber, acceptedCount.getOrDefault(proposalNumber, 0) + 1);

      if (acceptedCount.get(proposalNumber) == 5) {
        now = LocalDateTime.now();
        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        formattedDateTime = now.format(formatter);
        System.out
            .println("[" + formattedDateTime + "]" + " Proposal " + proposalNumber + " IS ACCEPTED BY THE MAJORITY");
        cancelTimeout("accept:" + proposalNumber);
        now = LocalDateTime.now();
        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        formattedDateTime = now.format(formatter);
        System.out.println(
            "[" + formattedDateTime + "]" + " Member " + proposerValue + " is decided to become a president");
        president = proposerValue;
        closeAllSockets();
        communication.closeServerSocket();
      }
    }
  }

  private void scheduleTimeoutForProposal(String proposalNumber) {
    ScheduledFuture<?> timeout = scheduler.schedule(() -> {
      // cancel proposal, start a new one
      promiseCount.remove(proposalNumber);
    }, 20, TimeUnit.SECONDS);
    timeouts.put("proposal:" + proposalNumber, timeout);
  }

  private void scheduleTimeoutForAcceptRequest(String proposalNumber) {
    ScheduledFuture<?> timeout = scheduler.schedule(() -> {
      // cancel accept request, start a new one
      acceptedCount.remove(proposalNumber);
    }, 20, TimeUnit.SECONDS);
    timeouts.put("accept:" + proposalNumber, timeout);
  }

  private void cancelTimeout(String key) {
    ScheduledFuture<?> timeout = timeouts.remove(key);
    if (timeout != null) {
      timeout.cancel(false);
    }
  }

  public boolean compareProposalNumbers(String proposalNumber1, String proposalNumber2) {
    if (proposalNumber1 == null) {
      return false;
    }
    if (proposalNumber2 == null) {
      return true;
    }

    if (proposalNumber1.equals(proposalNumber2)) {
      return true;
    }

    String[] parts1 = proposalNumber1.split(":");
    String[] parts2 = proposalNumber2.split(":");

    int count1 = Integer.parseInt(parts1[1]);
    int count2 = Integer.parseInt(parts2[1]);

    if (count1 < count2) {
      return false;
    } else if (count1 > count2) {
      return true;
    } else {
      return Integer.parseInt(parts1[0]) >= Integer.parseInt(parts2[0]);
    }
  }

  private void handleReject(String message, String memberId) {
    String[] parts = message.split(" ");
    String proposalNumber = parts[1];
    LocalDateTime now = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    String formattedDateTime = now.format(formatter);
    System.out
        .println("[" + formattedDateTime + "]" + " Proposal " + proposalNumber + " is rejected by member " + memberId);
  }

  public void closeAllSockets() {
    synchronized (lock) {
      for (Socket socket : socketMap.values()) {
        try {
          if (socket != null && !socket.isClosed()) {
            socket.close();
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    socketMap.clear();
  }

  public String getPresident() {
    return president;
  }

}
