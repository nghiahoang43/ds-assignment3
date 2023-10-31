# ASSIGNMENT 3 - Adelaide Suburbs Council Election - Paxos Voting Protocol Implementation
## Nghia Hoang - a1814303
## Assignment Description
This year, the Adelaide Suburbs Council is holding elections for the council president. Any of the nine members can become the council president. Members have varied responsiveness and preferences:

- `Member M1`: Extremely responsive and has been in the council president's race for a long time.

- `Member M2`: Desires the presidency but has a very slow response time due to their remote location.

- `Member M3`: Also vies for the presidency, but sometimes goes offline because of camping trips.

- `Members M4-M9`: Have no particular ambition for presidency and will vote fairly.

On the election day, one councillor will send a proposal for a president, and a majority is required to elect someone.

The task is to implement a Paxos voting protocol that is fault-tolerant and can handle various communication delays and failures. Communication is recommended to be done via sockets.

## Folder Structure
The project is structured in a modular fashion to maintain clarity and manageability. Here is the breakdown of the folder structure:

```bash
ds-assignment3
│
├── communication
│
├── member
│   ├── AcceptedProposalPair.java
│   └── Member.java
│
├── voteServer
│   ├── MessageHandler.java
│   ├── VoteServer.java
│   └── Main.class
│
├── test
│   ├── MemberTest.java
│   ├── PaxosTest.java
│   └── VoteServerTest.java
│
├── lib
│   ├── Various .jar files (e.g., junit, gson, mockito, etc.)
│
├── src
│   ├── main
│   │   ├── communication
│   │   ├── member
│   │   └── voteServer
│   └── test
│
├── .gitignore
│
├── makefile
│
└── README.md

```

## The Paxos Protocol

Paxos is a consensus algorithm designed to achieve agreement within distributed systems, even in the face of partial system failures. The protocol operates in distinct phases, ensuring that nodes in a distributed system agree upon a single piece of data or value.

### High-Level Overview

1. Proposal Initiation: A proposer generates a unique proposal ID and checks with the acceptors if anyone has already seen a proposal with a higher ID. If not, the proposer proposes a value.

2. Reaching Consensus: If a majority of acceptors promise to accept the proposer's proposal, consensus can be achieved.

### Detailed Breakdown

### Phase 1: Proposer (PREPARE) & Acceptor (PROMISE)

- The proposer creates a unique, ever-incrementing identification number (ID) for its proposal. This ID is sent to the acceptors in a `PREPARE` message.
- Acceptors, upon receiving the `PREPARE` message, compare the received ID with any they've previously seen.
If the received ID is less than or equal to a previously seen ID, the acceptor may either ignore the message or respond with a `REJECT` message.
  - If the ID is the highest the acceptor has seen, they promise not to accept proposals with smaller IDs. If they've already accepted a proposal in the past, they respond with a `PROMISE` message that includes that proposal's ID and value. Otherwise, they simply send back a `PROMISE` with the received ID.

### Phase 2a: Proposer (PROPOSE)

- Upon receiving `PROMISE` responses from a majority of acceptors, the proposer examines the responses:
  - If any of the acceptors sent back an accepted value, the proposer is obligated to use this value in its proposal.
  - Otherwise, the proposer can choose any value it sees fit.
- The proposer then sends out a `ACCEPT` message to the acceptors with its chosen value and ID.

### Phase 2b: Acceptor (ACCEPT)

- Acceptors decide on the proposal based on its ID:
  - If the ID is the largest they've seen, they accept the proposal, store its details, and notify all learners of the accepted value with an `ACCEPTED` message.
  - If not, they can choose to ignore or respond with a "fail" message.
The protocol achieves consensus when a majority of acceptors accept the same proposal. It's vital to note that the consensus is on the proposed value, not on the proposal ID.

In summary, the Paxos algorithm first ensures that proposals are unique and then gets the distributed system to agree on a particular proposal's value. Once this consensus is reached, the value can be safely acted upon or stored in the system.

## Member Class
The Member class represents a node in a distributed consensus protocol, likely inspired by the Paxos algorithm.

### Features

1. Identity & Communication: Each Member is uniquely identified by a memberId and utilizes the communication mechanism to interact with other nodes.

2. Proposal Management: Each member can generate and manage proposal numbers and keeps a record of the highest proposal it has observed.

3. Voting Mechanics: With the help of an associated VoteServer, a member can handle and store accepted proposals.

4. Delays & Latencies: The class can introduce operational delays, influenced by the member's identifier. This can be used to mimic real-world network latencies or for testing scenarios.

