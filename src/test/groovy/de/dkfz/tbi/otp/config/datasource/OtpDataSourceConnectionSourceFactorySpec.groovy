/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.config.datasource

import org.grails.datastore.gorm.jdbc.connections.DataSourceSettings
import org.grails.datastore.mapping.core.connections.ConnectionSource
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import org.springframework.jdbc.datasource.DelegatingDataSource
import spock.lang.Specification

import javax.sql.DataSource

class OtpDataSourceConnectionSourceFactorySpec extends Specification {

    private static final String CONNECTION_PROPERTIES = OtpDataSourceConnectionSourceFactory.CONNECTION_PROPERTIES

    void "mergePoolProperties, overrides replace base values and keep the others"() {
        expect:
        OtpDataSourceConnectionSourceFactory.mergePoolProperties([maxActive: 100, maxIdle: 50], [maxActive: 5]) ==
                [maxActive: 5, maxIdle: 50]
    }

    void "mergePoolProperties, connection properties of the override are appended to the base ones"() {
        expect:
        OtpDataSourceConnectionSourceFactory.mergePoolProperties(
                [(CONNECTION_PROPERTIES): "maxResultBuffer=10p;ssl=true"],
                [(CONNECTION_PROPERTIES): "maxResultBuffer=5p"],
        ) == [(CONNECTION_PROPERTIES): "maxResultBuffer=10p;ssl=true;maxResultBuffer=5p"]
    }

    void "mergePoolProperties, when base has no connection properties, uses the override"() {
        expect:
        OtpDataSourceConnectionSourceFactory.mergePoolProperties(null, [(CONNECTION_PROPERTIES): "maxResultBuffer=5p"]) ==
                [(CONNECTION_PROPERTIES): "maxResultBuffer=5p"]
    }

    void "poolSettings copies the connection, disables GORM's proxies and does not modify the original settings"() {
        given:
        DataSourceSettings settings = createSettings()
        settings.properties = [maxActive: 100]

        when:
        DataSourceSettings poolSettings = OtpDataSourceConnectionSourceFactory.poolSettings(settings, [maxActive: 5])

        then:
        poolSettings.url == settings.url
        poolSettings.driverClassName == settings.driverClassName
        poolSettings.username == settings.username
        poolSettings.password == settings.password
        !poolSettings.lazy
        !poolSettings.transactionAware
        poolSettings.properties == [maxActive: 5]
        settings.properties == [maxActive: 100]
        settings.lazy
        settings.transactionAware
    }

    void "create, for the default connection source, routes between a default and a console pool with the configured overrides"() {
        given:
        StandardEnvironment environment = new StandardEnvironment()
        String prefix = OtpDataSourceConnectionSourceFactory.CONSOLE_POOL_PROPERTIES
        Map<String, Object> config = [:]
        config.put("${prefix}.maxActive".toString(), 3)
        config.put("${prefix}.${CONNECTION_PROPERTIES}".toString(), "maxResultBuffer=5p")
        config.put("otherKey", "ignored")
        environment.propertySources.addFirst(new MapPropertySource("config", config))
        OtpDataSourceConnectionSourceFactory factory = new OtpDataSourceConnectionSourceFactory()
        factory.environment = environment
        DataSourceSettings settings = createSettings()
        settings.properties = [maxActive: 20]

        when:
        ConnectionSource<DataSource, DataSourceSettings> connectionSource = factory.create(ConnectionSource.DEFAULT, settings)

        then:
        connectionSource.settings.is(settings)

        and: "GORM's proxies wrap the routing data source"
        ConsoleRoutingDataSource routing = unwrap(connectionSource.source) as ConsoleRoutingDataSource
        routing != null
        !routing.defaultDataSource.is(routing.consoleDataSource)

        and: "only the console pool gets the overrides"
        routing.defaultDataSource.poolProperties.maxActive == 20
        routing.consoleDataSource.poolProperties.maxActive == 3

        and: "tomcat-jdbc parses the connection properties into the properties passed to the driver"
        routing.consoleDataSource.poolProperties.dbProperties.getProperty("maxResultBuffer") == "5p"
        routing.defaultDataSource.poolProperties.dbProperties.getProperty("maxResultBuffer") == null

        cleanup:
        connectionSource?.close()
    }

    void "create, without console overrides, still installs the routing data source"() {
        given:
        OtpDataSourceConnectionSourceFactory factory = new OtpDataSourceConnectionSourceFactory()
        factory.environment = new StandardEnvironment()

        when:
        ConnectionSource<DataSource, DataSourceSettings> connectionSource = factory.create(ConnectionSource.DEFAULT, createSettings())

        then:
        unwrap(connectionSource.source) instanceof ConsoleRoutingDataSource

        cleanup:
        connectionSource?.close()
    }

    void "create, when called twice, returns the same connection source"() {
        given:
        OtpDataSourceConnectionSourceFactory factory = new OtpDataSourceConnectionSourceFactory()
        DataSourceSettings settings = createSettings()

        when:
        ConnectionSource<DataSource, DataSourceSettings> first = factory.create(ConnectionSource.DEFAULT, settings)
        ConnectionSource<DataSource, DataSourceSettings> second = factory.create(ConnectionSource.DEFAULT, settings)

        then:
        first.is(second)

        cleanup:
        first?.close()
    }

    void "create, for another connection source, does not install the routing data source"() {
        given:
        OtpDataSourceConnectionSourceFactory factory = new OtpDataSourceConnectionSourceFactory()

        when:
        ConnectionSource<DataSource, DataSourceSettings> connectionSource = factory.create("other", createSettings())

        then:
        !(unwrap(connectionSource.source) instanceof ConsoleRoutingDataSource)

        cleanup:
        connectionSource?.close()
    }

    private static DataSourceSettings createSettings() {
        DataSourceSettings settings = new DataSourceSettings()
        settings.url = "jdbc:h2:mem:otpDataSourceConnectionSourceFactorySpec;DB_CLOSE_ON_EXIT=FALSE"
        settings.driverClassName = "org.h2.Driver"
        settings.username = "sa"
        settings.password = ""
        return settings
    }

    private static DataSource unwrap(DataSource dataSource) {
        DataSource current = dataSource
        while (current instanceof DelegatingDataSource) {
            current = ((DelegatingDataSource) current).targetDataSource
        }
        return current
    }
}
