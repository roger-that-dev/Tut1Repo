package com.iou

import net.corda.core.contracts.ContractState
import net.corda.core.identity.Party

class IOUState(val value: Int,
               val sender: Party,
               val recipient: Party) : ContractState {
    override val contract: IOUContract = IOUContract()

    override val participants get() = listOf(sender, recipient)
}