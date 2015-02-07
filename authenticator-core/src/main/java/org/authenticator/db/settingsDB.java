package org.authenticator.db;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration;
import org.authenticator.protobuf.ProtoSettings.BitcoinUnit;
import org.authenticator.protobuf.ProtoSettings.ConfigSettings;
import org.authenticator.protobuf.ProtoSettings.Languages;

public class SettingsDB extends DBBase {

	public SettingsDB(String filePath) throws IOException {
		super(filePath);
	}

	@Override
	public byte[] dumpToByteArray() {
		return getSettingsBuilder().build().toByteArray();
	}

	@Override
	public String dumpKey() { return "SettingsDb.db.authenticator.org"; }

	@Override
	public void restoreFromBytes(byte[] data) throws IOException {
		ConfigSettings settings = ConfigSettings.parseFrom(data);
		writeSettingsFile(settings.toBuilder());
	}
	
	//#####################################
	//
	//				General
	//
	//#####################################

	
	private synchronized AuthenticatorConfiguration.Builder getConfigFileBuilder() {
		AuthenticatorConfiguration.Builder auth = AuthenticatorConfiguration.newBuilder();
		try{ auth.mergeDelimitedFrom(new FileInputStream(filePath)); }
		catch(Exception e)
		{ 
			e.printStackTrace();
		}
		
		return auth;
	}
	
	private synchronized ConfigSettings.Builder getSettingsBuilder(){
		return getConfigFileBuilder().getConfigSettingsBuilder();
	}

	private synchronized void writeConfigFile(AuthenticatorConfiguration.Builder auth) throws IOException{
		FileOutputStream output = new FileOutputStream(filePath);  
		auth.build().writeDelimitedTo(output);          
		output.close();
	}

	private synchronized void writeSettingsFile(ConfigSettings.Builder settings) throws IOException{
		AuthenticatorConfiguration.Builder auth = getConfigFileBuilder();
		
		FileOutputStream output = new FileOutputStream(filePath);  
		auth.setConfigSettings(settings.build());
		auth.build().writeDelimitedTo(output);          
		output.close();
	}
	
	//#####################################
	//
	//				General
	//
	//#####################################
	
	public void setAccountUnit(BitcoinUnit value) throws IOException{
		ConfigSettings.Builder s = getSettingsBuilder();
		s.setAccountUnit(value);
		writeSettingsFile(s);
	}
	
	public BitcoinUnit getAccountUnit() throws IOException{
		ConfigSettings.Builder s = getSettingsBuilder();
		return s.getAccountUnit();
	}
	
	public void setDecimalPoint(int value) throws IOException{
		ConfigSettings.Builder s = getSettingsBuilder();
		s.setDecimalPoints(value);
		writeSettingsFile(s);
	}
	
	public int getDecimalPoint() throws IOException{
		ConfigSettings.Builder s = getSettingsBuilder();
		return s.getDecimalPoints();
	}
	
	public void setLocalCurrencySymbol(String value) throws IOException{
		ConfigSettings.Builder s = getSettingsBuilder();
		s.setLocalCurrencySymbol(value);
		writeSettingsFile(s);
	}
	
	public String getLocalCurrencySymbol() throws IOException{
		ConfigSettings.Builder s = getSettingsBuilder();
		return s.getLocalCurrencySymbol();
	}
	
	public void setLanguage(Languages value) throws IOException{
		ConfigSettings.Builder s = getSettingsBuilder();
		s.setLanguage(value);
		writeSettingsFile(s);
	}
	
	public Languages getLanguage() throws IOException{
		ConfigSettings.Builder s = getSettingsBuilder();
		return s.getLanguage();
	}
	
	public void setDefaultFee(long value) throws IOException{
		ConfigSettings.Builder s = getSettingsBuilder();
		s.setDefaultFee(value);
		writeSettingsFile(s);
	}
	
	public long getDefaultFee() throws IOException{
		ConfigSettings.Builder s = getSettingsBuilder();
		return s.getDefaultFee();
	}
	
	public void setIsUsingTOR(boolean value) throws IOException{
		ConfigSettings.Builder s = getSettingsBuilder();
		s.setTOR(value);
		writeSettingsFile(s);
	}
	
	public boolean getIsUsingTOR() throws IOException{
		ConfigSettings.Builder s = getSettingsBuilder();
		return s.getTOR();
	}
	
	public void setIsConnectingToLocalHost(boolean value) throws IOException{
		ConfigSettings.Builder s = getSettingsBuilder();
		s.setConnectOnLocalHost(value);
		writeSettingsFile(s);
	}
	
	public boolean getIsConnectingToLocalHost() throws IOException{
		ConfigSettings.Builder s = getSettingsBuilder();
		return s.getConnectOnLocalHost();
	}
	
	public void setIsConnectingToTrustedPeer(boolean value, String peerIP) throws IOException{
		ConfigSettings.Builder s = getSettingsBuilder();
		s.setConnectToTrustedPeer(value);
		s.setTrustedPeerIP(peerIP);
		writeSettingsFile(s);
	}
	
	public void setNotConnectingToTrustedPeer() throws IOException{
		ConfigSettings.Builder s = getSettingsBuilder();
		s.setConnectToTrustedPeer(false);
		s.setTrustedPeerIP("");
		writeSettingsFile(s);
	}
	
	public boolean getIsConnectingToTrustedPeer() throws IOException{
		ConfigSettings.Builder s = getSettingsBuilder();
		return s.getConnectToTrustedPeer();
	}
	
	public String getTrustedPeerIP() throws IOException{
		ConfigSettings.Builder s = getSettingsBuilder();
		return s.getTrustedPeerIP();
	}
	
	public void setBloomFilterFalsePositiveRate(float value) throws IOException{
		ConfigSettings.Builder s = getSettingsBuilder();
		s.setBloomFilterFalsePositiveRate(value);
		writeSettingsFile(s);
	}
	
	public float getBloomFilterFalsePositiveRate() throws IOException{
		ConfigSettings.Builder s = getSettingsBuilder();
		return s.getBloomFilterFalsePositiveRate();
	}
	
	public void setIsPortForwarding(boolean value) throws IOException{
		ConfigSettings.Builder s = getSettingsBuilder();
		s.setPortForwarding(value);
		writeSettingsFile(s);
	}
	
	public boolean getIsPortForwarding() throws IOException{
		ConfigSettings.Builder s = getSettingsBuilder();
		return s.getPortForwarding();
	}

	public boolean getShouldBackupToCloud() {
		ConfigSettings.Builder s = getSettingsBuilder();
		return s.getBackupToCloud();
	}

	public void setShouldBackupToCloud(boolean value) throws IOException {
		ConfigSettings.Builder s = getSettingsBuilder();
		s.setBackupToCloud(value);
		writeSettingsFile(s);
	}
}
