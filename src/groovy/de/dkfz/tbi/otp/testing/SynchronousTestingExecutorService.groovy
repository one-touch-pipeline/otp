package de.dkfz.tbi.otp.testing

import java.util.concurrent.Callable

/**
 * Simple Wrapper to provide an executor service which does not
 * create threads. It is used during Integration Tests and not meant
 * for productive usage.
 *
 */
class SynchronousTestingExecutorService {
    
    def submit(Callable callable) {
        return callable.call()
    }

}
