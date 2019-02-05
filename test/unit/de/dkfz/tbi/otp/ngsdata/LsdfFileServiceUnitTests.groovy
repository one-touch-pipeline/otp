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

package de.dkfz.tbi.otp.ngsdata

import grails.buildtestdata.mixin.Build
import org.codehaus.groovy.control.io.NullWriter
import org.junit.*
import org.junit.rules.TemporaryFolder

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.job.processing.CreateClusterScriptService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.utils.LocalShellHelper
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

@Build([Realm])
class LsdfFileServiceUnitTests {

    LsdfFilesService service

    @Before
    void setUp() {
        service = new LsdfFilesService()
    }

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder()

    @Test
    void testFileReadableAndNotEmptyMethods_AllCorrect() {
        File file = tempFolder.newFile()
        file << "content"
        assert LsdfFilesService.isFileReadableAndNotEmpty(file)
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)
    }

    @Test
    void testFileReadableAndNotEmptyMethods_IsNotAbsolute() {
        File file = new File("testFile.txt")
        assert shouldFail(AssertionError, { LsdfFilesService.isFileReadableAndNotEmpty(file) }) =~ /(?i)isAbsolute/
        assert shouldFail(AssertionError, {
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)
        }) =~ /(?i)isAbsolute/
    }

    @Test
    void testFileReadableAndNotEmptyMethods_DoesNotExist() {
        //file must be absolute to make sure that the test fails the 'exists?' assertion
        File file = new File(tempFolder.newFolder(), "testFile.txt")
        assert !LsdfFilesService.isFileReadableAndNotEmpty(file)
        assert shouldFail(AssertionError, {
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)
        }) =~ /(?i)not found\./
    }

    @Test
    void testFileReadableAndNotEmptyMethods_IsNotAFile() {
        File file = tempFolder.newFolder()
        assert !LsdfFilesService.isFileReadableAndNotEmpty(file)
        assert shouldFail(AssertionError, {
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)
        }) =~ /(?i)isRegularFile/
    }

    @Test
    void testFileReadableAndNotEmptyMethods_CanNotRead() {
        File file = tempFolder.newFile()
        file << "content"
        try {
            file.setReadable(false)
            assert !LsdfFilesService.isFileReadableAndNotEmpty(file)
            assert shouldFail(AssertionError, {
                LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)
            }) =~ /(?i)isReadable/
        } finally {
            file.setReadable(true)
        }
    }

    @Test
    void testFileReadableAndNotEmptyMethods_IsEmpty() {
        File file = tempFolder.newFile()
        assert !LsdfFilesService.isFileReadableAndNotEmpty(file)
        assert shouldFail(AssertionError, { LsdfFilesService.ensureFileIsReadableAndNotEmpty(file) }) =~ /(?i)size/
    }

    @Test
    void testDeleteFile() {
        testDeleteMethod(tempFolder.newFile(), { LsdfFilesService service, Realm realm, File file ->
            service.deleteFile(realm, file)
        })
    }

    @Test
    void testDeleteDirectory() {
        testDeleteMethod(tempFolder.newFolder(), { LsdfFilesService service, Realm realm, File file ->
            service.deleteDirectory(realm, file)
        })
    }

    @Test
    void test_deleteFilesRecursive_shouldBeFine() {
        Realm realm = DomainFactory.createRealm()
        service.remoteShellHelper = [
                executeCommand: { Realm realm2, String command ->
                    LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout(command)
                }
        ] as RemoteShellHelper
        service.createClusterScriptService = new CreateClusterScriptService()

        List<File> files = [
                tempFolder.newFolder(),
                tempFolder.newFile(),
        ]

        service.deleteFilesRecursive(realm, files)

        files.each {
            assert !it.exists()
        }
    }

    @Test
    void test_deleteFilesRecursive_FilesOrDirectoriesIsEmpty_shouldDoNothing() {
        Realm realm = DomainFactory.createRealm()
        service.remoteShellHelper = [
                executeCommand: { Realm realm2, String command ->
                    assert false: 'Should not be called'
                }
        ] as RemoteShellHelper
        service.createClusterScriptService = new CreateClusterScriptService()

        service.deleteFilesRecursive(realm, [])
    }

    @Test
    void test_deleteFilesRecursive_noRealm_shouldThrowException() {
        TestCase.shouldFailWithMessageContaining(AssertionError, 'realm may not be null.') {
            service.deleteFilesRecursive(null, [tempFolder.newFolder()])
        }
    }

    @Test
    void test_deleteFilesRecursive_FilesOrDirectoriesIsNull_shouldThrowException() {
        Realm realm = DomainFactory.createRealm()
        TestCase.shouldFailWithMessageContaining(AssertionError, 'filesOrDirectories may not be null.') {
            service.deleteFilesRecursive(realm, null)
        }
    }


    @Test
    void test_deleteFilesRecursive_deletionFail_shouldThrowException() {
        final String MESSAGE = HelperUtils.uniqueString
        Realm realm = DomainFactory.createRealm()
        service.remoteShellHelper = [
                executeCommand: { Realm realm2, String command ->
                    assert false: MESSAGE
                }
        ] as RemoteShellHelper
        service.createClusterScriptService = new CreateClusterScriptService()

        List<File> files = [
                tempFolder.newFolder(),
                tempFolder.newFile(),
        ]
        TestCase.shouldFailWithMessageContaining(AssertionError, MESSAGE) {
            service.deleteFilesRecursive(realm, files)
        }

        files.each {
            assert it.exists()
        }
    }


    private void testDeleteMethod(File file, Closure call) {
        Realm realm = new Realm()
        LsdfFilesService service = createInstanceWithMockedExecuteCommand(realm)

        LogThreadLocal.withThreadLog(new NullWriter()) {
            call(service, realm, file)
        }

        assert !file.exists()
    }

    private LsdfFilesService createInstanceWithMockedExecuteCommand(Realm expectedRealm) {
        LsdfFilesService service = new LsdfFilesService()
        service.remoteShellHelper = [
                executeCommand: { Realm realm, String command ->
                    assert realm.is(expectedRealm)
                    return ['bash', '-c', command].execute().getText()
                }
        ] as RemoteShellHelper
        return service
    }
}
