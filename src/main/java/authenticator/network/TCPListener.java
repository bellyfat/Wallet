package authenticator.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import javafx.application.Platform;

import org.controlsfx.dialog.Dialogs;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import wallettemplate.Main;
import authenticator.Authenticator;
import authenticator.BASE;
import authenticator.OnAuthenticatoGUIUpdateListener;
import authenticator.operations.ATOperation;
import authenticator.operations.OnOperationUIUpdate;
import authenticator.operations.OperationsFactory;
import authenticator.ui_helpers.PopUpNotification;

public class TCPListener extends BASE{
	public static Socket socket;
	private static Thread listenerThread;
	private static UpNp plugnplay;
	private static OnAuthenticatoGUIUpdateListener mListener;
	
	/**
	 * Flags
	 */
	private boolean shouldStopListener;
	private boolean isRunning;
	
	public TCPListener(OnAuthenticatoGUIUpdateListener listener){
		super(TCPListener.class);
		mListener = listener;
		isRunning = false;
	}
	
	public void run(final String[] args) throws Exception
	{
		shouldStopListener = false;   
	    this.listenerThread = new Thread(){
	    	@Override
		    public void run() {
	    		if(plugnplay != null)
	    		{
	    			//TODO - notify GUI that cannot run listener because one is already running
	    			return;
	    		}
	    		
	    		plugnplay = new UpNp();
	    		int port = Integer.parseInt(args[0]);
	    		ServerSocket ss = null;
	    		boolean canStartLoop = true;
	    		try {
					plugnplay.run(new String[]{args[0]});
				} catch (Exception e) {
					canStartLoop = false;
				}
	    		if(canStartLoop)
	    		{
	    			try {
						ss = new ServerSocket (port);
						ss.setSoTimeout(5000);
					} catch (IOException e) {
						try { plugnplay.removeMapping(); } catch (IOException | SAXException e1) { }
						canStartLoop = false;
					}
					
	    		}
				if(canStartLoop)
					try{
						boolean isConnected;
						isRunning = true;
						sendUpdatesToPendingRequests();
						while(true)
			    	    {
							isConnected = false;
							//notifyUiAndLog("Listening on port "+port+"...");
							try{
								socket = ss.accept();
								isConnected = true;
							}
							catch (SocketTimeoutException e){ isConnected = false; }
							
							//#################################
							//
							//		Inbound
							//
							//#################################
							try{
								if(isConnected){
									logAsInfo("Processing Pending Operation ...");
									DataInputStream inStream = new DataInputStream(socket.getInputStream());
									DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
									//get request ID
									String requestID = "";
									int keysize = inStream.readInt();
									byte[] reqIdPayload = new byte[keysize];
									inStream.read(reqIdPayload);
									JSONObject jo = new JSONObject(new String(reqIdPayload));
									requestID = jo.getString("requestID");
									//
									PendingRequest pendingReq = null;
									for(Object o:Authenticator.getPendingRequests()){
										PendingRequest po = (PendingRequest)o;
										if(po.requestID.equals(requestID))
										{
											pendingReq = po;
											break;
										}
									}
									//
									if(pendingReq != null){ // find pending request
										// Should we send something on connection ? 
										if(pendingReq.contract.SHOULD_SEND_PAYLOAD_ON_CONNECTION){
											outStream.writeInt(pendingReq.payloadToSendInCaseOfConnection.length);
											outStream.write(pendingReq.payloadToSendInCaseOfConnection);
											logAsInfo("Sent transaction");
										}
										// Should we receive something ?
										if(pendingReq.contract.SHOULD_RECEIVE_PAYLOAD_AFTER_SENDING_PAYLOAD_ON_CONNECTION){
											keysize = inStream.readInt();
											pendingReq.payloadIncoming = new byte[keysize];
											inStream.read(pendingReq.payloadIncoming);
										}
										//cleanup
										inStream.close();
										outStream.close();
										// Complete Operation ?
										switch(pendingReq.operationType){
										case SignTx:
											ATOperation op = OperationsFactory.SIGN_AND_BROADCAST_TX_OPERATION(pendingReq.rawTx,
													pendingReq.pairingID, 
													null,
													true,
													pendingReq.payloadIncoming);
											op.SetOperationUIUpdate(new OnOperationUIUpdate(){

												@Override
												public void onBegin(String str) { }

												@Override
												public void statusReport( String report) { }

												@Override
												public void onFinished( String str) { 
													Dialogs.create()
												        .owner(Main.stage)
												        .title("New Info.")
												        .masthead(null)
												        .message(str)
												        .showInformation();
												}

												@Override
												public void onUserCancel(String reason) {
													Platform.runLater(new Runnable() {
													      @Override public void run() {
													    	  Dialogs.create()
														        .owner(Main.stage)
														        .title("Error")
														        .masthead("Authenticator Refused The Transaction")
														        .message(reason)
														        .showError();   
													      }
													    });
												}

												@Override
												public void onUserOk(String msg) { }

												@Override
												public void onError( Exception e, Throwable t) { }
												
											});
											Authenticator.operationsQueue.add(op);
											break;
										}
										
										//
										Authenticator.removePendingRequest(pendingReq);
									}
									else{ // pending request not found
										logAsInfo("No Pending Request Found");
									}
								}
								else{
									//notifyUiAndLog("Timed-out, Not Connections");
								}
							}
							catch(Exception e){
								logAsInfo("Error Occured while executing Inbound operation:\n"
										+ e.toString());
							}
							
							//#################################
							//
							//		Outbound
							//
							//#################################
							
							logAsInfo("Checking For outbound operations...");
							if(Authenticator.operationsQueue.size() > 0)
							{
								while (Authenticator.operationsQueue.size() > 0){
									ATOperation op = Authenticator.operationsQueue.poll();
									if (op == null)
										break;
									logAsInfo("Executing Operation: " + op.getDescription());
									try{
										op.run(ss);
									}
									catch (Exception e)
									{
										logAsInfo("Error Occured while executing Outbound operation:\n"
												+ e.toString());
										op.OnExecutionError(e);
									}
								}
							}
							else
								logAsInfo("No Outbound Operations Found.");
							
							if(shouldStopListener)
								break;
			    	    }
					}
					catch (Exception e1) {
						
						//TODO - notify gui
						logAsInfo("Fatal Error, Authenticator Operation ShutDown Because Of: \n" + e1.toString());
					}
					finally
					{
						isRunning = false;
						try { ss.close(); plugnplay.removeMapping(); } catch (IOException | SAXException e) { } 
						logAsInfo("Listener Stopped");
						synchronized(this) {notify();}
					}
	    	    //TODO - return to main
	    	    //Main.inputCommand();
	    	}
	    };
	    this.listenerThread.start();
	}
	
	public void stop() throws InterruptedException{
		shouldStopListener = true;
		synchronized(this.listenerThread){
			logAsInfo("Stopping Listener ... ");
			this.listenerThread.wait();
		}
		
	}
	
	public boolean isRuning()
	{
		return isRunning ;
	}
	
	public void logAsInfo(String str)
    {
		if(Authenticator.getApplicationParams().getShouldPrintTCPListenerInfoToConsole())
			this.LOG.info(str);
    }
	
	//##
	public void sendUpdatesToPendingRequests(){
		for(Object o:Authenticator.getPendingRequests()){
			PendingRequest pending = (PendingRequest)o;
			ATOperation op = OperationsFactory.UPDATE_PENDING_REQUEST_IPS(pending.requestID, pending.pairingID);
			Authenticator.operationsQueue.add(op);
		}
	}
}