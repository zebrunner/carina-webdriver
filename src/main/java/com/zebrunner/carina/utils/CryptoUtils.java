package com.zebrunner.carina.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testng.SkipException;

import com.zebrunner.carina.crypto.Algorithm;
import com.zebrunner.carina.crypto.CryptoTool;
import com.zebrunner.carina.crypto.CryptoToolBuilder;

/**
 * Utility that allow to easy crypt/decrypt data<br>
 * 
 * todo move to carina-utils module
 */
public enum CryptoUtils {

    INSTANCE;

    private static final String CRYPTO_PATTERN = Configuration.get(Configuration.Parameter.CRYPTO_PATTERN);
    private CryptoTool cryptoTool = null;

    public String decryptIfEncrypted(String text) {
        Matcher cryptoMatcher = Pattern.compile(CRYPTO_PATTERN)
                .matcher(text);
        String decryptedText = text;
        if (cryptoMatcher.find()) {
            initCryptoTool();
            decryptedText = this.cryptoTool.decrypt(text, CRYPTO_PATTERN);
        }
        return decryptedText;
    }

    private void initCryptoTool() {
        if (this.cryptoTool == null) {
            String cryptoKey = Configuration.get(Configuration.Parameter.CRYPTO_KEY_VALUE);
            if (cryptoKey.isEmpty()) {
                throw new SkipException("Encrypted data detected, but the crypto key is not found!");
            }
            this.cryptoTool = CryptoToolBuilder.builder()
                    .chooseAlgorithm(Algorithm.find(Configuration.get(Configuration.Parameter.CRYPTO_ALGORITHM)))
                    .setKey(cryptoKey)
                    .build();
        }
    }
}
