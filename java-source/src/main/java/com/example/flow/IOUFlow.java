package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.example.contract.IOUContract;
import com.example.state.IOUState;
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
 * This flow allows two parties (the [Initiator] and the [Acceptor]) to come to an agreement about the IOU encapsulated
 * within an [IOUState].
 * <p>
 * In our simple example, the [Acceptor] always accepts a valid IOU.
 * <p>
 * These flows have deliberately been implemented by using only the call() method for ease of understanding. In
 * practice we would recommend splitting up the various stages of the flow into sub-routines.
 * <p>
 * All methods called within the [FlowLogic] sub-class need to be annotated with the @Suspendable annotation.
 */
public class IOUFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {
        private final Integer iouValue;
        private final Party otherParty;

        // The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
        // checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call()
        // function.
        private final ProgressTracker progressTracker = new ProgressTracker(
                GENERATING_TX, VERIFYING_TX, SIGNING_TX, GATHERING_SIGS, FINALISING_TX);

        private static final ProgressTracker.Step GENERATING_TX = new ProgressTracker.Step(
                "Generating transaction based on new IOU.");
        private static final ProgressTracker.Step VERIFYING_TX = new ProgressTracker.Step(
                "Verifying contract constraints.");
        private static final ProgressTracker.Step SIGNING_TX = new ProgressTracker.Step(
                "Signing transaction with our private key.");
        private static final ProgressTracker.Step GATHERING_SIGS = new ProgressTracker.Step(
                "Obtaining the counterparty's signature.");
        private static final ProgressTracker.Step FINALISING_TX = new ProgressTracker.Step(
                "Obtaining notary signature and recording transaction.");

        public Initiator(Integer iouValue, Party otherParty) {
            this.iouValue = iouValue;
            this.otherParty = otherParty;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            // Obtain our identity.
            final Party me = getServiceHub().getMyInfo().getLegalIdentity();
            // Obtain the identity of the notary we want to use.
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryNodes().get(0).getNotaryIdentity();

            // Stage 1.
            progressTracker.setCurrentStep(GENERATING_TX);
            final IOUState iou = new IOUState(iouValue, me, otherParty, new IOUContract());
            final List<PublicKey> signers = ImmutableList.of(iou.getSender().getOwningKey(), iou.getRecipient().getOwningKey());
            final Command txCommand = new Command(new IOUContract.Commands.Create(), signers);
            final TransactionBuilder unsignedTx = new TransactionType.General.Builder(notary).withItems(iou, txCommand);

            // Stage 2.
            progressTracker.setCurrentStep(VERIFYING_TX);
            unsignedTx.toWireTransaction().toLedgerTransaction(getServiceHub()).verify();

            // Stage 3.
            progressTracker.setCurrentStep(SIGNING_TX);
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(unsignedTx);

            // Stage 4.
            progressTracker.setCurrentStep(GATHERING_SIGS);
            final SignedTransaction signedTx = subFlow(new CollectSignaturesFlow(partSignedTx,
                    CollectSignaturesFlow.Companion.tracker()));

            // Stage 5.
            progressTracker.setCurrentStep(FINALISING_TX);
            final Set<Party> participants = ImmutableSet.of(getServiceHub().getMyInfo().getLegalIdentity(), otherParty);
            return subFlow(new FinalityFlow(signedTx, participants)).get(0);
        }
    }

    public static class Acceptor extends FlowLogic<Void> {

        private final Party otherParty;
        private final ProgressTracker progressTracker = new ProgressTracker(VERIFYING_AND_SIGNING_TX);

        private static final ProgressTracker.Step VERIFYING_AND_SIGNING_TX = new ProgressTracker.Step(
                "Verifying and signing the proposed transaction.");

        public Acceptor(Party otherParty) {
            this.otherParty = otherParty;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            // Stage 1.
            progressTracker.setCurrentStep(VERIFYING_AND_SIGNING_TX);

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