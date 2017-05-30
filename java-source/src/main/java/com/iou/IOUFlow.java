package com.iou;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.TransactionType;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.flows.CollectSignaturesFlow;
import net.corda.flows.FinalityFlow;
import net.corda.flows.SignTransactionFlow;

import java.security.PublicKey;
import java.util.List;
import java.util.Set;

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

        public Initiator(Integer iouValue, Party otherParty) {
            this.iouValue = iouValue;
            this.otherParty = otherParty;
        }

        /** The flow logic is encapsulated within the call() method. */
        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            // Obtain our identity.
            final Party me = getServiceHub().getMyInfo().getLegalIdentity();
            // Obtain the identity of the notary we want to use.
            final Party notary = getServiceHub().getNetworkMapCache().getAnyNotary(null);

            // Stage 1 - Generating the transaction.
            final IOUState iou = new IOUState(iouValue, me, otherParty, new IOUContract());
            final List<PublicKey> signers = ImmutableList.of(iou.getSender().getOwningKey(), iou.getRecipient().getOwningKey());
            final Command txCommand = new Command(new IOUContract.Create(), signers);
            final TransactionBuilder unsignedTx = new TransactionType.General.Builder(notary).withItems(iou, txCommand);

            // Stage 2 - Verifying the transaction.
            unsignedTx.toWireTransaction().toLedgerTransaction(getServiceHub()).verify();

            // Stage 3 - Signing the transaction.
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(unsignedTx);

            // Stage 4 - Gathering the signatures.
            final SignedTransaction signedTx = subFlow(
                    new CollectSignaturesFlow(partSignedTx, CollectSignaturesFlow.Companion.tracker()));

            // Stage 5 - Finalising the transaction.
            final Set<Party> participants = ImmutableSet.of(me, otherParty);
            return subFlow(
                    new FinalityFlow(signedTx, participants)).get(0);
        }
    }

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