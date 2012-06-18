Properties databaseProperties = new Properties()
try {
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
    environments {
        test {
            throw new Exception("Test system")
        }
    }
} catch (Exception e) {
    // no database configured yet, use h2
    databaseProperties.setProperty("otp.database.driver", "org.h2.Driver")
    databaseProperties.setProperty("otp.database.username", "sa")
    databaseProperties.setProperty("otp.database.password", "")
    databaseProperties.setProperty("otp.database.url", "jdbc:h2:mem:devDb")
    databaseProperties.setProperty("otp.database.pooled", "true")
    databaseProperties.setProperty("otp.database.dbCreate", "update")
}
def databaseConfig = new ConfigSlurper().parse(databaseProperties)
dataSource {
    pooled = Boolean.parseBoolean(databaseConfig.otp.database.pooled)
    driverClassName = "org.postgresql.Driver"
    dialect = org.hibernate.dialect.PostgreSQLDialect
    dbCreate = databaseConfig.otp.database.dbCreate
    username = databaseConfig.otp.database.username
    password = databaseConfig.otp.database.password
    url = databaseConfig.otp.database.url
}

hibernate {
    cache.use_second_level_cache = true
    cache.use_query_cache = true
    cache.provider_class = 'net.sf.ehcache.hibernate.EhCacheProvider'
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
            driverClassName = "org.h2.Driver"
            dialect = org.hibernate.dialect.H2Dialect
            username = "sa"
            password = ""
            pooled = true
            dbCreate = "update"
            url = "jdbc:h2:mem:testDb"
        }
    }
}
