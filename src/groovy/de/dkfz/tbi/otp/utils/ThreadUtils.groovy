package de.dkfz.tbi.otp.utils

class ThreadUtils {

    /**
     * Evaluates a condition repeatedly and blocks until it becomes true or a timeout occurs.
     * @return true if the condition evaluated to true; false if timed out.
     */
    static boolean waitFor(final Closure condition, final long millisTimeout, final long millisBetweenRetries) {
        final long startTimestamp = System.currentTimeMillis()
        while (true) {
            if (condition) {
                return true
            }
            if (System.currentTimeMillis() - startTimestamp > millisTimeout) {
                return false
            }
            Thread.sleep(millisBetweenRetries)
        }
    }
}
