/*
 * Copyright 2011-2023 The OTP authors
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

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.utils.LocalShellHelper
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

import java.nio.file.Path

class ExecutionHelperServiceSpec extends Specification implements DataTest {

    private final static String DUMMY_GROUP = "DUMMY_GROUP"
    private final static String DUMMY_PERMISSION = "DUMMY_PERMISSION"

    private ExecutionHelperService service

    @TempDir
    Path tempDir

    void setup() {
        service = new ExecutionHelperService()
    }

    void "test setGroup & getGroup allFine"() {
        given:
        Realm realmObject = new Realm()
        File tmpFile = tempDir.toFile()
        String group = new TestConfigService().testingGroup

        when:
        service.remoteShellHelper = [
                executeCommandReturnProcessOutput: { Realm realm, String command ->
                    LocalShellHelper.executeAndWait(command)
                }
        ] as RemoteShellHelper

        then:
        assert LogThreadLocal.withThreadLog(System.out) {
            service.getGroup(realmObject, tmpFile)
        } != group: "Cannot test setGroup if OTP property \"otp.testing.group\" is also the user's primary group." +
                " Please update your .otp.properties to use a different group that is not your primary group!"

        LogThreadLocal.withThreadLog(System.out) {
            service.setGroup(realmObject, tmpFile, group)
        }.empty

        LogThreadLocal.withThreadLog(System.out) {
            service.getGroup(realmObject, tmpFile)
        } == group
    }

    void "test getGroup directory is null should fail"() {
        when:
        LogThreadLocal.withThreadLog(System.out) {
            service.getGroup(new Realm(), null)
        }

        then:
        Throwable e = thrown(AssertionError)
        e.message.contains('directory')
    }

    @SuppressWarnings('ConstantAssertExpression')
    void "test getGroup remoteShellHelper executeCommand throws exception should fail"() {
        given:
        final String FAIL_MESSAGE = HelperUtils.uniqueString
        File tmpFile = tempDir.toFile()
        service.remoteShellHelper = [
                executeCommandReturnProcessOutput: { Realm realm, String command ->
                    assert false: FAIL_MESSAGE
                }
        ] as RemoteShellHelper

        when:
        LogThreadLocal.withThreadLog(System.out) {
            service.getGroup(new Realm(), tmpFile)
        }

        then:
        Throwable e = thrown(AssertionError)
        e.message.contains(FAIL_MESSAGE)
    }

    void "test setGroup directory is null should fail"() {
        when:
        LogThreadLocal.withThreadLog(System.out) {
            service.setGroup(new Realm(), null, DUMMY_GROUP)
        }

        then:
        Throwable e = thrown(AssertionError)
        e.message.contains('directory')
    }

    void "test setGroup group is null should fail"() {
        when:
        LogThreadLocal.withThreadLog(System.out) {
            service.setGroup(new Realm(), tempDir.toFile(), null)
        }

        then:
        Throwable e = thrown(AssertionError)
        e.message.contains('group')
    }

    @SuppressWarnings('ConstantAssertExpression')
    void "test setGroup remoteShellHelper executeCommand throws exception should fail"() {
        given:
        final String FAIL_MESSAGE = HelperUtils.uniqueString
        File tmpFile = tempDir.toFile()
        service.remoteShellHelper = [
                executeCommandReturnProcessOutput: { Realm realm, String command ->
                    assert false: FAIL_MESSAGE
                }
        ] as RemoteShellHelper

        when:
        LogThreadLocal.withThreadLog(System.out) {
            service.setGroup(new Realm(), tmpFile, DUMMY_GROUP)
        }

        then:
        Throwable e = thrown(AssertionError)
        e.message.contains(FAIL_MESSAGE)
    }

    void "test setPermission allFine"() {
        given:
        String PERMISSION = '777'
        File tmpFile = tempDir.toFile()
        service.remoteShellHelper = [
                executeCommandReturnProcessOutput: { Realm realm, String command ->
                    LocalShellHelper.executeAndWait(command)
                }
        ] as RemoteShellHelper

        expect:
        LogThreadLocal.withThreadLog(System.out) {
            LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout("stat -c '%a' ${tmpFile}")
        }.trim() != PERMISSION

        LogThreadLocal.withThreadLog(System.out) {
            service.setPermission(new Realm(), tmpFile, PERMISSION)
        }.empty

        LogThreadLocal.withThreadLog(System.out) {
            LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout("stat -c '%a' ${tmpFile}")
        }.trim() == PERMISSION
    }

    void "test setPermission directory is null should fail"() {
        when:
        LogThreadLocal.withThreadLog(System.out) {
            service.setPermission(new Realm(), null, DUMMY_PERMISSION)
        }

        then:
        Throwable e = thrown(AssertionError)
        e.message.contains('directory')
    }

    void "test setPermission permission is null should fail"() {
        when:
        LogThreadLocal.withThreadLog(System.out) {
            service.setPermission(new Realm(), tempDir.toFile(), null)
        }

        then:
        Throwable e = thrown(AssertionError)
        e.message.contains('permission')
    }

    @SuppressWarnings('ConstantAssertExpression')
    void "test setPermission remoteShellHelper executeCommand throws exception should fail"() {
        given:
        final String FAIL_MESSAGE = HelperUtils.uniqueString
        File tmpFile = tempDir.toFile()
        service.remoteShellHelper = [
                executeCommandReturnProcessOutput: { Realm realm, String command ->
                    assert false: FAIL_MESSAGE
                }
        ] as RemoteShellHelper

        when:
        LogThreadLocal.withThreadLog(System.out) {
            service.setPermission(new Realm(), tmpFile, DUMMY_PERMISSION)
        }

        then:
        Throwable e = thrown(AssertionError)
        e.message.contains(FAIL_MESSAGE)
    }
}
