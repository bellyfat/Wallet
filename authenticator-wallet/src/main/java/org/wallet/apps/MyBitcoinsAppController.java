package org.wallet.apps;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import com.sun.tools.javac.util.Name;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.authenticator.Authenticator;
import org.authenticator.walletCore.exceptions.CannotGetAddressException;
import org.bitcoinj.core.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wallet.ControllerHelpers.AsyncTask;
import org.wallet.Main;
import org.wallet.utils.BaseUI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by alonmuroch on 1/14/15.
 */
public class MyBitcoinsAppController extends BaseUI {
    public Main.OverlayUI overlayUi;

    @FXML private Pane myValuesLoadingPane;
    @FXML private Label myValuesLoadingLbl;
    @FXML private TableView myValuesTbl;
    @FXML private TableColumn myValuesDateCol;
    @FXML private TableColumn myValuesTxIDCol;
    @FXML private TableColumn myValuesDiffCol;

    private PriceData priceData;
    /**
     * contains all the trasactions that spent coins to a watched wallet address with no inputs from the wallet, thus, sent from outside
     */
    private List<EndPoint> endPoints;

    public void initialize() {
        super.initialize(MyBitcoinsAppController.class);

        animateLoadingLable(myValuesLoadingLbl, "Loading Data");
        downloadPriceDataFromBlockchainInfo(new AsyncCompletionHandler<Response>(){

            @Override
            public Response onCompleted(Response response) throws Exception{
                priceData = new PriceData(response.getResponseBody());

                endPoints = new ArrayList<EndPoint>();
                new CalculateEndPointPrices(Authenticator.getWalletOperation().getTrackedWallet()) {
                    @Override
                    protected void onPostExecute() {
                        stopLoadingAnimation();
                        myValuesLoadingLbl.setVisible(false);

                        MyBitcoinsAppController.this.endPoints = this.result;
                        System.out.println(endPoints.size());
                    }
                }.execute();
                return response;
            }

            @Override
            public void onThrowable(Throwable t) {
                t.printStackTrace();
                stopLoadingAnimation();
                Platform.runLater(() -> {
                    myValuesLoadingLbl.setText("Error !");
                });
            }
        });
    }

    @FXML
    protected void close(){
        overlayUi.done();
    }

    private void calculateEndPointPrices(int accountIdx) throws CannotGetAddressException {
        accountIdx = 2;
        List<TransactionOutput> data = Authenticator.getWalletOperation().getUnspentOutputsForAccount(accountIdx);

        new CalculateEndPointPrices(Authenticator.getWalletOperation().getTrackedWallet()) {
            @Override
            protected void onPreExecute() {
                myValuesLoadingLbl.setVisible(true);
                animateLoadingLable(myValuesLoadingLbl, "Loading Data");
            }

            @Override
            protected void onPostExecute() {
                stopLoadingAnimation();
                myValuesLoadingLbl.setVisible(false);

                MyBitcoinsAppController.this.endPoints = this.result;
                System.out.println(endPoints.size());
            }
        }
        .setOutputsData(data)
        .setPriceData(priceData)
        .execute();
    }

    //####################
    //
    //  Animations
    //
    //####################

