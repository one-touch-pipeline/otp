package de.dkfz.tbi

import org.junit.rules.TemporaryFolder

/**
 * A class for collection constants used in different tests.
 *
 *
 */
class TestConstants {


    /**
     * The default error message a Assert.notNull of spring produced.
     */
    public final static String ERROR_MESSAGE_SPRING_NOT_NULL = "[Assertion failed] - this argument is required; it must not be null"

    /**
     * The base otp test directory
     *
     * @deprecated Use {@link TestCase#getUniqueNonExistentPath()} or {@link TemporaryFolder} or
     * {@link TestCase#createEmptyTestDirectory()}.
     */
    @Deprecated
    public final static String BASE_TEST_DIRECTORY = TestCase.uniqueNonExistentPath

}
