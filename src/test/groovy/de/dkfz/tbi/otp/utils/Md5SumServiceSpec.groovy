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
import spock.lang.Unroll

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper

import java.nio.file.Path
import java.nio.file.Paths

class Md5SumServiceSpec extends Specification implements DataTest {

    @TempDir
    Path tempDir

    Md5SumService service

    void setup() {
        service = new Md5SumService()
        service.configService = Mock(ConfigService)
        service.fileService = new FileService()
        service.fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_, _) >> { realm1, cmd -> LocalShellHelper.executeAndWait(cmd) }
        }
    }

    @Unroll
    void "extractMd5Sum, if valid md5sum, return md5sum value"() {
        given:
        Path md5sumFile = tempDir.resolve("md5sum.txt")
        md5sumFile.text = prefix + md5sum + postfix

        when:
        String extractedMd5Sum = service.extractMd5Sum(md5sumFile)

        then:
        extractedMd5Sum == md5sum

        where:
        prefix | postfix
        ''     | ''
        ''     | '\n'
        '\n'   | ''
        '\n'   | '\n'

        md5sum = HelperUtils.randomMd5sum
    }

    void "extractMd5Sum, if md5sum file is null, then throw an assertion"() {
        when:
        service.extractMd5Sum(null)

        then:
        AssertionError e = thrown()
        e.message.contains('Parameter md5Sum is null')
    }

    void "extractMd5Sum, if md5sum file is relative, then throw an assertion"() {
        when:
        service.extractMd5Sum(Paths.get('tmp'))

        then:
        AssertionError e = thrown()
        e.message =~ /The md5sum file '[^']*' is not absolute/
    }

    //false positives, since rule can not recognize calling class
    @SuppressWarnings('ExplicitFlushForDeleteRule')
    void "extractMd5Sum, if md5sum file does not exist, then throw an assertion"() {
        given:
        File file = tempDir.resolve("md5sum.txt").toFile()
        file.delete()

        when:
        service.extractMd5Sum(file.toPath())

        then:
        AssertionError e = thrown()
        e.message =~ /The md5sum file '[^']*' does not exist/
    }

    void "extractMd5Sum, if md5sum file is not a regular file, then throw an assertion"() {
        when:
        service.extractMd5Sum(tempDir)

        then:
        AssertionError e = thrown()
        e.message =~ /The md5sum file '[^']*' is not a file/
    }

    void "extractMd5Sum, if md5sum file is not readable, then throw an assertion"() {
        given:
        File file = tempDir.resolve("md5sum.txt").toFile()
        file.text = HelperUtils.randomMd5sum
        file.readable = false

        when:
        service.extractMd5Sum(file.toPath())

        then:
        AssertionError e = thrown()
        e.message =~ /The md5sum file '[^']*' is not readable/
    }

    void "extractMd5Sum, if md5sum file is empty, then throw an assertion"() {
        given:
        File file = tempDir.resolve("md5sum.txt").toFile()
        file.text = ''

        when:
        service.extractMd5Sum(file.toPath())

        then:
        AssertionError e = thrown()
        e.message =~ /The md5sum file '[^']*' is empty/
    }

    @Unroll
    void "extractMd5Sum, if md5sum file is #name (#input), then throw an assertion"() {
        given:
        File file = tempDir.resolve("md5sum.txt").toFile()
        file.text = input

        when:
        service.extractMd5Sum(file.toPath())

        then:
        AssertionError e = thrown()
        e.message =~ /The md5sum file '[^']*' has not the correct form/

        where:
        name           | input
        'to short'     | '123'
        'to long'      | '1234567890123456789012345678901234567890'
        'invalid char' | '12rtz'
        'with space'   | '123 456'
    }
}
