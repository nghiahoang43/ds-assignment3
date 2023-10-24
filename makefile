all:
	javac src/main/communication/Communication.java src/main/member/Member.java src/main/member/AcceptedProposalPair.java src/main/voteServer/MessageHandler.java src/main/voteServer/VoteServer.java src/main/Main.java -d bin

run:
	make all
	java -cp bin main.Main

clean:
	rm -rf bin/*