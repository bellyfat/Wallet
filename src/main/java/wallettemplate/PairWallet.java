package wallettemplate;

import java.io.IOException;

import javax.annotation.Nullable;

import authenticator.Authenticator;
import authenticator.operations.ATOperation;
import authenticator.operations.OnOperationUIUpdate;
import authenticator.operations.OperationsFactory;
import wallettemplate.utils.BaseUI;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;

public class PairWallet extends BaseUI{
	
	public Button cancelBtn;
	public Button doneBtn;
	public Button runBtn;
	public TextArea textarea;
	public TextField textfield;
    public Main.OverlayUI overlayUi;
    
    public void initialize() {
        super.init();
        doneBtn.setDisable(true);
    }
    private void runPairing(String pairName) throws IOException
    {
    	ATOperation op = OperationsFactory.PAIRING_OPERATION(pairName, Authenticator.getApplicationParams().getBitcoinNetworkType()).SetOperationUIUpdate(new OnOperationUIUpdate(){

			@SuppressWarnings("restriction")
			@Override
			public void onBegin(String str) {
				Platform.runLater(new Runnable() {
			        @Override
			        public void run() {
			        	textarea.appendText(str + "\n------------------------------------------------\n");
			        }
				});
			}

			@SuppressWarnings("restriction")
			@Override
			public void statusReport(String report) {
				Platform.runLater(new Runnable() {
			        @Override
			        public void run() {
			        	textarea.appendText(report + "\n");
			        }
				});
				
			}

			@SuppressWarnings("restriction")
			@Override
			public void onFinished(String str) {
				Platform.runLater(new Runnable() {
			        @Override
			        public void run() {
			        	textarea.appendText("=============================\n" +
			        						str);
			        	
			        	cancelBtn.setDisable(true);
						doneBtn.setDisable(false);
						Authenticator.fireOnNewPairedAuthenticator();
			        }
				});
			}

			@SuppressWarnings("restriction")
			@Override
			public void onError(@Nullable Exception e, @Nullable Throwable t) {
				Platform.runLater(new Runnable() {
					@Override
			        public void run() {
			        	textarea.appendText("--------------------------\n" + 
			        						"Error: + " + e.toString() + "\n" + 
			        						e.getMessage());
			        }
				});
			}

			@Override
			public void onUserCancel(String reason) { }

			@Override
			public void onUserOk(String msg) { }
        	
        });
    	Authenticator.operationsQueue.add(op);
    }
    
    @FXML
    public void run(ActionEvent event) throws IOException {
    	if(textfield.getText().length() > 0)
    	{
    		// in case any messages are on 
    		textarea.clear();
    		this.runPairing(textfield.getText());
    	}
    	else
    	{
    		textarea.appendText("===========================================\n"+
    							"   Please Enter a Pairing Name In The Box\n" + 
    							"===========================================\n");
    	}
    }

    @FXML
    public void cancel(ActionEvent event) {
    	super.cancel();
        overlayUi.done();
    }
    
    @FXML
    public void done(ActionEvent event) {
    	overlayUi.done();
    }
}