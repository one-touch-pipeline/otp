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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.BamMetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class SampleTypeIndividualValidatorSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                Individual,
                Project,
                Sample,
                SampleType,
        ]
    }

    void 'validate, when column(s) is/are missing, adds error(s)'() {
        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext("""\
${header}
value1\tvalue2
""")

        when:
        new SampleTypeIndividualValidator().validate(context)

        then:
        containSame(context.problems*.message, messages)

        where:
        header                                                || messages
        "${BamMetadataColumn.SAMPLE_TYPE.name()}\tindividual" || ["Required column 'INDIVIDUAL' is missing."]
        "sampleType\t${BamMetadataColumn.INDIVIDUAL.name()}"  || ["Required column 'SAMPLE_TYPE' is missing."]
        "individual\tsampleType"                              || ["Required column 'INDIVIDUAL' is missing.", "Required column 'SAMPLE_TYPE' is missing."]
    }

    void 'validate, when combinations are in database, adds no problem'() {
        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext("""\
${BamMetadataColumn.SAMPLE_TYPE.name()}\t${BamMetadataColumn.INDIVIDUAL.name()}
SampleType1\tIndividual1\t
""")
        Individual individual = DomainFactory.createIndividual(pid: 'Individual1')
        SampleType sampleType = DomainFactory.createSampleType(name: 'sampletype1')
        DomainFactory.createSample(sampleType: sampleType, individual: individual)

        when:
        new SampleTypeIndividualValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when combinations are not in database, adds expected problem'() {
        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext("""\
${BamMetadataColumn.SAMPLE_TYPE.name()}\t${BamMetadataColumn.INDIVIDUAL.name()}
SampleType1\tIndividual1\t
""")
        Individual individual = DomainFactory.createIndividual(pid: 'anotherIndividual')
        SampleType sampleType = DomainFactory.createSampleType(name: 'anothersampletype')
        DomainFactory.createSample(sampleType: sampleType, individual: individual)

        when:
        new SampleTypeIndividualValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == LogLevel.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2', 'B2'])
        problem.message.contains("The sample as combination of the individual 'Individual1' and the sample type 'SampleType1' is not registered in OTP.")
    }
}
