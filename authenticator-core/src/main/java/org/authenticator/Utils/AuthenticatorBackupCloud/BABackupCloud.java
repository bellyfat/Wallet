package org.authenticator.Utils.AuthenticatorBackupCloud;

import org.authenticator.hierarchy.BAHierarchy;

import javax.crypto.SecretKey;

/**
 * This class is a communicator class with the backup cloud service for restoring the wallet's metadata.<br>
 *
 * <b>Problem:</b> bitcoin authenticator uses an bip44 like hierarchy of accounts to handle its keys.<br>
 * An account can be a standard Pay-To_PubHash account where redeeming coins require only a single key<br>
 * pair or a paired account which is a Pay-To-Hash account where redeeming coins require both the wallet’s<br>
 * key pair and a second device's (the authenticator) key pair, other account types could be created in the future.<br>
 * Such a scheme creates a problem when the wallet needs to be restored only from its seed,<br>
 * the user needs to remember the precise index of the different accounts and the exact paired devices<br>
 * (account-authenticator pair).<br><br>
 *
 * <b>Non scalable work around:</b> the user is required to remember the indexes and reconstruct
 *all accounts when restoring from seed.<br><br>
 *
 * <b>Cloud Solution:</b> an anonymous cloud back-up scheme that stores all the necessary wallet’s meta data,<br>
 * encrypted, automatically. The solution works as follows:<br>
 * <ol>
 *     <li>the user logs in to the services using a user name and a password</li>
 *     <li>A new wallet is being created for the user (a user can have several wallets associated with its user ID)</li>
 *      <li>AES encryption key generated using {@link org.authenticator.Utils.CryptoUtils#authenticatorAESEncryption(String AESHexKey, String salt, String accountIndex, String seed) authenticatorAESEncryption}</li>
 *      <li>A dump of all the accounts + pairing data is serialised to a byte array (the payload) + checksum</li>
 *      <li>AES encryption on (payload | checksum), denominated encrypted_payload</li>
 *      <li>the encrypted_payload is posted to the cloud service</li>
 *      <li>every change in the account hierarchy will trigger an update which will run through stages 4-7.</li>
 * </ol>
 * <br>
 * When the user restores his wallet he will be asked to login using his user name and password. Once the user is verified, an http get request with the wallet’s id will provide all the necessary data to completely restore the wallet’s hierarchy without the need of re-pairing any of the accounts.
 *
 * Created by alonmuroch on 1/8/15.
 */
public class BABackupCloud {
    final private String CLOUD_URL_API = "http://127.0.0.1:8000/api/";

    public BABackupCloud() {

    }

    byte[] getDataDump() {
        return "".getBytes();
    }

    SecretKey getEncryptionKey(BAHierarchy hierarchy) {

    }
}
