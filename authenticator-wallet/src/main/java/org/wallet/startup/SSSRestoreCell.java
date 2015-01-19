package org.wallet.startup;

import java.awt.Button;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.json.JSONException;
import org.wallet.Main;
import org.wallet.Main.OverlayUI;
import org.bitcoinj.core.Coin;

import org.authenticator.Utils.OneName.OneName;
import org.authenticator.protobuf.ProtoConfig.WalletAccountType;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Region;
import javafx.scene.Node;

public class SSSRestoreCell extends Region{	
	@FXML private TextField txf;
	@FXML private Label lbl;
		
	@SuppressWarnings("restriction")
	public SSSRestoreCell() {
        this.loadFXML();
        this.setSnapToPixel(true);
      }
	
	public void setLabel(String value){
		lbl.setText(value);
	}
	public String getShareStr(){ return txf.getText(); }
	
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
        return "startup/SSSRestoreCell.fxml";
    }

    private URL getViewURL() {
        return Main.class.getResource(this.getViewPath());
    }
}