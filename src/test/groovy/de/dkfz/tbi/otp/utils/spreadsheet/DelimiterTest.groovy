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
package de.dkfz.tbi.otp.utils.spreadsheet

import spock.lang.Specification

import static de.dkfz.tbi.otp.utils.spreadsheet.Delimiter.*

class DelimiterTest extends Specification {
    void "GetDelimiter should throw when called on AUTO_DETECT"() {
        when:
        AUTO_DETECT.delimiter

        then:
        thrown(IllegalArgumentException)
    }

    void "GetDisplayName should throw when called on AUTO_DETECT"() {
        when:
        AUTO_DETECT.displayName

        then:
        thrown(IllegalArgumentException)
    }

    void "detectDelimiter shouldn't consume input"() {
        given:
        String contents = """first line
second line
third line
fourth line"""
        BufferedReader r = new BufferedReader(new StringReader(contents))

        when:
        Delimiter.detectDelimiter(r)

        then:
        r.readLine() == "first line"
    }

    void "detectDelimiter finds the most simple case: TAB"() {
        setup:
        String contents = """Col1\tCol2
Val1\tVal2"""
        Reader r = new BufferedReader(new StringReader(contents))

        expect:
        Delimiter.detectDelimiter(r) == TAB
    }

    void "detectDelimiter finds the most simple case: #d"() {
        setup:
        String contents = """Col1${s}Col2
Val1${s}Val2"""
        Reader r = new BufferedReader(new StringReader(contents))

        expect:
        Delimiter.detectDelimiter(r) == d

        where:
        s    || d
        ','  || COMMA
        ';'  || SEMICOLON
        '\t' || TAB
    }

    void "detectDelimiter should default to TAB for single-column data"() {
        setup:
        String contents = """Col1
Val1"""
        Reader r = new BufferedReader(new StringReader(contents))

        expect:
        Delimiter.detectDelimiter(r) == TAB
    }

    void "detectDelimiter should give up if multiple delimiters are present: #d1/#d2"() {
        given:
        BufferedReader header = new BufferedReader(new StringReader("head1${d1.delimiter}head2${d2.delimiter}head3"))

        when:
        Delimiter.detectDelimiter(header)

        then:
        thrown(MultipleDelimitersDetectedException)

        where:
        d1        | d2
        TAB       | COMMA
        TAB       | SEMICOLON
        COMMA     | TAB
        COMMA     | SEMICOLON
        SEMICOLON | COMMA
        SEMICOLON | TAB
    }
}
