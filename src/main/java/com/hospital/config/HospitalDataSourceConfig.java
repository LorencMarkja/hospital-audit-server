package com.hospital.config;

import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Defines the H2 datasource for the application.
 * JPA will create the schema.
 */
@DataSourceDefinition(
        name = "java:jboss/datasources/HospitalDS",
        className = "org.h2.jdbcx.JdbcDataSource",
        url = "jdbc:h2:file:./data/hospital;AUTO_SERVER=TRUE;MODE=MySQL;DB_CLOSE_DELAY=-1",
        user = "sa",
        password = "sa"
)
@WebListener
public class HospitalDataSourceConfig implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // schema will be created by JPA (see persistence.xml)
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
