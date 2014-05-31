package authenticator.operations;

import static wallettemplate.utils.GuiUtils.crashAlert;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;

import javafx.application.Platform;

import javax.annotation.Nullable;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONException;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionConfidence;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionInput.ConnectMode;
import com.google.bitcoin.core.TransactionInput.ConnectionResult;
import com.google.bitcoin.core.TransactionOutPoint;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Wallet.SendResult;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.google.bitcoin.crypto.TransactionSignature;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.script.ScriptBuilder;
import com.google.bitcoin.script.ScriptChunk;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import authenticator.Authenticator;
import authenticator.BASE;
import authenticator.GCM.dispacher.Device;
import authenticator.GCM.dispacher.Dispacher;
import authenticator.GCM.dispacher.MessageType;
import authenticator.Utils.BAUtils;
import authenticator.db.KeyObject;
import authenticator.db.PairingObject;
import authenticator.network.PendingRequest;
import authenticator.operations.OperationsUtils.PairingProtocol;
import authenticator.operations.OperationsUtils.CommunicationObjects.SignMessage;

public class OperationsFactory extends BASE{
	
	public static Logger staticLooger;
	public OperationsFactory() {
		super(OperationsFactory.class);
		staticLooger = this.LOG;
	}

	static public ATOperation PAIRING_OPERATION(String pairingName){
		return new ATOperation(ATOperationType.Pairing)
					.SetDescription("Pair Wallet With an Authenticator Device")
					.SetBeginMsg("Pairing Started ...")
					.SetFinishedMsg("Finished pairing")
					.SetArguments(new String[]{pairingName, "blockchain", pairingName})
					.SetOperationAction(new OperationActions(){
						int timeout = 5;
						ServerSocket socket = null;
						@Override
						public void PreExecution(OnOperationUIUpdate listenerUI, String[] args)  throws Exception {
							// TODO Auto-generated method stub
							
						}

						@SuppressWarnings("static-access")
						@Override
						public void Execute(OnOperationUIUpdate listenerUI, ServerSocket ss, String[] args, OnOperationUIUpdate listener) throws Exception {
							 timeout = ss.getSoTimeout();
							 ss.setSoTimeout(0);
							 socket = ss;
							 PairingProtocol pair = new PairingProtocol();
							 pair.run(ss,args,listener); 
							 //Return to previous timeout
							 ss.setSoTimeout(timeout);
						}

						@Override
						public void PostExecution(OnOperationUIUpdate listenerUI, String[] args)  throws Exception {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void OnExecutionError(OnOperationUIUpdate listenerUI, Exception e) {
							try {
								socket.setSoTimeout(timeout);
							} catch (SocketException e1) {
							
							}
						}
						
					});
	}

	/**
	 * Responsable for the complete process of taking a raw transaction, singing it, maeke the authenticator sign it and then broadcast
	 * <br>
	 * <b>onlyComplete is True if should only complete the the deciphering of a received Authenticator signiture <b>
	 * 
	 * @param tx
	 * @param pairingID
	 * @param txMessage
	 * @param onlyComplete
	 * @return
	 */
	static public ATOperation SIGN_AND_BROADCAST_TX_OPERATION(Transaction tx, 
																String pairingID, 
																@Nullable String txMessage,
																boolean onlyComplete, 
																@Nullable byte[] authenticatorByteResponse){
		return new ATOperation(ATOperationType.SignTx)
				.SetDescription("Sign Raw Transaction By Authenticator device")
				.SetOperationAction(new OperationActions(){
					//int timeout = 5;
					//
					
					@Override
					public void PreExecution(OnOperationUIUpdate listenerUI, String[] args) throws Exception {
					
					}

					@Override
					public void Execute(OnOperationUIUpdate listenerUI, ServerSocket ss, String[] args,
							OnOperationUIUpdate listener) throws Exception {
						//
						if (!onlyComplete){
							byte[] cypherBytes = prepareTX(pairingID);
							String reqID = sendGCM();
							// prepare a pending request object
							PendingRequest pr = new PendingRequest();
							pr.pairingID = pairingID;
							pr.requestID = reqID;
							pr.operationType = ATOperationType.SignTx;
							pr.payloadToSendInCaseOfConnection = cypherBytes;
							pr.rawTx = tx;
							pr.contract.SHOULD_RECEIVE_PAYLOAD_AFTER_SENDING_PAYLOAD_ON_CONNECTION = true;
							pr.contract.SHOULD_SEND_PAYLOAD_ON_CONNECTION = true;
							Authenticator.addPendingRequest(pr,true);
						}
						else{
							PairingObject po = Authenticator.getWalletOperation().getPairingObject(pairingID);
							complete(authenticatorByteResponse,po);
							System.out.println("Signed Tx - " + BAUtils.getStringTransaction(tx));
							SendResult result = Authenticator.getWalletOperation().pushTxWithWallet(tx);
						}
						
						/*Futures.addCallback(result.broadcastComplete, new FutureCallback<Transaction>() {
			                @Override
			                public void onSuccess(Transaction result) {
			                	listenerUI.onFinished("Finished With Success");
			                }

			                @Override
			                public void onFailure(Throwable t) {
			                	listenerUI.onError(null,t);
			                }
			            });*/
					}

					@Override
					public void PostExecution(OnOperationUIUpdate listenerUI, String[] args) throws Exception { }

					@Override
					public void OnExecutionError(OnOperationUIUpdate listenerUI, Exception e) { }
					
					//###########
					// Helpers
					//###########
					byte[] prepareTX(String pairingID) throws Exception {
						//Create the payload
						PairingObject pairingObj = Authenticator.getWalletOperation().getPairingObject(pairingID);
						String formatedTx = BAUtils.getStringTransaction(tx);
						System.out.println("Raw unSigned Tx - " + formatedTx);
						//Get pub keys and indexes
						ArrayList<byte[]> pubKeysArr = new ArrayList<byte[]>();
						ArrayList<Integer> indexArr = new ArrayList<Integer>();
						for(TransactionInput in:tx.getInputs())
							for (KeyObject ko: pairingObj.keys.keys){
								String inAddress = in.getConnectedOutput().getScriptPubKey().getToAddress(Authenticator.getWalletOperation().getNetworkParams()).toString();
								if(inAddress.equals(ko.address)){
									indexArr.add(ko.index);
									BigInteger priv_key = new BigInteger(1, BAUtils.hexStringToByteArray(ko.priv_key));
									byte[] pubkey = ECKey.publicKeyFromPrivate(priv_key, true);//mpPublickeys.get(pairingID).get(a);
									pubKeysArr.add(pubkey);
									break;
								}
							}
						
						SignMessage signMsgPayload = new SignMessage()
										.setInputNumber(tx.getInputs().size())
										.setTxString(formatedTx)
										.setKeyIndexArray(pubKeysArr, indexArr)
										.setVersion(1)
										.setTestnet(false);
										;
						byte[] jsonBytes = signMsgPayload.serializeToBytes();
						
						Mac mac = Mac.getInstance("HmacSHA256");
						SecretKey secretkey = new SecretKeySpec(BAUtils.hexStringToByteArray(Authenticator.getWalletOperation().getAESKey(pairingID)), "AES");
						mac.init(secretkey);
						byte[] macbytes = mac.doFinal(jsonBytes);
						ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
						outputStream.write(jsonBytes);
						outputStream.write(macbytes);
						byte payload[] = outputStream.toByteArray( );
						
						//Encrypt the payload
						Cipher cipher = null;
						try {cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");} 
						 catch (NoSuchAlgorithmException e) {e.printStackTrace();} 
						 catch (NoSuchPaddingException e) {e.printStackTrace();}
						 try {cipher.init(Cipher.ENCRYPT_MODE, secretkey);} 
						 catch (InvalidKeyException e) {e.printStackTrace();}
						 byte[] cipherBytes = null;
						 try {cipherBytes = cipher.doFinal(payload);} 
						 catch (IllegalBlockSizeException e) {e.printStackTrace();} 
						 catch (BadPaddingException e) {e.printStackTrace();}
						 return cipherBytes;
					}
					
					 String sendGCM() throws JSONException, IOException{
						Dispacher disp;
						disp = new Dispacher(null,null);
						//Send the encrypted payload over to the Authenticator and wait for the response.
						SecretKey secretkey = new SecretKeySpec(BAUtils.hexStringToByteArray(Authenticator.getWalletOperation().getAESKey(pairingID)), "AES");						
						PairingObject po = Authenticator.getWalletOperation().getPairingObject(pairingID);
						byte[] gcmID = po.GCM.getBytes();
						assert(gcmID != null);
						Device d = new Device(po.chain_code.getBytes(),
								po.master_public_key.getBytes(),
								gcmID,
								pairingID.getBytes(),
								secretkey);
						
						// returns the request ID
						return disp.dispachMessage(MessageType.signTx, d, new String[]{ txMessage });
					 }
					 
					 @SuppressWarnings({ "static-access", "deprecation", "unused" })
					void complete(byte[] cipherKeyBytes, PairingObject po) throws JSONException, IOException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, ParseException
					 {
							//Decrypt the response
							SecretKey secretkey = new SecretKeySpec(BAUtils.hexStringToByteArray(Authenticator.getWalletOperation().getAESKey(pairingID)), "AES");
						    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
						    cipher.init(Cipher.DECRYPT_MODE, secretkey);
						    String message = BAUtils.bytesToHex(cipher.doFinal(cipherKeyBytes));
						    String sig = message.substring(0,message.length()-64);
						    String HMAC = message.substring(message.length()-64,message.length());
						    byte[] testsig = BAUtils.hexStringToByteArray(sig);
						    byte[] hash = BAUtils.hexStringToByteArray(HMAC);
						    //Calculate the HMAC of the message and verify it is valid
						    Mac mac = Mac.getInstance("HmacSHA256");
							mac.init(secretkey);
							byte[] macbytes = mac.doFinal(testsig);
							if (Arrays.equals(macbytes, hash)){
								staticLooger.info("Received Signature: " + BAUtils.bytesToHex(testsig));
								staticLooger.info("Building Transaction...");
							}
							else {
								staticLooger.info("Message authentication code is invalid");
							}
							//Prep the keys needed for signing
							byte[] key = BAUtils.hexStringToByteArray(po.master_public_key);
							byte[] chain = BAUtils.hexStringToByteArray(po.chain_code);
							List<TransactionInput> inputs = tx.getInputs();
							//Break apart the signature array sent over from the authenticator
							String sigstr = BAUtils.bytesToHex(testsig);
							ArrayList<byte[]> AuthSigs = SignMessage.deserializeToBytes(testsig);//=  new ArrayList<byte[]>();
							
							//Loop to create a signature for each input
							int i =0;
							Authenticator.getWalletOperation().connectInputs(tx.getInputs());
							for(TransactionInput in: tx.getInputs()){
								for (KeyObject ko:po.keys.keys){
									String inAddress = in.getConnectedOutput().getScriptPubKey().getToAddress(Authenticator.getWalletOperation().getNetworkParams()).toString();
									if(inAddress.equals(ko.address))
									{
										//Authenticator Key
										HDKeyDerivation HDKey = null;
										DeterministicKey mPubKey = HDKey.createMasterPubKeyFromBytes(key, chain);
										DeterministicKey childKey = HDKey.deriveChildKey(mPubKey, ko.index);
										byte[] childpublickey = childKey.getPubKey();
										ECKey authKey = new ECKey(null, childpublickey);
										
										//Wallet key
										BigInteger privatekey = new BigInteger(1, BAUtils.hexStringToByteArray(ko.priv_key));
										byte[] walletPublicKey = ECKey.publicKeyFromPrivate(privatekey, true);
										ECKey walletKey = new ECKey(privatekey, walletPublicKey, true);
										
										// Create Program for the script
										List<ECKey> keys = ImmutableList.of(authKey, walletKey);
										Script scriptpubkey = ScriptBuilder.createMultiSigOutputScript(2,keys);
										byte[] program = scriptpubkey.getProgram();
										
										//Create P2SH
										// IMPORTANT - AuthSigs and the signiture we create here should refer to the same input !!
										TransactionSignature sig1 = TransactionSignature.decodeFromBitcoin(AuthSigs.get(i), true);
										TransactionSignature sig2 = tx.calculateSignature(i, walletKey, scriptpubkey, Transaction.SigHash.ALL, false);
										List<TransactionSignature> sigs = ImmutableList.of(sig1, sig2);
										Script inputScript = ScriptBuilder.createP2SHMultiSigInputScript(sigs, program);
										TransactionInput input = inputs.get(i);
										input.setScriptSig(inputScript);
										break;
									}
								}
								i++;
							}
					 }
				});
	}
	
	static public ATOperation UPDATE_PENDING_REQUEST_IPS(String requestID, String pairingID){
		return new ATOperation(ATOperationType.updateIpAddressesForPreviousMessage)
					.SetDescription("Update pending requests to Authenticator")
					.SetBeginMsg("Updating Pending Requests ...")
					.SetFinishedMsg("Finished Pending Requests")
					.SetArguments(new String[]{requestID})
					.SetOperationAction(new OperationActions(){
						@Override
						public void PreExecution(OnOperationUIUpdate listenerUI, String[] args)  throws Exception { }

						@Override
						public void Execute(OnOperationUIUpdate listenerUI, ServerSocket ss, String[] args, OnOperationUIUpdate listener) throws Exception {
							PairingObject po = Authenticator.getWalletOperation().getPairingObject(pairingID);
							Dispacher disp;
							disp = new Dispacher(null,null);
							byte[] gcmID = po.GCM.getBytes();
							assert(gcmID != null);
							Device d = new Device(po.chain_code.getBytes(),
									po.master_public_key.getBytes(),
									gcmID,
									pairingID.getBytes(),
									null);
							
							// returns the request ID
							disp.dispachMessage(MessageType.updateIpAddressesForPreviousMessage, d, new String[]{ requestID });
						}

						@Override
						public void PostExecution(OnOperationUIUpdate listenerUI, String[] args)  throws Exception { }

						@Override
						public void OnExecutionError(OnOperationUIUpdate listenerUI, Exception e) { }
						
					});
	}
}
