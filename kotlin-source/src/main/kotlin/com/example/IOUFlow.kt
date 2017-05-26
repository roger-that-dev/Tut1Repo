package com.example

import com.example.IOUFlow.Acceptor
import com.example.IOUFlow.Initiator
import net.corda.core.flows.FlowLogic
import net.corda.core.transactions.SignedTransaction
import net.corda.flows.CollectSignaturesFlow
import net.corda.flows.FinalityFlow

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
    @net.corda.core.flows.InitiatingFlow
    @net.corda.core.flows.StartableByRPC
    class Initiator(val iouValue: Int,
                    val otherParty: net.corda.core.identity.Party): net.corda.core.flows.FlowLogic<SignedTransaction>() {
        /**
         * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
         * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call() function.
         */
        companion object {
            object GENERATING_TX : net.corda.core.utilities.ProgressTracker.Step("Generating transaction based on new IOU.")
            object VERIFYING_TX : net.corda.core.utilities.ProgressTracker.Step("Verifying contract constraints.")
            object SIGNING_TX : net.corda.core.utilities.ProgressTracker.Step("Signing transaction with our private key.")
            object GATHERING_SIGS : net.corda.core.utilities.ProgressTracker.Step("Obtaining the counterparty's signature.")
            object FINALISING_TX : net.corda.core.utilities.ProgressTracker.Step("Obtaining notary signature and recording transaction.")
            fun tracker() = net.corda.core.utilities.ProgressTracker(GENERATING_TX, VERIFYING_TX, SIGNING_TX, GATHERING_SIGS, FINALISING_TX)
        }

        override val progressTracker = com.example.IOUFlow.Initiator.Companion.tracker()

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @co.paralleluniverse.fibers.Suspendable
        override fun call(): net.corda.core.transactions.SignedTransaction {
            // Obtain our identity.
            val me = serviceHub.myInfo.legalIdentity
            // Obtain the identity of the notary we want to use.
            val notary = serviceHub.networkMapCache.notaryNodes.single().notaryIdentity

            // Stage 1.
            progressTracker.currentStep = com.example.IOUFlow.Initiator.Companion.GENERATING_TX
            val iou = IOUState(iouValue, me, otherParty, IOUContract())
            val txCommand = net.corda.core.contracts.Command(IOUContract.Commands.Create(), iou.participants.map { it.owningKey })
            val unsignedTx = net.corda.core.contracts.TransactionType.General.Builder(notary).withItems(iou, txCommand)

            // Stage 2.
            progressTracker.currentStep = com.example.IOUFlow.Initiator.Companion.VERIFYING_TX
            unsignedTx.toWireTransaction().toLedgerTransaction(serviceHub).verify()

            // Stage 3.
            progressTracker.currentStep = com.example.IOUFlow.Initiator.Companion.SIGNING_TX
            val partSignedTx = serviceHub.signInitialTransaction(unsignedTx)

            // Stage 4.
            progressTracker.currentStep = com.example.IOUFlow.Initiator.Companion.GATHERING_SIGS
            val signedTx = subFlow(net.corda.flows.CollectSignaturesFlow(partSignedTx, CollectSignaturesFlow.tracker()))

            // Stage 5.
            progressTracker.currentStep = com.example.IOUFlow.Initiator.Companion.FINALISING_TX
            return subFlow(net.corda.flows.FinalityFlow(listOf(signedTx), setOf(me, otherParty), FinalityFlow.tracker())).single()
        }
    }

    class Acceptor(val otherParty: net.corda.core.identity.Party) : net.corda.core.flows.FlowLogic<Unit>() {
        companion object {
            object VERIFYING_AND_SIGNING_TX : net.corda.core.utilities.ProgressTracker.Step("Verifying and signing the proposed transaction.")
            fun tracker() = net.corda.core.utilities.ProgressTracker(VERIFYING_AND_SIGNING_TX)
        }

        override val progressTracker = com.example.IOUFlow.Acceptor.Companion.tracker()

        @co.paralleluniverse.fibers.Suspendable
        override fun call() {
            // Stage 1.
            progressTracker.currentStep = com.example.IOUFlow.Acceptor.Companion.VERIFYING_AND_SIGNING_TX
            subFlow(object : net.corda.flows.SignTransactionFlow(otherParty, net.corda.flows.SignTransactionFlow.Companion.tracker()) {
                override fun checkTransaction(stx: net.corda.core.transactions.SignedTransaction) {
                    // Define custom verification logic here.
                }
            })
        }
    }
}