5. Handling Offline States: Members can be set to offline modes either forcibly or randomly, representing scenarios like node failures or network disconnections.

6. Consensus Communication: The member manages several consensus-related messages:
    - Prepare: Initiates a proposal.
    - Promise: Responds to prepare requests, possibly with previously accepted proposal details.
    - Accept: Solicits acceptance of a proposal from other nodes.
    - Reject: Declines a proposal.
    - Accepted: Confirms a proposal's acceptance.
    - Result: Announces the final consensus value to all nodes.

## VoteServer Class
The VoteServer class is responsible for handling the voting operations in a distributed system.

### Features

1. Consensus Mechanism: Uses the Paxos consensus algorithm to handle and process voting messages.
2. Message Handling: Processes various message types, including PREPARE, `PROMISE`, `ACCEPT`, and `ACCEPTED`.
3. Member Management: Manages a list of Member objects, each representing a participant in the voting process.
4. Timeout Management: Utilizes scheduled tasks to handle timeouts for proposals and accept requests.
5. President Election: Determines the "president" or the leader based on a majority consensus.

## Testing
### PaxosTest
1. `testConcurrentVotingProposals`:
This test simulates a scenario where two members concurrently send prepare requests to become the president. The `VoteServer` and a list of `Member` instances are created, and ports are assigned to each member. Two separate threads (`member1Thread` and `member2Thread`) are initiated, each representing a member sending a prepare request. After both threads complete execution, the test asserts that the president selected is "1". This test demonstrates the Paxos algorithm's ability to handle concurrent proposals and resolve them correctly.

2. `testImmediateResponse`
The test checks how the Paxos algorithm handles immediate responses from members when selecting a president. Similar to the previous test, a VoteServer is set up along with members. Here, three threads (`member1Thread`, `member2Thread`, and `member3Thread`) are used, each starting a prepare request from different members. The test asserts that member "3" is elected as president, verifying that the system can manage immediate and multiple responses in the election process.

3. `testM2orM3GoOffline`
This test investigates the algorithm's behavior when one of the members (member 3 in this case) goes offline during the election process. The setup is similar to previous tests but includes a call to `forceOffline()` for member 3. Despite member 3 initiating a prepare request, the test validates that member "1" becomes the president. This scenario tests the resilience of the Paxos implementation in handling member failures or network issues.

### VoteServerTest
the `VoteServerTest` includes various tests to validate the functionalities of the VoteServer class. These tests cover member management (`testSetMembers`, `testAddMember`), communication setup (`testSetCommunication`), broadcasting messages to all members (`testBroadcast`), and comparing proposal numbers (`testCompareProposalNumbers`). Additional tests ensure correct initial conditions like testGetPresidentInitiallyNull, and message handling (`testHandleMessagePrepare`, `testHandleMessagePromise`). The suite ensures the robustness and correctness of the `VoteServer` class's responsibilities in the Paxos algorithm.

### MemberTest
The `MemberTest` class focuses on testing the `Member` class functionalities. It includes tests for basic operations like retrieving a member ID (`testGetMemberId`), generating proposal numbers (`testGenerateProposalNumber`), and setting/getting the highest seen proposal number (`testSetAndGetHighestSeenProposalNumber`). It also tests critical functionalities related to the Paxos protocol, such as sending prepare requests (`testSendPrepareRequest`), managing response delays (`testDelayTimeInitialization`), and handling member states (online/offline) in sending responses (`testSendRejectWhenOffline`, `testSendPromiseWhenOnline`). Moreover, the ability of members to set and respect proposal pairs is checked (`testSetAcceptedProposalPair`, `testSetAcceptedProposalPairWithLowerProposalNumber`), along with the customization of delay times (`testSetDelayTime`). These tests collectively ensure the Member class accurately represents and behaves as a Paxos protocol participant.

## Run
### Compiling
To compile the main application, run:

```bash
make
```
This will compile all necessary .java files in the src/main directory into the bin directory.

### Running Tests
To compile and run the tests:

```bash
make test
```
This will compile both the main and test classes and then run the tests using JUnit.

### Note:
Please note that running `PaxosTest` can take up to approximately 6 minutes to complete. This duration is necessary due to the time required for the Paxos algorithm to execute within the test.

### Cleaning up

After running tests, or when you want to start fresh, you can clean up the compiled `.class` files by running:

```bash
make clean
```
This command will search for all `.class` files and remove them, ensuring a clean environment.