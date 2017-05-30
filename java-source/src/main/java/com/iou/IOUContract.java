package com.iou;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.AuthenticatedObject;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.TransactionForContract;
import net.corda.core.crypto.SecureHash;

import java.security.PublicKey;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

/**
 * A basic smart contract that enforces rules regarding the issuance of [IOUState].
 *
 * A transaction issuing a new [IOUState] onto the ledger must have:
 * - Zero input states
 * - One output state: the new [IOUState]
 * - A Create() command with the public keys of both the sender and the recipient
 */
public class IOUContract implements Contract {
    /**
     * If verify() doesn't throw an exception, the transaction is accepted by the contract.
     */
    @Override
    public void verify(TransactionForContract tx) {
        final AuthenticatedObject<Create> command = requireSingleCommand(tx.getCommands(), Create.class);

        requireThat(check -> {
            // Constraints on the shape of the transaction.
            check.using("No inputs should be consumed when issuing an IOU.", tx.getInputs().isEmpty());
            check.using("Only one output state should be created.", tx.getOutputs().size() == 1);
            final IOUState out = (IOUState) tx.getOutputs().get(0);
            check.using("The sender and the recipient cannot be the same entity.", out.getSender() != out.getRecipient());

            // Constraints on the signers.
            final List<PublicKey> requiredSigners = ImmutableList.of(
                    out.getSender().getOwningKey(),
                    out.getRecipient().getOwningKey());
            check.using("All of the participants must be signers.", command.getSigners().containsAll(requiredSigners));

            // IOU-specific constraints.
            check.using("The IOU's value must be non-negative.",out.getValue() > 0);

            return null;
        });
    }

    /** This contract only implements one command, Create. */
    public static class Create implements CommandData {}

    /** This is a reference to the underlying legal contract template and associated parameters. */
    private final SecureHash legalContractReference = SecureHash.sha256("<Legal prose of the contract.>");

    @Override
    public final SecureHash getLegalContractReference() {
        return legalContractReference;
    }
}