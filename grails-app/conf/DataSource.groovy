Properties databaseProperties = new Properties()
String propertiesFile = System.getenv("OTP_PROPERTIES")
if (propertiesFile && new File(propertiesFile).canRead()) {
    databaseProperties.load(new FileInputStream(propertiesFile))
} else {
    databaseProperties.load(new FileInputStream(System.getProperty("user.home") + "/.otp.properties"))
}
String server = databaseProperties.getProperty("otp.database.server")
String port = databaseProperties.getProperty("otp.database.port")
String database = databaseProperties.getProperty("otp.database.database")
databaseProperties.setProperty("otp.database.url", "jdbc:postgresql://${server}:${port}/${database}")
databaseProperties.setProperty("otp.database.pooled", "true")
// If dbCreate option is not set in the properties file, set it to validate
if (databaseProperties.getProperty("otp.database.dbCreate") == null) {
    databaseProperties.setProperty("otp.database.dbCreate", "validate")
}

def databaseConfig = new ConfigSlurper().parse(databaseProperties)
dataSource {
    pooled = Boolean.parseBoolean(databaseConfig.otp.database.pooled)
    driverClassName = "org.postgresql.Driver"
    dialect = org.hibernate.dialect.PostgreSQL9Dialect
    dbCreate = databaseConfig.otp.database.dbCreate
    username = databaseConfig.otp.database.username
    password = databaseConfig.otp.database.password
    url = databaseConfig.otp.database.url
}

hibernate {
    cache.use_second_level_cache = true
    cache.use_query_cache = true
    cache.region.factory_class = 'org.hibernate.cache.ehcache.SingletonEhCacheRegionFactory'
    singleSession = true // configure OSIV singleSession mode
    flush.mode = 'manual' // OSIV session flush mode outside of transactional context
}

// environment specific settings
environments {
    // Everything is set in general data source
    production {
        dataSource {
        }
    }
    // Everything is set in general data source
    development {
        dataSource {
            //loggingSql = true
        }
    }
    test {
        hibernate {
            cache.use_second_level_cache = false
            cache.use_query_cache = false
        }
        dataSource {
            pooled = true
            jmxExport = true
            driverClassName = "org.h2.Driver"
            dialect = org.hibernate.dialect.H2Dialect
            username = "sa"
            password = ""
            dbCreate = "update"
            url = "jdbc:h2:mem:testDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
        }
    }
    WORKFLOW_TEST {
        hibernate {
            cache.use_second_level_cache = false
            cache.use_query_cache = false
        }
        dataSource {
            pooled = true
            jmxExport = true
            driverClassName = "org.h2.Driver"
            dialect = org.hibernate.dialect.H2Dialect
            username = "sa"
            password = ""
            dbCreate = "update"
            url = "jdbc:h2:mem:testDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
        }
    }
}
