package de.dkfz.tbi

import java.util.concurrent.Callable

import org.junit.*
import org.junit.runners.model.MultipleFailureException

/**
 * A default base class for test cases. This provides some helper methods.
 *
 */
class TestCase extends GroovyTestCase {

    final List<Throwable> failures = []

    static Random random = new Random()

    // Implements OTP-686.
    /**
     * Performs an assertion and collects the {@link AssertionError} if the assertion fails. Then
     * continues regardless of whether the assertion failed or not.
     * <p>
     * The collected errors will finally be thrown by {@link #throwFailures()}, which is
     * automatically called after the test method returns.
     * <p>
     * Usage example: <code>assertAndContinue { assert 1 + 1 == 3 }</code>
     * @see <a href="http://jfunc.sourceforge.net/examples.html#failures">Something similar from JFunc</a>
     */
    protected void assertAndContinue(final Callable<Void> closure) {
        try {
            if (closure() != null) {
                throw new Error('The closure passed to assertAndContinue returned a value. ' +
                    'Did you forget the assert keyword in the closure?')
            }
        } catch (final AssertionError error) {
            failures << error
        }
    }

    @After
    void throwFailures() {
        if (!failures.empty) {
            throw new MultipleFailureException(failures)
        }
    }

    public static void assertEquals(final GString expected, final String actual) {
        Assert.assertEquals(expected.toString(), actual)
    }

    public static String getUniqueString() {
        return "${System.currentTimeMillis()}-${sprintf('%016X', random.nextLong())}"
    }

    public static File getUniqueNonExistentPath() {
        return new File("/dev/null/otp-test/${uniqueString}")
    }

    /**
     * You should delete the returned directory using Groovy's File.deleteDir() method as
     * soon as you do not need it anymore.
     */
    public static File createEmptyTestDirectory() {
        final File dir = new File("/tmp/otp-test/${uniqueString}")
        assert dir.mkdirs()  // This will fail if the directory already exists or if it could not be created.
        return dir
    }
}
