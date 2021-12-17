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
package de.dkfz.tbi.otp.dataprocessing

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase

class ProcessingOptionSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                ProcessingOption,
        ]
    }

    @Unroll
    void "validate #optionName with #value, should be valid"() {
        when:
        ProcessingOption processingOption = new ProcessingOption(
                name: optionName,
                type: null,
                value: value,

        )
        then:
        processingOption.validate()

        where:
        optionName                                                    | value
        ProcessingOption.OptionName.TICKET_SYSTEM_AUTO_IMPORT_ENABLED | 'true'
        ProcessingOption.OptionName.TICKET_SYSTEM_AUTO_IMPORT_ENABLED | 'false'
        ProcessingOption.OptionName.MAXIMUM_SFTP_CONNECTIONS          | '5'
    }

    @Unroll
    void "validate #optionName with #value, should be invalid"() {
        when:
        ProcessingOption processingOption = new ProcessingOption(
                name: optionName,
                type: null,
                value: value,

        )
        then:
        TestCase.assertValidateError(processingOption, 'value', 'validator.invalid', value)

        where:
        optionName                                                    | value
        ProcessingOption.OptionName.TICKET_SYSTEM_AUTO_IMPORT_ENABLED | 't'
        ProcessingOption.OptionName.TICKET_SYSTEM_AUTO_IMPORT_ENABLED | 'f'
        ProcessingOption.OptionName.MAXIMUM_SFTP_CONNECTIONS          | 'text'
    }

    @Unroll
    void "validate #optionName with invalid value #value and obsolete date, should be valid"() {
        when:
        ProcessingOption processingOption = new ProcessingOption(
                name: optionName,
                type: null,
                value: value,
                dateObsoleted: new Date(),
        )
        then:
        processingOption.validate()

        where:
        optionName                                                    | value
        ProcessingOption.OptionName.TICKET_SYSTEM_AUTO_IMPORT_ENABLED | 't'
        ProcessingOption.OptionName.TICKET_SYSTEM_AUTO_IMPORT_ENABLED | 'f'
        ProcessingOption.OptionName.MAXIMUM_SFTP_CONNECTIONS          | 'text'
    }

    void "validate pipeline min coverage with invalid type #type and obsolete date, should be valid"() {
        when:
        ProcessingOption processingOption = new ProcessingOption(
                name: ProcessingOption.OptionName.PIPELINE_MIN_COVERAGE,
                type: 'test',
                value: '20.0',
                dateObsoleted: new Date(),
        )
        then:
        processingOption.validate()
    }
}
