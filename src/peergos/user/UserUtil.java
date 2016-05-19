package peergos.user;

import com.lambdaworks.crypto.SCrypt;
import com.sun.crypto.provider.*;
import com.sun.net.ssl.internal.ssl.Provider;
import peergos.crypto.Hash;
import peergos.crypto.TweetNaCl;
import peergos.crypto.User;
import peergos.crypto.asymmetric.curve25519.Curve25519PublicKey;
import peergos.crypto.asymmetric.curve25519.Curve25519SecretKey;
import peergos.crypto.asymmetric.curve25519.Ed25519PublicKey;
import peergos.crypto.asymmetric.curve25519.Ed25519SecretKey;
import peergos.crypto.symmetric.SymmetricKey;
import peergos.crypto.symmetric.TweetNaClKey;
import sun.security.ec.*;
import sun.security.jgss.*;
import sun.security.provider.*;
import sun.security.rsa.*;

import java.security.*;
import java.util.Arrays;

public class UserUtil {
    private static final int N = 1 << 17;
    private static byte[] generateKeys(String username, String password) {
        byte[] hash = Arrays.copyOfRange(Hash.sha256(password.getBytes()), 2, 34);
        byte[] salt = username.getBytes();
        try {
            System.out.println("Starting Scrypt key generation");
//            System.setProperty("java.vm.specification.name", "Java");
//            Security.addProvider(new Sun());
//            Security.addProvider(new SunRsaSign());
//            Security.addProvider(new SunEC());
//            Security.addProvider(new Provider());
//            Security.addProvider(new SunJCE());
//            Security.addProvider(new SunProvider());
//            Security.addProvider(new com.sun.security.sasl.Provider());
//            Security.addProvider(new XMLDSigRI());
//            Security.addProvider(new SunPCSC());
//            System.out.println("Loaded crypto providers");
            return SCrypt.scrypt(hash, salt, N, 8, 1, 96);
        } catch (GeneralSecurityException gse) {
            throw new IllegalStateException(gse);
        }
    }

    public static UserWithRoot generateUser(String username, String password) {
        byte[] keyBytes = generateKeys(username, password);

        byte[] signBytesSeed = Arrays.copyOfRange(keyBytes, 0, 32);
        byte[] secretBoxBytes = Arrays.copyOfRange(keyBytes, 32, 64);
        byte[] rootKeyBytes = Arrays.copyOfRange(keyBytes, 64, 96);

        byte[] secretSignBytes = Arrays.copyOf(signBytesSeed, 64);
        byte[] publicSignBytes = new byte[32];

        boolean isSeeded = true;
        TweetNaCl.crypto_sign_keypair(publicSignBytes, secretSignBytes, isSeeded);

        byte[] pubilcBoxBytes = new byte[32];
        TweetNaCl.crypto_box_keypair(pubilcBoxBytes, secretBoxBytes, isSeeded);

        User user = new User(
                new Ed25519SecretKey(secretSignBytes),
                new Curve25519SecretKey(secretBoxBytes),
                new Ed25519PublicKey(publicSignBytes),
                new Curve25519PublicKey(pubilcBoxBytes));

        SymmetricKey root =  new TweetNaClKey(rootKeyBytes);

        return new UserWithRoot(user, root);
    }
}
