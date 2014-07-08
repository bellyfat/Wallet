package wallettemplate.controls;

import java.awt.Button;
import java.io.IOException;
import java.net.URL;

import org.json.JSONException;

import com.google.bitcoin.core.Coin;

import wallettemplate.Main;
import wallettemplate.Main.OverlayUI;
import authenticator.network.OneName;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ATAccount;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.fxml.FXMLLoader;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import javafx.scene.image.Image;

public class DisplayAccountCell extends Region{	
	private ATAccount account;
	private AccountCellEvents listener;
	
	private boolean isSettingsOpen;
	static Image imgSettingsOpen;
	static Image imgSettingsClose;
	
	@FXML private Label lblConfirmed;
	@FXML private Label lblUnconfirmed;
	@FXML private Label lblName;
	@FXML private Label lblAccountID;
	@FXML private ImageView btnSettingsImageView;
	
	@SuppressWarnings("restriction")
	public DisplayAccountCell(ATAccount account) {
        this.loadFXML();
        this.setSnapToPixel(true);
        
        this.account = account;
        isSettingsOpen = false;
        updateUI();
        
        if(imgSettingsOpen == null)
        	imgSettingsOpen = new Image(Main.class.getResourceAsStream("display_accounts/closeSettingsWindow.png"));
        if(imgSettingsClose == null)
        	imgSettingsClose = new Image(Main.class.getResourceAsStream("display_accounts/btnSettingsSmall.png"));
        
        
    }
	
	public void updateUI(){
		setConfirmed(Long.toString(this.account.getConfirmedBalance()));
		setUnonfirmed(Long.toString(this.account.getUnConfirmedBalance()));
		setAccountName(this.account.getAccountName());
		setAccountID(Integer.toString(this.account.getIndex()));
	}
	
	private void setAccountID(String id){
		this.lblAccountID.setText(id);
	}
	
	public ATAccount getAccount(){
		return this.account;
	}
	
	public void setAccount (ATAccount account){
		this.account = account;
	}
	
	public void setSettingsOpen(){
		btnSettingsImageView.setImage(imgSettingsOpen);
		openCellMenu();
		isSettingsOpen = true;
	}
	
	public void setSettingsClose(){
		btnSettingsImageView.setImage(imgSettingsClose);
		closeCellMenu();
		isSettingsOpen = false;
	}
	
	@SuppressWarnings("restriction")
	private void openCellMenu(){
		// Sync progress bar slides out ...
        TranslateTransition leave = new TranslateTransition(Duration.millis(600), this);
        leave.setByX(-300.0);
        leave.setCycleCount(1);
        leave.play();
    }
	
	@SuppressWarnings("restriction")
	private void closeCellMenu(){
		// Sync progress bar slides out ...
        TranslateTransition leave = new TranslateTransition(Duration.millis(600), this);
        leave.setByX(300.0);
        leave.setCycleCount(1);
        leave.play();
    }
	
	public DisplayAccountCell setListener(AccountCellEvents listener){
		this.listener = listener;
		return this;
	}
	
	@SuppressWarnings("restriction")
	private void loadFXML() {
        FXMLLoader loader = new FXMLLoader();
 
        loader.setController(this);
        loader.setLocation(this.getViewURL());
 
        try {
            Node root = (Node) loader.load();
            this.getChildren().add(root);
        }
        catch (IOException ex) {
           ex.printStackTrace();
        }    
    }
	
	private String getViewPath() {
        return "display_accounts/account_cell.fxml";
    }

    private URL getViewURL() {
        return Main.class.getResource(this.getViewPath());
    }
	
	public void setConfirmed(String amount){
		Coin a = Coin.valueOf(Long.parseLong(amount));
		lblConfirmed.setText(a.toFriendlyString());
	}
	
	public void setUnonfirmed(String amount){
		Coin a = Coin.valueOf(Long.parseLong(amount));
		lblUnconfirmed.setText(a.toFriendlyString());
	}
	
	public void setAccountName(String name){
		lblName.setText(name);
	}
	
	@FXML protected void btnSettingsClick(ActionEvent event){
		if(isSettingsOpen)
			setSettingsClose();
		else
			setSettingsOpen();
    	if(this.listener != null)
    		this.listener.onSettingsClick(this);
    }
	
	@FXML protected void deleteAccount(ActionEvent event){
		if(this.listener != null)
    		this.listener.onDeleteAccountRequest(this);
	}
	
	@FXML protected void changeAccountName(ActionEvent event){
		if(this.listener != null)
    		this.listener.onChangeNameRequest(this);
	}
	
	
	public interface AccountCellEvents{
		/**
		 * Will be called when the settings button is pressed
		 * @param account
		 */
		public void onSettingsClick(DisplayAccountCell cell);
		/**
		 * Will be called when user pressed the delete account button.<br>
		 * Delegates the delete method to the listener
		 * @param cell
		 */
		public void onDeleteAccountRequest(DisplayAccountCell cell);
		
		/**
		 * Wil be called when user presses on name label
		 * @param cell
		 */
		public void onChangeNameRequest(DisplayAccountCell cell);
	}
}