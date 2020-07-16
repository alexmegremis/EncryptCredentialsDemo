package com.alexmegremis.encryptdemo;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.*;
import org.springframework.core.io.*;

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
    public static final String  TO_BE_DECRYPTED_TOKEN_REGEX   = ".*(ENC\\{(.*)\\})$";
    public static final String  PBE_ALGO                      = "PBE";
    public static final Pattern TO_BE_DECRYPTED_TOKEN_PATTERN = Pattern.compile(TO_BE_DECRYPTED_TOKEN_REGEX);

    @Getter
    @Setter
    private ApplicationContext applicationContext;

    @Bean
    public PropertySourcesPlaceholderConfigurer properties(ConfigurableEnvironment environment) throws Exception {

        YamlPropertiesFactoryBean            yamlProperties = new YamlPropertiesFactoryBean();
        PropertySourcesPlaceholderConfigurer result         = new PropertySourcesPlaceholderConfigurer();

        List<Resource> yamlLocationsList = getPropertyFiles();

        yamlProperties.setResources(yamlLocationsList.toArray(new Resource[0]));
        yamlProperties.afterPropertiesSet();

        Properties encryptedProperties = yamlProperties.getObject();

        MutablePropertySources propertySources = environment.getPropertySources();
        final Cipher           cipher          = initCipher();
        Map<String, Object>    map             = new HashMap<>();

        for (Object key : encryptedProperties.keySet()) {
            Object decryptedValue = getDecrypted(cipher, encryptedProperties.get(key));
            if (! encryptedProperties.get(key).equals(decryptedValue)) {
                map.put(key.toString(), decryptedValue);
            }
        }

        propertySources.addFirst(new MapPropertySource("decryptedProperties", map));

        result.setProperties(encryptedProperties);
        result.setTrimValues(true);
        return result;
    }

    public String getProfile() {
        String         result;
        final String[] activeProfiles = applicationContext.getEnvironment().getActiveProfiles();
        if (ArrayUtils.isNotEmpty(activeProfiles)) {
            result = activeProfiles[0];
        } else {
            result = "test";
        }

        return result;
    }

    public List<Resource> getPropertyFiles() {
        String activeProfile = getProfile();
        log.info(">>> Active profile is {}", activeProfile);

        String profileSpecificApplicationPropertyName = "application-" + activeProfile + ".yml";

        Resource application                 = new ClassPathResource("application.yml");
        Resource applicationProfiledInternal = new ClassPathResource(profileSpecificApplicationPropertyName);
        Resource applicationProfiledExternal = new FileSystemResource(profileSpecificApplicationPropertyName);

        List<Resource> result = new ArrayList<>();

        result.add(application);

        if (applicationProfiledInternal.exists()) {
            result.add(applicationProfiledInternal);
        } else {
            log.warn(">>> Properties file {} is MISSING from classpath", applicationProfiledInternal.getFilename());
        }
        if (applicationProfiledExternal.exists()) {
            result.add(applicationProfiledExternal);
        } else {
            log.warn(">>> Properties file {} is MISSING from filesystem", applicationProfiledExternal.getFilename());
        }

        return result;
    }

    private Cipher initCipher() throws Exception {
        Cipher result = null;

        String cryptoKeyText = applicationContext.getEnvironment().getProperty("CRYPTO_KEY");
        if (StringUtils.isEmpty(cryptoKeyText)) {
            log.error(">>> INIT: The CRYPTO_KEY env var was empty or not present.");
            return result;
        }

        result = Cipher.getInstance(TRANSFORM_ALGO);

        // Init some crypto
        final String                 salt             = "1@ds#&6f";
        final AlgorithmParameterSpec pbeParam         = new PBEParameterSpec(salt.getBytes(), 65536);
        PBEKeySpec                   secretKeySpec    = new PBEKeySpec(cryptoKeyText.toCharArray(), salt.getBytes(), 65536, 256);
        SecretKeyFactory             secretKeyFactory = SecretKeyFactory.getInstance(PBE_ALGO);
        SecretKey                    secretKey        = secretKeyFactory.generateSecret(secretKeySpec);
        secretKey = new SecretKeySpec(secretKey.getEncoded(), PBE_ALGO);
        result.init(Cipher.DECRYPT_MODE, secretKey, pbeParam);

        return result;
    }

    public static Object getDecrypted(final Cipher cipher, final Object encrypted) throws Exception {

        Object  result          = encrypted;
        String  encryptedString = result.toString();
        Matcher matcher         = TO_BE_DECRYPTED_TOKEN_PATTERN.matcher(encryptedString);

        if (matcher.matches()) {
            // This should never happen, our regex runs greedy to end of line.
            if (matcher.groupCount() != 2) {
                throw new IllegalStateException("Each encrypted line should match the regex " + TO_BE_DECRYPTED_TOKEN_REGEX);
            }

            String encryptedValue = matcher.group(2);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedValue.getBytes()));
            result = new String(decryptedBytes);
        }

        return result;
    }
}
