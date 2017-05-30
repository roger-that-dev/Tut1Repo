package com.iou

import net.corda.core.contracts.ContractState
import net.corda.core.identity.Party

/**
 * The state object recording IOU agreements between two parties.
 *
 * A state must implement [ContractState] or one of its descendants.
 *
 * @param value     the amount of the IOU.
 * @param sender    the party issuing the IOU.
 * @param recipient the party receiving and approving the IOU.
 * @param contract  the contract which governs which transactions are valid for this state object.
 */
data class IOUState(val value: Int,
                    val sender: Party,
                    val recipient: Party,
                    override val contract: IOUContract) : ContractState {

    /** The parties involved in this state. */
    override val participants get() = listOf(sender, recipient)
}
