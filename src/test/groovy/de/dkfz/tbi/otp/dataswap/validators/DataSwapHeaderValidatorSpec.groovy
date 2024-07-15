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
package de.dkfz.tbi.otp.dataswap.validators

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataCorrection.DataSwapService
import de.dkfz.tbi.otp.dataCorrection.validators.DataSwapHeaderValidator
import de.dkfz.tbi.otp.dataswap.DataSwapColumn
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.utils.MessageSourceService
import de.dkfz.tbi.otp.utils.spreadsheet.Delimiter
import de.dkfz.tbi.otp.utils.spreadsheet.Spreadsheet
import de.dkfz.tbi.otp.utils.spreadsheet.validation.ValidationContext

class DataSwapHeaderValidatorSpec extends Specification implements DataTest, DomainFactoryCore {

    DataSwapService service
    DataSwapHeaderValidator validator

    void setup() {
        service = new DataSwapService()

        service.messageSourceService = Mock(MessageSourceService) {
            _ * getMessage(_) >> { templateName, args -> templateName }
        }

        validator = new DataSwapHeaderValidator()
        validator.dataSwapService = service
    }

    void "validate, should add #problemCount problems when headers for #missingHeaders are missing"() {
        given:
        Delimiter delimiter = Delimiter.COMMA
        String headers = service.dataSwapHeaders.findAll {
            !missingHeaders*.message.any { header -> it.contains(header) }
        }.join(delimiter.delimiter as String)

        Spreadsheet spreadsheet = new Spreadsheet(headers, delimiter)
        ValidationContext context = new ValidationContext(spreadsheet)

        when:
        validator.validate(context)

        then:
        context.problems.size() == problemCount
        !missingHeaders || missingHeaders.each { header ->
            assert context.problems*.message.any { problemMessage -> problemMessage.contains(header.message) }
        }

        where:
        missingHeaders                                 | problemCount
        []                                             | 0
        [DataSwapColumn.SEQ_TRACK]                     | 1
        [DataSwapColumn.SEQ_TYPE, DataSwapColumn.LANE] | 3
    }
}
