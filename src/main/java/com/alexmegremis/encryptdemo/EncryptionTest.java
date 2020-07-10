package com.alexmegremis.encryptdemo;

import javax.crypto.Cipher;
import java.io.FileInputStream;
import java.security.*;
import java.security.cert.Certificate;

public class EncryptionTest {

    public static final String PLAINTEXT = "hello world";

    public static void main(String[] args) throws Exception {
        Certificate certificate = getCertificate("certs/testKeyStore.jks", "welcome1", "test", "welcome1");

        byte[] bytesEncrypted = doEncrypt(certificate, PLAINTEXT);
//        System.out.printf(PLAINTEXT + " : " + new String(bytesEncrypted));
        byte[] bytesDecrypted = doDecrypt(certificate, bytesEncrypted);
        System.out.printf(new String(bytesDecrypted) + " : " + new String(bytesDecrypted));
    }

    public static Certificate getCertificate(String keyStoreFilePath, String keyStorePassword, String privateKeyCertAlias, String privateKeyPassword) throws Exception {
        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(new FileInputStream(keyStoreFilePath), keyStorePassword.toCharArray());
        Certificate certificate = keystore.getCertificate(privateKeyCertAlias);
        return certificate;
    }

    public static byte[] doEncrypt(final Certificate certificate, final String input) throws Exception {
        Cipher encryptCipher = Cipher.getInstance("SHA-256");
        encryptCipher.init(Cipher.ENCRYPT_MODE, certificate.getPublicKey());
        byte[] result = encryptCipher.doFinal(input.getBytes());
        return result;
    }

    public static byte[] doDecrypt(final Certificate certificate, final byte[] encryptedBytes) throws Exception {
        Cipher decryptCipher = Cipher.getInstance("RSA");
        decryptCipher.init(Cipher.DECRYPT_MODE, certificate);
        byte[] result = decryptCipher.doFinal(encryptedBytes);
        return result;
    }

//    public static PrivateKey getPrivateKeyFromKeyStore(String keyStoreFilePath, String keyStorePassword, String privateKeyCertAlias, String privateKeyPassword) throws Exception {
//        PrivateKey privateKey = null;
//        KeyStore   keystore   = KeyStore.getInstance("JKS");
//        keystore.load(new FileInputStream(keyStoreFilePath), keyStorePassword.toCharArray());
//        Certificate certificate = keystore.getCertificate(privateKeyCertAlias);
//        Cipher      cipher      = Cipher.getInstance("RSA");
//        cipher.init(Cipher.DECRYPT_MODE, certificate);
//        FileInputStream encrypted      = new FileInputStream("./certs/out.txt");
//        byte[]          bytesEncrypted = encrypted.readAllBytes();
//        byte[]          bytesDecrypted = cipher.doFinal(bytesEncrypted);
//        String          plaintext      = new String(bytesDecrypted);
//
//        Key key = keystore.getKey(privateKeyCertAlias, keyStorePassword.toCharArray());
//        if (key instanceof PrivateKey) {
//            Certificate cert      = keystore.getCertificate(privateKeyCertAlias);
//            PublicKey   publicKey = cert.getPublicKey();
//            KeyPair     keyPair   = new KeyPair(publicKey, (PrivateKey) key);
//            privateKey = keyPair.getPrivate();
//        }
//        //privateKeyEncoded = encoder.encode(privateKey.getEncoded());
//        return privateKey;
//    }
}
