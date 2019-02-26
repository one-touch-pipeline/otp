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

package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import grails.test.mixin.Mock
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.BamMetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.SAMPLE_TYPE
import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

@Mock([SampleType])
class SampleTypeValidatorSpec extends Specification {

    void 'validate, when column SAMPLE_TYPE missing, then add expected problem'() {

        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "SomeColumn\n"+
                        "SomeValue"
        )
        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), Level.ERROR,
                        "Required column '${SAMPLE_TYPE}' is missing.")
        ]

        when:
        new SampleTypeValidator().validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }

    void 'validate, when column exist and sampleType is registered in OTP, succeeds'() {

        given:
        String SAMPLE_TYPE_NAME = "sampleTypeName"

        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${SAMPLE_TYPE}\n" +
                        "${SAMPLE_TYPE_NAME}\n"
        )

        DomainFactory.createSampleType([name: SAMPLE_TYPE_NAME])

        when:
        new SampleTypeValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when column exist but sampleType is not registered in OTP, adds problems'() {

        given:
        String SAMPLE_TYPE_NAME = "sampleTypeName"

        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${SAMPLE_TYPE}\n" +
                        "${SAMPLE_TYPE_NAME}\n"
        )

        when:
        new SampleTypeValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2'])
        problem.message.contains("The sample type '${SAMPLE_TYPE_NAME}' is not registered in OTP.")
    }
}
