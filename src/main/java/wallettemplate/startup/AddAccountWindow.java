package wallettemplate.startup;

import java.awt.Button;
import java.io.IOException;
import java.net.URL;

import org.controlsfx.dialog.Dialogs;
import org.json.JSONException;

import com.google.bitcoin.core.Coin;

import wallettemplate.Controller;
import wallettemplate.Main;
import wallettemplate.Main.OverlayUI;
import wallettemplate.startup.StartupController.AddAccountListener;
import wallettemplate.startup.StartupController.AddedAccountObject;
import wallettemplate.utils.TextFieldValidator;
import authenticator.network.OneName;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ATAccount;
import authenticator.protobuf.ProtoConfig.WalletAccountType;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.fxml.FXMLLoader;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import javafx.scene.image.Image;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class AddAccountWindow{	
	@FXML private TextArea txfAccountID;
	@FXML private TextArea txfAccountName;
	//@FXML private TextArea txfAccountName;
	
	static AddAccountListener listener;
	WalletAccountType type;
	
	static Stage stage;
	
	public AddAccountWindow(){}
	@SuppressWarnings("restriction")
	public AddAccountWindow( WalletAccountType type, AddAccountListener lis) {
		 listener = lis;
		 this.type = type;
		 stage = loadFXML(stage, getViewURL(getViewPath(type)), 328, 370);
    }
	
	@SuppressWarnings("restriction")
	private Stage loadFXML(Stage s, URL url, int width, int height) {    	
		s = new Stage();
		try {
			FXMLLoader loader = new FXMLLoader(url);
			s.setTitle("Add Account");
	    	Scene scene;
			scene = new Scene((AnchorPane) loader.load(), width, height);
			final String file = TextFieldValidator.class.getResource("GUI.css").toString();
	        scene.getStylesheets().add(file); 
	        s.setScene(scene);	
	        return s;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
    }
	
	private String getViewPath(WalletAccountType type) {
		if (type == WalletAccountType.StandardAccount)
			return "startup/addStandardAccount.fxml";
		else
			return "startup/addPairedAccount.fxml";
    }

    private URL getViewURL(String path) {
        return Main.class.getResource(path);
    }
    
    public void show(){
    	stage.show();
    }
    
    private boolean validateDone(){
    	try{
    		Integer.parseInt(txfAccountID.getText());
    	}
    	catch(Exception e){
    		return false;
    	}
    	
    	if(txfAccountName.getText().length() <= 2)
    		return false;
    	
    	return true;
    }
    
    @FXML protected void done(ActionEvent event){
    	if(validateDone()){
    		if(this.listener != null)
    			this.listener.addedAccount(new AddedAccountObject(type, 
    					txfAccountName.getText(), 
    					Integer.parseInt(txfAccountID.getText())));
    		stage.close();
    	}
    	else{
    		Dialogs.create()
		        .owner(Main.stage)
		        .title("Error !")
		        .masthead("Something is wrong")
		        .message("Make sure u entered all fields correctly")
		        .showInformation(); 
    	}
	 }
    
    @FXML protected void pair(ActionEvent event){
    	Stage pairWindow = null;
    	pairWindow = loadFXML(pairWindow, getViewURL("/wallettemplate/pair_wallet.fxml"), 635, 340);
    	pairWindow.show();
    }
}
