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
package de.dkfz.tbi.otp.config

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqType

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*

class PropertiesValidationServiceSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ProcessingOption,
                SeqType,
        ]
    }

    static final String INVALID = 'i\nvalid'
    static final String SEQ_TYPE_RODDY_NAME = 'seqTypeRoddyName'

    PropertiesValidationService propertiesValidationService

    void setup() {
        propertiesValidationService = new PropertiesValidationService()
        propertiesValidationService.processingOptionService = new ProcessingOptionService()
        DomainFactory.createSeqType(roddyName: SEQ_TYPE_RODDY_NAME)
    }

    @Unroll
    void "test validateProcessingOptionName"() {
        given:
        ProcessingOption processingOption = new ProcessingOption(
                name: name,
                value: value,
                type: type,
        )
        processingOption.save(flush: true, validate: false)

        expect:
        propertiesValidationService.validateProcessingOptionName(name, type)?.type == problem

        where:
        name                                      | value   | type                || problem
        PIPELINE_RODDY_SNV_DEFAULT_PLUGIN_NAME    | '{}'    | null                || null
        PIPELINE_RODDY_SNV_DEFAULT_PLUGIN_NAME    | INVALID | null                || OptionProblem.ProblemType.VALUE_INVALID
        PIPELINE_RODDY_SNV_DEFAULT_PLUGIN_NAME    | '{}'    | SEQ_TYPE_RODDY_NAME || OptionProblem.ProblemType.TYPE_INVALID
        PIPELINE_RODDY_SNV_DEFAULT_PLUGIN_VERSION | '1.2.3' | SEQ_TYPE_RODDY_NAME || null
        PIPELINE_RODDY_SNV_DEFAULT_PLUGIN_VERSION | INVALID | SEQ_TYPE_RODDY_NAME || OptionProblem.ProblemType.VALUE_INVALID
        PIPELINE_RODDY_SNV_DEFAULT_PLUGIN_VERSION | '1.2.3' | INVALID             || OptionProblem.ProblemType.TYPE_INVALID
        PIPELINE_RODDY_SNV_DEFAULT_PLUGIN_VERSION | '1.2.3' | null                || OptionProblem.ProblemType.TYPE_INVALID
        and: 'test deprecated processing option'
        PIPELINE_RODDY_ALIGNMENT_BWA_PATHS        | INVALID | null                || null
    }

    void "test validateProcessingOptionName, when option is missing"() {
        expect:
        propertiesValidationService.validateProcessingOptionName(name, SEQ_TYPE_RODDY_NAME)?.type == problem

        where:
        name                                      || problem
        PIPELINE_RODDY_SNV_DEFAULT_PLUGIN_VERSION || OptionProblem.ProblemType.MISSING
        PIPELINE_RODDY_ALIGNMENT_BWA_PATHS        || null
    }
}
