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
 * {@code new Sql(dataSource)} usage all go through it. While a test transaction is active (opened via {@link #begin()}),
 * every {@link #getConnection()} call returns a proxy over one single, shared, pinned physical connection whose
 * {@code commit()}, {@code close()}, {@code setAutoCommit(true)} and (no-arg) {@code rollback()} calls are suppressed.
 * As a result all database work runs inside one long-lived, uncommitted transaction, undone completely with a single
 * {@link #rollback()} — this is what gives each Cypress spec file a clean database without a full restore.</p>
 *
 * <p>The proxy does NOT serialize access itself: a single OTP request can touch the database from several threads at
 * once (e.g. {@code parallelStream()} in validators), and serializing borrows here would deadlock (the request thread
 * would hold the lock while waiting for its own worker threads). Instead we rely on the JDBC driver, which already
 * serializes individual statements on one physical connection. Everything ends up in the one outer transaction and is
 * discarded together on rollback, so interleaving between those threads is harmless for test isolation. Cypress runs
 * requests serially, so cross-request contention does not arise.</p>
 *
 * <p>{@link #begin()} / {@link #rollback()} form a stack: the first {@code begin()} opens the outer transaction
 * (depth 1), each further {@code begin()} opens a nested savepoint (depth 2, 3, …), and {@code rollback()} unwinds the
 * innermost layer first — a nested savepoint while any are open, otherwise the whole transaction.</p>
 */
@Slf4j
class PinningDataSource extends DelegatingDataSource {

    /**
     * The single active instance (for the default data source), so {@code TestTransactionService} can reach it without
     * being coupled to the Spring wiring. Only one is ever installed (OTP has a single data source).
     */
    static volatile PinningDataSource activeInstance

    /** Guards {@link #begin()} / {@link #rollback()} so the pinned connection is only ever set up or torn down once at a time. */
    private final Object lock = new Object()

    /** The real, pooled connection held open (uncommitted) for the duration of a test transaction, or null when inactive. */
    private volatile Connection pinnedConnection

    /**
     * The stack of nested savepoints opened by repeated {@link #begin()} calls on top of the outer transaction. Empty
     * means only the outer transaction (depth 1) is open. Access is always guarded by {@link #lock}.
     */
    private final Deque<Savepoint> savepointStack = new ArrayDeque<>()

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
     * Borrow a single real connection straight from the underlying pool (bypassing any lazy/transaction-aware proxies)
     * and hold it open with auto-commit disabled. From now until {@link #rollback()} all application connections are
     * routed through this one pinned connection.
     */
    void begin() {
        synchronized (lock) {
            if (pinnedConnection == null) {
                Connection connection = underlyingDataSource().connection
                connection.autoCommit = false
                pinnedConnection = connection
                log.info("Opened pinned test transaction (nesting depth 1).")
            } else {
                // Already inside the outer transaction: open a nested layer via a savepoint. The testing endpoints are
                // excepted from the DB-touching interceptors, so no application per-request work is interleaving here.
                savepointStack.push(pinnedConnection.setSavepoint())
                log.info("Opened nested test savepoint (nesting depth ${savepointStack.size() + 1}).")
            }
        }
    }

    /**
     * Roll back the innermost layer: a nested savepoint if any are open, otherwise the whole transaction (releasing the
     * pinned connection back to the pool).
     */
    void rollback() {
        synchronized (lock) {
            if (pinnedConnection == null) {
                throw new TestTransactionException("rollback() called without an active test transaction (unbalanced begin/rollback).")
            }
            if (!savepointStack.isEmpty()) {
                Savepoint savepoint = savepointStack.pop()
                pinnedConnection.rollback(savepoint)
                try {
                    pinnedConnection.releaseSavepoint(savepoint)
                } catch (SQLException e) {
                    log.debug("Could not release savepoint after rolling back to it.", e)
                }
                log.info("Rolled back nested test savepoint (nesting depth now ${savepointStack.size() + 1}).")
                return
            }
            Connection connection = pinnedConnection
            pinnedConnection = null
            try {
                connection.rollback()
            } finally {
                try {
                    connection.autoCommit = true
                } catch (SQLException e) {
                    log.warn("Could not restore auto-commit on the pinned connection.", e)
                }
                connection.close()
                log.info("Rolled back and released pinned test transaction.")
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
     * (via {@link PinningDataSource#rollback()}) may end it.
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
