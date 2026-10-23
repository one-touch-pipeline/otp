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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.datasource.DelegatingDataSource
import spock.lang.IgnoreIf
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.SeqType

import javax.sql.DataSource
import java.sql.SQLException

@Rollback
@Integration
class ConsoleRoutingDataSourceIntegrationSpec extends Specification {

    /** PostgreSQL error code for a statement canceled, e.g. by statement_timeout */
    private static final String QUERY_CANCELED = "57014"

    /** Upper bound of borrows until the pool hands out a returned connection again, larger than the default pool size */
    private static final int MAX_BORROWS_TO_REUSE = 20

    @Autowired
    DataSource dataSource

    void cleanup() {
        ConsoleDataSourceContext.clear()
    }

    void "the dataSource bean is backed by the console routing data source"() {
        expect:
        routingDataSource != null
    }

    void "Hibernate, outside of the console, uses a connection of the default pool"() {
        given:
        ConsoleRoutingDataSource routing = routingDataSource

        when:
        List<Integer> activeConnections = activeConnectionsDuringQuery(routing)

        then:
        activeConnections == [1, 0]
    }

    void "Hibernate, inside the console, uses a connection of the console pool"() {
        given:
        ConsoleRoutingDataSource routing = routingDataSource
        ConsoleDataSourceContext.markConsole()

        when:
        List<Integer> activeConnections = activeConnectionsDuringQuery(routing)

        then:
        activeConnections == [0, 1]
    }

    @IgnoreIf({ !(System.getProperty("databaseForIntegrationTest") in ["DOCKER", "DOCKER_CI"]) })
    void "the statement timeout of the console pool is read from consoleDataSource.poolProperties"() {
        given:
        ConsoleRoutingDataSource routing = routingDataSource

        when:
        String defaultTimeout = queryString(routing, "show statement_timeout")
        ConsoleDataSourceContext.markConsole()
        String consoleTimeout = queryString(routing, "show statement_timeout")

        then:
        defaultTimeout == "0"
        consoleTimeout == "1s"
    }

    @IgnoreIf({ !(System.getProperty("databaseForIntegrationTest") in ["DOCKER", "DOCKER_CI"]) })
    void "a statement in the console pool is canceled after the statement timeout"() {
        given:
        ConsoleRoutingDataSource routing = routingDataSource
        ConsoleDataSourceContext.markConsole()

        when:
        queryString(routing, "select pg_sleep(3)")

        then:
        SQLException e = thrown()
        e.class.name == "org.postgresql.util.PSQLException"
        e.SQLState == QUERY_CANCELED
    }

    @IgnoreIf({ !(System.getProperty("databaseForIntegrationTest") in ["DOCKER", "DOCKER_CI"]) })
    void "a statement_timeout set by a console script is reset when the connection is borrowed again"() {
        given:
        ConsoleRoutingDataSource routing = routingDataSource
        ConsoleDataSourceContext.markConsole()
        String backendPid = routing.connection.withCloseable { connection ->
            connection.createStatement().withCloseable { it.execute("SET statement_timeout = 0") }
            return connection.createStatement().withCloseable { statement ->
                statement.executeQuery("select pg_backend_pid()").withCloseable { resultSet ->
                    resultSet.next()
                    return resultSet.getString(1)
                }
            }
        }

        when: "borrowing until the same connection is handed out again"
        List<String> timeoutOfSameConnection = (1..MAX_BORROWS_TO_REUSE).findResults {
            List<String> pidAndTimeout = queryStrings(routing, "select pg_backend_pid(), current_setting('statement_timeout')")
            return pidAndTimeout[0] == backendPid ? pidAndTimeout[1] : null
        }

        then:
        timeoutOfSameConnection
        timeoutOfSameConnection.every { it == "1s" }
    }

    /**
     * Runs the query on a connection of the pool selected by the routing data source, bypassing the connection bound to
     * the test transaction.
     *
     * @return the first column of the first row
     */
    private static String queryString(ConsoleRoutingDataSource routing, String sql) {
        return queryStrings(routing, sql).first()
    }

    /**
     * Like {@link #queryString}, but returns all columns of the first row.
     */
    private static List<String> queryStrings(ConsoleRoutingDataSource routing, String sql) {
        return routing.connection.withCloseable { connection ->
            connection.createStatement().withCloseable { statement ->
                statement.executeQuery(sql).withCloseable { resultSet ->
                    resultSet.next()
                    return (1..resultSet.metaData.columnCount).collect { int column -> resultSet.getString(column) }
                }
            }
        }
    }

    /**
     * @return the active connections of the [default, console] pool while a query runs in a new Hibernate session
     */
    private static List<Integer> activeConnectionsDuringQuery(ConsoleRoutingDataSource routing) {
        List<Integer> activeConnections = []
        SeqType.withNewSession { session ->
            session.doWork { connection ->
                connection.createStatement().withCloseable { it.execute("select 1") }
                activeConnections = [routing.defaultDataSource.numActive, routing.consoleDataSource.numActive]
            }
        }
        return activeConnections
    }

    private ConsoleRoutingDataSource getRoutingDataSource() {
        DataSource current = dataSource
        while (current instanceof DelegatingDataSource) {
            current = ((DelegatingDataSource) current).targetDataSource
        }
        return current as ConsoleRoutingDataSource
    }
}