    boolean shouldStop = false;
    private void animateLoadingLable(Label lbl, final String baseMsg) {
        shouldStop = false;
        Thread t = new Thread() {
            @Override
            public void run() {
                while(true) {
                    try {
                        if(shouldStop)
                            break;
                        Platform.runLater(() -> {
                            lbl.setText(baseMsg);
                        });
                        Thread.sleep(500);

                        if(shouldStop)
                            break;
                        Platform.runLater(() -> {
                            lbl.setText(baseMsg + " .");
                        });
                        Thread.sleep(500);

                        if(shouldStop)
                            break;
                        Platform.runLater(() -> {
                            lbl.setText(baseMsg + " ..");
                        });
                        Thread.sleep(500);

                        if(shouldStop)
                            break;
                        Platform.runLater(() -> {
                            lbl.setText(baseMsg + " ...");
                        });
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        t.setName("Loading animaiton thread");
        t.start();
    }

    private void stopLoadingAnimation() {
        shouldStop = true;
    }

    //####################
    //
    //  Private
    //
    //####################

    private void downloadPriceDataFromBlockchainInfo(AsyncCompletionHandler<Response> listener) {
        String url = "https://blockchain.info/charts/market-price?showDataPoints=false&timespan=all&show_header=true&daysAverageString=1&scale=0&format=json&address=";
        AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
        try {
            asyncHttpClient.prepareGet(url).execute(listener);
        } catch (IOException e) {
            e.printStackTrace();
            listener.onThrowable(e);
        }
    }

    public class PriceData {
        private List<PricePoint> data;

        public PriceData(String rawData) {
            data = parsePriceDataFromBlockchainInfo(rawData);
        }

        //####################
        //
        //  API
        //
        //####################

        /**
         * Could return null if not found. Assuming the data is in ascending unix time order with no major time gaps.
         * @param unix
         * @return
         */
        public PricePoint getClosestPriceToUnixTime(Long unix) {
            int dataGap = 60*60*24; // 86400
            // 1)
            PricePoint firstPoint = data.get(0);
            Long diff = unix - firstPoint.getUnixTime() + dataGap;
            if(diff < 0)
                return null;
            if(diff == 0)
                return firstPoint;

            //2)
            diff /= dataGap;
            int startIdx = Math.min(diff.intValue(), data.size() - 1);
            PricePoint closest = data.get(startIdx);
            for(int i = startIdx - 1; i > 0; i--) {
                PricePoint p = data.get(i);
                Long closestDiff = closest.getUnixTime() - unix;
                Long currentDiff = p.getUnixTime() - unix;

                if(closestDiff == 0)
                    return closest;
                if(currentDiff == 0)
                    return p;

                if(currentDiff < 0 && closestDiff > 0) {
                    currentDiff = Math.abs(currentDiff);
                    if(currentDiff > closestDiff)
                        return closest;
                    else
                        return p;
                }
                closest = p;
            }

            return null;
        }


        //####################
        //
        //  Private
        //
        //####################

        private List<PricePoint> parsePriceDataFromBlockchainInfo(String s) {
            try {
                List<PricePoint> ret = new ArrayList<PricePoint>();
                JSONObject ob = new JSONObject(s);

                JSONArray arr = ob.getJSONArray("values");
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject row = arr.getJSONObject(i);
                    ret.add(new PricePoint(row));
                }

                Collections.sort(ret);
                return ret;
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    private class PricePoint implements Comparable {
        private Long unixTime;
        private Float price;

        public PricePoint(JSONObject data) throws JSONException {
            unixTime = data.getLong("x");
            price = (float) data.getLong("y");
        }

        //####################
        //
        //  API
        //
        //####################
        public Long getUnixTime() {
            return unixTime;
        }

        public Float getPrice() {
            return price;
        }

        //####################
        //
        //  Private
        //
        //####################

        @Override
        public int compareTo(Object o) {
            Long t2 = ((PricePoint)o).getUnixTime();
            return this.getUnixTime().intValue() - t2.intValue();
        }
    }

    public static class EndPoint {
        private TransactionOutput outPoint;
        public Coin tot;
        private List<Price> prices;

        public EndPoint(TransactionOutput outPoint) {
            this.outPoint = outPoint;
            tot = outPoint.getValue();
            prices = new ArrayList<Price>();
        }

        public Long getTotalSatoshies() {
            return outPoint.getValue().longValue();
        }

        public void addPrice(Price p) {
            List<Price> toAdd = new ArrayList<Price>();
            toAdd.add(p);
            addPrices(toAdd);
        }

        public void addPrices(List<Price> p) {
            prices.addAll(p);
        }

        /**
         * Will calculate the amount of coins entered from each source
         * @return
         */
        public List<Price> getPrices() {
            List<Price> ret = new ArrayList<Price>(prices);
            long tot = 0;
            for(Price p: prices)
                tot += p.sathosies;

            for(Price p: ret) {
                double d = (((double)p.sathosies) / ((double)tot));
                long dd = outPoint.getValue().longValue();

                p.sathosies = (long)(dd * d);
            }

            return prices;
        }


    }

    public class Price {
        public Long sathosies;
        public long time;
        public float price;
    }

    public class  CalculateEndPointPrices extends AsyncTask {
        public List<EndPoint> result;
        private List<TransactionOutput> outputsData;
        private PriceData priceData;
        private Wallet wallet;

        public CalculateEndPointPrices(Wallet wallet) {
            result = new ArrayList<EndPoint>();
            this.wallet = wallet;
        }

        public CalculateEndPointPrices setOutputsData(List<TransactionOutput> data) {
            outputsData = data;
            return this;
        }

        public CalculateEndPointPrices setPriceData(PriceData priceData) {
            priceData = priceData;
            return this;
        }

        @Override
        protected void onPreExecute() {
            calculateEndPointPrices(outputsData, priceData, wallet);
        }

        @Override
        protected void doInBackground() {

        }

        @Override
        protected void onPostExecute() { }

        @Override
        protected void progressCallback(Object... params) { }
    }

    public List<EndPoint> calculateEndPointPrices(List<TransactionOutput> data, PriceData priceData, Wallet wallet) {
        if(data == null) return null;

        List<EndPoint> ret = new ArrayList<EndPoint>();
        for (TransactionOutput out: data) {
            EndPoint endPoint = new EndPoint(out);

            Transaction masterParentTx = out.getParentTransaction();

            if(masterParentTx.getValueSentFromMe(wallet).signum() == 0) {
                Price p = new Price();
                p.sathosies = out.getValue().longValue();
                p.time = masterParentTx.getUpdateTime().getTime();
                p.price = priceData.getClosestPriceToUnixTime(p.time).price;
                endPoint.addPrice(p);
            }
            else {
                for(TransactionInput in: masterParentTx.getInputs()) {
                    Coin inV = in.getOutpoint().getConnectedOutput().getValue();
                    List<Price> prices = calculatePricesRecursively(in, priceData, inV, new ArrayList<Price>(), wallet);
                    endPoint.addPrices(prices);
                }
            }

            ret.add(endPoint);
        }

        return ret;
    }

    /**
     * Giving a {@link org.bitcoinj.core.TransactionInput TransactionInput}, the method will trace back the blockchain to
     * find the original transaction which transferred coins to the wallet.<br>
     * <b>Original Tx</b> - a Tx which does not have any wallet inputs, meaning, an outside source transferred coins to the wallet.<br>
     * When the method gets to said origin transaction, it will add to a list the output amount from that transaction and the
     * price in USD of 1 bitcoin at the time of the transaction.
     *
     *
     * @param masterInput
     * @param value
     * @param lst
     * @param wallet
     * @return
     */
    private List<Price> calculatePricesRecursively(TransactionInput masterInput, PriceData priceData, Coin value, List<Price> lst, Wallet wallet) {
        // check if this Tx even sends coins from this wallet
        Transaction masterParentTx = masterInput.getParentTransaction();
        if(masterParentTx.getValueSentFromMe(wallet).signum() == 0) {
            Price p = new Price();
            p.sathosies = value.longValue();
            p.time = masterParentTx.getUpdateTime().getTime();
            p.price = priceData.getClosestPriceToUnixTime(p.time).price;
            lst.add(p);
            return lst;
        }

        // If not, recursively get to the origin Tx
        TransactionOutPoint masterOutPoint =  masterInput.getOutpoint();
        TransactionOutput masterConnectedOutput = masterOutPoint.getConnectedOutput();
        Transaction masterConnectedParentTx = masterConnectedOutput.getParentTransaction();

        if(masterConnectedParentTx.getValueSentFromMe(wallet).signum() > 0)
            for(TransactionInput in: masterConnectedParentTx.getInputs()) {
                TransactionOutPoint oPoint = in.getOutpoint();
                TransactionOutput   oPut = oPoint.getConnectedOutput();
                calculatePricesRecursively(in, priceData, oPut.getValue(), lst, wallet);
            }
        else {
            Price p = new Price();
            p.sathosies = value.longValue();
            p.time = masterConnectedParentTx.getUpdateTime().getTime();
            p.price = priceData.getClosestPriceToUnixTime(p.time).price;
            lst.add(p);
        }
        return lst;
    }

}
