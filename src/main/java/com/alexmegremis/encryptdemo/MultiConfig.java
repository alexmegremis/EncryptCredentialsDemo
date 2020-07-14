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

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Configuration
public class MultiConfig implements ApplicationContextAware {

    public static final String  patternText = ".*(__DEC\\{(.*)\\})$";
    public static final Pattern pattern     = Pattern.compile(patternText);

    @Getter
    @Setter
    private ApplicationContext applicationContext;

    @Bean
    public PropertySourcesPlaceholderConfigurer properties() throws IOException {
        YamlPropertiesFactoryBean            yamlProperties = new YamlPropertiesFactoryBean();
        PropertySourcesPlaceholderConfigurer properties     = new PropertySourcesPlaceholderConfigurer();

        String activeProfile;
        final String[] activeProfiles = applicationContext.getEnvironment().getActiveProfiles();
        if (ArrayUtils.isNotEmpty(activeProfiles)) {
            activeProfile = activeProfiles[0];
        } else {
            activeProfile = "test";
        }

        String profileSpecificApplicationPropertyName = "application-" + activeProfile + ".yml";

        Resource application = new ClassPathResource("application.yml");
        Resource applicationProfiled = new ClassPathResource(profileSpecificApplicationPropertyName);

        List<Resource> yamlLocationsList = new ArrayList<>();

        yamlLocationsList.add(application);

        if(applicationProfiled.exists()) {
            yamlLocationsList.add(applicationProfiled);
        } else {
            log.warn(">>> Properties file {} is MISSING from classpath");
        }

        yamlProperties.setResources(yamlLocationsList.toArray(new Resource[0]));
        yamlProperties.afterPropertiesSet();

        yamlProperties.getObject();

        Properties encryptedProperties = yamlProperties.getObject();
        Properties decryptedProperties = new Properties();

        encryptedProperties.keySet().stream().forEach(k -> {
            String value = (String) encryptedProperties.get(k);
        });

        properties.setProperties(yamlProperties.getObject());
        properties.setTrimValues(true);
        return properties;
    }
}
