package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.utils.ProcessHelperService
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import grails.test.mixin.*
import grails.test.mixin.support.*
import de.dkfz.tbi.otp.ngsdata.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Unit tests for the {@link ExecutionHelperService}.
 *
 */
@TestMixin(GrailsUnitTestMixin)
@Mock([ProcessingStep, Realm])
class ExecutionHelperServiceUnitTests {

    final static String DUMMY_GROUP = "DUMMY_GROUP"
    final static String DUMMY_PERMISSION = "DUMMY_PERMISSION"

    ExecutionHelperService service

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Before
    void setUp() {
        service = new ExecutionHelperService()
    }

    @After
    void tearDown() {
        GroovySystem.metaClassRegistry.removeMetaClass(ProcessHelperService)
    }


    @Test
    void "test setGroup & getGroup allFine"() {
        File tmpFile = temporaryFolder.newFile()
        String group = TestCase.testingGroup(grailsApplication)

        service.executionService = [
                executeCommand: { Realm realm, String command ->
                    ProcessHelperService.executeAndAssertExitCodeAndErrorOutAndReturnStdout(command)
                }
        ] as ExecutionService

        LogThreadLocal.withThreadLog(System.out) {
            String output = service.getGroup(tmpFile)
            assert group != output
            output = service.setGroup(new Realm(), tmpFile, group)
            assert output.isEmpty()
            output = service.getGroup(tmpFile)
            assert group == output
        }
    }

    @Test
    void "test getGroup directory is null should fail"() {
        TestCase.shouldFailWithMessageContaining (AssertionError, 'directory') {
            LogThreadLocal.withThreadLog(System.out) {
                service.getGroup(null)
            }
        }
    }

    @Test
    void "test getGroup ProcessHelperService throws exception should fail"() {
        final String FAIL_MESSAGE = HelperUtils.uniqueString
        File tmpFile = temporaryFolder.newFile()
        ProcessHelperService.metaClass.static.executeAndAssertExitCodeAndErrorOutAndReturnStdout = { String cmd ->
            assert false: FAIL_MESSAGE
        }

        TestCase.shouldFailWithMessageContaining (AssertionError, FAIL_MESSAGE) {
            LogThreadLocal.withThreadLog(System.out) {
                service.getGroup(tmpFile)
            }
        }
    }

    @Test
    void "test setGroup realm is null should fail"() {
        TestCase.shouldFailWithMessageContaining (AssertionError, 'realm') {
            LogThreadLocal.withThreadLog(System.out) {
                service.setGroup(null, temporaryFolder.newFile(), DUMMY_GROUP)
            }
        }
    }

    @Test
    void "test setGroup directory is null should fail"() {
        TestCase.shouldFailWithMessageContaining (AssertionError, 'directory') {
            LogThreadLocal.withThreadLog(System.out) {
                service.setGroup(new Realm(), null, DUMMY_GROUP)
            }
        }
    }

    @Test
    void "test setGroup group is null should fail"() {
        TestCase.shouldFailWithMessageContaining (AssertionError, 'group') {
            LogThreadLocal.withThreadLog(System.out) {
                service.setGroup(new Realm(), temporaryFolder.newFile(), null)
            }
        }
    }

    @Test
    void "test setGroup executionService executeCommand throws exception should fail"() {
        final String FAIL_MESSAGE = HelperUtils.uniqueString
        File tmpFile = temporaryFolder.newFile()
        service.executionService = [
                executeCommand: { Realm realm, String command ->
                    assert false: FAIL_MESSAGE
                }
        ] as ExecutionService

        TestCase.shouldFailWithMessageContaining (AssertionError, FAIL_MESSAGE) {
            LogThreadLocal.withThreadLog(System.out) {
                service.setGroup(new Realm(), tmpFile, DUMMY_GROUP)
            }
        }
    }



    @Test
    void "test setPermission allFine"() {
        String PERMISSION = '777'
        File tmpFile = temporaryFolder.newFile()
        service.executionService = [
                executeCommand: { Realm realm, String command ->
                    ProcessHelperService.executeAndAssertExitCodeAndErrorOutAndReturnStdout(command)
                }
        ] as ExecutionService

        LogThreadLocal.withThreadLog(System.out) {
            String output = ProcessHelperService.executeAndAssertExitCodeAndErrorOutAndReturnStdout("stat -c '%a' ${tmpFile}")
            assert PERMISSION != output.trim()
            output = service.setPermission(new Realm(), tmpFile, PERMISSION)
            assert output.isEmpty()

            output = ProcessHelperService.executeAndAssertExitCodeAndErrorOutAndReturnStdout("stat -c '%a' ${tmpFile}")
            assert PERMISSION == output.trim()
        }
    }

    @Test
    void "test setPermission realm is null should fail"() {
        TestCase.shouldFailWithMessageContaining (AssertionError, 'realm') {
            LogThreadLocal.withThreadLog(System.out) {
                service.setPermission(null, temporaryFolder.newFile(), DUMMY_PERMISSION)
            }
        }
    }

    @Test
    void "test setPermission directory is null should fail"() {
        TestCase.shouldFailWithMessageContaining (AssertionError, 'directory') {
            LogThreadLocal.withThreadLog(System.out) {
                service.setPermission(new Realm(), null, DUMMY_PERMISSION)
            }
        }
    }

    @Test
    void "test setPermission permission is null should fail"() {
        TestCase.shouldFailWithMessageContaining (AssertionError, 'permission') {
            LogThreadLocal.withThreadLog(System.out) {
                service.setPermission(new Realm(), temporaryFolder.newFile(), null)
            }
        }
    }

    @Test
    void "test setPermission executionService executeCommand throws exception should fail"() {
        final String FAIL_MESSAGE = HelperUtils.uniqueString
        File tmpFile = temporaryFolder.newFile()
        service.executionService = [
                executeCommand: { Realm realm, String command ->
                    assert false: FAIL_MESSAGE
                }
        ] as ExecutionService

        TestCase.shouldFailWithMessageContaining (AssertionError, FAIL_MESSAGE) {
            LogThreadLocal.withThreadLog(System.out) {
                service.setPermission(new Realm(), tmpFile, DUMMY_PERMISSION)
            }
        }
    }
}
