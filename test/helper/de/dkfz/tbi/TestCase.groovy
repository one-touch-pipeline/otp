package de.dkfz.tbi

import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.HelperUtils
import org.junit.*
import org.junit.runners.model.MultipleFailureException
import org.springframework.validation.Errors
import org.springframework.validation.FieldError

import java.nio.file.Files
import java.util.concurrent.Callable

import static de.dkfz.tbi.otp.utils.LocalShellHelper.executeAndWait

/**
 * A default base class for test cases. This provides some helper methods.
 */
class TestCase {

    /**
     * @see #createEmptyTestDirectory()
     * @see #cleanTestDirectory()
     */
    private static final File TEST_DIRECTORY
    static {
        String tmpdir = System.getProperty('java.io.tmpdir')
        assert tmpdir
        TEST_DIRECTORY = new File(tmpdir, HelperUtils.uniqueString)
        assert TEST_DIRECTORY.isAbsolute()
    }
    private static boolean cleanTestDirectoryShutdownHookInstalled = false

    final List<Throwable> failures = []

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

    static void assertEquals(final GString expected, final String actual) {
        Assert.assertEquals(expected.toString(), actual)
    }

    static String createUniqueString() {
        return HelperUtils.getUniqueString()
    }

    static File getUniqueNonExistentPath() {
        return new File("/dev/null/otp-test/${HelperUtils.uniqueString}")
    }

    /**
     * You should delete the returned directory using Groovy's File.deleteDir() method as
     * soon as you do not need it anymore, or delete all test directories using {@link #cleanTestDirectory()}.
     */
    @Deprecated
    static File createEmptyTestDirectory() {
        if (!cleanTestDirectoryShutdownHookInstalled) {
            addShutdownHook { cleanTestDirectory() }
            cleanTestDirectoryShutdownHookInstalled = true
        }
        final File dir = new File(TEST_DIRECTORY, HelperUtils.uniqueString)
        assert dir.mkdirs()  // This will fail if the directory already exists or if it could not be created.
        return dir
    }

    @Deprecated
    static boolean cleanTestDirectory() {
        return TEST_DIRECTORY.deleteDir()
    }

    /**
     * @see CollectionUtils#containSame(java.util.Collection, java.util.Collection)
     */
    @Deprecated
    static <T> boolean containSame(final Collection<? extends T> c1, final Collection<? extends T> c2) {
        return CollectionUtils.containSame(c1, c2)
    }

    static <T> boolean assertContainSame(final Collection<? extends T> c1, final Collection<? extends T> c2) {
        if (!CollectionUtils.containSame(c1, c2)) {
            Set c1Set = c1.toSet()
            Set c2Set = c2.toSet()
            Set c3Set = c1.toSet()
            c1Set.removeAll(c2)
            c2Set.removeAll(c1)
            c3Set.removeAll(c1Set)
            throw new AssertionError("\nIn c1, but not in c2:\n${c1Set*.toString().sort().join('\n')}\nIn c2, but not in c1:\n${c2Set*.toString().sort().join('\n')}\nin both:\n${c3Set*.toString().sort().join('\n')}\n")
        }
        return true
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

    static void assertAtLeastExpectedValidateError(def objectToCheck, String failedField, String failedConstraint, def rejectedValue) {
        assert !objectToCheck.validate()
        assert objectToCheck.errors.fieldErrors.any {
            it.field == failedField && it.code == failedConstraint && it.rejectedValue == rejectedValue
        }, "validation has not failed with expected error:\nEXPECTED: ${failedField} ${failedConstraint} ${rejectedValue}\nFOUND: ${objectToCheck.errors}"
    }

    static void removeMetaClass(final Class clazz, final Object object) {
        // Both of the following statements are necessary. See http://stackoverflow.com/a/15953102
        // But their order does not matter.
        // Replacing the clazz parameter with object.class does not work because of Spring's magic wrapper classes.
        GroovySystem.metaClassRegistry.removeMetaClass(clazz)
        object.metaClass = null
    }

    /**
     * Wrapper for instance method {@link GroovyTestCase#shouldFail}
     */
    @Deprecated
    static String shouldFail(Closure code) {
        return new GroovyTestCase().shouldFail(code)
    }

    /**
     * Wrapper for instance method {@link GroovyTestCase#shouldFail}
     */
    @Deprecated
    static String shouldFail(Class clazz, Closure code) {
        return new GroovyTestCase().shouldFail(clazz, code)
    }

    @Deprecated
    static shouldFailWithMessage(Class clazz, String pattern, Closure code) {
        assert shouldFail(clazz, code) ==~ pattern
    }

    @Deprecated
    static shouldFailWithMessageContaining(Class clazz, String messagePart, Closure code) {
        assert shouldFail(clazz, code).contains(messagePart)
    }


    static void checkDirectoryContentHelper(File baseDir, List<File> expectedDirs, List<File> expectedFiles = [], List<File> expectedLinks = []) {
        expectedDirs.each {
            assert it.exists() && it.isDirectory() && it.canRead() && it.canExecute()
        }
        expectedFiles.each {
            assert it.exists() && it.isFile() && it.canRead() && it.size() > 0
        }
        expectedLinks.each {
            assert it.exists() && Files.isSymbolicLink(it.toPath()) && it.canRead()
        }

        Set<File> expectedEntries = (expectedDirs + expectedFiles + expectedLinks).findAll {it.parentFile == baseDir} as Set
        Set<File> foundEntries = baseDir.listFiles() as Set
        assert expectedEntries == foundEntries
    }

    @Deprecated
    static void withMockedremoteShellHelper(RemoteShellHelper remoteShellHelper, Closure code) {
        assert remoteShellHelper != null
        assert code != null
        try {
            mockExecuteCommandReturnProcessOutput(remoteShellHelper)
            code()
        } finally {
            removeMetaClass(RemoteShellHelper, remoteShellHelper)
        }
    }

    private static void mockExecuteCommandReturnProcessOutput(RemoteShellHelper remoteShellHelper) {
        remoteShellHelper.metaClass.executeCommandReturnProcessOutput = { Realm realm, String command, String userName = null ->
            return executeAndWait(command).assertExitCodeZeroAndStderrEmpty()
        }
    }

    static String primaryGroup() {
        return executeAndWait("id -g -n").assertExitCodeZeroAndStderrEmpty().stdout.trim()
    }
}
