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


import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.BamMetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.INDIVIDUAL
import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class IndividualValidatorSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                Individual,
                Realm,
                Project,
        ]
    }

    void 'validate, when column INDIVIDUAL missing, then add expected problem'() {
        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "SomeColumn\n" +
                        "SomeValue"
        )
        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), LogLevel.ERROR,
                        "Required column '${INDIVIDUAL}' is missing.")
        ]

        when:
        new IndividualValidator().validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }

    void 'validate, when column exist and individual is registered in OTP, succeeds'() {
        given:
        String INDIVIDUAL_PID = "individualPid"

        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${INDIVIDUAL}\n" +
                        "${INDIVIDUAL_PID}\n"
        )

        DomainFactory.createIndividual([pid: INDIVIDUAL_PID])

        when:
        new IndividualValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when column exist but individual is not registered in OTP, adds problems'() {
        given:
        String INDIVIDUAL_PID = "individualPid"

        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${INDIVIDUAL}\n" +
                        "${INDIVIDUAL_PID}\n"
        )

        when:
        new IndividualValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == LogLevel.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2'])
        problem.message.contains("The individual '${INDIVIDUAL_PID}' is not registered in OTP.")
    }
}
