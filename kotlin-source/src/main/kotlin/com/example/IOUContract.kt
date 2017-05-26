package com.example

import net.corda.core.contracts.*
import net.corda.core.crypto.SecureHash

/**
 * A basic smart contract that enforces rules regarding the issuance of [IOUState].
 *
 * A transaction issuing a new [IOUState] onto the ledger must have:
 * - Zero input states.
 * - One output state: the new [IOUState].
 * - A Create() command with the public keys of both the sender and the recipient.
 */
open class IOUContract : Contract {
    /** If verify() doesn't throw an exception, the contract accepts the transaction. */
    override fun verify(tx: TransactionForContract) {
        val command = tx.commands.requireSingleCommand<Commands>()
        requireThat {
            // Generic constraints around the IOU transaction.
            "No inputs should be consumed when issuing an IOU." using (tx.inputs.isEmpty())
            "Only one output state should be created." using (tx.outputs.size == 1)
            val out = tx.outputs.single() as IOUState
            "The sender and the recipient cannot be the same entity." using (out.sender != out.recipient)
            "All of the participants must be signers." using (command.signers.containsAll(out.participants.map { it.owningKey }))
            // IOU-specific constraints.
            "The IOU's value must be non-negative." using (out.value > 0)
        }
    }

    /**
     * This contract only implements one command, Create.
     */
    interface Commands : CommandData {
        class Create : Commands
    }

    /** This is a reference to the underlying legal contract template and associated parameters. */
    override val legalContractReference: SecureHash = SecureHash.sha256("IOU contract template and params")
}
