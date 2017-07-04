package com.iou

import net.corda.core.contracts.*
import net.corda.core.crypto.SecureHash

open class IOUContract : Contract {
    // Our Create command.
    class Create : CommandData

    override fun verify(tx: TransactionForContract) {
        val command = tx.commands.requireSingleCommand<Create>()

        requireThat {
            // Constraints on the shape of the transaction.
            "No inputs should be consumed when issuing an IOU." using (tx.inputs.isEmpty())
            "Only one output state should be created." using (tx.outputs.size == 1)

            // IOU-specific constraints.
            val out = tx.outputs.single() as IOUState
            "The IOU's value must be non-negative." using (out.value > 0)
            "The sender and the recipient cannot be the same entity." using (out.sender != out.recipient)

            // Constraints on the signers.
            "The sender must be a signer." using (command.signers.contains(out.sender.owningKey))
        }
    }

    // The legal contract reference - we'll leave this as a dummy hash for now.
    override val legalContractReference = SecureHash.sha256("Prose contract.")
}