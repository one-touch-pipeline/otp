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
package de.dkfz.tbi.otp.testing

import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.jdbc.connections.CachedDataSourceConnectionSourceFactory
import org.grails.datastore.gorm.jdbc.connections.DataSourceConnectionSource
import org.grails.datastore.gorm.jdbc.connections.DataSourceSettings
import org.grails.datastore.mapping.core.connections.ConnectionSource
import org.springframework.core.env.PropertyResolver

import javax.sql.DataSource

/**
 * Drop-in replacement for GORM's {@code dataSourceConnectionSourceFactory}, installed only when
 * {@code otp.testing.endpoints.enabled=true} (see {@link PinningDataSourceRegistrar}). It wraps the {@link DataSource}
 * that GORM's connection source hands to Hibernate with a {@link PinningDataSource}.
 *
 * <p>This is the correct interception point: in Grails 6 / gorm-hibernate5 the {@code dataSource} bean and Hibernate's
 * connection source are BOTH resolved from {@code dataSourceConnectionSourceFactory.create(...).source}, so wrapping the
 * source here pins every database consumer uniformly. (Wrapping the {@code dataSource} bean alone does not, because
 * Hibernate holds the connection source's {@code source} directly, bypassing the bean.)</p>
 */
@Slf4j
class PinningDataSourceConnectionSourceFactory extends CachedDataSourceConnectionSourceFactory {

    private final Object cacheLock = new Object()
    private final Map<String, ConnectionSource<DataSource, DataSourceSettings>> pinnedConnectionSources = [:]

    @Override
    ConnectionSource<DataSource, DataSourceSettings> create(String name, PropertyResolver configuration) {
        ConnectionSource<DataSource, DataSourceSettings> existing = pinnedConnectionSources.get(name)
        if (existing != null) {
            return existing
        }
        synchronized (cacheLock) {
            existing = pinnedConnectionSources.get(name)
            return existing != null ? existing : pin(name, super.create(name, configuration))
        }
    }

    @Override
    ConnectionSource<DataSource, DataSourceSettings> create(String name, DataSourceSettings settings) {
        ConnectionSource<DataSource, DataSourceSettings> existing = pinnedConnectionSources.get(name)
        if (existing != null) {
            return existing
        }
        synchronized (cacheLock) {
            existing = pinnedConnectionSources.get(name)
            return existing != null ? existing : pin(name, super.create(name, settings))
        }
    }

    private ConnectionSource<DataSource, DataSourceSettings> pin(String name, ConnectionSource<DataSource, DataSourceSettings> original) {
        DataSource pinned = new PinningDataSource(original.source)
        ConnectionSource<DataSource, DataSourceSettings> wrapped = new DataSourceConnectionSource(name, pinned, original.settings)
        pinnedConnectionSources.put(name, wrapped)
        log.warn("Testing endpoints are ENABLED: pinned data source connection source '${name}' for Cypress test " +
                "isolation. This must never happen in production.")
        return wrapped
    }
}
