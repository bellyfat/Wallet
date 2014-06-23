package wallettemplate;

import authenticator.Authenticator;
import authenticator.OnAuthenticatoGUIUpdateListener;
import authenticator.ui_helpers.BAApplication;

import com.aquafx_project.AquaFx;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.kits.WalletAppKit;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.TestNet3Params;
import com.google.bitcoin.store.BlockStoreException;
import com.google.bitcoin.utils.BriefLogFormatter;
import com.google.bitcoin.utils.Threading;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.Service.State;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import wallettemplate.utils.GuiUtils;
import wallettemplate.utils.ProtoConfig;
import wallettemplate.utils.TextFieldValidator;
import wallettemplate.utils.ProtoConfig.ReceiveAddresses;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialog;
import org.controlsfx.dialog.Dialogs;

import static com.google.common.util.concurrent.Service.State.NEW;
import static com.google.common.util.concurrent.Service.State.TERMINATED;
import static wallettemplate.utils.GuiUtils.*;

public class Main extends BAApplication {
    public static String APP_NAME = "WalletTemplate";

    public static NetworkParameters params = MainNetParams.get();
    public static WalletAppKit bitcoin;
    public static Main instance;
    static Stage stg;
    private StackPane uiStack;
    private AnchorPane mainUI;
    public static Controller controller;
    public static Stage stage;
    public static Authenticator auth;
    public static boolean paired = false;
    public static ConfigFile config;

    @Override
    public void start(Stage mainWindow) throws Exception {
    	stg = mainWindow;
        instance = this;
        // Show the crash dialog for any exceptions that we don't handle and that hit the main loop.
        GuiUtils.handleCrashesOnThisThread();
        
        if(super.BAInit(APP_NAME))
	        try {
	            init(mainWindow);
	        } catch (Throwable t) {
	            // Nicer message for the case where the block store file is locked.
	            if (Throwables.getRootCause(t) instanceof BlockStoreException) {
	                GuiUtils.informationalAlert("Already running", "This application is already running and cannot be started twice.");
	            } else {
	                throw t;
	            }
	        }
        else
        	Runtime.getRuntime().exit(0);
    }

    private void init(Stage mainWindow) throws Exception {
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            //AquaFx.style();
        }
        
      //Load the config file
        String filePath = new java.io.File( "." ).getCanonicalPath() + "/" + Main.APP_NAME + ".config";
        File f = new File(filePath);
        config = new ConfigFile();
        if(f.exists() && !f.isDirectory()) { 
        	paired = config.getPaired();
        }
        else {
        	config.setPaired(false);
        }
        
        // Load the GUI. The Controller class will be automagically created and wired up.
        mainWindow.initStyle(StageStyle.UNDECORATED);
        URL location = getClass().getResource("gui.fxml");
        FXMLLoader loader = new FXMLLoader(location);
		mainUI = (AnchorPane) loader.load();
        controller = loader.getController();
        // Configure the window with a StackPane so we can overlay things on top of the main UI.
        uiStack = new StackPane(mainUI);
        mainWindow.setTitle(APP_NAME);
        final Scene scene = new Scene(uiStack, 850, 483);
        final String file = TextFieldValidator.class.getResource("GUI.css").toString();
        scene.getStylesheets().add(file);  // Add CSS that we need.
        mainWindow.setScene(scene);

        // Make log output concise.
        BriefLogFormatter.init();
        // Tell bitcoinj to run event handlers on the JavaFX UI thread. This keeps things simple and means
        // we cannot forget to switch threads when adding event handlers. Unfortunately, the DownloadListener
        // we give to the app kit is currently an exception and runs on a library thread. It'll get fixed in
        // a future version.
        Threading.USER_THREAD = Platform::runLater;
        // Create the app kit. It won't do any heavyweight initialization until after we start it.
        /*bitcoin = new WalletAppKit(params, new File("."), APP_NAME);
        if (params == RegTestParams.get()) {
            bitcoin.connectToLocalHost();   // You should run a regtest mode bitcoind locally.
        } else if (params == MainNetParams.get()) {
            // Checkpoints are block headers that ship inside our app: for a new user, we pick the last header
            // in the checkpoints file and then download the rest from the network. It makes things much faster.
            // Checkpoint files are made using the BuildCheckpoints tool and usually we have to download the
            // last months worth or more (takes a few seconds).
            bitcoin.setCheckpoints(getClass().getResourceAsStream("checkpoints"));
            // As an example!
            bitcoin.useTor();
        }*/
        NetworkParameters np = null;
        if(this.ApplicationParams.getBitcoinNetworkType() == NetworkType.MAIN_NET){
        	np = MainNetParams.get();
        	bitcoin = new WalletAppKit(np, new File("."), APP_NAME);
            // Checkpoints are block headers that ship inside our app: for a new user, we pick the last header
            // in the checkpoints file and then download the rest from the network. It makes things much faster.
            // Checkpoint files are made using the BuildCheckpoints tool and usually we have to download the
            // last months worth or more (takes a few seconds).
            bitcoin.setCheckpoints(getClass().getResourceAsStream("checkpoints"));
            // As an example!
            bitcoin.useTor();
        }
        else if(this.ApplicationParams.getBitcoinNetworkType() == NetworkType.TEST_NET){
        	np = TestNet3Params.get();
        	bitcoin = new WalletAppKit(np, new File("."), APP_NAME+"_testnet");
        	bitcoin.useTor();
        }

