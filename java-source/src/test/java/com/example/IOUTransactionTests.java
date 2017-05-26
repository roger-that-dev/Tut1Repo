package com.example;

import net.corda.core.identity.Party;
import org.junit.BeforeClass;
import org.junit.Test;

import java.security.PublicKey;

import static net.corda.testing.CoreTestUtils.*;

public class IOUTransactionTests {
    static private final Party miniCorp = getMINI_CORP();
    static private final Party megaCorp = getMEGA_CORP();
    static private final PublicKey[] keys = new PublicKey[2];

    @BeforeClass
    public static void setUpClass() {
        keys[0] = getMEGA_CORP_PUBKEY();
        keys[1] = getMINI_CORP_PUBKEY();
    }

    @Test
    public void transactionMustIncludeCreateCommand() {
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(new IOUState(1, miniCorp, megaCorp, new IOUContract()));
                txDSL.fails();
                txDSL.command(keys, IOUContract.Commands.Create::new);
                txDSL.verifies();
                return null;
            });
            return null;
        });
    }

    @Test
    public void transactionMustHaveNoInputs() {
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.input(new IOUState(1, miniCorp, megaCorp, new IOUContract()));
                txDSL.output(new IOUState(1, miniCorp, megaCorp, new IOUContract()));
                txDSL.command(keys, IOUContract.Commands.Create::new);
                txDSL.failsWith("No inputs should be consumed when issuing an IOU.");
                return null;
            });
            return null;
        });
    }

    @Test
    public void transactionMustHaveOneOutput() {
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(new IOUState(1, miniCorp, megaCorp, new IOUContract()));
                txDSL.output(new IOUState(1, miniCorp, megaCorp, new IOUContract()));
                txDSL.command(keys, IOUContract.Commands.Create::new);
                txDSL.failsWith("Only one output state should be created.");
                return null;
            });
            return null;
        });
    }

    @Test
    public void senderMustSignTransaction() {
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(new IOUState(1, miniCorp, megaCorp, new IOUContract()));
                PublicKey[] keys = new PublicKey[1];
                keys[0] = getMINI_CORP_PUBKEY();
                txDSL.command(keys, IOUContract.Commands.Create::new);
                txDSL.failsWith("All of the participants must be signers.");
                return null;
            });
            return null;
        });
    }

    @Test
    public void recipientMustSignTransaction() {
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(new IOUState(1, miniCorp, megaCorp, new IOUContract()));
                PublicKey[] keys = new PublicKey[1];
                keys[0] = getMEGA_CORP_PUBKEY();
                txDSL.command(keys, IOUContract.Commands.Create::new);
                txDSL.failsWith("All of the participants must be signers.");
                return null;
            });
            return null;
        });
    }

    @Test
    public void senderIsNotRecipient() {
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(new IOUState(1, megaCorp, megaCorp, new IOUContract()));
                PublicKey[] keys = new PublicKey[1];
                keys[0] = getMEGA_CORP_PUBKEY();
                txDSL.command(keys, IOUContract.Commands.Create::new);
                txDSL.failsWith("The sender and the recipient cannot be the same entity.");
                return null;
            });
            return null;
        });
    }

    @Test
    public void cannotCreateNegativeValueIOUs() {
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(new IOUState(-1, miniCorp, megaCorp, new IOUContract()));
                txDSL.command(keys, IOUContract.Commands.Create::new);
                txDSL.failsWith("The IOU's value must be non-negative.");
                return null;
            });
            return null;
        });
    }
}