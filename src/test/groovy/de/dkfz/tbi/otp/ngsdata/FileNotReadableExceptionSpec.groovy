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

class FileNotReadableExceptionSpec extends Specification implements DataTest {

    void testFileNotReadableExceptionString() {
        given:
        FileNotReadableException e = new FileNotReadableException("tmp")

        expect:
        "can not read file: tmp" == e.message
    }

    void testFileNotReadableExceptionStringParamIsNull() {
        given:
        FileNotReadableException e = new FileNotReadableException(null as String)

        expect:
        "can not read file: null" == e.message
    }

    void testFileNotReadableExceptionFile() {
        given:
        FileNotReadableException e = new FileNotReadableException(new File("tmp"))

        expect:
        "can not read file: tmp" == e.message
    }

    void testFileNotReadableExceptionFileParamIsNull() {
        given:
        FileNotReadableException e = new FileNotReadableException(null as File)

        expect:
        "can not read file: null" == e.message
    }
}
