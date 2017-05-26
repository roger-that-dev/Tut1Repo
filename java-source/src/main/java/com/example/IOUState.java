package com.example;

import com.example.IOUContract;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static net.corda.core.crypto.CryptoUtils.getKeys;

/**
 * The state object recording IOU agreements between two parties.
 *
 * A state must implement [ContractState] or one of its descendants.
 */
public class IOUState implements LinearState {
    private final Integer value;
    private final Party sender;
    private final Party recipient;
    private final IOUContract contract;
    private final UniqueIdentifier linearId;

    /**
     * @param value details of the IOU.
     * @param sender the party issuing the IOU.
     * @param recipient the party receiving and approving the IOU.
     * @param contract the contract which governs which transactions are valid for this state object.
     */
    public IOUState(Integer value,
                    Party sender,
                    Party recipient,
                    IOUContract contract)
    {
        this.value = value;
        this.sender = sender;
        this.recipient = recipient;
        this.contract = contract;
        this.linearId = new UniqueIdentifier();
    }

    public Integer getValue() { return value; }
    public Party getSender() { return sender; }
    public Party getRecipient() { return recipient; }
    @Override public IOUContract getContract() { return contract; }
    @Override public UniqueIdentifier getLinearId() { return linearId; }
    @Override public List<AbstractParty> getParticipants() {
        return Arrays.asList(sender, recipient);
    }

    /**
     * This returns true if the state should be tracked by the vault of a particular node. In this case the logic is
     * simple; track this state if we are one of the involved parties.
     */
    @Override public boolean isRelevant(Set<? extends PublicKey> ourKeys) {
        return ourKeys.contains(sender.getOwningKey()) || ourKeys.contains(recipient.getOwningKey());
    }
}