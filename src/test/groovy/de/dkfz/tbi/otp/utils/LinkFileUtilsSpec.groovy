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
package de.dkfz.tbi.otp.utils

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper

import java.nio.file.Path
import java.nio.file.Paths

class LinkFileUtilsSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return []
    }

    LinkFileUtils linkFileUtils

    File testDirectory

    @TempDir
    Path tempDir

    void setup() {
        testDirectory = tempDir.toFile()
        if (!testDirectory.exists()) {
            assert testDirectory.mkdirs()
        }

        linkFileUtils = new LinkFileUtils()
        linkFileUtils.fileSystemService = Mock(FileSystemService) {
            _ * getRemoteFileSystem() >>  testDirectory.toPath().fileSystem
        }
        linkFileUtils.fileService = new FileService()
    }

    void "test createAndValidateLinks, when map is null, should fail"() {
        when:
        linkFileUtils.createAndValidateLinks(null)

        then:
        thrown(AssertionError)
    }

    void "test createAndValidateLinks, when base dir exists and links does not exist"() {
        given:
        File sourceFile = new File(testDirectory, "sourceFile")
        sourceFile.createNewFile()
        File linkFile = new File(testDirectory, "linkFile")

        when:
        linkFileUtils.createAndValidateLinks([(sourceFile): linkFile])

        then:
        linkFile.exists()
        Paths.get(sourceFile.absolutePath) == Paths.get(linkFile.absolutePath).toRealPath()
    }

    void "test createAndValidateLinks, when base dir exists and file exists in place of link"() {
        given:
        String oldContent = "OldContent"
        File sourceFile = new File(testDirectory, "sourceFile")
        sourceFile.createNewFile()
        File linkFile = new File(testDirectory, "linkFile")
        linkFile << oldContent

        when:
        linkFileUtils.createAndValidateLinks([(sourceFile): linkFile])

        then:
        linkFile.exists()
        linkFile.text != oldContent
        Paths.get(sourceFile.absolutePath) == Paths.get(linkFile.absolutePath).toRealPath()
    }

    void "test createAndValidateLinks, when base dir exists and dir exists in place of link"() {
        given:
        File sourceFile = new File(testDirectory, "sourceFile")
        sourceFile.createNewFile()
        File linkFile = new File(testDirectory, "linkFile")
        assert linkFile.mkdirs()

        when:
        linkFileUtils.createAndValidateLinks([(sourceFile): linkFile])

        then:
        linkFile.exists() && !linkFile.isDirectory()
        Paths.get(sourceFile.absolutePath) == Paths.get(linkFile.absolutePath).toRealPath()
    }

    void "test createAndValidateLinks, when base dir does not exist"() {
        given:
        File sourceFile = new File(testDirectory, "sourceFile")
        sourceFile.createNewFile()
        File linkDir = new File(testDirectory, "linkDir")
        assert !linkDir.exists()
        File linkFile = new File(linkDir, "linkFile")

        linkFileUtils.fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            1 * executeCommandReturnProcessOutput(_) >> { String command ->
                command ==~ "chmod 2750 ${linkDir}"
                return new ProcessOutput(command, '', 0)
            }
        }

        when:
        linkFileUtils.createAndValidateLinks([(sourceFile): linkFile])

        then:
        linkFile.exists()
        Paths.get(sourceFile.absolutePath) == Paths.get(linkFile.absolutePath).toRealPath()
    }
}
