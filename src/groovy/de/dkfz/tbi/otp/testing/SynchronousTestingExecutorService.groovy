package de.dkfz.tbi.otp.testing

import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.FutureTask

/**
 * Simple Wrapper to provide an executor service which executes synchronously <strong>on
 * the same thread</strong>. It is used during Integration Tests and not meant
 * for productive usage.
 */
class SynchronousTestingExecutorService {
    @Delegate grails.plugin.executor.PersistenceContextExecutorWrapper trueExecutorService

    public <T> Future<T> submit(Callable<T> callable) {
        FutureTask<T> value = new FutureTask<T>(callable)
        value.run()
        value.get()
        return value
    }

}
