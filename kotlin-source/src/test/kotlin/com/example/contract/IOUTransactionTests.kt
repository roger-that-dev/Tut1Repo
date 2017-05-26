package com.example.contract

import com.example.state.IOUState
import net.corda.testing.*
import org.junit.Test

class IOUTransactionTests {
    @Test
    fun `transaction must include Create command`() {
        ledger {
            transaction {
                output { IOUState(1, MINI_CORP, MEGA_CORP, IOUContract()) }
                fails()
                command(MEGA_CORP_PUBKEY, MINI_CORP_PUBKEY) { IOUContract.Commands.Create() }
                verifies()
            }
        }
    }

    @Test
    fun `transaction must have no inputs`() {
        ledger {
            transaction {
                input { IOUState(1, MINI_CORP, MEGA_CORP, IOUContract()) }
                output { IOUState(1, MINI_CORP, MEGA_CORP, IOUContract()) }
                command(MEGA_CORP_PUBKEY) { IOUContract.Commands.Create() }
                `fails with`("No inputs should be consumed when issuing an IOU.")
            }
        }
    }

    @Test
    fun `transaction must have one output`() {
        ledger {
            transaction {
                output { IOUState(1, MINI_CORP, MEGA_CORP, IOUContract()) }
                output { IOUState(1, MINI_CORP, MEGA_CORP, IOUContract()) }
                command(MEGA_CORP_PUBKEY, MINI_CORP_PUBKEY) { IOUContract.Commands.Create() }
                `fails with`("Only one output state should be created.")
            }
        }
    }

    @Test
    fun `sender must sign transaction`() {
        ledger {
            transaction {
                output { IOUState(1, MINI_CORP, MEGA_CORP, IOUContract()) }
                command(MINI_CORP_PUBKEY) { IOUContract.Commands.Create() }
                `fails with`("All of the participants must be signers.")
            }
        }
    }

    @Test
    fun `recipient must sign transaction`() {
        ledger {
            transaction {
                output { IOUState(1, MINI_CORP, MEGA_CORP, IOUContract()) }
                command(MEGA_CORP_PUBKEY) { IOUContract.Commands.Create() }
                `fails with`("All of the participants must be signers.")
            }
        }
    }

    @Test
    fun `sender is not recipient`() {
        ledger {
            transaction {
                output { IOUState(1, MEGA_CORP, MEGA_CORP, IOUContract()) }
                command(MEGA_CORP_PUBKEY, MINI_CORP_PUBKEY) { IOUContract.Commands.Create() }
                `fails with`("The sender and the recipient cannot be the same entity.")
            }
        }
    }

    @Test
    fun `cannot create negative-value IOUs`() {
        ledger {
            transaction {
                output { IOUState(-1, MINI_CORP, MEGA_CORP, IOUContract()) }
                command(MEGA_CORP_PUBKEY, MINI_CORP_PUBKEY) { IOUContract.Commands.Create() }
                `fails with`("The IOU's value must be non-negative.")
            }
        }
    }
}