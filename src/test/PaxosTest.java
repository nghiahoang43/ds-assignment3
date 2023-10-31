package test;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import main.communication.Communication;
import main.member.Member;
import main.voteServer.VoteServer;

import java.util.ArrayList;
import java.util.List;

public class PaxosTest {
  private List<Member> members;
  private VoteServer voteServer;
  private List<Integer> ports;
  Communication serverCommunication;

  @Test
  public void testConcurrentVotingProposals() throws InterruptedException {
    voteServer = new VoteServer();

    serverCommunication = new Communication(voteServer);
    voteServer.setCommunication(serverCommunication);

    members = new ArrayList<>();
    ports = new ArrayList<>();
    int basePort = 9000;
    for (int i = 1; i <= 9; i++) {
      String memberId = "" + i;
      Communication communication = new Communication(voteServer);
      Member member = new Member(memberId, communication, voteServer);
      int port = basePort + i;
      ports.add(port);
      member.getCommunication().startServer(port, member.getMemberId());
      members.add(member);
    }

    voteServer.setMembers(members, ports);
    Thread member1Thread = new Thread(() -> members.get(0).sendPrepareRequest());
    Thread member2Thread = new Thread(() -> members.get(1).sendPrepareRequest());

    member1Thread.start();
    member2Thread.start();

    member1Thread.join();
    member2Thread.join();

    // No delay time for all members

    // sleep for 30 seconds
    Thread.sleep(30000);

    assertEquals("1", voteServer.getPresident(), "Concurrent proposal resolution failed");
  }

  @Test
  public void testImmediateResponse() throws InterruptedException {
    voteServer = new VoteServer();

    serverCommunication = new Communication(voteServer);
    voteServer.setCommunication(serverCommunication);

    members = new ArrayList<>();
    ports = new ArrayList<>();
    int basePort = 8000;
    for (int i = 1; i <= 9; i++) {
      String memberId = "" + i;
      Communication communication = new Communication(voteServer);
      Member member = new Member(memberId, communication, voteServer);
      int port = basePort + i;
      ports.add(port);
      member.getCommunication().startServer(port, member.getMemberId());
      members.add(member);
    }

    voteServer.setMembers(members, ports);

    // No delay time for all members
    for (Member member : members) {
      member.setDelayTime(0);
    }

    Thread member1Thread = new Thread(() -> members.get(0).sendPrepareRequest());
    Thread member2Thread = new Thread(() -> members.get(1).sendPrepareRequest());
    Thread member3Thread = new Thread(() -> members.get(2).sendPrepareRequest());

    member1Thread.start();
    member2Thread.start();
    member3Thread.start();

    member1Thread.join();
    member2Thread.join();
    member3Thread.join();

    // sleep for 30 seconds
    Thread.sleep(60000);

    assertEquals("3", voteServer.getPresident(), "Immediate response failed");
  }

  @Test
  public void testM2orM3GoOffline() throws InterruptedException {
    voteServer = new VoteServer();

    serverCommunication = new Communication(voteServer);
    voteServer.setCommunication(serverCommunication);

    members = new ArrayList<>();
    ports = new ArrayList<>();
    int basePort = 2000;
    for (int i = 1; i <= 9; i++) {
      String memberId = "" + i;
      Communication communication = new Communication(voteServer);
      Member member = new Member(memberId, communication, voteServer);
      int port = basePort + i;
      ports.add(port);
      member.getCommunication().startServer(port, member.getMemberId());
      members.add(member);
    }

    voteServer.setMembers(members, ports);

    // No delay time for all members

    Thread member1Thread = new Thread(() -> members.get(0).sendPrepareRequest());
    Thread member2Thread = new Thread(() -> members.get(1).sendPrepareRequest());
    Thread member3Thread = new Thread(() -> members.get(2).sendPrepareRequest());
    members.get(2).forceOffline();

    member1Thread.start();
    member2Thread.start();
    member3Thread.start();

    member1Thread.join();
    member2Thread.join();
    member3Thread.join();

    // sleep for 90 seconds
    Thread.sleep(60000);
    if (voteServer.getPresident() == null) {
      member1Thread = new Thread(() -> members.get(0).sendPrepareRequest());
      member1Thread.start();
      member1Thread.join();
      Thread.sleep(60000);
    }
    assertEquals("1", voteServer.getPresident(), "Immediate response failed");
  }

}
