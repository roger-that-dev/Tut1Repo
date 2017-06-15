package com.iou

import net.corda.core.contracts.*
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.SecureHash.Companion.sha256

/**
 * A basic smart contract that enforces rules regarding the issuance of [IOUState].
 *
 * A transaction issuing a new [IOUState] onto the ledger must have:
 * - Zero input states.
 * - One output state: the new [IOUState].
 * - A Create() command with the public keys of both the sender and the recipient.
 */
class IOUContract : Contract {
    /** If verify() doesn't throw an exception, the contract accepts the transaction. */
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
            "All of the participants must be signers." using (command.signers.toSet() == out.participants.map { it.owningKey }.toSet())
        }
    }

    /** This contract only implements one command, Create. */
    class Create : CommandData

    /** This is a reference to the underlying legal contract template and associated parameters. */
    override val legalContractReference: SecureHash = sha256("<Legal prose of the contract.>")
}