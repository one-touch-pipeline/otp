package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.*
import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*
import org.junit.rules.*

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
        GroovySystem.metaClassRegistry.removeMetaClass(LocalShellHelper)
    }


    @Test
    void "test setGroup & getGroup allFine"() {
        File tmpFile = temporaryFolder.newFile()
        String group = new TestConfigService().getTestingGroup()

        service.remoteShellHelper = [
                executeCommandReturnProcessOutput: { Realm realm, String command ->
                    LocalShellHelper.executeAndWait(command)
                }
        ] as RemoteShellHelper

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
        LocalShellHelper.metaClass.static.executeAndAssertExitCodeAndErrorOutAndReturnStdout = { String cmd ->
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
    void "test setGroup remoteShellHelper executeCommand throws exception should fail"() {
        final String FAIL_MESSAGE = HelperUtils.uniqueString
        File tmpFile = temporaryFolder.newFile()
        service.remoteShellHelper = [
                executeCommandReturnProcessOutput: { Realm realm, String command ->
                    assert false: FAIL_MESSAGE
                }
        ] as RemoteShellHelper

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
        service.remoteShellHelper = [
                executeCommandReturnProcessOutput: { Realm realm, String command ->
                    LocalShellHelper.executeAndWait(command)
                }
        ] as RemoteShellHelper

        LogThreadLocal.withThreadLog(System.out) {
            String output = LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout("stat -c '%a' ${tmpFile}")
            assert PERMISSION != output.trim()
            output = service.setPermission(new Realm(), tmpFile, PERMISSION)
            assert output.isEmpty()

            output = LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout("stat -c '%a' ${tmpFile}")
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
    void "test setPermission remoteShellHelper executeCommand throws exception should fail"() {
        final String FAIL_MESSAGE = HelperUtils.uniqueString
        File tmpFile = temporaryFolder.newFile()
        service.remoteShellHelper = [
                executeCommandReturnProcessOutput: { Realm realm, String command ->
                    assert false: FAIL_MESSAGE
                }
        ] as RemoteShellHelper

        TestCase.shouldFailWithMessageContaining (AssertionError, FAIL_MESSAGE) {
            LogThreadLocal.withThreadLog(System.out) {
                service.setPermission(new Realm(), tmpFile, DUMMY_PERMISSION)
            }
        }
    }
}
