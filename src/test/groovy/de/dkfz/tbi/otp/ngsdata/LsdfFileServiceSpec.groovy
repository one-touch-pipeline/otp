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

import grails.testing.gorm.DataTest
import org.codehaus.groovy.control.io.NullWriter
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import de.dkfz.tbi.otp.job.processing.CreateClusterScriptService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.utils.LocalShellHelper
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

class LsdfFileServiceSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                Realm,
        ]
    }


    LsdfFilesService service

    void setup() {
        service = new LsdfFilesService()
    }

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder()

    void "test isFileReadableAndNotEmpty"() {
        given:
        File file = tempFolder.newFile()
        file << "content"

        expect:
        LsdfFilesService.isFileReadableAndNotEmpty(file)
    }

    void "test ensureFileIsReadableAndNotEmpty"() {
        given:
        File file = tempFolder.newFile()
        file << "content"

        when:
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)

        then:
        notThrown()
    }

    void "test isFileReadableAndNotEmpty, when path is not absolute, should fail"() {
        given:
        File file = new File("testFile.txt")

        when:
        LsdfFilesService.isFileReadableAndNotEmpty(file)

        then:
        def e = thrown(AssertionError)
        e.message =~ /(?i)isAbsolute/
    }

    void "test ensureFileIsReadableAndNotEmpty, when path is not absolute, should fail"() {
        given:
        File file = new File("testFile.txt")

        when:
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)

        then:
        def e = thrown(AssertionError)
        e.message =~ /(?i)isAbsolute/
    }

    void "test isFileReadableAndNotEmpty, when does not exist"() {
        given:
        //file must be absolute to make sure that the test fails the 'exists?' assertion
        File file = new File(tempFolder.newFolder(), "testFile.txt")

        expect:
        !LsdfFilesService.isFileReadableAndNotEmpty(file)
    }

    void "test ensureFileIsReadableAndNotEmpty, when does not exist, should fail"() {
        given:
        //file must be absolute to make sure that the test fails the 'exists?' assertion
        File file = new File(tempFolder.newFolder(), "testFile.txt")

        when:
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)

        then:
        def e = thrown(AssertionError)
        e.message =~ /(?i)not found\./
    }

    void "test isFileReadableAndNotEmpty, when file is not a regular file"() {
        given:
        File file = tempFolder.newFolder()

        expect:
        !LsdfFilesService.isFileReadableAndNotEmpty(file)
    }

    void "test ensureFileIsReadableAndNotEmpty, when file is not a regular file, should fail"() {
        given:
        File file = tempFolder.newFolder()

        when:
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)

        then:
        def e = thrown(AssertionError)
        e.message =~ /(?i)isRegularFile/
    }


    void "test isFileReadableAndNotEmpty, when file isn't readable"() {
        given:
        File file = tempFolder.newFile()
        file << "content"
        file.setReadable(false)

        expect:
        !LsdfFilesService.isFileReadableAndNotEmpty(file)

        cleanup:
        file.setReadable(true)
    }

    void "test ensureFileIsReadableAndNotEmpty, when file isn't readable, should fail"() {
        given:
        File file = tempFolder.newFile()
        file << "content"
        file.setReadable(false)

        when:
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)

        then:
        def e = thrown(AssertionError)
        e.message =~ /(?i)isReadable/

        cleanup:
        file.setReadable(true)
    }

    void "test isFileReadableAndNotEmpty, when file is empty"() {
        given:
        File file = tempFolder.newFile()

        expect:
        !LsdfFilesService.isFileReadableAndNotEmpty(file)
    }

    void "test ensureFileIsReadableAndNotEmpty, when file is empty, should fail"() {
        given:
        File file = tempFolder.newFile()

        when:
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)

        then:
        def e = thrown(AssertionError)
        e.message =~ /(?i)size/
    }

    void "test deleteFile"() {
        given:
        File file = tempFolder.newFile()
        Realm realm = new Realm()
        LsdfFilesService service = createInstanceWithMockedExecuteCommand(realm)

        when:
        LogThreadLocal.withThreadLog(new NullWriter()) {
            service.deleteFile(realm, file)
        }

        then:
        !file.exists()
    }

    void "test deleteDirectory"() {
        given:
        File file = tempFolder.newFolder()
        Realm realm = new Realm()
        LsdfFilesService service = createInstanceWithMockedExecuteCommand(realm)

        when:
        LogThreadLocal.withThreadLog(new NullWriter()) {
            service.deleteDirectory(realm, file)
        }

        then:
        !file.exists()
    }

    void "test deleteFilesRecursive"() {
        given:
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

        when:
        service.deleteFilesRecursive(realm, files)

        then:
        files.each {
            assert !it.exists()
        }
    }

    void "test deleteFilesRecursive, when filesOrDirectories is empty"() {
        given:
        Realm realm = DomainFactory.createRealm()
        service.remoteShellHelper = [
                executeCommand: { Realm realm2, String command ->
                    assert false: 'Should not be called'
                }
        ] as RemoteShellHelper
        service.createClusterScriptService = new CreateClusterScriptService()

        expect:
        service.deleteFilesRecursive(realm, [])
    }

    void "test deleteFilesRecursive, when realm is null, should fail"() {
        when:
        service.deleteFilesRecursive(null, [tempFolder.newFolder()])

        then:
        def e = thrown(AssertionError)
        e.message.contains('realm may not be null.')
    }

    void "test deleteFilesRecursive, when filesOrDirectories is null, should fail"() {
        given:
        Realm realm = DomainFactory.createRealm()

        when:
        service.deleteFilesRecursive(realm, null)

        then:
        def e = thrown(AssertionError)
        e.message.contains('filesOrDirectories may not be null.')
    }


    void "test deleteFilesRecursive, when deletion fails, should fail"() {
        given:
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

        when:
        service.deleteFilesRecursive(realm, files)

        then:
        def e = thrown(AssertionError)
        e.message.contains(MESSAGE)
        files.each {
            assert it.exists()
        }
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
