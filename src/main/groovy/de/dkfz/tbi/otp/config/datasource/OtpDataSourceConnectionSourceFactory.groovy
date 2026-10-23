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

import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.jdbc.connections.CachedDataSourceConnectionSourceFactory
import org.grails.datastore.gorm.jdbc.connections.DataSourceConnectionSource
import org.grails.datastore.gorm.jdbc.connections.DataSourceSettings
import org.grails.datastore.mapping.core.connections.ConnectionSource
import org.springframework.context.EnvironmentAware
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.EnumerablePropertySource
import org.springframework.core.env.Environment
import org.springframework.core.env.PropertySource

import javax.sql.DataSource

/**
 * Replacement for GORM's {@code dataSourceConnectionSourceFactory} (installed by {@link OtpDataSourceRegistrar}).
 *
 * <p>For the default connection source it builds two pools from the same {@link DataSourceSettings}: the default pool
 * and a restricted pool for the groovy web console, whose pool properties are overridden by
 * {@value #CONSOLE_POOL_PROPERTIES} (see {@code runtime.groovy}). Both are combined by a
 * {@link ConsoleRoutingDataSource}, which is then wrapped by GORM's usual lazy / transaction-aware proxies. So Hibernate,
 * the {@code dataSource} bean and transactions all work on the routing data source, while the physical connection
 * comes from the pool selected for the current thread.</p>
 *
 * <p>Without console overrides (e.g. H2 in tests) the console pool is identical to the default pool.</p>
 */
@Slf4j
class OtpDataSourceConnectionSourceFactory extends CachedDataSourceConnectionSourceFactory implements EnvironmentAware {

    static final String CONSOLE_POOL_PROPERTIES = "consoleDataSource.poolProperties"

    static final String CONNECTION_PROPERTIES = "connectionProperties"

    private final Object cacheLock = new Object()
    private final Map<String, ConnectionSource<DataSource, DataSourceSettings>> routedConnectionSources = [:]

    private ConfigurableEnvironment environment

    @Override
    void setEnvironment(Environment environment) {
        // Spring Boot always provides a ConfigurableEnvironment
        this.environment = (ConfigurableEnvironment) environment
    }

    @Override
    ConnectionSource<DataSource, DataSourceSettings> create(String name, DataSourceSettings settings) {
        if (name != ConnectionSource.DEFAULT || settings.jndiName) {
            return super.create(name, settings)
        }
        synchronized (cacheLock) {
            ConnectionSource<DataSource, DataSourceSettings> existing = routedConnectionSources.get(name)
            if (existing == null) {
                existing = createRouted(name, settings)
                routedConnectionSources.put(name, existing)
            }
            return existing
        }
    }

    private ConnectionSource<DataSource, DataSourceSettings> createRouted(String name, DataSourceSettings settings) {
        Map consoleOverrides = consolePoolProperties
        DataSource defaultPool = super.create("${name}.defaultPool".toString(), poolSettings(settings, [:])).source
        DataSource consolePool = super.create("${name}.consolePool".toString(), poolSettings(settings, consoleOverrides)).source
        DataSource routing = new ConsoleRoutingDataSource(defaultPool, consolePool)
        log.info("Installed routing data source for the groovy web console; console pool overrides: ${withoutSecrets(consoleOverrides)}")
        return new DataSourceConnectionSource(name, proxy(routing, settings), settings)
    }

    /**
     * The Spring environment only knows the flattened keys (e.g. {@code consoleDataSource.poolProperties.maxActive}),
     * so the pool properties are collected by prefix from all enumerable property sources.
     */
    private Map<String, Object> getConsolePoolProperties() {
        Map<String, Object> poolProperties = [:]
        if (environment == null) {
            return poolProperties
        }
        String prefix = "${CONSOLE_POOL_PROPERTIES}."
        environment.propertySources.each { PropertySource<?> source ->
            if (source instanceof EnumerablePropertySource) {
                ((EnumerablePropertySource<?>) source).propertyNames.findAll { String name -> name.startsWith(prefix) }.each { String name ->
                    poolProperties.putIfAbsent(name.substring(prefix.length()), environment.getProperty(name))
                }
            }
        }
        return poolProperties
    }

    /**
     * Settings for a raw pool: same connection as {@code settings}, merged pool properties, and without GORM's proxies,
     * which are applied once around the routing data source instead.
     */
    static DataSourceSettings poolSettings(DataSourceSettings settings, Map overrides) {
        return new DataSourceSettings().tap {
            url = settings.url
            driverClassName = settings.driverClassName
            username = settings.username
            password = settings.password
            pooled = settings.pooled
            readOnly = settings.readOnly
            type = settings.type
            lazy = false
            transactionAware = false
            properties = mergePoolProperties(settings.properties, overrides)
        }
    }

    /**
     * Overrides replace base values, except {@value #CONNECTION_PROPERTIES}: they are appended, so the override wins for
     * the same key (tomcat-jdbc reads them as {@link Properties}) and other driver properties are kept.
     */
    static Map mergePoolProperties(Map base, Map overrides) {
        Map merged = new LinkedHashMap(base ?: [:])
        overrides.each { Object key, Object value ->
            Object baseValue = merged.get(key)
            if (key == CONNECTION_PROPERTIES && baseValue) {
                merged.put(key, "${baseValue};${value}".toString())
            } else {
                merged.put(key, value)
            }
        }
        return merged
    }

    private static Map withoutSecrets(Map properties) {
        return properties.findAll { Object key, Object value -> !key.toString().toLowerCase().contains("password") }
    }
}
