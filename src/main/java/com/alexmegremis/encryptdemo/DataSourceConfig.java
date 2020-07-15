package com.alexmegremis.encryptdemo;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.lang.reflect.Member;

@Configuration
public class DataSourceConfig {

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    @Primary
    @ConfigurationProperties ("com.alexmegremis.datasource.mariadb")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    @ConfigurationProperties ("com.alexmegremis.datasource.mariadb.configuration")
    public DataSource dataSource() {
        DataSourceProperties dataSourceProperties = dataSourceProperties();
        applicationContext.getEnvironment();
        return dataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Primary
    @Bean (name = "entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean memberEntityManagerFactory(EntityManagerFactoryBuilder builder) {
        return builder.dataSource(dataSource()).packages(Member.class).build();
    }

    @Primary
    @Bean
    public PlatformTransactionManager memberTransactionManager(final @Qualifier ("entityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory.getObject());
    }
}
