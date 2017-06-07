package com.iou;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.TransactionType;
import net.corda.core.crypto.DigitalSignature;
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
import java.security.SignatureException;
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

        /** The progress tracker provides checkpoints indicating the progress of the flow to observers. */
        private final ProgressTracker progressTracker = new ProgressTracker(
                new ProgressTracker.Step("Generating transaction based on new IOU."),
                new ProgressTracker.Step("Verifying contract constraints."),
                new ProgressTracker.Step("Signing transaction with our private key."),
                new ProgressTracker.Step("Obtaining the counterparty's signature."),
                new ProgressTracker.Step("Obtaining notary signature and recording transaction."));

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
            // Obtain our identity.
            final Party me = getServiceHub().getMyInfo().getLegalIdentity();
            // Obtain the identity of the notary we want to use.
            final Party notary = getServiceHub().getNetworkMapCache().getAnyNotary(null);

            // Stage 1 - Generating the transaction.
            progressTracker.nextStep();
            final IOUState iou = new IOUState(iouValue, me, otherParty, new IOUContract());
            final List<PublicKey> signers = ImmutableList.of(iou.getSender().getOwningKey(), iou.getRecipient().getOwningKey());
            final Command txCommand = new Command(new IOUContract.Create(), signers);
            final TransactionBuilder unsignedTx = new TransactionType.General.Builder(notary).withItems(iou, txCommand);

            final PublicKey otherKey = getServiceHub().getKeyManagementService().freshKey();

            final List<PublicKey> otherKeys = ImmutableList.of(getServiceHub().getKeyManagementService().freshKey(),
                    getServiceHub()
                    .getKeyManagementService().freshKey());


            final SignedTransaction signedTx1 = getServiceHub().signInitialTransaction(unsignedTx);
            final SignedTransaction signedTx2 = getServiceHub().signInitialTransaction(unsignedTx, otherKey);
            final SignedTransaction signedTx3 = getServiceHub().signInitialTransaction(unsignedTx, otherKeys);


            try {
                signedTx1.verifySignatures(otherKey, otherKey);
            } catch (SignatureException e) {
                // Handle the exception.
            }


            // Stage 2 - Verifying the transaction.
            progressTracker.nextStep();
            unsignedTx.toWireTransaction().toLedgerTransaction(getServiceHub()).verify();

            // Stage 3 - Signing the transaction.
            progressTracker.nextStep();
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(unsignedTx);

            // Stage 4 - Gathering the signatures.
            progressTracker.nextStep();
            final SignedTransaction signedTx = subFlow(
                    new CollectSignaturesFlow(partSignedTx, CollectSignaturesFlow.Companion.tracker()));

            // Stage 5 - Finalising the transaction.
            progressTracker.nextStep();
            final Set<Party> participants = ImmutableSet.of(me, otherParty);
            return subFlow(
                    new FinalityFlow(signedTx, participants)).get(0);
        }
    }

    @InitiatedBy(Initiator::class)
    public static class Acceptor extends FlowLogic<Void> {

        private final Party otherParty;
        private final ProgressTracker progressTracker = new ProgressTracker(
                new ProgressTracker.Step("Verifying and signing the proposed transaction.")
        );

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
            // Stage 1 - Verifying and signing the transaction.
            progressTracker.nextStep();

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