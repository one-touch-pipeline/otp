/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.util.spreadsheet

import groovy.transform.InheritConstructors

import de.dkfz.tbi.otp.OtpRuntimeException

/**
 * Base exception for when spreadsheet parsing is impossible.
 *
 * {@link Spreadsheet} is created in constructors, whose only failure mode is exception-throwing.
 * This is the base class for all things that can go wrong in parsing a spreadsheet, except for I/O errors.
 */
@InheritConstructors
class SpreadsheetParseException extends OtpRuntimeException { }

class EmptyHeaderException extends SpreadsheetParseException {
    EmptyHeaderException(String columnAddress) {
        super("Column '${columnAddress}' doesn't have a header")
    }
}

class DuplicateHeaderException extends SpreadsheetParseException {
    DuplicateHeaderException(String header) {
        super("Duplicate column '${header}'")
    }
}

class MultipleDelimitersDetectedException extends SpreadsheetParseException {
    MultipleDelimitersDetectedException(List<String> delimiters) {
        super("detected multiple delimiter candidates, can't make a choice: ${delimiters}")
    }
}

/**
 * Thrown when delimiters couldn't be identified, but {@link Delimiter#AUTO_DETECT} was passed.
 *
 * @see Delimiter#detectDelimiter(BufferedReader)
 */
class DelimiterDetectionFailedException extends SpreadsheetParseException {
    DelimiterDetectionFailedException(String headerLine) {
        super("Well darn, our autodetect-Fu is not yet strong enough to handle the following content:\n\t${headerLine}")
    }
}
