dataSource {
    pooled = true
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
            dbCreate = "update" // one of 'create', 'create-drop', 'update', 'validate', ''
            //url = "jdbc:h2:mem:devDb"
            username = "otp"
            password = "otp"
            url = "jdbc:postgresql://localhost:5432/otp"
			//url = "jdbc:mysql://localhost/test"
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
            // TODO: make configurable
            driverClassName = "org.postgresql.Driver"
            dialect = org.hibernate.dialect.PostgreSQLDialect
            dbCreate = "update"
            username = "otp"
            password = "otp"
            url = "jdbc:postgresql://localhost:5432/otp"
        }
    }
}
