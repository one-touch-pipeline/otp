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
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions


class ChecksumFileServiceSpec extends Specification implements DataTest {
    ChecksumFileService checksumFileService
    Path file

    @Rule
    TemporaryFolder tempFolder = new TemporaryFolder()

    void setup() {
        checksumFileService = new ChecksumFileService()
        file = tempFolder.newFile("asdf.md5").toPath()
    }


    void "test firstMD5ChecksumFromFile"() {
        given:
        String expectedMd5 = "68b329da9893e34099c7d8ad5cb9c940"
        file << """\
            ${expectedMd5}  opt-test.file
            999329da9893e34099c7d8ad5cb9c940  opt-test2.file
            """.stripIndent()

        expect:
        expectedMd5 == checksumFileService.firstMD5ChecksumFromFile(file)
    }

    void "test firstMD5ChecksumFromFile with non existing file"() {
        given:
        Files.delete(file)

        when:
        checksumFileService.firstMD5ChecksumFromFile(file)

        then:
        thrown(RuntimeException)
    }

    void "test firstMD5ChecksumFromFile with unreadable file"() {
        given:
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("-wx-wx-wx"))

        when:
        checksumFileService.firstMD5ChecksumFromFile(file)

        then:
        thrown(RuntimeException)
    }

    void "test firstMD5ChecksumFromFile with empty file"() {
        when:
        checksumFileService.firstMD5ChecksumFromFile(file)

        then:
        thrown(RuntimeException)
    }

    @Unroll
    void "test firstMD5ChecksumFromFile with wrong md5sum format, case '#name'"() {
        given:
        file << "${value}  opt-test.file"

        when:
        checksumFileService.firstMD5ChecksumFromFile(file)

        then:
        thrown(RuntimeException)

        where:
        name            | value
        'to short'      | '1234567890123456789012345678901'
        'to long'       | '123456789012345678901234567890123'
        'wrong symbols' | '12345678901234567890123456789xyz'
    }
}
