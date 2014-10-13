package de.dkfz.tbi

import java.util.concurrent.Callable

import org.junit.*
import org.junit.runners.model.MultipleFailureException
import org.springframework.validation.Errors
import org.springframework.validation.FieldError

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

    /**
     * Returns whether two collections contain the same set of elements. Each collection must not contain any two
     * elements which are equal.
     */
    public static <T> boolean containSame(final Collection<? extends T> c1, final Collection<? extends T> c2) {
        final c1Set = c1.toSet()
        assert c1Set.size() == c1.size() : "c1 contains elements which are equal."
        final c2Set = c2.toSet()
        assert c2Set.size() == c2.size() : "c2 contains elements which are equal."
        return c1Set == c2Set
    }

    /**
     * Helper to check that a validation failed for the given constraint on the given field with the given value.
     * The assert fails if
     * <ul>
     * <li>The validate method return true</li>
     * <li>There is more then one error</li>
     * <li>The error is not of type {@link FieldError} </li>
     * <li>The {@link FieldError} is not caused by the given field</li>
     * <li>The {@link FieldError} is not caused by the given constraint</li>
     * <li>The {@link FieldError} is not caused by the given value</li>
     * </ul>
     *
     * @param objectToCheck the object which validate should be failed
     * @param fieldToCheck the name of the property the check fails for
     * @param failedConstraint the name of the constraint the check fails for
     * @param rejectedValue the value which is rejected by the check
     */
    static void assertValidateError(def objectToCheck, String failedField, String failedConstraint, def rejectedValue) {
        assert !objectToCheck.validate()
        Errors errors = objectToCheck.errors
        assert errors.errorCount == 1
        assert errors.fieldErrorCount == 1
        FieldError fieldError = errors.fieldError
        assert fieldError.field == failedField
        assert fieldError.code == failedConstraint
        assert fieldError.rejectedValue == rejectedValue
    }    
}
