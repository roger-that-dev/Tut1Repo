package com.iou

import co.paralleluniverse.fibers.Suspendable
import com.iou.IOUFlow.Acceptor
import com.iou.IOUFlow.Initiator
import net.corda.core.contracts.Command
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.flows.CollectSignaturesFlow
import net.corda.flows.FinalityFlow
import net.corda.flows.SignTransactionFlow

/**
 * This flow allows the [Initiator] and the [Acceptor] to agree on the issuance of an [IOUState].
 *
 * In our simple example, the [Acceptor] always accepts a valid IOU.
 */
object IOUFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(val iouValue: Int,
                    val otherParty: Party): FlowLogic<SignedTransaction>() {

        /** The progress tracker provides checkpoints indicating the progress of the flow to observers. */
        override val progressTracker = ProgressTracker()

        /** The flow logic is encapsulated within the call() method. */
        @Suspendable
        override fun call(): SignedTransaction {
            // Stage 1 - Generating the transaction.

            // We create a transaction builder.
            val txBuilder = TransactionBuilder()
            val notaryIdentity = serviceHub.networkMapCache.getAnyNotary()
            txBuilder.notary = notaryIdentity

            // We create the transaction's components.
            val ourIdentity = serviceHub.myInfo.legalIdentity
            val iou = IOUState(iouValue, ourIdentity, otherParty)
            val txCommand = Command(IOUContract.Create(), iou.participants.map { it.owningKey })

            // Adding the item's to the builder.
            txBuilder.withItems(iou, txCommand)

            // Stage 2 - Verifying the transaction.
            txBuilder.toWireTransaction().toLedgerTransaction(serviceHub).verify()

            // Stage 3 - Signing the transaction.
            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

            // Stage 4 - Gathering the signatures.
            val signedTx = subFlow(CollectSignaturesFlow(partSignedTx, CollectSignaturesFlow.tracker()))

            // Stage 5 - Finalising the transaction.
            return subFlow(FinalityFlow(signedTx)).single()
        }
    }

    @InitiatedBy(Initiator::class)
    class Acceptor(val otherParty: Party) : FlowLogic<Unit>() {

        @Suspendable
        override fun call() {
            // Stage 1 - Verifying and signing the transaction.
            subFlow(object : SignTransactionFlow(otherParty, tracker()) {
                override fun checkTransaction(stx: SignedTransaction) {
                    // Define custom verification logic here.
                }
            })
        }
    }
}