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
			//loggingSql = true
            dbCreate = "update" // one of 'create', 'create-drop', 'update', 'validate', ''
            //url = "jdbc:h2:mem:devDb" 
			url = "jdbc:h2:~/h2/ngsdata"
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
            dbCreate = "update"
            url = "jdbc:h2:prodDb"
            // For MySQL production scenarios enable the following settings
//          pooled = true
//          properties {
//               minEvictableIdleTimeMillis=1800000
//               timeBetweenEvictionRunsMillis=1800000
//               numTestsPerEvictionRun=3
//               testOnBorrow=true
//               testWhileIdle=true
//               testOnReturn=true
//               validationQuery="SELECT 1"
//          }
        }
    }
}
