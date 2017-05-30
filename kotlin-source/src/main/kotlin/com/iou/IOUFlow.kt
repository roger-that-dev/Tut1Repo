package com.iou

import co.paralleluniverse.fibers.Suspendable
import com.iou.IOUFlow.Acceptor
import com.iou.IOUFlow.Initiator
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
 * This flow allows the [Initiator] and the [Acceptor] to agree on the issuance of an [IOUState].
 *
 * In our simple example, the [Acceptor] always accepts a valid IOU.
 */
object IOUFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(val iouValue: Int,
                    val otherParty: Party): FlowLogic<SignedTransaction>() {

        /** The flow logic is encapsulated within the call() method. */
        @Suspendable
        override fun call(): SignedTransaction {
            // Obtain our identity.
            val me = serviceHub.myInfo.legalIdentity
            // Obtain the identity of the notary we want to use.
            val notary = serviceHub.networkMapCache.getAnyNotary()

            // Stage 1 - Generating the transaction.
            val iou = IOUState(iouValue, me, otherParty, IOUContract())
            val txCommand = Command(IOUContract.Create(), iou.participants.map { it.owningKey })
            val unsignedTx = TransactionType.General.Builder(notary).withItems(iou, txCommand)

            // Stage 2 - Verifying the transaction.
            unsignedTx.toWireTransaction().toLedgerTransaction(serviceHub).verify()

            // Stage 3 - Signing the transaction.
            val partSignedTx = serviceHub.signInitialTransaction(unsignedTx)

            // Stage 4 - Gathering the signatures.
            val signedTx = subFlow(
                    CollectSignaturesFlow(partSignedTx, CollectSignaturesFlow.tracker()))

            // Stage 5 - Finalising the transaction.
            return subFlow(
                    FinalityFlow(listOf(signedTx), setOf(me, otherParty), FinalityFlow.tracker())).single()
        }
    }

    class Acceptor(val otherParty: Party) : FlowLogic<Unit>() {
        @Suspendable
        override fun call() {
            // Stage 1 - Verifying and signing the transaction.
            progressTracker.nextStep()
            subFlow(object : SignTransactionFlow(otherParty, tracker()) {
                override fun checkTransaction(stx: SignedTransaction) {
                    // Define custom verification logic here.
                }
            })
        }
    }
}