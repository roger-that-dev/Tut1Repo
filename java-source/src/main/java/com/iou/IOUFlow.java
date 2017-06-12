package com.iou;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.Command;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.flows.CollectSignaturesFlow;
import net.corda.flows.FinalityFlow;
import net.corda.flows.SignTransactionFlow;

import java.security.PublicKey;
import java.util.List;

/**
 * This flow allows the [Initiator] and the [Acceptor] to agree on the issuance of an [IOUState].
 *
 * In our simple example, the [Acceptor] always accepts a valid IOU.
 */
public class IOUFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {
        private final Integer iouValue;
        private final Party otherParty;

        /** The progress tracker provides checkpoints indicating the progress of the flow to observers. */
        private final ProgressTracker progressTracker = new ProgressTracker();

        public Initiator(Integer iouValue, Party otherParty) {
            this.iouValue = iouValue;
            this.otherParty = otherParty;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        /** The flow logic is encapsulated within the call() method. */
        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            // Stage 1 - Generating the transaction.

            // We create a transaction builder.
            final TransactionBuilder txBuilder = new TransactionBuilder();
            final Party notary = getServiceHub().getNetworkMapCache().getAnyNotary(null);
            txBuilder.setNotary(notary);

            // We create the transaction's components.
            final Party ourIdentity = getServiceHub().getMyInfo().getLegalIdentity();
            final IOUState iou = new IOUState(iouValue, ourIdentity, otherParty, new IOUContract());
            final List<PublicKey> signers = ImmutableList.of(ourIdentity.getOwningKey(), otherParty.getOwningKey());
            final Command txCommand = new Command(new IOUContract.Create(), signers);

            // Adding the item's to the builder.
            txBuilder.withItems(iou, txCommand);

            // Stage 2 - Verifying the transaction.
            txBuilder.toWireTransaction().toLedgerTransaction(getServiceHub()).verify();

            // Stage 3 - Signing the transaction.
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            // Stage 4 - Gathering the signatures.
            final SignedTransaction signedTx = subFlow(
                    new CollectSignaturesFlow(partSignedTx, CollectSignaturesFlow.Companion.tracker()));

            // Stage 5 - Finalising the transaction.
            return subFlow(new FinalityFlow(signedTx)).get(0);
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Acceptor extends FlowLogic<Void> {

        private final Party otherParty;

        public Acceptor(Party otherParty) {
            this.otherParty = otherParty;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            // Stage 1 - Verifying and signing the transaction.

            class signTxFlow extends SignTransactionFlow {
                private signTxFlow(Party otherParty, ProgressTracker progressTracker) {
                    super(otherParty, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction signedTransaction) {
                    // Define custom verification logic here.
                }
            }

            subFlow(new signTxFlow(otherParty, SignTransactionFlow.Companion.tracker()));

            return null;
        }
    }
}