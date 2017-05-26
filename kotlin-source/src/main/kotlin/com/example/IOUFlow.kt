package com.example

import co.paralleluniverse.fibers.Suspendable
import com.example.IOUFlow.Acceptor
import com.example.IOUFlow.Initiator
import net.corda.core.contracts.Command
import net.corda.core.contracts.TransactionType
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
import net.corda.flows.CollectSignaturesFlow
import net.corda.flows.FinalityFlow
import net.corda.flows.SignTransactionFlow

/**
 * This flow allows two parties (the [Initiator] and the [Acceptor]) to come to an agreement about the IOU encapsulated
 * within an [IOUState].
 *
 * In our simple example, the [Acceptor] always accepts a valid IOU.
 *
 * These flows have deliberately been implemented by using only the call() method for ease of understanding. In
 * practice we would recommend splitting up the various stages of the flow into sub-routines.
 *
 * All methods called within the [FlowLogic] sub-class need to be annotated with the @Suspendable annotation.
 */

object IOUFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(val iouValue: Int,
                    val otherParty: Party): FlowLogic<SignedTransaction>() {
        /**
         * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
         * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call() function.
         */
        companion object {
            object GENERATING_TX : ProgressTracker.Step("Generating transaction based on new IOU.")
            object VERIFYING_TX : ProgressTracker.Step("Verifying contract constraints.")
            object SIGNING_TX : ProgressTracker.Step("Signing transaction with our private key.")
            object GATHERING_SIGS : ProgressTracker.Step("Obtaining the counterparty's signature.")
            object FINALISING_TX : ProgressTracker.Step("Obtaining notary signature and recording transaction.")
            fun tracker() = ProgressTracker(GENERATING_TX, VERIFYING_TX, SIGNING_TX, GATHERING_SIGS, FINALISING_TX)
        }

        override val progressTracker = tracker()

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        override fun call(): SignedTransaction {
            // Obtain our identity.
            val me = serviceHub.myInfo.legalIdentity
            // Obtain the identity of the notary we want to use.
            val notary = serviceHub.networkMapCache.notaryNodes.single().notaryIdentity

            // Stage 1.
            progressTracker.currentStep = GENERATING_TX
            val iou = IOUState(iouValue, me, otherParty, IOUContract())
            val txCommand = Command(IOUContract.Commands.Create(), iou.participants.map { it.owningKey })
            val unsignedTx = TransactionType.General.Builder(notary).withItems(iou, txCommand)

            // Stage 2.
            progressTracker.currentStep = VERIFYING_TX
            unsignedTx.toWireTransaction().toLedgerTransaction(serviceHub).verify()

            // Stage 3.
            progressTracker.currentStep = SIGNING_TX
            val partSignedTx = serviceHub.signInitialTransaction(unsignedTx)

            // Stage 4.
            progressTracker.currentStep = GATHERING_SIGS
            val signedTx = subFlow(CollectSignaturesFlow(partSignedTx, CollectSignaturesFlow.tracker()))

            // Stage 5.
            progressTracker.currentStep = FINALISING_TX
            return subFlow(FinalityFlow(listOf(signedTx), setOf(me, otherParty), FinalityFlow.tracker())).single()
        }
    }

    class Acceptor(val otherParty: Party) : FlowLogic<Unit>() {
        companion object {
            object VERIFYING_AND_SIGNING_TX : ProgressTracker.Step("Verifying and signing the proposed transaction.")
            fun tracker() = ProgressTracker(VERIFYING_AND_SIGNING_TX)
        }

        override val progressTracker = tracker()

        @Suspendable
        override fun call() {
            // Stage 1.
            progressTracker.currentStep = VERIFYING_AND_SIGNING_TX
            subFlow(object : SignTransactionFlow(otherParty, SignTransactionFlow.tracker()) {
                override fun checkTransaction(stx: SignedTransaction) {
                    // Define custom verification logic here.
                }
            })
        }
    }
}