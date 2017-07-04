package com.iou

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.flows.FinalityFlow

@InitiatingFlow
@StartableByRPC
class IOUFlow(val iouValue: Int,
              val otherParty: Party) : FlowLogic<Unit>() {

    /** The progress tracker provides checkpoints indicating the progress of the flow to observers. */
    override val progressTracker = ProgressTracker()

    /** The flow logic is encapsulated within the call() method. */
    @Suspendable
    override fun call() {
        // We retrieve the required identities from the network map.
        val me = serviceHub.myInfo.legalIdentity
        val notary = serviceHub.networkMapCache.getAnyNotary()

        // We create a transaction builder
        val txBuilder = TransactionBuilder(notary = notary)

        // We add the items to the builder.
        txBuilder.withItems(
                IOUState(iouValue, me, otherParty),
                Command(IOUContract.Create(), me.owningKey))

        // Verifying the transaction.
        txBuilder.toWireTransaction().toLedgerTransaction(serviceHub).verify()

        // Signing the transaction.
        val signedTx = serviceHub.signInitialTransaction(txBuilder)

        // Finalising the transaction.
        subFlow(FinalityFlow(signedTx))
    }
}