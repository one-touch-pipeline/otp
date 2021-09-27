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
package de.dkfz.tbi.otp.utils

import grails.testing.gorm.DataTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Path
import java.nio.file.Paths

class Md5SumServiceSpec extends Specification implements DataTest {

    @Rule
    TemporaryFolder temporaryFolder

    @Unroll
    void "extractMd5Sum, if valid md5sum, return md5sum value"() {
        given:
        Path md5sumFile = temporaryFolder.newFile().toPath()
        md5sumFile.text = prefix + md5sum + postfix

        when:
        String extractedMd5Sum = new Md5SumService().extractMd5Sum(md5sumFile)

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
        new Md5SumService().extractMd5Sum(null)

        then:
        AssertionError e = thrown()
        e.message.contains('Parameter md5Sum is null')
    }

    void "extractMd5Sum, if md5sum file is relative, then throw an assertion"() {
        when:
        new Md5SumService().extractMd5Sum(Paths.get('tmp'))

        then:
        AssertionError e = thrown()
        e.message =~ /The md5sum file '[^']*' is not absolute/
    }

    //false positives, since rule can not recognize calling class
    @SuppressWarnings('ExplicitFlushForDeleteForUnitTestRule')
    void "extractMd5Sum, if md5sum file does not exist, then throw an assertion"() {
        given:
        File file = temporaryFolder.newFile()
        file.delete()

        when:
        new Md5SumService().extractMd5Sum(file.toPath())

        then:
        AssertionError e = thrown()
        e.message =~ /The md5sum file '[^']*' does not exist/
    }

    void "extractMd5Sum, if md5sum file is not a regular file, then throw an assertion"() {
        given:
        File file = temporaryFolder.newFolder()

        when:
        new Md5SumService().extractMd5Sum(file.toPath())

        then:
        AssertionError e = thrown()
        e.message =~ /The md5sum file '[^']*' is not a file/
    }

    void "extractMd5Sum, if md5sum file is not readable, then throw an assertion"() {
        given:
        File file = temporaryFolder.newFile()
        file.text = HelperUtils.randomMd5sum
        file.readable = false

        when:
        new Md5SumService().extractMd5Sum(file.toPath())

        then:
        AssertionError e = thrown()
        e.message =~ /The md5sum file '[^']*' is not readable/
    }

    void "extractMd5Sum, if md5sum file is empty, then throw an assertion"() {
        given:
        File file = temporaryFolder.newFile()
        file.text = ''

        when:
        new Md5SumService().extractMd5Sum(file.toPath())

        then:
        AssertionError e = thrown()
        e.message =~ /The md5sum file '[^']*' is empty/
    }

    @Unroll
    void "extractMd5Sum, if md5sum file is #name (#input), then throw an assertion"() {
        given:
        File file = temporaryFolder.newFile()
        file.text = input

        when:
        new Md5SumService().extractMd5Sum(file.toPath())

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
