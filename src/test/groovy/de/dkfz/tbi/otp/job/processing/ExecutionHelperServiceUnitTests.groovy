/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.job.processing

import grails.test.mixin.Mock
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.*
import org.junit.rules.TemporaryFolder

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.utils.LocalShellHelper
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

/**
 * Unit tests for the {@link ExecutionHelperService}.
 */
@TestMixin(GrailsUnitTestMixin)
@Mock([ProcessingStep, Realm])
class ExecutionHelperServiceUnitTests {

    private final static String DUMMY_GROUP = "DUMMY_GROUP"
    private final static String DUMMY_PERMISSION = "DUMMY_PERMISSION"

    private ExecutionHelperService service

    @SuppressWarnings('PublicInstanceField') //Rule required public field
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
        Realm realmObject = new Realm()
        File tmpFile = temporaryFolder.newFile()
        String group = new TestConfigService().testingGroup

        service.remoteShellHelper = [
                executeCommandReturnProcessOutput: { Realm realm, String command ->
                    LocalShellHelper.executeAndWait(command)
                }
        ] as RemoteShellHelper

        LogThreadLocal.withThreadLog(System.out) {
            String output = service.getGroup(realmObject, tmpFile)
            assert group != output : "Cannot test setGroup if OTP property \"otp.testing.group\" is also the user's primary group." +
                    " Please update your .otp.properties to use a different group that is not your primary group!"
            output = service.setGroup(realmObject, tmpFile, group)
            assert output.empty
            output = service.getGroup(realmObject, tmpFile)
            assert group == output
        }
    }

    @Test
    void "test getGroup directory is null should fail"() {
        TestCase.shouldFailWithMessageContaining(AssertionError, 'directory') {
            LogThreadLocal.withThreadLog(System.out) {
                service.getGroup(new Realm(), null)
            }
        }
    }

    @Test
    void "test getGroup realm is null should fail"() {
        TestCase.shouldFailWithMessageContaining(AssertionError, 'realm') {
            LogThreadLocal.withThreadLog(System.out) {
                service.getGroup(null, temporaryFolder.newFile())
            }
        }
    }

    @SuppressWarnings('ConstantAssertExpression')
    @Test
    void "test getGroup remoteShellHelper executeCommand throws exception should fail"() {
        final String FAIL_MESSAGE = HelperUtils.uniqueString
        File tmpFile = temporaryFolder.newFile()
        service.remoteShellHelper = [
                executeCommandReturnProcessOutput: { Realm realm, String command ->
                    assert false: FAIL_MESSAGE
                }
        ] as RemoteShellHelper

        TestCase.shouldFailWithMessageContaining(AssertionError, FAIL_MESSAGE) {
            LogThreadLocal.withThreadLog(System.out) {
                service.getGroup(new Realm(), tmpFile)
            }
        }
    }

    @Test
    void "test setGroup realm is null should fail"() {
        TestCase.shouldFailWithMessageContaining(AssertionError, 'realm') {
            LogThreadLocal.withThreadLog(System.out) {
                service.setGroup(null, temporaryFolder.newFile(), DUMMY_GROUP)
            }
        }
    }

    @Test
    void "test setGroup directory is null should fail"() {
        TestCase.shouldFailWithMessageContaining(AssertionError, 'directory') {
            LogThreadLocal.withThreadLog(System.out) {
                service.setGroup(new Realm(), null, DUMMY_GROUP)
            }
        }
    }

    @Test
    void "test setGroup group is null should fail"() {
        TestCase.shouldFailWithMessageContaining(AssertionError, 'group') {
            LogThreadLocal.withThreadLog(System.out) {
                service.setGroup(new Realm(), temporaryFolder.newFile(), null)
            }
        }
    }

    @SuppressWarnings('ConstantAssertExpression')
    @Test
    void "test setGroup remoteShellHelper executeCommand throws exception should fail"() {
        final String FAIL_MESSAGE = HelperUtils.uniqueString
        File tmpFile = temporaryFolder.newFile()
        service.remoteShellHelper = [
                executeCommandReturnProcessOutput: { Realm realm, String command ->
                    assert false: FAIL_MESSAGE
                }
        ] as RemoteShellHelper

        TestCase.shouldFailWithMessageContaining(AssertionError, FAIL_MESSAGE) {
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
            assert output.empty

            output = LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout("stat -c '%a' ${tmpFile}")
            assert PERMISSION == output.trim()
        }
    }

    @Test
    void "test setPermission realm is null should fail"() {
        TestCase.shouldFailWithMessageContaining(AssertionError, 'realm') {
            LogThreadLocal.withThreadLog(System.out) {
                service.setPermission(null, temporaryFolder.newFile(), DUMMY_PERMISSION)
            }
        }
    }

    @Test
    void "test setPermission directory is null should fail"() {
        TestCase.shouldFailWithMessageContaining(AssertionError, 'directory') {
            LogThreadLocal.withThreadLog(System.out) {
                service.setPermission(new Realm(), null, DUMMY_PERMISSION)
            }
        }
    }

    @Test
    void "test setPermission permission is null should fail"() {
        TestCase.shouldFailWithMessageContaining(AssertionError, 'permission') {
            LogThreadLocal.withThreadLog(System.out) {
                service.setPermission(new Realm(), temporaryFolder.newFile(), null)
            }
        }
    }

    @SuppressWarnings('ConstantAssertExpression')
    @Test
    void "test setPermission remoteShellHelper executeCommand throws exception should fail"() {
        final String FAIL_MESSAGE = HelperUtils.uniqueString
        File tmpFile = temporaryFolder.newFile()
        service.remoteShellHelper = [
                executeCommandReturnProcessOutput: { Realm realm, String command ->
                    assert false: FAIL_MESSAGE
                }
        ] as RemoteShellHelper

        TestCase.shouldFailWithMessageContaining(AssertionError, FAIL_MESSAGE) {
            LogThreadLocal.withThreadLog(System.out) {
                service.setPermission(new Realm(), tmpFile, DUMMY_PERMISSION)
            }
        }
    }
}
