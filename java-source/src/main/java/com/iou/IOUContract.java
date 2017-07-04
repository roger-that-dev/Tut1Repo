package com.iou;

import net.corda.core.contracts.AuthenticatedObject;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.TransactionForContract;
import net.corda.core.crypto.SecureHash;
import net.corda.core.identity.Party;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class IOUContract implements Contract {
    // Our Create command.
    public static class Create implements CommandData {}

    @Override
    public void verify(TransactionForContract tx) {
        final AuthenticatedObject<Create> command = requireSingleCommand(tx.getCommands(), Create.class);

        requireThat(check -> {
            // Constraints on the shape of the transaction.
            check.using("No inputs should be consumed when issuing an IOU.", tx.getInputs().isEmpty());
            check.using("Only one output state should be created.", tx.getOutputs().size() == 1);

            // IOU-specific constraints.
            final IOUState out = (IOUState) tx.getOutputs().get(0);
            final Party sender = out.getSender();
            final Party recipient = out.getRecipient();
            check.using("The IOU's value must be non-negative.",out.getValue() > 0);
            check.using("The sender and the recipient cannot be the same entity.", out.getSender() != out.getRecipient());

            // Constraints on the signers.
            check.using("The sender must be a signer.", command.getSigners().contains(out.getSender().getOwningKey()));

            return null;
        });
    }

    // The legal contract reference - we'll leave this as a dummy hash for now.
    private final SecureHash legalContractReference = SecureHash.sha256("Prose contract.");
    @Override public final SecureHash getLegalContractReference() { return legalContractReference; }
}