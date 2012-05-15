Properties databaseProperties = new Properties()
try {
    databaseProperties.load(new FileInputStream(System.getProperty("user.home") + "/.otp.properties"))
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
    // no database configured yet, use hsqldb
    databaseProperties.setProperty("otp.database.driver", "org.hsqldb.jdbcDriver")
    databaseProperties.setProperty("otp.database.username", "sa")
    databaseProperties.setProperty("otp.database.password", "")
    databaseProperties.setProperty("otp.database.url", "jdbc:hsqldb:mem:devDb")
    databaseProperties.setProperty("otp.database.pooled", "false")
    databaseProperties.setProperty("otp.database.dbCreate", "create")
    databaseProperties.setProperty("otp.database.dialect", "org.hibernate.dialect.HSQLDialect")
}
def databaseConfig = new ConfigSlurper().parse(databaseProperties)

dataSource {
    pooled = Boolean.parseBoolean(databaseConfig.otp.database.pooled)
    driverClassName = "org.h2.Driver" 
	//driverClassName = "com.mysql.jdbc.Driver"
    //dialect = org.hibernate.dialect.MySQL5InnoDBDialect
    username = "sa"
    password = ""
}

hibernate {
    cache.use_second_level_cache = true
    cache.use_query_cache = true
    cache.provider_class = 'net.sf.ehcache.hibernate.EhCacheProvider'
}
// environment specific settings
environments {
    development {
        dataSource {
            driverClassName = "org.postgresql.Driver"
            dialect = org.hibernate.dialect.PostgreSQLDialect
			//loggingSql = true
            dbCreate = databaseConfig.otp.database.dbCreate
            username = databaseConfig.otp.database.username
            password = databaseConfig.otp.database.password
            url = databaseConfig.otp.database.url
        }
    }
    test {
        dataSource {
            dbCreate = "update"
            url = "jdbc:h2:mem:testDb"
        }
    }
    production {
        dataSource {
            driverClassName = "org.postgresql.Driver"
            dialect = org.hibernate.dialect.PostgreSQLDialect
            dbCreate = databaseConfig.otp.database.dbCreate
            username = databaseConfig.otp.database.username
            password = databaseConfig.otp.database.password
            url = databaseConfig.otp.database.url
        }
    }
}
