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
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource
import org.springframework.util.ReflectionUtils

import javax.sql.DataSource
import java.lang.reflect.Method

/**
 * Routes every connection request either to the default pool or, while {@link ConsoleDataSourceContext#isConsole()},
 * to the restricted console pool. Both pools point to the same database; only the limits differ.
 *
 * <p>Installed by {@link OtpDataSourceConnectionSourceFactory} below GORM's lazy and transaction-aware proxies, so a
 * single Hibernate session factory serves both pools.</p>
 */
@Slf4j
class ConsoleRoutingDataSource extends AbstractRoutingDataSource implements Closeable {

    static final String DEFAULT_KEY = "default"
    static final String CONSOLE_KEY = "console"

    final DataSource defaultDataSource
    final DataSource consoleDataSource

    ConsoleRoutingDataSource(DataSource defaultDataSource, DataSource consoleDataSource) {
        this.defaultDataSource = defaultDataSource
        this.consoleDataSource = consoleDataSource
        Map<Object, Object> targets = [:]
        targets.put(DEFAULT_KEY, defaultDataSource)
        targets.put(CONSOLE_KEY, consoleDataSource)
        targetDataSources = targets
        defaultTargetDataSource = defaultDataSource
        lenientFallback = false
        afterPropertiesSet()
    }

    @Override
    protected Object determineCurrentLookupKey() {
        if (ConsoleDataSourceContext.console) {
            return CONSOLE_KEY
        }
        return DEFAULT_KEY
    }

    /**
     * Closes both pools. Found and called reflectively by GORM's {@code DataSourceConnectionSource#close()} on shutdown.
     */
    @Override
    @SuppressWarnings("CatchRuntimeException") // closing must continue with the next pool, whatever the first one throws
    void close() {
        [defaultDataSource, consoleDataSource].each { DataSource dataSource ->
            Method closeMethod = ReflectionUtils.findMethod(dataSource.class, "close")
            if (closeMethod) {
                try {
                    ReflectionUtils.invokeMethod(closeMethod, dataSource)
                } catch (RuntimeException e) {
                    log.warn("Could not close data source ${dataSource}", e)
                }
            }
        }
    }
}
