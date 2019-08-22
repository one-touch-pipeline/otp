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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.validators

import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame

class SpaceValidatorSpec extends Specification {

    void 'validate adds expected errors'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                " x\n" +
                "x \n" +
                "x  x\n" +
                "  \n" +
                "x\n" +
                "x\n" +
                " x\n" +
                "x \n" +
                "x  x\n" +
                "  \n")
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.header.cells + context.spreadsheet.dataRows[5].cells as Set, Level.WARNING,
                        "' x' starts with a space character.", "At least one value starts with a space character."),
                new Problem(context.spreadsheet.dataRows[0].cells + context.spreadsheet.dataRows[6].cells as Set, Level.WARNING,
                        "'x ' ends with a space character.", "At least one value ends with a space character."),
                new Problem(context.spreadsheet.dataRows[1].cells + context.spreadsheet.dataRows[7].cells as Set, Level.WARNING,
                        "'x  x' contains subsequent space characters.", "At least one value contains subsequent space characters."),
                new Problem(context.spreadsheet.dataRows[2].cells + context.spreadsheet.dataRows[8].cells as Set, Level.WARNING,
                        "'  ' starts with a space character.", "At least one value starts with a space character."),
                new Problem(context.spreadsheet.dataRows[2].cells + context.spreadsheet.dataRows[8].cells as Set, Level.WARNING,
                        "'  ' ends with a space character.", "At least one value ends with a space character."),
                new Problem(context.spreadsheet.dataRows[2].cells + context.spreadsheet.dataRows[8].cells as Set, Level.WARNING,
                        "'  ' contains subsequent space characters.", "At least one value contains subsequent space characters."),
        ]

        when:
        new SpaceValidator().validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }
}
