package org.wallet.apps;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import javafx.application.Application;
import javafx.stage.Stage;
import org.bitcoinj.core.*;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * Created by alonmuroch on 1/15/15.
 */
public class MyBitcoinsAppTest {

    @Test
    public void CalculateEndPointPricesTest() throws IOException {
        Wallet wallet = Mockito.mock(Wallet.class);
        MyBitcoinsAppController a = new MyBitcoinsAppController();

        List<MyBitcoinsAppController.EndPoint> res = a.calculateEndPointPrices(outputData(wallet), getPriceData(), wallet);

        for(MyBitcoinsAppController.EndPoint ep:res) {
            List<MyBitcoinsAppController.Price> r = ep.getPrices();
            System.out.println("EndPoint " + ep.tot.toFriendlyString() + ":");
            for (MyBitcoinsAppController.Price p: r) {
                System.out.println("   - " + Coin.valueOf(p.sathosies).toFriendlyString() + " Coins, at $" + p.price );
            }
            System.out.println();
        }
    }

    /**
     *  ext - external input or address
     *  *   - a transaction
     *  0   - wallet address
     *
     *             A                     B         C         D     E
     *
     *           2 ext                                          16 ext
     * 1)          *                                               *---- ext 1btc
     *             |                                               |
     * 2)     4btc 0                                          3btc 0
     *             |                                               |
     * 3)          *-- ext 1btc                    ext 1btc \      *-- ext 1btc
     *             |                                         \     |
     * 4)     3btc 0                 ext 2            1btc 0--*----0 2btc
     *             |                     |                 |
     * 5)          ----------------------*------------------
     *                                   |
     * 6)                           2btc 0--------*--- ext 5
     *                                   | 2btc   |
     *                                   |        |
     * 7)                    ext 2btc -- *        0 5btc
     *                                   |
     * 8)                           2btc 0
     *
     *
     *
     *
     * @return
     */
    private List<TransactionOutput> outputData(Wallet wallet) {
        TransactionInput in1 = null;
        TransactionInput in2 = null;

        // transaction 1A - update time 3/3/14:12:12:12, unix 1393848732
        Transaction tx1A = generateMokcedTx(wallet, 2, Coin.ZERO, new Date(1393848732));
        // transaction 3A - update time 24/3/14:12:12:12, unix 1395663132
        List<TransactionInput> ins = new ArrayList<TransactionInput>();
        Transaction tx3A = generateMokcedTx(wallet, 1, Coin.COIN.multiply(4), new Date(1395663132));
        in1 = generateMockedTransactionInput(tx3A, tx1A, Coin.COIN.multiply(4));
        ins.add(in1); Mockito.when(tx3A.getInputs()).thenReturn(ins);

        // transaction 1E, update time 3/5/2014:12:12:12
        Transaction tx1E = generateMokcedTx(wallet, 16, Coin.ZERO, new Date(1399119132));

        // transaction 3E, update time 4/6/2014:12:12:12
        Transaction tx3E = generateMokcedTx(wallet, 1, Coin.COIN.multiply(3), new Date(1401883932));
        ins = new ArrayList<TransactionInput>();
        in1 = generateMockedTransactionInput(tx3E, tx1E, Coin.COIN.multiply(3));
        ins.add(in1); Mockito.when(tx3E.getInputs()).thenReturn(ins);
        // transaction 4D, update time 1/10/2014:12:12:12
        Transaction tx4D = generateMokcedTx(wallet, 1, Coin.COIN.multiply(2), new Date(1412165532));
        ins = new ArrayList<TransactionInput>();
        in1 = generateMockedTransactionInput(tx4D, tx3E, Coin.COIN.multiply(2));
        ins.add(in1); Mockito.when(tx4D.getInputs()).thenReturn(ins);

        // transaction 5B, update time 18/10/2014:12:12:12
        Transaction tx5B = generateMokcedTx(wallet, 2, Coin.COIN.multiply(4), new Date(1413634332));
        ins = new ArrayList<TransactionInput>();
        in1 = generateMockedTransactionInput(tx5B, tx3A, Coin.COIN.multiply(3));
        in2 = generateMockedTransactionInput(tx5B, tx4D, Coin.COIN.multiply(1));
        ins.add(in1); ins.add(in2);
        Mockito.when(tx5B.getInputs()).thenReturn(ins);

        // transaction 6C, update time 20/12/2014:12:12:12
        Transaction tx6C = generateMokcedTx(wallet, 5, Coin.ZERO, new Date(1419077532));

        // transaction 7B, update time 24/12/2014:12:12:12
        Transaction tx7B = generateMokcedTx(wallet, 2, Coin.COIN.multiply(4), new Date(1419423132));
        ins = new ArrayList<TransactionInput>();
        in1 = generateMockedTransactionInput(tx7B, tx6C, Coin.COIN.multiply(2));
        in2 = generateMockedTransactionInput(tx7B, tx5B, Coin.COIN.multiply(2));
        ins.add(in1); ins.add(in2);
        Mockito.when(tx7B.getInputs()).thenReturn(ins);


        List<TransactionOutput> ret = new ArrayList<TransactionOutput>();
        TransactionOutput output8B = Mockito.mock(TransactionOutput.class);
        Mockito.when(output8B.getValue()).thenReturn(Coin.COIN.multiply(2));
        Mockito.when(output8B.getParentTransaction()).thenReturn(tx7B);

        TransactionOutput output7C = Mockito.mock(TransactionOutput.class);
        Mockito.when(output7C.getValue()).thenReturn(Coin.COIN.multiply(5));
        Mockito.when(output7C.getParentTransaction()).thenReturn(tx6C);
        ret.add(output7C);
        ret.add(output8B);

        return ret;
    }

    private Transaction generateMokcedTx(Wallet wallet, int numOfInputs, Coin valueSentFromMe, Date updateTime) {
        Transaction tx = Mockito.mock(Transaction.class);
        Mockito.when(tx.getValueSentFromMe(wallet)).thenReturn(valueSentFromMe);
        Mockito.when(tx.getUpdateTime()).thenReturn(updateTime);

        return tx;
    }

    private TransactionInput generateMockedTransactionInput(Transaction parentTx, Transaction connectedTx, Coin value) {
        TransactionInput in = Mockito.mock(TransactionInput.class);
        Mockito.when(in.getParentTransaction()).thenReturn(parentTx);

        TransactionOutPoint op = Mockito.mock(TransactionOutPoint.class);
        Mockito.when(in.getOutpoint()).thenReturn(op);

        TransactionOutput connectedOp = Mockito.mock(TransactionOutput.class);
        Mockito.when(connectedOp.getParentTransaction()).thenReturn(connectedTx);
        Mockito.when(connectedOp.getValue()).thenReturn(value);
        Mockito.when(op.getConnectedOutput()).thenReturn(connectedOp);

        return in;
    }

    private MyBitcoinsAppController.PriceData getPriceData() throws IOException {
        URL url = Resources.getResource("org/wallet/apps/blockchain-info-usd-price-chart-data.json");
        String rawData = Resources.toString(url, Charsets.UTF_8);

        MyBitcoinsAppController.PriceData ret = new MyBitcoinsAppController(). new PriceData(rawData);
        return ret;
    }
}
