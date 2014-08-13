package wallettemplate.ControllerHelpers;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import org.controlsfx.dialog.Dialogs;
import org.json.JSONException;
import org.json.JSONObject;

import wallettemplate.Controller;
import wallettemplate.Main;
import wallettemplate.OneNameControllerDisplay;
import wallettemplate.controls.ScrollPaneContentManager;
import authenticator.Authenticator;
import authenticator.Utils.EncodingUtils;
import authenticator.db.walletDB;
import authenticator.db.exceptions.AccountWasNotFoundException;
import authenticator.walletCore.exceptions.AddressWasNotFoundException;
import authenticator.hierarchy.exceptions.KeyIndexOutOfRangeException;
import authenticator.network.ONData;
import authenticator.network.OneName;
import authenticator.operations.BAOperation;
import authenticator.operations.OnOperationUIUpdate;
import authenticator.operations.OperationsFactory;
import authenticator.protobuf.ProtoConfig.ATAddress;
import authenticator.protobuf.ProtoConfig.WalletAccountType;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Coin;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.params.MainNetParams;
import com.google.common.base.Throwables;

import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;

public class SendTxHelper {
	@SuppressWarnings("restriction")
	static public boolean ValidateTx(ScrollPaneContentManager scrlContent, Coin fee) throws Exception
    {
    	//Check Outputs
    	if(scrlContent.getCount() == 0)
    		return false;
    	for(Node n:scrlContent.getChildren())
    	{
    		NewAddress na = (NewAddress)n;
    		try {Address outAddr = new Address(Authenticator.getWalletOperation().getNetworkParams(), na.txfAddress.getText().toString());}
    		catch (AddressFormatException e) {return false;}
    		
    	}
    	//check sufficient funds
    	Coin amount = Coin.ZERO;
    	for(Node n:scrlContent.getChildren())
    	{
    		NewAddress na = (NewAddress)n;
    		na.validate();
    		double a;
			if (na.cbCurrency.getValue().toString().equals("BTC")){
				a = (double) Double.parseDouble(na.txfAmount.getText())*100000000;
			}
			else {
				// TOOD - it is not async !
				JSONObject json = EncodingUtils.readJsonFromUrl("https://api.bitcoinaverage.com/ticker/global/" + na.cbCurrency.getValue().toString() + "/");
				double last = json.getDouble("last");
				a = (double) (Double.parseDouble(na.txfAmount.getText())/last)*100000000;
			}
			long satoshis = (long) a;
    		amount = amount.add(Coin.valueOf((long)a));
    	}
    	amount = amount.add(fee);
    	//
    	Coin confirmed = Authenticator.getWalletOperation().getConfirmedBalance(Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getIndex());   	
    	Coin unconfirmed = Authenticator.getWalletOperation().getUnConfirmedBalance(Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getIndex());
    	Coin balance = confirmed.add(unconfirmed);
    	if(amount.compareTo(balance) > 0) return false;
    	
    	//Check min dust amount 
    	if(amount.compareTo(Transaction.MIN_NONDUST_OUTPUT) < 0) return false;
    	
    	return true;
    }
	
	/**
	 * 
	 * @param tx
	 * @param txLabel
	 * @param to
	 * @param WALLET_PW
	 * @param opUpdateListener
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws AddressWasNotFoundException
	 * @throws JSONException
	 * @throws AddressFormatException
	 * @throws KeyIndexOutOfRangeException
	 * @throws AccountWasNotFoundException 
	 */
	static public boolean broadcastTx (Transaction tx, 
			String txLabel, 
			String to, @Nullable String WALLET_PW, 
			OnOperationUIUpdate opUpdateListener) throws NoSuchAlgorithmException, AddressWasNotFoundException, JSONException, AddressFormatException, KeyIndexOutOfRangeException, AccountWasNotFoundException {
		// broadcast
		walletDB config = Authenticator.getWalletOperation().configFile;
		if (!txLabel.isEmpty()){
			try {config.writeNextSavedTxData(tx.getHashAsString(), "", txLabel);}
			catch (IOException e) {e.printStackTrace();}
		}
		BAOperation op = null;
		if(Authenticator.getWalletOperation().getActiveAccount().getActiveAccount().getAccountType() == WalletAccountType.StandardAccount){
			Map<String,ATAddress> keys = new HashMap<String,ATAddress>();
			for(TransactionInput in:tx.getInputs()){				
				// get address
				String add = in.getConnectedOutput().getScriptPubKey().getToAddress(Authenticator.getWalletOperation().getNetworkParams()).toString();
				
				// find key
				ATAddress ca = Authenticator.getWalletOperation().findAddressInAccounts(add);
				
				//add key
				keys.put(add, ca);
			}
			op = OperationsFactory.BROADCAST_NORMAL_TRANSACTION(txLabel, 
					to, 
					Authenticator.getWalletOperation(),
					tx,
					keys,
					WALLET_PW);
		}
		else{
			String pairID = Authenticator.getWalletOperation().getActiveAccount().getPairedAuthenticator().getPairingID();
			op = OperationsFactory.SIGN_AND_BROADCAST_AUTHENTICATOR_TX_OPERATION(Authenticator.getWalletOperation(),
					tx, 
					pairID, 
					txLabel,
					to,
					false,
					null,
					null,
					WALLET_PW);
		}
		
		// operation listeners
		op.SetOperationUIUpdate(opUpdateListener);
		if(Authenticator.checkForOperationNetworkRequirements(op) == true)
			return Authenticator.addOperation(op);
		else
			opUpdateListener.onError(new Exception("Cannot add operation to queue, network requirements not available"), null);
		return false;
    }	
	
