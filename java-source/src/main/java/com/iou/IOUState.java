package com.iou;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;

import java.util.List;

public class IOUState implements ContractState {
    private final int value;
    private final Party sender;
    private final Party recipient;
    private final IOUContract contract = new IOUContract();

    public IOUState(int value, Party sender, Party recipient) {
        this.value = value;
        this.sender = sender;
        this.recipient = recipient;
    }

    public int getValue() {
        return value;
    }

    public Party getSender() {
        return sender;
    }

    public Party getRecipient() {
        return recipient;
    }

    @Override
    // TODO: Once we've defined IOUContract, come back and update this.
    public IOUContract getContract() {
        return contract;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(sender, recipient);
    }
}