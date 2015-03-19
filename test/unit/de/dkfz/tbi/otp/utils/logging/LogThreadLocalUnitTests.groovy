package de.dkfz.tbi.otp.utils.logging

import org.junit.Test

class LogThreadLocalUnitTests {

    @Test
    void testWithThreadLog() {
        String message = 'Test log message'
        StringBuilder out = new StringBuilder()

        LogThreadLocal.withThreadLog(out, {
            LogThreadLocal.threadLog.info(message)
        })

        assert out.toString() == "[INFO] ThreadLog - ${message}\n"
        assert LogThreadLocal.threadLog == null
    }
}
