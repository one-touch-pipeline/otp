/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.infrastructure.fastqc

import groovy.transform.TupleConstructor

/**
 * Support for compression formats not supported by FastQC natively.
 */
@TupleConstructor
enum CompressionFormat {
    // gzip compressed files are supported by FastQC, so they're not listed here
    /** The FastQC tool should work for bz2 files, we get often problems with this file type. Therefore we extract the files ourselves. */
    BZIP2(".bz2", "bzip2 --decompress"),
    TAR_BZIP2(".tar.bz2", "bzip2 --decompress | tar --extract --to-stdout"),
    TAR_GZIP(".tar.gz", "gzip --decompress | tar --extract --to-stdout"),

    final String suffix
    final String decompressionCommand

    static CompressionFormat getUsedFormat(String fileName) {
        // sort by length descending, so eg. ".tar.bz2" is found before ".bz2"
        return values().sort { -it.suffix.length() }.find { fileName.endsWith(it.suffix) }
    }
}
