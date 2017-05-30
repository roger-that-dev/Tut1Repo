package com.iou;

import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;

import java.util.Arrays;
import java.util.List;

/**
 * The state object recording IOU agreements between two parties.
 */
public class IOUState implements ContractState {
    private final Integer value;
    private final Party sender;
    private final Party recipient;
    private final IOUContract contract;

    /**
     * @param value     the amount of the IOU.
     * @param sender    the party issuing the IOU.
     * @param recipient the party receiving and approving the IOU.
     * @param contract  the contract which governs which transactions are valid for this state object.
     */
    public IOUState(Integer value,
                    Party sender,
                    Party recipient,
                    IOUContract contract) {
        this.value = value;
        this.sender = sender;
        this.recipient = recipient;
        this.contract = contract;
    }

    public Integer getValue() {
        return value;
    }

    public Party getSender() {
        return sender;
    }

    public Party getRecipient() {
        return recipient;
    }

    @Override
    public IOUContract getContract() {
        return contract;
    }

    /** The parties involved in this state. */
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(sender, recipient);
    }
}