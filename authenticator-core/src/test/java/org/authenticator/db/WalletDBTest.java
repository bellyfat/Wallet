package org.authenticator.db;

import static org.junit.Assert.*;

import com.google.protobuf.ByteString;
import org.authenticator.GCM.dispacher.MessageBuilder;
import org.authenticator.db.exceptions.AccountWasNotFoundException;
import org.authenticator.protobuf.AuthWalletHierarchy;
import org.authenticator.protobuf.ProtoConfig;
import org.authenticator.protobuf.ProtoConfig.ATGCMMessageType;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ProtoConfig.AuthenticatorConfiguration.Builder.class,
		ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet.Builder.class,
		ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet.class,
		ProtoConfig.ATAccount.class,
		ProtoConfig.ATAccount.Builder.class,
		ProtoConfig.ATAddress.class,
		ProtoConfig.ATAddress.Builder.class})
public class WalletDBTest {

	@Test
	public void setPairedTest() {
		walletDB mockedWalletdb = Mockito.spy(new walletDB());

		PowerMockito.mockStatic(ProtoConfig.AuthenticatorConfiguration.Builder.class);
		ProtoConfig.AuthenticatorConfiguration.Builder mockedAuthConfBuilder = PowerMockito.mock(ProtoConfig.AuthenticatorConfiguration.Builder.class);

		PowerMockito.mockStatic(ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet.Builder.class);
		ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet.Builder mockedAuthWalletBuilder =  PowerMockito.mock(ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet.Builder.class);

		PowerMockito.when(mockedAuthConfBuilder.getConfigAuthenticatorWalletBuilder()).thenReturn(mockedAuthWalletBuilder);

		try {
			Mockito.doReturn(mockedAuthConfBuilder).when(mockedWalletdb).getConfigFileBuilder();
			Mockito.doNothing().when(mockedWalletdb).writeConfigFile(Mockito.anyObject());
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}

		try {
			mockedWalletdb.setPaired(true);

			Mockito.verify(mockedAuthWalletBuilder, Mockito.times(1)).setPaired(true);
			Mockito.verify(mockedAuthWalletBuilder, Mockito.times(0)).setPaired(false);
		} catch (IOException e) { e.printStackTrace(); }
	}

