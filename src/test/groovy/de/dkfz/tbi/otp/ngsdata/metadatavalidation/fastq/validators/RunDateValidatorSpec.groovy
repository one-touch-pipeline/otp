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

import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class RunDateValidatorSpec extends Specification {

    void 'validate context with 3 errors'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.RUN_DATE}\n" +
                        "2016-01-01\n" +
                        "2016-01-32\n" +
                        "2016-0s1-22\n" +
                        "9999-01-01\n")
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[1].cells as Set, Level.ERROR,
                        "The format of the run date '2016-01-32' is invalid, it must match yyyy-MM-dd.", "The format of at least one run date is invalid, it must match yyyy-MM-dd."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, Level.ERROR,
                        "The format of the run date '2016-0s1-22' is invalid, it must match yyyy-MM-dd.", "The format of at least one run date is invalid, it must match yyyy-MM-dd."),
                new Problem(context.spreadsheet.dataRows[3].cells as Set, Level.ERROR,
                        "The run date '9999-01-01' must not be from the future.", "No run date may be from the future."),
                ]

        when:
        new RunDateValidator().validate(context)


        then:
        containSame(expectedProblems, context.problems)
    }

    void 'validate context without RUN_DATE column'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()

        when:
        new RunDateValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        problem.message == "Required column 'RUN_DATE' is missing."
    }
}
