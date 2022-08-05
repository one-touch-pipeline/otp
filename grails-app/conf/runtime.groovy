/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import org.hibernate.dialect.H2Dialect
import org.hibernate.dialect.PostgreSQL9Dialect

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.config.OtpProperty

import static java.util.concurrent.TimeUnit.*

Properties otpProperties = ConfigService.parsePropertiesFile()

// set per-environment serverURL stem for creating absolute links
environments {
    development {
        grails.logging.jul.usebridge = true
        grails.serverURL = otpProperties.getProperty(OtpProperty.CONFIG_SERVER_URL.key) ?: "http://localhost:8080"
    }
    production {
        grails.logging.jul.usebridge = false
        grails.serverURL = otpProperties.getProperty(OtpProperty.CONFIG_SERVER_URL.key)
        grails.plugin.springsecurity.secureChannel.useHeaderCheckChannelSecurity = Boolean.parseBoolean(otpProperties.getProperty(OtpProperty.CONFIG_SECURECHANNEL_HEADERCHECK.key))
    }
    WORKFLOW_TEST {
        grails.serverURL = "http://localhost"
    }
    test {
        grails.serverURL = "http://example.com"
    }
}

// ldap
if (!Boolean.parseBoolean(otpProperties.getProperty(OtpProperty.LDAP_ENABLED.key))) {
    grails.plugin.springsecurity.providerNames = [
            'daoAuthenticationProvider',
            'anonymousAuthenticationProvider',
    ]
} else {
    grails.plugin.springsecurity.providerNames = [
            'ldapDaoAuthenticationProvider',
            'anonymousAuthenticationProvider',
    ]
    if (otpProperties.getProperty(OtpProperty.LDAP_MANAGER_DN.key)) {
        grails.plugin.springsecurity.ldap.context.managerDn     = otpProperties.getProperty(OtpProperty.LDAP_MANAGER_DN.key)
    }
    grails.plugin.springsecurity.ldap.context.managerPassword = otpProperties.getProperty(OtpProperty.LDAP_MANAGER_PASSWORD.key)
    if (otpProperties.getProperty(OtpProperty.LDAP_MANAGER_PASSWORD.key)) {
        grails.plugin.springsecurity.ldap.auth.useAuthPassword = true
    } else {
        grails.plugin.springsecurity.ldap.auth.useAuthPassword = false
    }
    grails.plugin.springsecurity.ldap.context.server            = otpProperties.getProperty(OtpProperty.LDAP_SERVER.key)
    grails.plugin.springsecurity.ldap.search.base               = otpProperties.getProperty(OtpProperty.LDAP_SEARCH_BASE.key)
    grails.plugin.springsecurity.ldap.authorities.searchSubtree = otpProperties.getProperty(OtpProperty.LDAP_SEARCH_SUBTREE.key)
    grails.plugin.springsecurity.ldap.search.filter             = "(${otpProperties.getProperty(OtpProperty.LDAP_SEARCH_ATTRIBUTE.key)}={0})"

    // static options
    grails.plugin.springsecurity.ldap.authorities.ignorePartialResultException = true
    grails.plugin.springsecurity.ldap.authorities.retrieveGroupRoles = false
    grails.plugin.springsecurity.ldap.authorities.retrieveDatabaseRoles = true
    grails.plugin.springsecurity.ldap.mapper.userDetailsClass = 'inetOrgPerson'
}

String server = otpProperties.getProperty(OtpProperty.DATABASE_SERVER.key)
String port = otpProperties.getProperty(OtpProperty.DATABASE_PORT.key)
String database = otpProperties.getProperty(OtpProperty.DATABASE_SCHEMA.key)

dataSource {
    driverClassName = "org.postgresql.Driver"
    dialect = PostgreSQL9Dialect
    username = otpProperties.getProperty(OtpProperty.DATABASE_USERNAME.key)
    password = otpProperties.getProperty(OtpProperty.DATABASE_PASSWORD.key)
    url = "jdbc:postgresql://${server}:${port}/${database}"
    dbCreate = "none"
}

hibernate {
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
           // logSql = true
           // formatSql = true
        }
    }
    test {
        hibernate {
            cache.use_second_level_cache = false
            cache.use_query_cache = false
        }
        dataSource {
            logSql = false
            jmxExport = true
            String usePostgresDocker = System.properties['usePostgresDocker']

            if ("TRUE".equalsIgnoreCase(usePostgresDocker)) {
                String workerId = System.properties['org.gradle.test.worker']
                String dockerPorts = System.properties['dockerPorts']
                String maxParallelForks = System.properties['maxParallelForks']
                String dockerPort = dockerPorts.split(',')[(workerId as int) % (maxParallelForks as int)]

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
                url = 'jdbc:h2:mem:testDb;MODE=PostgreSQL;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE'
            }
        }
    }
    WORKFLOW_TEST {
        hibernate {
            //queries = false
            cache.use_second_level_cache = false
            cache.use_query_cache = false
        }
        dataSource {
            //jmxExport = true
            //pooled = true
            driverClassName = "org.h2.Driver"
            dialect = H2Dialect
            username = "sa"
            password = ""
            dbCreate = "update"
            url = "jdbc:h2:mem:testDb;MODE=PostgreSQL;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
        }
    }
}

// mail config
grails.mail.host = otpProperties.getProperty(OtpProperty.CONFIG_EMAIL_SERVER.key) ?: OtpProperty.CONFIG_EMAIL_SERVER.defaultValue
grails.mail.port = (otpProperties.getProperty(OtpProperty.CONFIG_EMAIL_PORT.key) ?: OtpProperty.CONFIG_EMAIL_PORT.defaultValue) as int
grails.mail.username = otpProperties.getProperty(OtpProperty.CONFIG_EMAIL_USERNAME.key) ?: OtpProperty.CONFIG_EMAIL_USERNAME.defaultValue
grails.mail.password = otpProperties.getProperty(OtpProperty.CONFIG_EMAIL_PASSWORD.key) ?: OtpProperty.CONFIG_EMAIL_PASSWORD.defaultValue

// WARNING: This setting (same as this entire application.groovy) has no effect on unit tests. See:
// * OTP-1126
// * http://grails.1312388.n4.nabble.com/unit-testing-grails-gorm-failOnError-true-td4231435.html
// * http://grails.1312388.n4.nabble.com/Unit-testing-with-failOnError-true-td2718543.html
grails.gorm.failOnError=true

// Shared constraints
grails.gorm.default.constraints = {
    greaterThanZero validator: { val, obj ->
        if (val <= 0) {
            return "validator.greater.than.zero"
        }
    }
    pathComponent validator: { val, obj ->
        if (val && !de.dkfz.tbi.otp.utils.validation.OtpPathValidator.isValidPathComponent(val)) {
            return "validator.path.component"
        }
    }
    relativePath validator: { val, obj ->
        if (val && !de.dkfz.tbi.otp.utils.validation.OtpPathValidator.isValidRelativePath(val)) {
            return "validator.relative.path"
        }
    }
    absolutePath validator: { val, obj ->
        if (val && !de.dkfz.tbi.otp.utils.validation.OtpPathValidator.isValidAbsolutePath(val)) {
            return "validator.absolute.path"
        }
    }
}
grails.gorm.default.mapping = {
    id generator:'sequence'
}
