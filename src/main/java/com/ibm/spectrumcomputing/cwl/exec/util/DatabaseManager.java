/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.spectrumcomputing.cwl.exec.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import com.ibm.spectrumcomputing.cwl.model.persistence.CWLStepProcessRecord;
import com.ibm.spectrumcomputing.cwl.model.persistence.CWLMainProcessRecord;

/**
 * Holds a Hibernate session factory
 */
public class DatabaseManager {

    private final SessionFactory sessionFactory;

    /**
     * Constructs this object and create a Hibernate session factory
     */
    public DatabaseManager() {
        Metadata metadata = init(databaseConfig());
        sessionFactory = metadata.getSessionFactoryBuilder().build();
    }

    /**
     * Constructs this object and create a Hibernate session factory by a given
     * database configuration
     * 
     * @param dbConfig
     *            A given database configuration
     */
    public DatabaseManager(Properties dbConfig) {
        Metadata metadata = init(dbConfig);
        sessionFactory = metadata.getSessionFactoryBuilder().build();
    }

    /**
     * @return A Hibernate session factory
     */
    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    private Metadata init(Properties dbConfig) {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySettings(dbConfig)
                .build();
        return new MetadataSources(registry)
                .addAnnotatedClass(CWLMainProcessRecord.class)
                .addAnnotatedClass(CWLStepProcessRecord.class)
                .getMetadataBuilder()
                .applyImplicitNamingStrategy(ImplicitNamingStrategyJpaCompliantImpl.INSTANCE)
                .build();
    }

    private Properties databaseConfig() {
        Properties properties = new Properties();
        Path dbDir = Paths.get(System.getProperty("user.home"), ".cwlexec", "processesdb");
        properties.put("hibernate.connection.url", String.format("jdbc:hsqldb:file:%s", dbDir));
        properties.put("hibernate.connection.username", "sa");
        properties.put("hibernate.connection.password", "sa");
        properties.put("hibernate.connection.driver_class", "org.hsqldb.jdbcDriver");
        properties.put("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
        properties.put("hibernate.show_sql", "false");
        properties.put("hibernate.hbm2ddl.auto", "update");
        return properties;
    }
}
