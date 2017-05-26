package com.example

import net.corda.core.contracts.ContractState
import net.corda.core.crypto.keys

/**
 * The state object recording IOU agreements between two parties.
 *
 * A state must implement [ContractState] or one of its descendants.
 *
 * @param iou details of the IOU.
 * @param sender the party issuing the IOU.
 * @param recipient the party receiving and approving the IOU.
 * @param contract the contract which governs which transactions are valid for this state object.
 */
data class IOUState(val value: Int,
                    val sender: net.corda.core.identity.Party,
                    val recipient: net.corda.core.identity.Party,
                    override val contract: IOUContract,
                    override val linearId: net.corda.core.contracts.UniqueIdentifier = net.corda.core.contracts.UniqueIdentifier()): net.corda.core.contracts.LinearState {

    /** The public keys of the involved parties. */
    override val participants get() = listOf(sender, recipient)

    /** Tells the vault to track a state if we are one of the parties involved. */
    override fun isRelevant(ourKeys: Set<java.security.PublicKey>) = ourKeys.intersect(participants.flatMap { it.owningKey.keys }).isNotEmpty()
}
