package org.authenticator.db;

import static org.junit.Assert.*;

import com.google.protobuf.ByteString;
import org.authenticator.db.exceptions.AccountWasNotFoundException;
import org.authenticator.db.exceptions.PairingObjectWasNotFoundException;
import org.authenticator.protobuf.AuthWalletHierarchy;
import org.authenticator.protobuf.ProtoConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ProtoConfig.AuthenticatorConfiguration.Builder.class,
		ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet.Builder.class,
		ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet.class,
		ProtoConfig.ATAccount.class,
		ProtoConfig.ATAccount.Builder.class,
		ProtoConfig.ATAddress.class,
		ProtoConfig.ATAddress.Builder.class,
		ProtoConfig.PairedAuthenticator.Builder.class,
		ProtoConfig.PairedAuthenticator.class,
		ProtoConfig.PendingRequest.class,
		ProtoConfig.PendingRequest.Builder.class})
public class WalletDBTest {

	@Test
	public void setPairedTest() {
		WalletDB mockedWalletdb = Mockito.spy(new WalletDB());

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
		WalletDB mockedWalletdb = Mockito.spy(new WalletDB());

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
		WalletDB mockedWalletdb = Mockito.spy(new WalletDB());

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
		WalletDB mockedWalletdb = Mockito.spy(new WalletDB());

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
		byte[] key 					= "i am the AES key".getBytes();
		String GCM 					= "i am the GCM";
		String pairingID 			= "1";
		int accountIndex 			= 1;
		boolean isEncrypted 		= true;
		byte[] salt 				= "i am the salt".getBytes();

		// getConfigFileBuilder method stub
		WalletDB mockedWalletdb = Mockito.spy(new WalletDB());
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
			assertArrayEquals(returned.getAesKey().toByteArray(), key);
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

	@Test
	public void updatePairingGCMRegistrationIDTest() {
		String searchedPairingID = "2";

		String mpubkey 				= "i am the master pub key ";
		String chaincode 			= "i am the chaincode ";
		String key 					= "i am the AES key ";
		String GCM 					= "i am the GCM ";
		int accountIndex 			= 1;
		boolean isEncrypted 		= true;
		String salt 				= "i am the salt ";

		List<ProtoConfig.PairedAuthenticator> all = new ArrayList<ProtoConfig.PairedAuthenticator>();
		ProtoConfig.PairedAuthenticator lookedPairingObject = null;

		// mock the new pair builder
		PowerMockito.mockStatic(ProtoConfig.PairedAuthenticator.Builder.class);
		ProtoConfig.PairedAuthenticator.Builder newPairBuilder = PowerMockito.mock(ProtoConfig.PairedAuthenticator.Builder.class);
		// create pairing objects
		for(int i = 1; i< 10; i ++) {
			PowerMockito.mockStatic(ProtoConfig.PairedAuthenticator.class);
			ProtoConfig.PairedAuthenticator newPair = PowerMockito.mock(ProtoConfig.PairedAuthenticator.class);
						Mockito.doReturn(ByteString.copyFrom((key + i).getBytes())).when(newPair).getAesKey();
						Mockito.doReturn(mpubkey).when(newPair).getMasterPublicKey();
						Mockito.doReturn(chaincode).when(newPair).getChainCode();
						Mockito.doReturn(GCM).when(newPair).getGCM();
						Mockito.doReturn(new Integer(i).toString()).when(newPair).getPairingID();
						Mockito.doReturn(false).when(newPair).getTestnet();
						Mockito.doReturn(0).when(newPair).getKeysN();
						Mockito.doReturn(i).when(newPair).getWalletAccountIndex();
						Mockito.doReturn(isEncrypted).when(newPair).getIsEncrypted();
						Mockito.doReturn(ByteString.copyFrom((salt + i).getBytes())).when(newPair).getKeySalt();

			Mockito.doReturn(newPairBuilder).when(newPair).toBuilder();

			if((new Integer(i).toString()).equals(searchedPairingID)) {
				lookedPairingObject = newPair;
			}

			all.add(newPair);
		}

		// walletDB methods stub
		WalletDB mockedWalletdb = Mockito.spy(new WalletDB());
		PowerMockito.mockStatic(ProtoConfig.AuthenticatorConfiguration.Builder.class);
		ProtoConfig.AuthenticatorConfiguration.Builder mockedAuthConfBuilder = PowerMockito.mock(ProtoConfig.AuthenticatorConfiguration.Builder.class);
		try {
			Mockito.doReturn(mockedAuthConfBuilder).when(mockedWalletdb).getConfigFileBuilder();
			Mockito.doNothing().when(mockedWalletdb).writeConfigFile(Mockito.anyObject());
			Mockito.doReturn(all).when(mockedWalletdb).getAllPairingObjectArray();
			Mockito.doNothing().when(mockedWalletdb).removePairingObject(Mockito.anyObject());
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
		// ConfigAuthenticatorWallet.Builder
		PowerMockito.mockStatic(ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet.Builder.class);
		ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet.Builder mockedAuthWalletBuilder =  PowerMockito.mock(ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet.Builder.class);
		PowerMockito.when(mockedAuthConfBuilder.getConfigAuthenticatorWalletBuilder()).thenReturn(mockedAuthWalletBuilder);

		// test setGCM and removePairingObject are called
		try {
			mockedWalletdb.updatePairingGCMRegistrationID(searchedPairingID, "i am the GCM new");
			Mockito.verify(mockedWalletdb, Mockito.times(1)).removePairingObject(searchedPairingID);
			Mockito.verify(newPairBuilder, Mockito.times(1)).setGCM("i am the GCM new");
			Mockito.verify(newPairBuilder, Mockito.times(0)).setGCM("no the new GCM");
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}

		// test throws PairingObjectWasNotFoundException
		boolean didFallOnException = false;
		try {
			mockedWalletdb.updatePairingGCMRegistrationID("100", "does not really matter");
		} catch (Exception  e) {
			didFallOnException = true;
			if(e instanceof PairingObjectWasNotFoundException)
				assertTrue(true);
			else
				assertTrue(false);
		}
		if(!didFallOnException) assertTrue(false);
	}

	@Test
	public void removePairingObjectTest() {
		String mpubkey 				= "i am the master pub key ";
		String chaincode 			= "i am the chaincode ";
		String key 					= "i am the AES key ";
		String GCM 					= "i am the GCM ";
		int accountIndex 			= 1;
		boolean isEncrypted 		= true;
		String salt 				= "i am the salt ";

		List<ProtoConfig.PairedAuthenticator> all = new ArrayList<ProtoConfig.PairedAuthenticator>();
		// mock the new pair builder
		PowerMockito.mockStatic(ProtoConfig.PairedAuthenticator.Builder.class);
		ProtoConfig.PairedAuthenticator.Builder newPairBuilder = PowerMockito.mock(ProtoConfig.PairedAuthenticator.Builder.class);
		// create pairing objects
		for(int i = 0; i < 10; i ++) {
			PowerMockito.mockStatic(ProtoConfig.PairedAuthenticator.class);
			ProtoConfig.PairedAuthenticator newPair = PowerMockito.mock(ProtoConfig.PairedAuthenticator.class);
			Mockito.doReturn(ByteString.copyFrom((key + i).getBytes())).when(newPair).getAesKey();
			Mockito.doReturn(mpubkey).when(newPair).getMasterPublicKey();
			Mockito.doReturn(chaincode).when(newPair).getChainCode();
			Mockito.doReturn(GCM).when(newPair).getGCM();
			Mockito.doReturn(new Integer(i).toString()).when(newPair).getPairingID();
			Mockito.doReturn(false).when(newPair).getTestnet();
			Mockito.doReturn(0).when(newPair).getKeysN();
			Mockito.doReturn(i).when(newPair).getWalletAccountIndex();
			Mockito.doReturn(isEncrypted).when(newPair).getIsEncrypted();
			Mockito.doReturn(ByteString.copyFrom((salt + i).getBytes())).when(newPair).getKeySalt();

			Mockito.doReturn(newPairBuilder).when(newPair).toBuilder();

			all.add(newPair);
		}

		// walletDB methods stub
		WalletDB mockedWalletdb = Mockito.spy(new WalletDB());
		PowerMockito.mockStatic(ProtoConfig.AuthenticatorConfiguration.Builder.class);
		ProtoConfig.AuthenticatorConfiguration.Builder mockedAuthConfBuilder = PowerMockito.mock(ProtoConfig.AuthenticatorConfiguration.Builder.class);
		try {
			Mockito.doReturn(mockedAuthConfBuilder).when(mockedWalletdb).getConfigFileBuilder();
			Mockito.doNothing().when(mockedWalletdb).writeConfigFile(Mockito.anyObject());
			Mockito.doReturn(all).when(mockedWalletdb).getAllPairingObjectArray();
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
		// ConfigAuthenticatorWallet.Builder
		PowerMockito.mockStatic(ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet.Builder.class);
		ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet.Builder mockedAuthWalletBuilder =  PowerMockito.mock(ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet.Builder.class);
		PowerMockito.when(mockedAuthConfBuilder.getConfigAuthenticatorWalletBuilder()).thenReturn(mockedAuthWalletBuilder);

		// test
		try {
			mockedWalletdb.removePairingObject("2");

			Mockito.verify(mockedAuthConfBuilder, Mockito.times(1)).clearConfigAuthenticatorWallet();

			ArgumentCaptor<ProtoConfig.PairedAuthenticator> arg = ArgumentCaptor.forClass(ProtoConfig.PairedAuthenticator.class);
			Mockito.verify(mockedAuthWalletBuilder, Mockito.times(9)).addPairedWallets(arg.capture());

			List<ProtoConfig.PairedAuthenticator> ret = arg.getAllValues();
			assertTrue(ret.size() == 9);
			for (ProtoConfig.PairedAuthenticator po: ret) {
				if(po.getPairingID().equals("2"))
					assertTrue(false);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		// check does not throw exception if pairing object is not found
		try {
			mockedWalletdb.removePairingObject("200");
		} catch (Exception  e) {
			assertTrue(false);
		}
	}

	@Test
	public void getAccountTest() {
		List<ProtoConfig.ATAccount> all = new ArrayList<ProtoConfig.ATAccount>();

		PowerMockito.mockStatic(ProtoConfig.ATAccount.class);
		for(int i=0; i < 100; i++) {
			ProtoConfig.ATAccount mockedAccount = PowerMockito.mock(ProtoConfig.ATAccount.class);
			Mockito.doReturn(i).when(mockedAccount).getIndex();

			all.add(mockedAccount);
		}

		// walletDB methods stub
		WalletDB mockedWalletdb = Mockito.spy(new WalletDB());
		try {
			Mockito.doReturn(all).when(mockedWalletdb).getAllAccounts();
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}

		try {
			for(int i = 0; i < 1000; i++) {
				int rand = randInt(0, 99);
				ProtoConfig.ATAccount result = mockedWalletdb.getAccount(rand);
				assertTrue(result.getIndex() == rand);
			}


		} catch (AccountWasNotFoundException e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}

	@Test
	public void removeAccountTest() {
		List<ProtoConfig.ATAccount> all = new ArrayList<ProtoConfig.ATAccount>();

		PowerMockito.mockStatic(ProtoConfig.ATAccount.class);
		for(int i=0; i < 100; i++) {
			ProtoConfig.ATAccount mockedAccount = PowerMockito.mock(ProtoConfig.ATAccount.class);
			Mockito.doReturn(i).when(mockedAccount).getIndex();

			all.add(mockedAccount);
		}

		// walletDB methods stub
		WalletDB mockedWalletdb = Mockito.spy(new WalletDB());
		PowerMockito.mockStatic(ProtoConfig.AuthenticatorConfiguration.Builder.class);
		ProtoConfig.AuthenticatorConfiguration.Builder mockedAuthConfBuilder = PowerMockito.mock(ProtoConfig.AuthenticatorConfiguration.Builder.class);
		try {
			Mockito.doReturn(all).when(mockedWalletdb).getAllAccounts();
			Mockito.doReturn(mockedAuthConfBuilder).when(mockedWalletdb).getConfigFileBuilder();
			Mockito.doNothing().when(mockedWalletdb).writeConfigFile(Mockito.anyObject());
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}

		// test removal
		for(int i = 0; i< 10; i++) {
			// test
			try {
				mockedWalletdb.removeAccount(i);

				Mockito.verify(mockedAuthConfBuilder, Mockito.atLeastOnce()).clearConfigAccounts();

				ArgumentCaptor<ProtoConfig.ATAccount> arg = ArgumentCaptor.forClass(ProtoConfig.ATAccount.class);
				Mockito.verify(mockedAuthConfBuilder, Mockito.atLeastOnce()).addConfigAccounts(arg.capture());

				List<ProtoConfig.ATAccount> ret = arg.getAllValues();
				ret = ret.subList(99*(i), ret.size()); // because ArgumentCaptor catches all calls ever
				assertTrue(ret.size() == 99);
				for (ProtoConfig.ATAccount acc: ret) {
					if(acc.getIndex() == i)
						assertTrue(false);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	public void removePendingRequestTest() {
		List<ProtoConfig.PendingRequest> all = new ArrayList<ProtoConfig.PendingRequest>();
		List<ProtoConfig.PendingRequest> forDeletetion = new ArrayList<ProtoConfig.PendingRequest>();
		PowerMockito.mockStatic(ProtoConfig.PendingRequest.class);
		for(Integer i=0; i < 100; i++) {
			ProtoConfig.PendingRequest mockedReq = PowerMockito.mock(ProtoConfig.PendingRequest.class);
			Mockito.doReturn(i.toString()).when(mockedReq).getRequestID();

			if(i < 10)
				forDeletetion.add(mockedReq);

			all.add(mockedReq);
		}

		// walletDB methods stub
		WalletDB mockedWalletdb = Mockito.spy(new WalletDB());
		PowerMockito.mockStatic(ProtoConfig.AuthenticatorConfiguration.Builder.class);
		ProtoConfig.AuthenticatorConfiguration.Builder mockedAuthConfBuilder = PowerMockito.mock(ProtoConfig.AuthenticatorConfiguration.Builder.class);
		// ConfigAuthenticatorWallet.Builder
		PowerMockito.mockStatic(ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet.Builder.class);
		ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet.Builder mockedAuthWalletBuilder =  PowerMockito.mock(ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet.Builder.class);
		PowerMockito.when(mockedAuthConfBuilder.getConfigAuthenticatorWalletBuilder()).thenReturn(mockedAuthWalletBuilder);
		try {
			Mockito.doReturn(all).when(mockedWalletdb).getPendingRequests();
			Mockito.doReturn(mockedAuthConfBuilder).when(mockedWalletdb).getConfigFileBuilder();
			Mockito.doNothing().when(mockedWalletdb).writeConfigFile(Mockito.anyObject());
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}

		// test
		try {
			mockedWalletdb.removePendingRequest(forDeletetion);

			Mockito.verify(mockedAuthWalletBuilder, Mockito.atLeastOnce()).clearPendingRequests();

			ArgumentCaptor<ProtoConfig.PendingRequest> arg = ArgumentCaptor.forClass(ProtoConfig.PendingRequest.class);
			Mockito.verify(mockedAuthWalletBuilder, Mockito.atLeastOnce()).addPendingRequests(arg.capture());

			List<ProtoConfig.PendingRequest> ret = arg.getAllValues();
			for(ProtoConfig.PendingRequest prReturned: ret)
				for(ProtoConfig.PendingRequest prForDeletion: forDeletetion)
					assertFalse(prReturned.getRequestID().equals(prForDeletion.getRequestID()));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void getExtensionTest() {
		// walletDB methods stub
		WalletDB mockedWalletdb = Mockito.spy(new WalletDB());
		PowerMockito.mockStatic(ProtoConfig.AuthenticatorConfiguration.Builder.class);
		ProtoConfig.AuthenticatorConfiguration.Builder mockedAuthConfBuilder = PowerMockito.mock(ProtoConfig.AuthenticatorConfiguration.Builder.class);
		// ConfigAuthenticatorWallet.Builder
		PowerMockito.mockStatic(ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet.Builder.class);
		ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet.Builder mockedAuthWalletBuilder =  PowerMockito.mock(ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet.Builder.class);
		PowerMockito.when(mockedAuthConfBuilder.getConfigAuthenticatorWalletBuilder()).thenReturn(mockedAuthWalletBuilder);

		List<ProtoConfig.Extenstion> extensions = new ArrayList<>();
		for(Integer i=0; i < 100; i++ ) {
			ProtoConfig.Extenstion.Builder b = ProtoConfig.Extenstion.newBuilder();
			b.setExtentionID(i.toString());
			b.setDescription("ext " + i.toString());
			b.setData(ByteString.copyFrom(("data for extension " + i.toString()).getBytes()));
			extensions.add(b.build());
		}
		Mockito.doReturn(extensions).when(mockedWalletdb).getExtensions();


		// test return extension 4
		ProtoConfig.Extenstion ret = mockedWalletdb.getExtension("4");
		assertEquals(ret.getExtentionID(), "4");
		assertEquals(ret.getDescription(), "ext 4");
		assertEquals(Hex.toHexString(ret.getData().toByteArray()), Hex.toHexString("data for extension 4".getBytes()));

		// test return extension 17
		ret = mockedWalletdb.getExtension("17");
		assertEquals(ret.getExtentionID(), "17");
		assertEquals(ret.getDescription(), "ext 17");
		assertEquals(Hex.toHexString(ret.getData().toByteArray()), Hex.toHexString("data for extension 17".getBytes()));

		// test return extension 54
		ret = mockedWalletdb.getExtension("54");
		assertEquals(ret.getExtentionID(), "54");
		assertEquals(ret.getDescription(), "ext 54");
		assertEquals(Hex.toHexString(ret.getData().toByteArray()), Hex.toHexString("data for extension 54".getBytes()));

		boolean didThrow = false;
		try {
			mockedWalletdb.getExtension(null);
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
			didThrow = true;
		}
		assertTrue(didThrow);

		didThrow = false;
		try {
			mockedWalletdb.getExtension("");
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
			didThrow = true;
		}
		assertTrue(didThrow);
	}

	@Test
	public void removeExtensionTest() {
		// walletDB methods stub
		WalletDB mockedWalletdb = Mockito.spy(new WalletDB());
		PowerMockito.mockStatic(ProtoConfig.AuthenticatorConfiguration.Builder.class);
		ProtoConfig.AuthenticatorConfiguration.Builder mockedAuthConfBuilder = PowerMockito.mock(ProtoConfig.AuthenticatorConfiguration.Builder.class);
		Mockito.doReturn(mockedAuthConfBuilder).when(mockedWalletdb).getConfigFileBuilder();
		// ConfigAuthenticatorWallet.Builder
		PowerMockito.mockStatic(ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet.Builder.class);
		ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet.Builder mockedAuthWalletBuilder =  PowerMockito.mock(ProtoConfig.AuthenticatorConfiguration.ConfigAuthenticatorWallet.Builder.class);
		PowerMockito.when(mockedAuthConfBuilder.getConfigAuthenticatorWalletBuilder()).thenReturn(mockedAuthWalletBuilder);

		List<ProtoConfig.Extenstion> extensions = new ArrayList<>();
		for(Integer i=0; i < 100; i++ ) {
			ProtoConfig.Extenstion.Builder b = ProtoConfig.Extenstion.newBuilder();
			b.setExtentionID(i.toString());
			b.setDescription("ext " + i.toString());
			b.setData(ByteString.copyFrom(("data for extension " + i.toString()).getBytes()));
			extensions.add(b.build());
		}
		Mockito.doReturn(extensions).when(mockedWalletdb).getExtensions();

		try {
			Mockito.doNothing().when(mockedWalletdb).writeConfigFile(Mockito.any(ProtoConfig.AuthenticatorConfiguration.Builder.class));
			Mockito.doReturn(null).when(mockedAuthConfBuilder).clearConfigExtensions();

			// remove extension 37
			mockedWalletdb.removeExtension("37");
			ArgumentCaptor<ProtoConfig.Extenstion> arg = ArgumentCaptor.forClass(ProtoConfig.Extenstion.class);
			Mockito.verify(mockedAuthConfBuilder, Mockito.times(99)).addConfigExtensions(arg.capture());
			List<ProtoConfig.Extenstion> lst = arg.getAllValues();
			for(ProtoConfig.Extenstion ext: lst)
				assertFalse(ext.getExtentionID().equals("37"));

		} catch (IOException e) {
			e.printStackTrace();
		}

		boolean didThrow = false;
		try {
			mockedWalletdb.removeExtension(null);
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
			didThrow = true;
		}
		assertTrue(didThrow);

		didThrow = false;
		try {
			mockedWalletdb.removeExtension("");
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
			didThrow = true;
		}
		assertTrue(didThrow);
	}

	@Test
	public void addExtensionTest() {
		// walletDB methods stub
		WalletDB mockedWalletdb = Mockito.spy(new WalletDB());
		ProtoConfig.AuthenticatorConfiguration.Builder mockedAuthConfBuilder = PowerMockito.mock(ProtoConfig.AuthenticatorConfiguration.Builder.class);
		Mockito.doReturn(mockedAuthConfBuilder).when(mockedWalletdb).getConfigFileBuilder();

		try {
			Mockito.doNothing().when(mockedWalletdb).writeConfigFile(Mockito.any(ProtoConfig.AuthenticatorConfiguration.Builder.class));

			mockedWalletdb.addExtension("3", "description", "some data".getBytes());
			ArgumentCaptor<ProtoConfig.Extenstion.Builder> arg = ArgumentCaptor.forClass(ProtoConfig.Extenstion.Builder.class);
			Mockito.verify(mockedAuthConfBuilder, Mockito.atLeastOnce()).addConfigExtensions(arg.capture());
			ProtoConfig.Extenstion.Builder ext = arg.getValue();

			assertEquals(ext.getExtentionID(), "3");
			assertEquals(ext.getDescription(), "description");
			assertEquals(new String(ext.getData().toByteArray()), "some data");

		} catch (IOException e) {
			e.printStackTrace();
		}


		boolean didThrow = false;
		try {
			mockedWalletdb.addExtension(null, "description", "some data".getBytes());
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
			didThrow = true;
		}
		assertTrue(didThrow);

		didThrow = false;
		try {
			mockedWalletdb.addExtension("", "description", "some data".getBytes());
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
			didThrow = true;
		}
		assertTrue(didThrow);

		didThrow = false;
		try {
			mockedWalletdb.addExtension("3", "description", null);
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
			didThrow = true;
		}
		assertTrue(didThrow);

		didThrow = false;
		try {
			mockedWalletdb.addExtension("3", "description", "".getBytes());
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
			didThrow = true;
		}
		assertTrue(didThrow);

		didThrow = false;
		try {
			mockedWalletdb.addExtension("3", "", "some data".getBytes());
		} catch (Exception e) {
			didThrow = true;
		}
		assertFalse(didThrow);

		didThrow = false;
		try {
			mockedWalletdb.addExtension("3", null, "some data".getBytes());
		} catch (Exception e) {
			didThrow = true;
		}
		assertFalse(didThrow);
	}

	@Test
	public void updateExtensionTest() {
		// walletDB methods stub
		WalletDB mockedWalletdb = Mockito.spy(new WalletDB());
		ProtoConfig.AuthenticatorConfiguration.Builder mockedAuthConfBuilder = PowerMockito.mock(ProtoConfig.AuthenticatorConfiguration.Builder.class);
		Mockito.doReturn(mockedAuthConfBuilder).when(mockedWalletdb).getConfigFileBuilder();

		List<ProtoConfig.Extenstion> extensions = new ArrayList<>();
		for(Integer i=0; i < 100; i++ ) {
			ProtoConfig.Extenstion.Builder b = ProtoConfig.Extenstion.newBuilder();
			b.setExtentionID(i.toString());
			b.setDescription("ext " + i.toString());
			b.setData(ByteString.copyFrom(("data for extension " + i.toString()).getBytes()));
			extensions.add(b.build());
		}
		Mockito.doReturn(extensions).when(mockedWalletdb).getExtensions();

		try {
			Mockito.doNothing().when(mockedWalletdb).writeConfigFile(Mockito.any(ProtoConfig.AuthenticatorConfiguration.Builder.class));
			Mockito.doReturn(null).when(mockedAuthConfBuilder).setConfigExtensions(Mockito.anyInt(), Mockito.any(ProtoConfig.Extenstion.Builder.class));

			// update extension 37
			mockedWalletdb.updateExtension("37", "new description", "new data".getBytes());
			ArgumentCaptor<ProtoConfig.Extenstion.Builder> argBuilder = ArgumentCaptor.forClass(ProtoConfig.Extenstion.Builder.class);
			ArgumentCaptor<Integer> argIdx = ArgumentCaptor.forClass(Integer.class);
			Mockito.verify(mockedAuthConfBuilder, Mockito.atLeastOnce()).setConfigExtensions(argIdx.capture(), argBuilder.capture());
			ProtoConfig.Extenstion.Builder v = argBuilder.getValue();

			assertEquals(v.getExtentionID(), "37");
			assertEquals(v.getDescription(), "new description");
			assertEquals(new String(v.getData().toByteArray()), "new data");
		} catch (IOException e) {
			e.printStackTrace();
		}


		boolean didThrow = false;
		try {
			mockedWalletdb.updateExtension(null, "description", "some data".getBytes());
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
			didThrow = true;
		}
		assertTrue(didThrow);

		didThrow = false;
		try {
			mockedWalletdb.updateExtension("", "description", "some data".getBytes());
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
			didThrow = true;
		}
		assertTrue(didThrow);

		didThrow = false;
		try {
			mockedWalletdb.updateExtension("3", "description", null);
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
			didThrow = true;
		}
		assertTrue(didThrow);

		didThrow = false;
		try {
			mockedWalletdb.updateExtension("3", "description", "".getBytes());
		} catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
			didThrow = true;
		}
		assertTrue(didThrow);

		didThrow = false;
		try {
			mockedWalletdb.updateExtension("3", "", "some data".getBytes());
		} catch (Exception e) {
			didThrow = true;
		}
		assertFalse(didThrow);

		didThrow = false;
		try {
			mockedWalletdb.updateExtension("3", null, "some data".getBytes());
		} catch (Exception e) {
			didThrow = true;
		}
		assertFalse(didThrow);
	}

	@Test
	public void updateExtensionWithNullDescriptionTest() {
		// walletDB methods stub
		WalletDB mockedWalletdb = Mockito.spy(new WalletDB());
		ProtoConfig.AuthenticatorConfiguration.Builder mockedAuthConfBuilder = PowerMockito.mock(ProtoConfig.AuthenticatorConfiguration.Builder.class);
		Mockito.doReturn(mockedAuthConfBuilder).when(mockedWalletdb).getConfigFileBuilder();

		List<ProtoConfig.Extenstion> extensions = new ArrayList<>();
		for (Integer i = 0; i < 100; i++) {
			ProtoConfig.Extenstion.Builder b = ProtoConfig.Extenstion.newBuilder();
			b.setExtentionID(i.toString());
			b.setDescription("ext " + i.toString());
			b.setData(ByteString.copyFrom(("data for extension " + i.toString()).getBytes()));
			extensions.add(b.build());
		}
		Mockito.doReturn(extensions).when(mockedWalletdb).getExtensions();

		try {
			Mockito.doNothing().when(mockedWalletdb).writeConfigFile(Mockito.any(ProtoConfig.AuthenticatorConfiguration.Builder.class));
			Mockito.doReturn(null).when(mockedAuthConfBuilder).setConfigExtensions(Mockito.anyInt(), Mockito.any(ProtoConfig.Extenstion.Builder.class));

			// update extension 37
			mockedWalletdb.updateExtension("37", null, "new data".getBytes());
			ArgumentCaptor<ProtoConfig.Extenstion.Builder> argBuilder = ArgumentCaptor.forClass(ProtoConfig.Extenstion.Builder.class);
			ArgumentCaptor<Integer> argIdx = ArgumentCaptor.forClass(Integer.class);
			Mockito.verify(mockedAuthConfBuilder, Mockito.atLeastOnce()).setConfigExtensions(argIdx.capture(), argBuilder.capture());
			ProtoConfig.Extenstion.Builder v = argBuilder.getValue();

			assertEquals(v.getExtentionID(), "37");
			assertEquals(v.getDescription(), "");
			assertEquals(new String(v.getData().toByteArray()), "new data");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void updateNotFoundExtensionTest() {
		// should create a new extension

		// walletDB methods stub
		WalletDB mockedWalletdb = Mockito.spy(new WalletDB());
		ProtoConfig.AuthenticatorConfiguration.Builder mockedAuthConfBuilder = PowerMockito.mock(ProtoConfig.AuthenticatorConfiguration.Builder.class);
		Mockito.doReturn(mockedAuthConfBuilder).when(mockedWalletdb).getConfigFileBuilder();

		List<ProtoConfig.Extenstion> extensions = new ArrayList<>();
		for(Integer i=0; i < 100; i++ ) {
			ProtoConfig.Extenstion.Builder b = ProtoConfig.Extenstion.newBuilder();
			b.setExtentionID(i.toString());
			b.setDescription("ext " + i.toString());
			b.setData(ByteString.copyFrom(("data for extension " + i.toString()).getBytes()));
			extensions.add(b.build());
		}
		Mockito.doReturn(extensions).when(mockedWalletdb).getExtensions();

		try {
			Mockito.doNothing().when(mockedWalletdb).writeConfigFile(Mockito.any(ProtoConfig.AuthenticatorConfiguration.Builder.class));

			mockedWalletdb.updateExtension("101", "description", "some data".getBytes());
			ArgumentCaptor<ProtoConfig.Extenstion.Builder> arg = ArgumentCaptor.forClass(ProtoConfig.Extenstion.Builder.class);
			Mockito.verify(mockedAuthConfBuilder, Mockito.atLeastOnce()).addConfigExtensions(arg.capture());
			ProtoConfig.Extenstion.Builder ext = arg.getValue();

			assertEquals(ext.getExtentionID(), "101");
			assertEquals(ext.getDescription(), "description");
			assertEquals(new String(ext.getData().toByteArray()), "some data");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static int randInt(int min, int max) {

		// NOTE: Usually this should be a field rather than a method
		// variable so that it is not re-seeded every call.
		Random rand = new Random();

		// nextInt is normally exclusive of the top value,
		// so add 1 to make it inclusive
		int randomNum = rand.nextInt((max - min) + 1) + min;

		return randomNum;
	}
}
