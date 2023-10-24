package main.member;

public class AcceptedProposalPair {
  private String proposalNumber;
  private String proposalValue;

  public AcceptedProposalPair() {
    this.proposalNumber = null;
    this.proposalValue = null;
  }

  public AcceptedProposalPair getAcceptedProposalPair() {
    return this;
  }

  public void setAcceptedProposalPair(String proposalNumber, String proposalValue) {
    this.proposalNumber = proposalNumber;
    this.proposalValue = proposalValue;
  }

  public String getProposalNumber() {
    return this.proposalNumber;
  }

  public String getProposalValue() {
    return this.proposalValue;
  }

  public boolean isNull() {
    return this.proposalNumber != null && this.proposalValue != null;
  }
}
