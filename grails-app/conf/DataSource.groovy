import de.dkfz.tbi.otp.config.*
import org.hibernate.dialect.*

import static java.util.concurrent.TimeUnit.*

Properties otpProperties = ConfigService.parsePropertiesFile()

String server = otpProperties.getProperty(OtpProperty.DATABASE_SERVER.key)
String port = otpProperties.getProperty(OtpProperty.DATABASE_PORT.key)
String database = otpProperties.getProperty(OtpProperty.DATABASE_SCHEMA.key)

dataSource {
    pooled = true
    driverClassName = "org.postgresql.Driver"
    dialect = PostgreSQL9Dialect
    dbCreate = otpProperties.getProperty(OtpProperty.DATABASE_CREATE.key)
    username = otpProperties.getProperty(OtpProperty.DATABASE_USERNAME.key)
    password = otpProperties.getProperty(OtpProperty.DATABASE_PASSWORD.key)
    url = "jdbc:postgresql://${server}:${port}/${database}"
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
        //noinspection GroovyAssignabilityCheck
        dataSource {
            //the properties are described on http://tomcat.apache.org/tomcat-7.0-doc/jdbc-pool.html
            properties {
                maxActive = 100                                         //max parallel connection
                maxIdle = 50                                            //max parallel idle connection
                minIdle = 25                                            //min idle connection
                maxAge = HOURS.toMillis(1)                              //the time after which a connection will be closed
                minEvictableIdleTimeMillis = MINUTES.toMillis(5)        //minimum time a connection need to be idle before remove

                maxWait = SECONDS.toMillis(10)                          //max time a request wait for a free connection

                testWhileIdle = true                                    //test idle connection
                timeBetweenEvictionRunsMillis = MINUTES.toMillis(1)     //how often idle connections are checked

                testOnBorrow = true                                     //validate connection before return
                validationInterval = SECONDS.toMillis(10)               //min interval of check a connection on borrow

                validationQuery = "SELECT 1"                            //validation query
                //comment out because 9.1.21 does not support the option
                //validationQueryTimeout = SECONDS.toSeconds(5)           //timeout in seconds for validation query

                logValidationErrors = true                              //log errors during validation
            }
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
            jmxExport = true  // FIXME: contradicts documentation. do we need that?
            String phase = System.properties['grails.test.phase']
            // NOTE: FUNCTIONAL, BUT DISABLED UNTIL ALL TESTS ARE FIXED. ACTIVATE BY DELETING "false &&". (AND REMOVE THIS NOTE.)
            if (false && phase == 'integration') {
                // Setup the project name for use by the Continuous Integration server the prevent race conditions.
                String projectName = System.getenv('BUILD_NUMBER') ?
                        "--project-name otptest-build-${System.getenv('BUILD_NUMBER')}" : ''
                // Where is docker-compose?
                String dockerCompose = '/usr/bin/which docker-compose'.execute()?.text
                if (!dockerCompose) {
                    throw new IllegalStateException('Unable to find docker-compose. Is it installed and available via PATH?')
                }
                // Construct command string
                String dc = "${dockerCompose} ${projectName} --file docker/otp-test/docker-compose.yml"
                // Stop and destroy all remaining containers. Will break if you re-use BUILD_NUMBER. Don't do that!
                "${dc} stop".execute().waitFor()
                "${dc} rm -f".execute().waitFor()
                // Start a fresh container
                if ("${dc} up -d".execute().waitFor() != 0) {
                    throw new IllegalStateException('Unable to start new database container.')
                }
                // Get the (dynamically assigned local) port
                def dockerProcess = "${dc} port ${phase} 5432".execute()
                // If we do not get a reply in time or the return code indicates a failure, fail.
                if (!dockerProcess.waitFor(5, SECONDS) || dockerProcess.exitValue() != 0) {
                    throw new IllegalStateException('Unable to get port information from Docker. Is the test database container running?')
                }
                String dockerPort = dockerProcess.text.split(':').last().trim() // should work for IPv6 also
                driverClassName = 'org.postgresql.Driver'
                dialect = PostgreSQL9Dialect
                dbCreate = 'update'
                username = 'postgres'
                password = ''
                url = "jdbc:postgresql://localhost:${dockerPort}/postgres"
            } else {
                driverClassName = 'org.h2.Driver'
                dialect = H2Dialect
                username = 'sa'
                password = ''
                dbCreate = 'update'
                url = 'jdbc:h2:mem:testDb;MVCC=TRUE;MODE=PostgreSQL;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE'
            }
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
            dialect = H2Dialect
            username = "sa"
            password = ""
            dbCreate = "update"
            url = "jdbc:h2:mem:testDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
        }
    }
}
