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

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.jdbc.datasource.DelegatingDataSource

import javax.sql.DataSource
import java.lang.reflect.*
import java.sql.*

/**
 * A {@link DataSource} wrapper used exclusively for isolating end-to-end (Cypress) tests. It is only installed when
 * {@code otp.testing.endpoints.enabled=true} (see {@code resources.groovy} / {@link PinningDataSourceConnectionSourceFactory});
 * in production it is never installed and therefore adds no overhead whatsoever.
 *
 * <p>It wraps the data source that GORM's connection source hands to Hibernate, so that GORM/Hibernate AND any direct
 * {@code new Sql(dataSource)} usage all go through it. While a test transaction is active (opened via {@link #beginPage()}),
 * every {@link #getConnection()} call returns a proxy over one single, shared, pinned physical connection whose
 * {@code commit()}, {@code close()}, {@code setAutoCommit(true)} and (no-arg) {@code rollback()} calls are suppressed.
 * As a result all database work runs inside one long-lived, uncommitted transaction that is discarded completely rather
 * than committed — this is what gives each Cypress spec a clean database without a full restore.</p>
 *
 * <p>The proxy does NOT serialize access itself: a single OTP request can touch the database from several threads at
 * once (e.g. {@code parallelStream()} in validators), and serializing borrows here would deadlock (the request thread
 * would hold the lock while waiting for its own worker threads). Instead we rely on the JDBC driver, which already
 * serializes individual statements on one physical connection. Everything ends up in the one outer transaction and is
 * discarded together, so interleaving between those threads is harmless for test isolation. Cypress runs requests
 * serially, so cross-request contention does not arise.</p>
 *
 * <p>There are exactly two, reset-first nesting levels, matching the two Cypress boundaries:</p>
 * <ul>
 *   <li>{@link #beginPage()} (page / spec level) discards any transaction left open by a previous spec and opens a
 *   fresh outer transaction. It is self-healing: a spec that crashed without cleaning up is reconciled here.</li>
 *   <li>{@link #beginTest()} (test level) keeps a single savepoint on top of the page transaction and rolls back to it
 *   on every call, so each test starts from the same page-seed state.</li>
 * </ul>
 *
 * <p>Both operations reset before they open, so there is no separate "rollback"/"close" entry point: the next
 * {@code begin*} call performs the cleanup. The last spec's transaction is simply discarded (never committed) when the
 * connection is finally released or reset.</p>
 */
@Slf4j
class PinningDataSource extends DelegatingDataSource {

    /**
     * The single active instance (for the default data source), so {@code TestTransactionService} can reach it without
     * being coupled to the Spring wiring. Only one is ever installed (OTP has a single data source).
     */
    static volatile PinningDataSource activeInstance

    /** Guards {@link #beginPage()} / {@link #beginTest()} so the pinned connection is only ever set up or reset once at a time. */
    private final Object lock = new Object()

    /** The real, pooled connection held open (uncommitted) for the duration of a spec's page transaction, or null when inactive. */
    private volatile Connection pinnedConnection

    /**
     * The single test-level savepoint on top of the page transaction, or null when no test layer has been opened yet
     * (or after {@link #beginPage()} reset the transaction). Access is always guarded by {@link #lock}.
     */
    private Savepoint testSavepoint

    PinningDataSource(DataSource targetDataSource) {
        super(targetDataSource)
        activeInstance = this
    }

    static PinningDataSource getActiveInstance() {
        return activeInstance
    }

    boolean isActive() {
        return pinnedConnection != null
    }

    /**
     * Page level (level 1), reset-first: discard any transaction left open by a previous spec (for example one that
     * crashed without cleaning up), then borrow a single real connection straight from the underlying pool (bypassing
     * any lazy/transaction-aware proxies) and hold it open with auto-commit disabled. From now until the next
     * {@link #beginPage()} all application connections are routed through this one pinned connection.
     */
    void beginPage() {
        synchronized (lock) {
            discardActiveTransaction()
            Connection connection = underlyingDataSource().connection
            connection.autoCommit = false
            pinnedConnection = connection
            testSavepoint = null
            log.info("Opened pinned page test transaction.")
        }
    }

