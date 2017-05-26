package com.example;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.AuthenticatedObject;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.TransactionForContract;
import net.corda.core.crypto.SecureHash;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

/**
 * A basic smart contract that enforces rules regarding the issuance of [IOUState].
 *
 * A transaction issuing a new [IOUState] onto the ledger must have:
 * - Zero input states.
 * - One output state: the new [IOUState].
 * - A Create() command with the public keys of both the sender and the recipient.
 */
public class IOUContract implements Contract {
    /** If verify() doesn't throw an exception, the contract accepts the transaction. */
    @Override
    public void verify(TransactionForContract tx) {
        final AuthenticatedObject<Commands> command = requireSingleCommand(tx.getCommands(), Commands.class);
        requireThat(require -> {
            // Generic constraints around the IOU transaction.
            require.using("No inputs should be consumed when issuing an IOU.",
                    tx.getInputs().isEmpty());
            require.using("Only one output state should be created.",
                    tx.getOutputs().size() == 1);
            final IOUState out = (IOUState) tx.getOutputs().get(0);
            require.using("The sender and the recipient cannot be the same entity.",
                    out.getSender() != out.getRecipient());
            require.using("All of the participants must be signers.",
                    command.getSigners().containsAll(ImmutableList.of(
                            out.getSender().getOwningKey(),
                            out.getRecipient().getOwningKey())));
            // IOU-specific constraints.
            require.using("The IOU's value must be non-negative.",
                    out.getValue() > 0);

            return null;
        });
    }

    /** This contract only implements one command, Create. */
    public interface Commands extends CommandData {
        class Create implements Commands {}
    }

    /** This is a reference to the underlying legal contract template and associated parameters. */
    private final SecureHash legalContractReference = SecureHash.sha256("IOU contract template and params");
    @Override public final SecureHash getLegalContractReference() { return legalContractReference; }
}