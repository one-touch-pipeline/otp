/*
 * Copyright 2011-2024 The OTP authors
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
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.utils.CreateFileHelper

import java.nio.file.Path

class LsdfFileServiceSpec extends Specification implements DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                FastqImportInstance,
                SeqTrack,
        ]
    }

    LsdfFilesService service

    void setup() {
        service = new LsdfFilesService()
    }

    @TempDir
    Path tempDir

    void "test ensureFileIsReadableAndNotEmpty"() {
        given:
        File file = tempDir.resolve("test.txt").toFile()
        file << "content"

        when:
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)

        then:
        noExceptionThrown()
    }

    void "test ensureFileIsReadableAndNotEmpty, when path is not absolute, should fail"() {
        given:
        File file = new File("testFile.txt")

        when:
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)

        then:
        def e = thrown(AssertionError)
        e.message =~ /(?i)absolute/
    }

    void "test ensureFileIsReadableAndNotEmpty, when does not exist, should fail"() {
        given:
        // file must be absolute to make sure that the test fails the 'exists?' assertion
        File file = tempDir.resolve("testFile.txt").toFile()

        when:
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)

        then:
        def e = thrown(AssertionError)
        e.message =~ /(?i)on local filesystem is not accessible or does not exist\./
    }

    void "test ensureFileIsReadableAndNotEmpty, when file is not a regular file, should fail"() {
        when:
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(tempDir.toFile())

        then:
        def e = thrown(AssertionError)
        e.message =~ /(?i)isRegularFile/
    }

    void "test ensureFileIsReadableAndNotEmpty, when file isn't readable, should fail"() {
        given:
        File file = tempDir.resolve("test.txt").toFile()
        file << "content"
        file.readable = false

        when:
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)

        then:
        def e = thrown(AssertionError)
        e.message =~ /(?i)isReadable/

        cleanup:
        file.readable = true
    }

    void "test ensureFileIsReadableAndNotEmpty, when file is empty, should fail"() {
        given:
        File file = CreateFileHelper.createFile(tempDir.resolve("test.txt").toFile(), "")

        when:
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)

        then:
        def e = thrown(AssertionError)
        e.message =~ /(?i)size/
    }
}