    /**
     * Test level (level 2), reset-first: on top of the page transaction, keep one savepoint and roll back to it so each
     * test starts from the same page-seed state. The first call creates the savepoint; every later call rolls back to
     * it (PostgreSQL keeps a savepoint after ROLLBACK TO SAVEPOINT, so it is reused, not recreated).
     */
    void beginTest() {
        synchronized (lock) {
            if (pinnedConnection == null) {
                throw new TestTransactionException("beginTest() called without an active page transaction; call beginPage() first.")
            }
            if (testSavepoint == null) {
                testSavepoint = pinnedConnection.setSavepoint()
                log.info("Opened test savepoint on the pinned page transaction.")
            } else {
                pinnedConnection.rollback(testSavepoint)
                log.info("Rolled back to the test savepoint (reset for the next test).")
            }
        }
    }

    /**
     * Roll back and release the pinned connection if one is open. Best-effort: the connection is discarded regardless,
     * so a failure in any cleanup step must not prevent {@link #beginPage()} from opening a fresh transaction.
     */
    private void discardActiveTransaction() {
        Connection connection = pinnedConnection
        if (connection == null) {
            return
        }
        pinnedConnection = null
        testSavepoint = null
        try {
            connection.rollback()
        } catch (SQLException e) {
            // Reset-first cleanup: this connection is being discarded and its uncommitted work dies with it, so a failed
            // rollback must not block opening a fresh page transaction. Log and carry on.
            log.warn("Could not roll back the previous pinned test transaction while resetting; discarding it anyway.", e)
        } finally {
            try {
                connection.autoCommit = true
            } catch (SQLException e) {
                // Cosmetic on a connection we are about to close(); log rather than mask the reset.
                log.warn("Could not restore auto-commit on the discarded pinned connection.", e)
            }
            try {
                connection.close()
            } catch (SQLException e) {
                log.warn("Could not close the discarded pinned connection.", e)
            }
        }
    }

    @Override
    Connection getConnection() throws SQLException {
        Connection connection = pinnedConnection
        return connection != null ? suppress(connection) : super.connection
    }

    @Override
    Connection getConnection(String username, String password) throws SQLException {
        Connection connection = pinnedConnection
        return connection != null ? suppress(connection) : super.getConnection(username, password)
    }

    /** Wrap the pinned connection so callers cannot commit, close, roll it back, or re-enable auto-commit on it. */
    private static Connection suppress(Connection connection) {
        InvocationHandler handler = new PinnedConnectionInvocationHandler(connection)
        return (Connection) Proxy.newProxyInstance(PinningDataSource.classLoader, [Connection] as Class[], handler)
    }

    /** Unwrap any {@link DelegatingDataSource} layers (lazy / transaction-aware proxies) to reach the real pool. */
    private DataSource underlyingDataSource() {
        DataSource dataSource = targetDataSource
        while (DelegatingDataSource.isInstance(dataSource)) {
            dataSource = ((DelegatingDataSource) dataSource).targetDataSource
        }
        return dataSource
    }

    /**
     * Intercepts calls on a borrowed connection to keep the outer test transaction open and undivertable: the
     * application can neither commit it, close it, roll it back, nor re-enable auto-commit. Only the testing endpoints
     * (via {@link PinningDataSource#beginPage()} / {@link PinningDataSource#beginTest()}) may end or reset it.
     */
    @CompileStatic
    private static class PinnedConnectionInvocationHandler implements InvocationHandler {

        private final Connection connection

        PinnedConnectionInvocationHandler(Connection connection) {
            this.connection = connection
        }

        @Override
        Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            switch (method.name) {
                case "close":
                    // keep the physical connection open and pinned for the rest of the test
                    return null
                case "commit":
                    // the application "commits", but the change only becomes part of the still-open outer transaction
                    return null
                case "rollback":
                    if (args == null || args.length == 0) {
                        // never let the application roll back the whole outer transaction; only the testing endpoints may
                        return null
                    }
                    // rollback(Savepoint) is the application's own nested savepoint handling: let it through
                    break
                case "setAutoCommit":
                    // never let the application re-enable auto-commit and thereby commit the outer transaction
                    return null
                case "getAutoCommit":
                    return false
                case "isClosed":
                    return false
                default:
                    break
            }
            try {
                return method.invoke(connection, args)
            } catch (InvocationTargetException e) {
                // surface the real underlying exception (e.g. SQLException) so callers/Spring react to it correctly
                throw e.targetException
            }
        }
    }
}
