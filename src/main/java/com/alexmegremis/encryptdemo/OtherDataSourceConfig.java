package com.alexmegremis.encryptdemo;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.lang.reflect.Member;

@Configuration
public class OtherDataSourceConfig {

    @Bean
    @ConfigurationProperties ("com.alexmegremis.datasource.othermariadb")
    public DataSourceProperties otherDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties ("com.alexmegremis.datasource.othermariadb.configuration")
    public DataSource otherDataSource() {
        return otherDataSourceProperties().initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean (name = "otherEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean otherEntityManagerFactory(EntityManagerFactoryBuilder builder) {
        return builder.dataSource(otherDataSource()).packages(Member.class).build();
    }

    @Bean
    public PlatformTransactionManager otherTransactionManager(final @Qualifier ("otherEntityManagerFactory") LocalContainerEntityManagerFactoryBean otherEntityManagerFactory) {
        return new JpaTransactionManager(otherEntityManagerFactory.getObject());
    }
}