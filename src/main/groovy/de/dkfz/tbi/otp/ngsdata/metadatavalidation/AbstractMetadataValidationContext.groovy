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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation

import de.dkfz.tbi.util.spreadsheet.Spreadsheet
import de.dkfz.tbi.util.spreadsheet.validation.Problems
import de.dkfz.tbi.util.spreadsheet.validation.ValidationContext

import java.nio.charset.Charset
import java.nio.file.Path

abstract class AbstractMetadataValidationContext extends ValidationContext {

    static final Charset CHARSET = Charset.forName('UTF-8')
    static final long MAX_METADATA_FILE_SIZE_IN_MIB = 10
    static final long MAX_ADDITIONAL_FILE_SIZE_IN_GIB = 1

    final Path metadataFile
    final String metadataFileMd5sum
    final byte[] content

    protected AbstractMetadataValidationContext(Path metadataFile, String metadataFileMd5sum, Spreadsheet spreadsheet, Problems problems, byte[] content) {
        super(spreadsheet, problems)
        this.metadataFile = metadataFile
        this.content = content
        this.metadataFileMd5sum = metadataFileMd5sum
    }
}

class ContentWithPathAndProblems {
    byte[] content
    Path path
    Problems problems

    ContentWithPathAndProblems(byte[] content, Path path) {
        this.content = content
        this.path = path
        this.problems = new Problems()
    }

    ContentWithPathAndProblems(byte[] content, Path path, Problems problems) {
        this.problems = problems
        this.path = path
        this.content = content
    }
}
