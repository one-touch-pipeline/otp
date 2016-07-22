package de.dkfz.tbi

import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.utils.ProcessHelperService
import org.junit.*
import org.junit.runners.model.MultipleFailureException
import org.springframework.validation.Errors
import org.springframework.validation.FieldError

import java.nio.file.Files
import java.util.concurrent.Callable

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
        TEST_DIRECTORY = new File(new File(tmpdir, 'otp-test'), HelperUtils.uniqueString)
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

    public static void assertEquals(final GString expected, final String actual) {
        Assert.assertEquals(expected.toString(), actual)
    }

    public static String createUniqueString() {
        return HelperUtils.getUniqueString()
    }

    public static File getUniqueNonExistentPath() {
        return new File("/dev/null/otp-test/${HelperUtils.uniqueString}")
    }

    /**
     * You should delete the returned directory using Groovy's File.deleteDir() method as
     * soon as you do not need it anymore, or delete all test directories using {@link #cleanTestDirectory()}.
     */
    public static File createEmptyTestDirectory() {
        if (!cleanTestDirectoryShutdownHookInstalled) {
            addShutdownHook { cleanTestDirectory() }
            cleanTestDirectoryShutdownHookInstalled = true
        }
        final File dir = new File(TEST_DIRECTORY, HelperUtils.uniqueString)
        assert dir.mkdirs()  // This will fail if the directory already exists or if it could not be created.
        return dir
    }

    public static boolean cleanTestDirectory() {
        return TEST_DIRECTORY.deleteDir()
    }

    /**
     * @see CollectionUtils#containSame(java.util.Collection, java.util.Collection)
     */
    public static <T> boolean containSame(final Collection<? extends T> c1, final Collection<? extends T> c2) {
        return CollectionUtils.containSame(c1, c2)
    }

    public static <T> boolean assertContainSame(final Collection<? extends T> c1, final Collection<? extends T> c2) {
        if (!CollectionUtils.containSame(c1, c2)) {
            Set c1Set = c1.toSet()
            Set c2Set = c2.toSet()
            c1Set.removeAll(c2)
            c2Set.removeAll(c1)
            throw new AssertionError("\nIn c1, but not in c2:\n${c1Set*.toString().sort().join('\n')}\nIn c2, but not in c1:\n${c2Set*.toString().sort().join('\n')}")
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

    public static void removeMetaClass(final Class clazz, final Object object) {
        // Both of the following statements are necessary. See http://stackoverflow.com/a/15953102
        // But their order does not matter.
        // Replacing the clazz parameter with object.class does not work because of Spring's magic wrapper classes.
        GroovySystem.metaClassRegistry.removeMetaClass(clazz)
        object.metaClass = null
    }

    /**
     * Wrapper for instance method {@link GroovyTestCase#shouldFail}
     */
    static String shouldFail(Closure code) {
        return new GroovyTestCase().shouldFail(code)
    }

    /**
     * Wrapper for instance method {@link GroovyTestCase#shouldFail}
     */
    static String shouldFail(Class clazz, Closure code) {
        return new GroovyTestCase().shouldFail(clazz, code)
    }

    static shouldFailWithMessage(Class clazz, String pattern, Closure code) {
        assert shouldFail(clazz, code) ==~ pattern
    }

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

    static void withMockedExecuteCommand(ExecutionService executionService, Closure code) {
        assert executionService != null
        assert code != null
        try {
            mockExecuteCommand(executionService)
            code()
        } finally {
            removeMetaClass(ExecutionService, executionService)
        }
    }

    static void mockExecuteCommand(ExecutionService executionService) {
        executionService.metaClass.executeCommand = { Realm realm, String command ->
            return ProcessHelperService.executeAndAssertExitCodeAndErrorOutAndReturnStdout(command)
        }
    }

    /**
     * @deprecated Use {@link #withMockedExecuteCommand(ExecutionService, Closure)} instead.
     */
    @Deprecated
    def static mockCreateDirectory(LsdfFilesService lsdfFilesService) {
        lsdfFilesService.metaClass.createDirectory = { File file, Project project1 ->
            file.mkdirs()
        }
    }

    /**
     * @deprecated Use {@link #withMockedExecuteCommand(ExecutionService, Closure)} instead.
     */
    @Deprecated
    def static mockDeleteDirectory(Object lsdfFilesService) {
        lsdfFilesService.metaClass.deleteDirectoryRecursive = { Realm realm, File dir ->
            if (!dir.deleteDir()) {
                throw new IOException("Unable to delete path '${dir}'.")
            }
        }
    }

    def static removeMockFileService(LsdfFilesService lsdfFilesService) {
        TestCase.removeMetaClass(LsdfFilesService, lsdfFilesService)
    }
}
