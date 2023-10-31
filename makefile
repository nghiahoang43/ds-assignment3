JAVA = java
JAVAC = javac
CPTEST = -cp lib/*:bin/
MAIN_SOURCES = src/main/communication/Communication.java \
          src/main/member/Member.java \
          src/main/member/AcceptedProposalPair.java \
          src/main/voteServer/MessageHandler.java \
          src/main/voteServer/VoteServer.java \
          src/main/Main.java

TEST_SOURCES = src/test/PaxosTest.java \
					src/test/MemberTest.java \
					src/test/VoteServerTest.java

# TEST_SOURCES = src/test/MemberTest.java

TEST_MAIN_CLASS = org.junit.platform.console.ConsoleLauncher

all: compile

compile:
	javac $(CPTEST) $(MAIN_SOURCES) -d bin

compile-test: compile
	@$(JAVAC) $(CPTEST) $(MAIN_SOURCES) $(TEST_SOURCES) -d bin

test: compile-test
	@$(JAVA) $(CPTEST) $(TEST_MAIN_CLASS) --scan-classpath

run: compile
	java -cp bin main.Main

clean:
	rm -rf bin/*
	rm -rf src/main/*/*.class
	rm -rf src/main/*.class
	rm -rf src/test/*.class
