package com.alexmegremis.encryptdemo;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.spec.AlgorithmParameterSpec;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Configuration
public class MultiConfig implements ApplicationContextAware {

    public static final String  TRANSFORM_ALGO                = "PBEWithMD5AndTripleDES";
    public static final String  TO_BE_DECRYPTED_REGEX         = ".*(ENC\\{(.*)\\})$";
    public static final String  PBE_ALGO                      = "PBE";
    public static final Pattern TO_BE_DECRYPTED_TOKEN_PATTERN = Pattern.compile(TO_BE_DECRYPTED_REGEX);

    // This weakens our output, but it's an acceptable compromise.
    private static final String SALT = "1@ds#&6f";

    public static final AlgorithmParameterSpec PBE_PARAM = new PBEParameterSpec(SALT.getBytes(), 65536);

    public static SecretKeyFactory secretKeyFactory = null;
    public static SecretKey        secretKey        = null;
    public static PBEKeySpec       secretKeySpec    = null;
    public static Cipher           cipher           = null;

    @Getter
    @Setter
    private ApplicationContext applicationContext;

    @Bean
    public PropertySourcesPlaceholderConfigurer properties() throws Exception {

        initCrypto();

        YamlPropertiesFactoryBean            yamlProperties = new YamlPropertiesFactoryBean();
        PropertySourcesPlaceholderConfigurer properties     = new PropertySourcesPlaceholderConfigurer();

        String         activeProfile;
        final String[] activeProfiles = applicationContext.getEnvironment().getActiveProfiles();
        if (ArrayUtils.isNotEmpty(activeProfiles)) {
            activeProfile = activeProfiles[0];
        } else {
            activeProfile = "test";
        }

        String profileSpecificApplicationPropertyName = "application-" + activeProfile + ".yml";

        Resource application         = new ClassPathResource("application.yml");
        Resource applicationProfiled = new ClassPathResource(profileSpecificApplicationPropertyName);

        List<Resource> yamlLocationsList = new ArrayList<>();

        yamlLocationsList.add(application);

        if (applicationProfiled.exists()) {
            yamlLocationsList.add(applicationProfiled);
        } else {
            log.warn(">>> Properties file {} is MISSING from classpath");
        }

        yamlProperties.setResources(yamlLocationsList.toArray(new Resource[0]));
        yamlProperties.afterPropertiesSet();

        yamlProperties.getObject();

        Properties encryptedProperties = yamlProperties.getObject();
        Properties decryptedProperties = new Properties();

        for (Object key : encryptedProperties.keySet()) {
            Object decryptedValue = getDecrypted(encryptedProperties.get(key));
            decryptedProperties.put(key, decryptedValue);
        }

        properties.setProperties(decryptedProperties);
        properties.setTrimValues(true);
        return properties;
    }

    private void initCrypto() throws Exception {
        final String cryptoKeyString = applicationContext.getEnvironment().getProperty("CRYPTO_KEY");
        // Init some crypto
        cipher = Cipher.getInstance(TRANSFORM_ALGO);
        secretKeySpec = new PBEKeySpec(cryptoKeyString.toCharArray(), SALT.getBytes(), 65536, 256);
        secretKeyFactory = SecretKeyFactory.getInstance(PBE_ALGO);
        secretKey = secretKeyFactory.generateSecret(secretKeySpec);
        secretKey = new SecretKeySpec(secretKey.getEncoded(), PBE_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, PBE_PARAM);
    }

    public static Object getDecrypted(final Object encrypted) throws Exception {

        Object  result          = encrypted;
        String  encryptedString = result.toString();
        Matcher matcher         = TO_BE_DECRYPTED_TOKEN_PATTERN.matcher(encryptedString);

        if (matcher.matches()) {
            // This should never happen, our regex runs greedy to end of line.
            if (matcher.groupCount() != 2) {
                throw new IllegalStateException("Each encrypted line should match the regex " + TO_BE_DECRYPTED_REGEX);
            }

            String encryptedValue = matcher.group(2);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedValue.getBytes()));
            result = new String(decryptedBytes);
        }

        return result;
    }
}
