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

package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.TestCase.assertContainSame

class TagmentationBasedLibraryValidatorSpec extends Specification {

    void 'validate adds expected error'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.TAGMENTATION_BASED_LIBRARY}\n" +
                        "1\n" +
                        "0\n" +
                        "True\n" +
                        "TRUE\n" +
                        "true\n" +
                        "false\n" +
                        "FALSE\n" +
                        "test\n" +
                        "\n")
        Collection<Problem> expectedProblems = [

                new Problem(context.spreadsheet.dataRows[1].cells as Set, Level.ERROR,
                        "The tagmentation based library column value should be '1', 'true', 'false' or an empty string instead of '0'.", "The tagmentation based library column value should be '1', 'true', 'false' or an empty string."),
                new Problem(context.spreadsheet.dataRows[7].cells as Set, Level.ERROR,
                        "The tagmentation based library column value should be '1', 'true', 'false' or an empty string instead of 'test'.", "The tagmentation based library column value should be '1', 'true', 'false' or an empty string."),
        ]

        when:
        new TagmentationBasedLibraryValidator().validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }
}