	/**
	 * 
	 * @author alon
	 *
	 */
	static public class NewAddress extends HBox {
    	public TextField txfAddress;
    	public TextField txfAmount;
    	public ChoiceBox cbCurrency;
    	public Label lblScanQR;
    	public Label lblContacts;
    	public Label amountInSatoshi;
    	public Label cancelLabel;
    	public int index;
    	
    	Method onCancel;
    	@SuppressWarnings("restriction")
		public NewAddress setCancelOnMouseClick(Object object, Method method){
    		cancelLabel.setOnMouseClicked(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent event) {
					Object[] parameters = new Object[1];
			        parameters[0] = index;
			        try {
						method.invoke(object, parameters);
					} catch (IllegalAccessException | IllegalArgumentException
							| InvocationTargetException e) {
						e.printStackTrace();
					}
				}
            });
    		return this;
    	}
    	
    	@SuppressWarnings({ "restriction", "static-access" })
		public NewAddress(int index)
    	{
    		this.index = index;
    		VBox main = new VBox();
    		VBox cancel = new VBox();
    		VBox oneNameAvi = new VBox();
    		//Cancel
    		cancelLabel = new Label();
    		cancelLabel.setPadding(new Insets(0,5,0,0));
    		cancelLabel.setFont(new Font("Arial", 18));
    		AwesomeDude.setIcon(cancelLabel, AwesomeIcon.TIMES_CIRCLE);
            Tooltip.install(cancelLabel, new Tooltip("Remove Output"));
            cancel.setAlignment(Pos.TOP_CENTER);
            cancel.setMargin(cancelLabel, new Insets(2,0,0,0));
            cancel.getChildren().add(cancelLabel);
            this.getChildren().add(cancel);
    		
    		//Text Fields
            HBox a = new HBox();
    		txfAddress = new TextField();
    		txfAddress.setPromptText("Recipient Address or OneName");
    		txfAddress.setPrefWidth(400);
    		txfAddress.setStyle("-fx-background-insets: 0, 0, 1, 2; -fx-background-color:#ecf0f1;");
    		//Validates the address or onename
    		txfAddress.focusedProperty().addListener(new ChangeListener<Boolean>()
    		{
    		    @Override
    		    public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue)
    		    {
    		        if (!newPropertyValue){
    		        	if (Authenticator.getWalletOperation().getNetworkParams() == MainNetParams.get()){
    		        		if (!txfAddress.getText().equals("") && !txfAddress.getText().substring(0, 1).equals("1") && !txfAddress.getText().substring(0, 1).equals("3")){
    		        			ONData onename = null;
    		        			String onenameID = txfAddress.getText();
    		        			try { 
    		        				onename = OneName.getOneNameData(txfAddress.getText());
    		        				txfAddress.setStyle("-fx-background-insets: 0, 0, 1, 2; -fx-background-color:#ecf0f1; -fx-text-fill: #98d947;");
    		        				txfAddress.setText(onename.getBitcoinAddress());
    		        			} catch (JSONException | NullPointerException | IOException e){
    		        				txfAddress.setStyle("-fx-background-insets: 0, 0, 1, 2; -fx-background-color:#ecf0f1; -fx-text-fill: #f06e6e;");
    		        			}
    		        			if (onename != null){
    		        				VBox v = new VBox();
    		        				Image avatar = null;
    		        				try {avatar = OneName.downloadImage(onename.getAvatarURL());} 
    		        				catch (IOException e) {e.printStackTrace();}
    		        				ImageView avi = new ImageView(avatar);
    		        				avi.setStyle("-fx-cursor: hand;");
    		        				avi.setFitHeight(50);
    		        				avi.setFitWidth(50);
    		        				avi.setOnMouseClicked(new EventHandler<MouseEvent>() {
    		        					   @Override
    		        					   public void handle(MouseEvent event) {
    		        						   Main.instance.overlayUI("DisplayOneName.fxml");
    		        						   OneNameControllerDisplay.loadOneName(onenameID);
    		        					   }
    		        				   });
    		        				v.getChildren().add(avi);
    		        				Label name = new Label(onename.getNameFormatted());
    		        				v.getChildren().add(name);
    		        				v.setAlignment(Pos.CENTER);
    		        				oneNameAvi.getChildren().add(v);
    		        				oneNameAvi.setMargin(v, new Insets(0,0,0,10));
    		        			}
    		        		}
    		        		else {
    		        			try {
    		        				Address outAddr = new Address(Authenticator.getWalletOperation().getNetworkParams(), txfAddress.getText().toString());
    		        				txfAddress.setStyle("-fx-background-insets: 0, 0, 1, 2; -fx-background-color:#ecf0f1; -fx-text-fill: #98d947;");}
    		        			catch (AddressFormatException e) {
    		        				txfAddress.setStyle("-fx-background-insets: 0, 0, 1, 2; -fx-background-color:#ecf0f1; -fx-text-fill: #f06e6e;");
    		        			}
    		        		}
    		        	}
    		        	else {
    		        		try {
    		        			Address outAddr = new Address(Authenticator.getWalletOperation().getNetworkParams(), txfAddress.getText().toString());
    		        			txfAddress.setStyle("-fx-background-insets: 0, 0, 1, 2; -fx-background-color:#ecf0f1; -fx-text-fill: #98d947;");}
    		        		catch (AddressFormatException e) {
    		        			txfAddress.setStyle("-fx-background-insets: 0, 0, 1, 2; -fx-background-color:#ecf0f1; -fx-text-fill: #f06e6e;");
    		        		}
    		        	}
    		        }
    		    }
    		});
    		a.getChildren().add(txfAddress);
    		lblScanQR = new Label();
    		lblScanQR.setFont(new Font("Arial", 18));
    		lblScanQR.setPrefSize(25, 25);
    		Tooltip.install(lblScanQR, new Tooltip("Scan QR code"));
    		a.setMargin(lblScanQR, new Insets(1,0,0,8));
    		AwesomeDude.setIcon(lblScanQR, AwesomeIcon.QRCODE);
    		a.getChildren().add(lblScanQR);
    		lblScanQR.setOnMouseClicked(new EventHandler<MouseEvent>() {
    		    @Override
    		    public void handle(MouseEvent mouseEvent) {
                   
    		    }
    		});
    		lblContacts = new Label();
    		lblContacts.setFont(new Font("Arial", 18));
    		lblContacts.setPrefSize(25, 25);
    		Tooltip.install(lblContacts, new Tooltip("Select from address book"));
    		a.setMargin(lblContacts, new Insets(1,0,0,0));
    		AwesomeDude.setIcon(lblContacts, AwesomeIcon.USERS);
    		a.getChildren().add(lblContacts);
    		main.getChildren().add(a);

    		HBox b = new HBox();
    		txfAmount = new TextField();
    		txfAmount.setPromptText("Amount");
    		txfAmount.setStyle("-fx-background-insets: 0, 0, 1, 2; -fx-background-color:#ecf0f1;");
    		txfAmount.lengthProperty().addListener(new ChangeListener<Number>(){
                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) { 
                      if(newValue.intValue() > oldValue.intValue()){
                          char ch = txfAmount.getText().charAt(oldValue.intValue());  
                          //Check if the new character is the number or other's
                          if(!(ch >= '0' && ch <= '9') && ch != '.'){       
                               //if it's not number then just setText to previous one
                               txfAmount.setText(txfAmount.getText().substring(0,txfAmount.getText().length()-1)); 
                          }
                     }
                }
    		});
    		cbCurrency = new ChoiceBox();
    		cbCurrency.getItems().add("BTC");
    		cbCurrency.getItems().add("USD");
    		cbCurrency.setValue("BTC");
    		cbCurrency.setPrefSize(55, 18);
    		b.setMargin(txfAmount, new Insets(4,6,0,0));
    		b.setMargin(cbCurrency, new Insets(6,0,0,0));
    		b.getChildren().add(txfAmount);
    		b.getChildren().add(cbCurrency);
    		main.getChildren().add(b);
    		this.getChildren().add(main);
    		this.getChildren().add(oneNameAvi);
    	}
    	
    	public boolean validate()
        {
        	if(txfAddress.getText().length() == 0)
        		return false;
        	if(txfAmount.getText().length() == 0)
        		return false;
        	if(txfAmount.getText().matches("[a-zA-Z]+"))
        		return false;
        	// check dust amount 
        	double fee = (double) Double.parseDouble(txfAmount.getText())*100000000;
        	Coin am = Coin.valueOf((long)fee);
        	if(am.compareTo(Transaction.MIN_NONDUST_OUTPUT) < 0)
        		return false;
        	return true;
        }
    }
}
