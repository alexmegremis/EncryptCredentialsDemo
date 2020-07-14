import javax.crypto.*;
import javax.crypto.spec.*;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.spec.AlgorithmParameterSpec;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlainEncryption {

    public static final String TRANSFORM_ALGO = "PBEWithMD5AndTripleDES";
    public static final String TO_BE_ENCRYPTED_REGEX = ".*(###(.*)###)$";

    public static final Pattern      TO_BE_ENCRYPTED_TOKEN_PATTERN = Pattern.compile(TO_BE_ENCRYPTED_REGEX);
    public static final List<String> ENCRTYPTED_LINES              = new ArrayList<>();
    public static final String PBE_ALGO = "PBE";

    // This weakens our output, but it's an acceptable compromise.
    private static final String SALT = "1@ds#&6f";

    public static final AlgorithmParameterSpec PBE_PARAM = new PBEParameterSpec(SALT.getBytes(), 65536);

    public static SecretKeyFactory secretKeyFactory = null;
    public static SecretKey        secretKey        = null;
    public static PBEKeySpec       secretKeySpec    = null;

    public static boolean didMatch = false;

    public static void main(String[] args) throws Exception {

        String unencryptedFilePath = "unencrypted.txt";
        if (args != null && args.length > 0) {
            unencryptedFilePath = args[0];
        }
        File cryptoKeyFile   = new File("key.txt");
        File unencryptedFile = new File(unencryptedFilePath);

        // Validate
        if (! cryptoKeyFile.exists()) {
            throw new IllegalArgumentException("File " + cryptoKeyFile.getName() + " must be present and contain the one-line crypto key in the first line.");
        }
        if (! unencryptedFile.exists()) {
            throw new IllegalArgumentException("File " + unencryptedFilePath + " must be present with the plaintext content.");
        }

        // Prepare output
        String encryptedFilePath = unencryptedFilePath.replaceAll(unencryptedFile.getName(), "ENCRYPTED_" + unencryptedFile.getName());
        File   encryptedFile     = new File(encryptedFilePath);
        if (encryptedFile.exists()) {
            System.out.println(">>> Deleting existing file " + encryptedFilePath);
            encryptedFile.delete();
        }

        // Init some crypto
        secretKeySpec = new PBEKeySpec(Files.readAllLines(cryptoKeyFile.toPath()).get(0).toCharArray(), SALT.getBytes(), 65536, 256);
        secretKeyFactory = SecretKeyFactory.getInstance(PBE_ALGO);
        secretKey = secretKeyFactory.generateSecret(secretKeySpec);
        secretKey = new SecretKeySpec(secretKey.getEncoded(), PBE_ALGO);

        // Do encrypt.
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
            throw new IllegalArgumentException("File " + unencryptedFilePath + " did not have any lines that match the regex " + TO_BE_ENCRYPTED_REGEX);
        }

        // Output
        StringBuilder sb = new StringBuilder();
        ENCRTYPTED_LINES.stream().forEach(sb :: append);
        FileOutputStream fos = new FileOutputStream(encryptedFile.toPath().toString());
        for (String o : ENCRTYPTED_LINES) {
            fos.write((o + '\n').getBytes());
        }
        fos.close();
    }

    public static final void encryptLine(final String unencryptedLine) throws Exception {
        Matcher matcher = TO_BE_ENCRYPTED_TOKEN_PATTERN.matcher(unencryptedLine);

        if (matcher.matches()) {
            didMatch = true;

            // This should never happen, our regex runs greedy to end of line.
            if (matcher.groupCount() != 2) {
                throw new IllegalStateException("Each line should match the regex " + TO_BE_ENCRYPTED_REGEX);
            }

            // DEC{aPassword}
            String delimitedUnencryptedText = matcher.group(1);
            // aPassword
            String unencryptedText = matcher.group(2);

            String encryptedText = getEncrypted(unencryptedText);
            String decryptedText = getDecrypted(encryptedText);

            if(encryptedText.equals(decryptedText)) {
                throw new IllegalStateException("No encryption took place.");
            }
            if(!unencryptedText.equals(decryptedText)) {
                throw new IllegalStateException("Encrypted text could not be test decrypted successfully.");
            }

            String outputLine = unencryptedLine.substring(0, unencryptedLine.length() - delimitedUnencryptedText.length())
                                        + "ENC{" + encryptedText + "}";
            ENCRTYPTED_LINES.add(outputLine);
        } else {
            ENCRTYPTED_LINES.add(unencryptedLine);
        }
    }

    public static final String getEncrypted(final String unencrypted) throws Exception {

        Cipher cipher = Cipher.getInstance(TRANSFORM_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, PBE_PARAM);

        byte[] encryptedBytes = cipher.doFinal(unencrypted.getBytes());

        Base64.Encoder encoder               = Base64.getEncoder();
        byte[]         encodedEncryptedBytes = encoder.encode(encryptedBytes);
        String         encryptedText         = new String(encodedEncryptedBytes, StandardCharsets.UTF_8);

        return encryptedText;
    }

    public static final String getDecrypted(final String encrypted) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORM_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, PBE_PARAM);

        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encrypted.getBytes()));
        String decryptedText  = new String(decryptedBytes);

        return decryptedText;
    }
}