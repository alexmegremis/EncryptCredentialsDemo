package com.alexmegremis.encryptdemo;

import javax.crypto.Cipher;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.security.spec.AlgorithmParameterSpec;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlainEncryption {

    public static final String                 patternText = ".*(###.*###)$";
    public static final Pattern                pattern     = Pattern.compile(patternText);
    public static final List<String>           output      = new ArrayList<>();
    public static final AlgorithmParameterSpec param       = new PBEParameterSpec("1@ds#&6f".getBytes(), 5000);

    public static String        cryptoKeyText  = null;
    public static byte[]        cryptoKeyBytes = null;
    public static SecretKeySpec cryptoKey      = null;

    public static boolean didMatch = false;

    public static void main(String[] args) throws Exception {
        File cryptoKeyFile   = new File("key.txt");
        File unencryptedFile = new File("unencrypted.txt");
        File encryptedFile   = new File("encrypted.txt");

        // Validate
        if (! cryptoKeyFile.exists()) {
            throw new IllegalArgumentException("File key.txt must be present and contain the one-line crypto key in the first line.");
        }
        if (! unencryptedFile.exists()) {
            throw new IllegalArgumentException("File unencrypted.txt must be present with the plaintext content.");
        }
        if (encryptedFile.exists()) {
            encryptedFile.delete();
        }

        // Process
        cryptoKeyText = Files.readAllLines(cryptoKeyFile.toPath()).get(0);
        cryptoKeyBytes = cryptoKeyText.getBytes();
        cryptoKey = new SecretKeySpec(cryptoKeyBytes, "PBE");

        // Doing it this way because streams can't throw exceptions.
        // /facewall
        // /facewall
        // /facewall
        List<String> allUnencryptedLines = Files.readAllLines(unencryptedFile.toPath());
        for (String line : allUnencryptedLines) {
            encryptLine(line);
        }

        // Warn if no encryption took place
        if (! didMatch) {
            throw new IllegalArgumentException("File unencrypted.txt did not have any lines that match the regex " + patternText);
        }

        // Output
        StringBuilder sb = new StringBuilder();
        output.stream().forEach(sb :: append);
        FileOutputStream fos = new FileOutputStream(encryptedFile.toPath().toString());

        for (String o : output) {
            fos.write((o + '\n').getBytes());
        }
        System.out.println(sb.toString());
    }

    public static final void encryptLine(final String unencryptedLine) throws Exception {
        Matcher matcher = pattern.matcher(unencryptedLine);

        if (matcher.matches()) {
            didMatch = true;

            // This should never happen, our regex runs greedy to end of line.
            if (matcher.groupCount() > 1) {
                throw new IllegalStateException("Each line should match the regex " + patternText);
            }

            String match = matcher.group(1);

            String encryptedText = getEncrypted(match);
            String decryptedText = getDecrypted(encryptedText);
            System.out.println(match + " -> " + encryptedText + " -> " + decryptedText);
            output.add(unencryptedLine.replaceAll(match, "ENC{" + encryptedText + "}"));
        } else {
            output.add(unencryptedLine);
        }
    }

    public static final String getEncrypted(final String unencrypted) throws Exception {

        Cipher cipher = Cipher.getInstance("PBEWithMD5AndTripleDES", "SunJCE");
        cipher.init(Cipher.ENCRYPT_MODE, cryptoKey, param);

//        byte[] unencryptedBytes = unencrypted.getBytes();
//        byte[] encryptedBytes = new byte[cipher.getOutputSize(unencryptedBytes.length)];
//        int    ctLength       = cipher.update(unencryptedBytes, 0, unencryptedBytes.length, encryptedBytes, 0);
//        ctLength += cipher.doFinal(encryptedBytes, ctLength);
        // CXdr+RqzNtwoTwpHVHimRwCEHbK4hs8o
        // XE/oey7RhfgoqhdaMYN0UbkVRgdX4KKu
        byte[] encryptedBytes = cipher.doFinal(unencrypted.getBytes());

        Base64.Encoder encoder               = Base64.getEncoder();
        byte[]         encodedEncryptedBytes = encoder.encode(encryptedBytes);
        String         encryptedText         = new String(encodedEncryptedBytes, "UTF-8");

        return encryptedText;
    }

    public static final String getDecrypted(final String encrypted) throws Exception {
        Cipher cipher = Cipher.getInstance("PBEWithMD5AndTripleDES", "SunJCE");
        cipher.init(Cipher.DECRYPT_MODE, cryptoKey, param);

        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encrypted.getBytes()));
        String decryptedText  = new String(decryptedBytes);

        return decryptedText;
    }
}