        // Now configure and start the appkit. This will take a second or two - we could show a temporary splash screen
        // or progress widget to keep the user engaged whilst we initialise, but we don't.
        bitcoin.setDownloadListener(controller.progressBarUpdater())
               .setBlockingStartup(false)
               .setUserAgent(APP_NAME, "1.0");
        bitcoin.startAsync();
        bitcoin.awaitRunning();
        // Don't make the user wait for confirmations for now, as the intention is they're sending it their own money!
        bitcoin.wallet().allowSpendingUnconfirmedTransactions();
        bitcoin.peerGroup().setMaxConnections(11);
        System.out.println(bitcoin.wallet());
        controller.onBitcoinSetup();
        mainWindow.show();
        stage = mainWindow;
       
        
        /**
         * Authenticator Operation Setup
         */
        //if (paired){
        	auth = new Authenticator(bitcoin.wallet(), bitcoin.peerGroup(), new OnAuthenticatoGUIUpdateListener(){
        		@Override
        		public void simpleTextMessage(String msg) {
        			
        		}

        		@Override
        		public void riseAlertToUser(String msg, String title) {

        		}
        	})
        	.setApplicationParams(ApplicationParams);
        	auth.startAsync();
        	auth.awaitRunning();
        
        	stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
        		@Override
        		public void handle(WindowEvent e) {
        			Action response = null;
        			if(Authenticator.getPendingRequestSize() > 0 || Authenticator.operationsQueue.size() > 0){
        				response = Dialogs.create()
                	        .owner(stage)
                	        .title("Warning !")
                	        .masthead("Pending Requests/ Operations")
                	        .message("Exiting now will cancell all pending requests and operations.\nDo you want to continue?")
                	        .actions(Dialog.Actions.YES, Dialog.Actions.NO)
                	        .showConfirm();
        			}
        			
            	// Or no conditioning needed or user pressed Ok
            	if (response == null || (response != null && response == Dialog.Actions.YES)) {
            		bitcoin.addListener(new Service.Listener() {
						@Override public void terminated(State from) {
							if(!auth.isRunning())
								Runtime.getRuntime().exit(0);
				         }
					}, MoreExecutors.sameThreadExecutor());
					bitcoin.stopAsync();
                
                    auth.addListener(new Service.Listener() {
						@Override public void terminated(State from) {
							if(!bitcoin.isRunning())
								Runtime.getRuntime().exit(0);
				         }
					}, MoreExecutors.sameThreadExecutor());
                    auth.stopAsync();
                    
                    // Forcibly terminate the JVM because Orchid likes to spew non-daemon threads everywhere.
                    //Runtime.getRuntime().exit(0);
            	}
            	else if(response != null && response == Dialog.Actions.NO)
            		e.consume();
        		}
        	});
        //}
    }

    public class OverlayUI<T> {
        public Node ui;
        public T controller;

        public OverlayUI(Node ui, T controller) {
            this.ui = ui;
            this.controller = controller;
        }

        public void show() {
            blurOut(mainUI);
            uiStack.getChildren().add(ui);
            fadeIn(ui);
        }

        public void done() {
            checkGuiThread();
            fadeOutAndRemove(ui, uiStack);
            blurIn(mainUI);
            this.ui = null;
            this.controller = null;
        }
    }

    public <T> OverlayUI<T> overlayUI(Node node, T controller) {
        checkGuiThread();
        OverlayUI<T> pair = new OverlayUI<T>(node, controller);
        // Auto-magically set the overlayUi member, if it's there.
        try {
            controller.getClass().getDeclaredField("overlayUi").set(controller, pair);
        } catch (IllegalAccessException | NoSuchFieldException ignored) {
        }
        pair.show();
        return pair;
    }

    /** Loads the FXML file with the given name, blurs out the main UI and puts this one on top. */
    public <T> OverlayUI<T> overlayUI(String name) {
        try {
            checkGuiThread();
            // Load the UI from disk.
            URL location = getClass().getResource(name);
            FXMLLoader loader = new FXMLLoader(location);
            Pane ui = loader.load();
            T controller = loader.getController();
            OverlayUI<T> pair = new OverlayUI<T>(ui, controller);
            // Auto-magically set the overlayUi member, if it's there.
            try {
                controller.getClass().getDeclaredField("overlayUi").set(controller, pair);
            } catch (IllegalAccessException | NoSuchFieldException ignored) {
            }
            pair.show();
            return pair;
        } catch (IOException e) {
            throw new RuntimeException(e);  // Can't happen.
        }
    }

    @Override
    public void stop() throws Exception {
        /*bitcoin.stopAsync();
        bitcoin.awaitTerminated();
        // Forcibly terminate the JVM because Orchid likes to spew non-daemon threads everywhere.
        Runtime.getRuntime().exit(0);*/
    }

    public static void main(String[] args) {
        launch(args);
    }
}