	@Test
	public void getPairedTest() {
		walletDB mockedWalletdb = Mockito.spy(new walletDB());

		PowerMockito.mockStatic(ProtoConfig.AuthenticatorConfiguration.Builder.class);
		ProtoConfig.AuthenticatorConfiguration.Builder mockedAuthConfBuilder = PowerMockito.mock(ProtoConfig.AuthenticatorConfiguration.Builder.class);

		PowerMockito.mockStatic(ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet.Builder.class);
		ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet mockedAuthWallet =  PowerMockito.mock(ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet.class);
		Mockito.doReturn(true).when(mockedAuthWallet).getPaired();

		PowerMockito.when(mockedAuthConfBuilder.getConfigAuthenticatorWallet()).thenReturn(mockedAuthWallet);

		try {
			Mockito.doReturn(mockedAuthConfBuilder).when(mockedWalletdb).getConfigFileBuilder();
			Mockito.doNothing().when(mockedWalletdb).writeConfigFile(Mockito.anyObject());
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}

		try {
			boolean result = mockedWalletdb.getPaired();
			Mockito.verify(mockedAuthWallet, Mockito.times(1)).getPaired();
			assertTrue(result);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Test
	public void markAddressAsUsedTest() {
		walletDB mockedWalletdb = Mockito.spy(new walletDB());

		// getConfigFileBuilder method stub
		PowerMockito.mockStatic(ProtoConfig.AuthenticatorConfiguration.Builder.class);
		ProtoConfig.AuthenticatorConfiguration.Builder mockedAuthConfBuilder = PowerMockito.mock(ProtoConfig.AuthenticatorConfiguration.Builder.class);
		try {
			Mockito.doReturn(mockedAuthConfBuilder).when(mockedWalletdb).getConfigFileBuilder();
			Mockito.doNothing().when(mockedWalletdb).writeConfigFile(Mockito.anyObject());
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}


		// ATAccount stub
		PowerMockito.mockStatic(ProtoConfig.ATAccount.class);
		ProtoConfig.ATAccount mockedAccount = PowerMockito.mock(ProtoConfig.ATAccount.class);
		try { Mockito.doReturn(mockedAccount).when(mockedWalletdb).getAccount(1); } catch (AccountWasNotFoundException e) { e.printStackTrace(); }
		try { Mockito.doThrow(AccountWasNotFoundException.class).when(mockedWalletdb).getAccount(2); } catch (AccountWasNotFoundException e) { e.printStackTrace(); }
		// ATAccount.Builder stub
		PowerMockito.mockStatic(ProtoConfig.ATAccount.Builder.class);
		ProtoConfig.ATAccount.Builder mockedAccountBuilder = PowerMockito.mock(ProtoConfig.ATAccount.Builder.class);
//		Mockito.doNothing().when(mockedAccountBuilder).addUsedExternalKeys(1);
		// return the stubbed builder
		Mockito.when(ProtoConfig.ATAccount.newBuilder(mockedAccount)).thenReturn(mockedAccountBuilder);
		// make sure updateAccount is stubbed
		try { Mockito.doNothing().when(mockedWalletdb).updateAccount(mockedAccount); } catch (IOException e) { e.printStackTrace(); }


		// test external account
		try {
			mockedWalletdb.markAddressAsUsed(1, 1, AuthWalletHierarchy.HierarchyAddressTypes.External);
			Mockito.verify(mockedAccountBuilder, Mockito.times(1)).addUsedExternalKeys(1);
		} catch (IOException | AccountWasNotFoundException e) {
			e.printStackTrace();
		}

		// test account not found
		boolean didFallOnException = false;
		try {
			mockedWalletdb.markAddressAsUsed(2, 1, AuthWalletHierarchy.HierarchyAddressTypes.External);
		} catch (Exception  e) {
			didFallOnException = true;
			if(e instanceof AccountWasNotFoundException)
				assertTrue(true);
			else
				assertTrue(false);
		}
		if(!didFallOnException) assertTrue(false);
	}

	@Test
	public void isUsedAddressTest() {
		walletDB mockedWalletdb = Mockito.spy(new walletDB());

		// getConfigFileBuilder method stub
		PowerMockito.mockStatic(ProtoConfig.AuthenticatorConfiguration.Builder.class);
		ProtoConfig.AuthenticatorConfiguration.Builder mockedAuthConfBuilder = PowerMockito.mock(ProtoConfig.AuthenticatorConfiguration.Builder.class);
		try {
			Mockito.doReturn(mockedAuthConfBuilder).when(mockedWalletdb).getConfigFileBuilder();
			Mockito.doNothing().when(mockedWalletdb).writeConfigFile(Mockito.anyObject());
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}


		// ATAccount stub
		PowerMockito.mockStatic(ProtoConfig.ATAccount.class);
		ProtoConfig.ATAccount mockedAccount = PowerMockito.mock(ProtoConfig.ATAccount.class);
		try { Mockito.doReturn(mockedAccount).when(mockedWalletdb).getAccount(1); } catch (AccountWasNotFoundException e) { e.printStackTrace(); }
		try { Mockito.doThrow(AccountWasNotFoundException.class).when(mockedWalletdb).getAccount(2); } catch (AccountWasNotFoundException e) { e.printStackTrace(); }
		// used external keys list
		List<Integer> usedExternalKeys = new ArrayList<Integer>();
		{
			usedExternalKeys.add(0);
			usedExternalKeys.add(1);
			usedExternalKeys.add(2);
			usedExternalKeys.add(3);
		}
		Mockito.doReturn(usedExternalKeys).when(mockedAccount).getUsedExternalKeysList();
		// used internal keys list
		List<Integer> usedInternalKeys = new ArrayList<Integer>();
		{
			usedInternalKeys.add(4);
			usedInternalKeys.add(5);
			usedInternalKeys.add(6);
			usedInternalKeys.add(7);
		}
		Mockito.doReturn(usedInternalKeys).when(mockedAccount).getUsedInternalKeysList();

		// test external keys
		try {
			assertTrue(mockedWalletdb.isUsedAddress(1, AuthWalletHierarchy.HierarchyAddressTypes.External, 0));
			assertTrue(mockedWalletdb.isUsedAddress(1, AuthWalletHierarchy.HierarchyAddressTypes.External, 1));
			assertTrue(mockedWalletdb.isUsedAddress(1, AuthWalletHierarchy.HierarchyAddressTypes.External, 2));
			assertTrue(mockedWalletdb.isUsedAddress(1, AuthWalletHierarchy.HierarchyAddressTypes.External, 3));
			for(int i=4; i < 100; i ++)
				assertFalse(mockedWalletdb.isUsedAddress(1, AuthWalletHierarchy.HierarchyAddressTypes.External, i));
		} catch (AccountWasNotFoundException e) {
			e.printStackTrace();
		}

		// test internal keys
		try {
			assertTrue(mockedWalletdb.isUsedAddress(1, AuthWalletHierarchy.HierarchyAddressTypes.Internal, 4));
			assertTrue(mockedWalletdb.isUsedAddress(1, AuthWalletHierarchy.HierarchyAddressTypes.Internal, 5));
			assertTrue(mockedWalletdb.isUsedAddress(1, AuthWalletHierarchy.HierarchyAddressTypes.Internal, 6));
			assertTrue(mockedWalletdb.isUsedAddress(1, AuthWalletHierarchy.HierarchyAddressTypes.Internal, 7));
			for(int i=8; i < 100; i ++)
				assertFalse(mockedWalletdb.isUsedAddress(1, AuthWalletHierarchy.HierarchyAddressTypes.External, i));
		} catch (AccountWasNotFoundException e) {
			e.printStackTrace();
		}

		// test throws AccountWasNotFoundException
		boolean didFallOnException = false;
		try {
			mockedWalletdb.isUsedAddress(2, AuthWalletHierarchy.HierarchyAddressTypes.External, 0);
		} catch (Exception  e) {
			didFallOnException = true;
			if(e instanceof AccountWasNotFoundException)
				assertTrue(true);
			else
				assertTrue(false);
		}
		if(!didFallOnException) assertTrue(false);
	}

	@Test
	public void writePairingDataTest() {
		String mpubkey 				= "i am the master pub key";
		String chaincode 			= "i am the chaincode";
		String key 					= "i am the AES key";
		String GCM 					= "i am the GCM";
		String pairingID 			= "1";
		int accountIndex 			= 1;
		boolean isEncrypted 		= true;
		byte[] salt 				= "i am the salt".getBytes();

		// getConfigFileBuilder method stub
		walletDB mockedWalletdb = Mockito.spy(new walletDB());
		PowerMockito.mockStatic(ProtoConfig.AuthenticatorConfiguration.Builder.class);
		ProtoConfig.AuthenticatorConfiguration.Builder mockedAuthConfBuilder = PowerMockito.mock(ProtoConfig.AuthenticatorConfiguration.Builder.class);
		try {
			Mockito.doReturn(mockedAuthConfBuilder).when(mockedWalletdb).getConfigFileBuilder();
			Mockito.doNothing().when(mockedWalletdb).writeConfigFile(Mockito.anyObject());
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}

		//  ConfigAuthenticatorWallet.Builder
		PowerMockito.mockStatic(ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet.Builder.class);
		ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet.Builder mockedAuthWalletBuilder =  PowerMockito.mock(ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet.Builder.class);
		PowerMockito.when(mockedAuthConfBuilder.getConfigAuthenticatorWalletBuilder()).thenReturn(mockedAuthWalletBuilder);

		try {
			ProtoConfig.PairedAuthenticator returned = mockedWalletdb.writePairingData(mpubkey,
                                                                                        chaincode,
                                                                                        key,
                                                                                        GCM,
                                                                                        pairingID,
                                                                                        accountIndex,
                                                                                        isEncrypted,
                                                                                        salt);
			assertTrue(returned.getMasterPublicKey().equals(mpubkey));
			assertTrue(returned.getChainCode().equals(chaincode));
			assertTrue(returned.getAesKey().equals(key));
			assertTrue(returned.getGCM().equals(GCM));
			assertTrue(returned.getPairingID().equals(pairingID));
			assertTrue(returned.getWalletAccountIndex() == accountIndex);
			assertTrue(returned.getIsEncrypted() == isEncrypted);
			assertTrue(Arrays.equals(returned.getKeySalt().toByteArray(), salt));
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}

}
