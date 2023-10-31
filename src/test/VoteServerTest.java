package test;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import main.communication.Communication;
import main.member.Member;
import main.voteServer.VoteServer;

class VoteServerTest {

  @Test
  void testSetMembers() {
    VoteServer server = new VoteServer();
    List<Member> members = new ArrayList<>();
    List<Integer> ports = new ArrayList<>();
    members.add(new Member("1", mock(Communication.class), server));
    members.add(new Member("2", mock(Communication.class), server));
    ports.add(4567);
    ports.add(4568);

    server.setMembers(members, ports);

    Map<String, Integer> portMap = server.getPortMap();
    assertEquals(2, portMap.size());
    assertEquals(4567, portMap.get("1"));
    assertEquals(4568, portMap.get("2"));
  }

  @Test
  void testAddMember() {
    VoteServer server = new VoteServer();
    Socket mockSocket = mock(Socket.class);

    server.addMember("123", mockSocket);

    Map<String, Socket> socketMap = server.getSocketMap();
    assertEquals(1, socketMap.size());
    assertEquals(mockSocket, socketMap.get("123"));
  }

  @Test
  void testSetCommunication() {
    VoteServer server = new VoteServer();
    Communication mockCommunication = mock(Communication.class);
    server.setCommunication(mockCommunication);

  }

  @Test
  void testBroadcast() {
    VoteServer server = new VoteServer();
    Communication mockCommunication = mock(Communication.class);
    server.setCommunication(mockCommunication);

    Member member1 = mock(Member.class);
    when(member1.getMemberId()).thenReturn("1");

    Member member2 = mock(Member.class);
    when(member2.getMemberId()).thenReturn("2");

    server.setMembers(Arrays.asList(member1, member2), Arrays.asList(8080, 8081));
    server.broadcast("testMessage");

    verify(mockCommunication, times(1)).sendMessage("1", "testMessage");
    verify(mockCommunication, times(1)).sendMessage("2", "testMessage");
  }

  @Test
  void testCompareProposalNumbers() {
    VoteServer server = new VoteServer();
    assertTrue(server.compareProposalNumbers("2:1", "1:1"));
    assertFalse(server.compareProposalNumbers("1:1", "2:1"));
  }

  @Test
    void testGetPresidentInitiallyNull() {
        VoteServer server = new VoteServer();
        assertNull(server.getPresident(), "Initially, president should be null");
    }

    @Test
    void testHandleMessagePrepare() {
        VoteServer server = new VoteServer();
        Communication mockCommunication = mock(Communication.class);
        server.setCommunication(mockCommunication);
        Member member = new Member("1", mockCommunication, server);
        List<Member> members = Arrays.asList(member);
        server.setMembers(members, Arrays.asList(8080));

        server.handleMessage("PREPARE 1:1", "1");
    }

    @Test
    void testHandleMessagePromise() {
        VoteServer server = new VoteServer();
        Communication mockCommunication = mock(Communication.class);
        server.setCommunication(mockCommunication);
        Member member = new Member("1", mockCommunication, server);
        List<Member> members = Arrays.asList(member);
        server.setMembers(members, Arrays.asList(8080));

        server.handleMessage("PROMISE 1:1", "1");

    }


    @Test
    void testCloseAllSockets() throws IOException {
        VoteServer server = new VoteServer();
        Socket socket1 = mock(Socket.class);
        Socket socket2 = mock(Socket.class);
        server.addMember("1", socket1);
        server.addMember("2", socket2);

        server.closeAllSockets();

        verify(socket1, times(1)).close();
        verify(socket2, times(1)).close();
    }
  
}