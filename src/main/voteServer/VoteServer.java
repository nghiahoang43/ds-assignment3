package main.voteServer;

import java.util.*;
import java.net.*;
import java.util.concurrent.*;

import main.communication.Communication;
import main.member.AcceptedProposalPair;
import main.member.Member;

public class VoteServer implements MessageHandler {
  private List<Member> members;
  private Map<String, Integer> promiseCount;
  private Map<String, Integer> acceptedCount;
  Communication communication;
  Map<String, Socket> socketMap = new HashMap<>();
  Map<String, Integer> portMap = new HashMap<>();
  private Member currentMember;
  private final Object lock = new Object();
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private final Map<String, ScheduledFuture<?>> timeouts = new ConcurrentHashMap<>();

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
  public void setCurrentMember(String memberId) {
    this.currentMember = members.stream().filter(member -> member.getMemberId().equals(memberId)).findFirst().get();
  }

  @Override
  public Map<String, Integer> getPortMap() {
    return this.portMap;
  }

  @Override
  public void handleMessage(String message) {
    String[] parts = message.split(" ");
    String messageType = parts[0];
    if (messageType.startsWith("PREPARE")) {
      handlePrepareRequest(message);
    } else if (messageType.startsWith("PROMISE")) {
      handlePromise(message);
      // Parse message and send promise
    } else if (messageType.startsWith("ACCEPT")) {
      handleAcceptRequest(message);
    } else if (messageType.startsWith("ACCEPTED")) {
      handleAccepted(message);
    } else {
      System.out.println("Received message: " + message);
    }
  }

  public void broadcast(String message) {
    for (Member member : members) {
      communication.sendMessage(socketMap, member.getMemberId(), message);
    }
  }

  @Override
  public Map<String, Socket> getSocketMap() {
    return this.socketMap;
  }

  public void addMember(String memberId, Socket socket) {
    socketMap.put(memberId, socket);
  }

  private void handlePrepareRequest(String message) {
    // System.out.println("message: " + message);
    String[] parts = message.split(" ");
    String proposalNumber = parts[1];
    String proposerId = parts[1].split(":")[0];
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

  private void handlePromise(String message) {
    synchronized (lock) {
      String[] parts = message.split(" ");
      String proposalNumber = parts[1]; // Corrected string splitting
      promiseCount.put(proposalNumber, promiseCount.getOrDefault(proposalNumber, 0) + 1);
      System.out.println("promiseCount: " + promiseCount.get(proposalNumber));
      if (parts.length > 2) {
        AcceptedProposalPair newPair = new AcceptedProposalPair();
        newPair.setAcceptedProposalPair(parts[2], parts[3]);
        currentMember.setAcceptedProposalPair(parts[1], newPair);
      }
      if (promiseCount.get(proposalNumber) > members.size() / 2) {
        cancelTimeout("proposal:" + proposalNumber);
        String proposalValue = currentMember.getAcceptedProposalPair() != null
            ? currentMember.getAcceptedProposalPair().getProposalValue()
            : null;
        if (currentMember.getAcceptedProposalPair() != null) {
          proposalValue = currentMember.getAcceptedProposalPair().getProposalValue();
        } else {
          proposalValue = currentMember.getMemberProposalValue();
        }
        currentMember.sendAcceptRequest(proposalNumber, proposalValue);
      }
    }
  }

  private void handleAcceptRequest(String message) {
    String[] parts = message.split(" ");
    String proposalNumber = parts[1].split("\n")[0];
    String proposalValue = parts[2];

    if (currentMember.getHighestSeenProposalNumber() == null
        || compareProposalNumbers(proposalNumber, currentMember.getHighestSeenProposalNumber())) {
      currentMember.setHighestSeenProposalNumber(proposalNumber);
      currentMember.sendAccepted(proposalNumber, proposalValue);
    }
    scheduleTimeoutForAcceptRequest(proposalNumber);
  }

  private void handleAccepted(String message) {
    synchronized (lock) {
      String[] parts = message.split(" ");
      String proposalNumber = parts[1].split("\n")[0]; // Corrected string splitting
      System.out.println("acceptedCount: " + acceptedCount.get(proposalNumber));
      acceptedCount.put(proposalNumber, acceptedCount.getOrDefault(proposalNumber, 0) + 1);

      if (acceptedCount.get(proposalNumber) > members.size() / 2) {
        cancelTimeout("accept:" + proposalNumber);
        currentMember.sendResult("Proposal " + proposalNumber + " is accepted by the majority");
      }
    }
  }

  private void scheduleTimeoutForProposal(String proposalNumber) {
    ScheduledFuture<?> timeout = scheduler.schedule(() -> {
      // cancel proposal, start a new one
      promiseCount.remove(proposalNumber);
      // currentMember.sendPrepareRequest();
    }, 10, TimeUnit.SECONDS);
    timeouts.put("proposal:" + proposalNumber, timeout);
  }

  private void scheduleTimeoutForAcceptRequest(String proposalNumber) {
    ScheduledFuture<?> timeout = scheduler.schedule(() -> {
      // cancel accept request, start a new one
      acceptedCount.remove(proposalNumber);
      // currentMember.sendPrepareRequest();
    }, 10, TimeUnit.SECONDS);
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
    String[] parts1 = proposalNumber1.split(":");
    String[] parts2 = proposalNumber2.split(":");

    int count1 = Integer.parseInt(parts1[1]);
    int count2 = Integer.parseInt(parts2[1]);

    if (count1 < count2) {
      return false;
    } else if (count1 > count2) {
      return true;
    } else {
      return Integer.parseInt(parts1[0]) > Integer.parseInt(parts2[0]);
    }
  }

}
