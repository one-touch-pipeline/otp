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
package de.dkfz.tbi.otp.dataswap

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.springframework.context.ApplicationContext
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import spock.lang.Specification

import de.dkfz.tbi.otp.dataCorrection.DataSwapService
import de.dkfz.tbi.otp.dataCorrection.DataSwapValidator
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.utils.MessageSourceService
import de.dkfz.tbi.otp.utils.spreadsheet.Delimiter

class DataSwapServiceSpec extends Specification implements DataTest, ServiceUnitTest<DataSwapService>, DomainFactoryCore {

    void setup() {
        service.messageSourceService = Mock(MessageSourceService) {
            _ * getMessage(_) >> { templateName, args -> templateName }
        }
    }

    void "getDataSwapHeaders, should produce expected number of headers"() {
        expect:
        service.dataSwapHeaders.size() == 19
    }

    void "checkFileType, should throw, when original file type is different from csv"() {
        given:
        MultipartFile file = new MockMultipartFile("file.csv", "file.xml", "text/plain", 'some content'.bytes)

        when:
        service.checkFileType(file)

        then:
        thrown(DataSwapValidationException)
    }

    void "validate, should call all dataSwapValidator instances and check file type"() {
        given:
        MultipartFile file = new MockMultipartFile("file.csv", "file.csv", "text/plain", 'some content'.bytes)
        service.applicationContext = Mock(ApplicationContext) {
            1 * getBeansOfType(DataSwapValidator) >> [
                    "dataSwapValidator1": Mock(DataSwapValidator) { 1 * validate(_) },
                    "dataSwapValidator2": Mock(DataSwapValidator) { 1 * validate(_) },
            ]
        }

        expect:
        service.validate(file, Delimiter.COMMA)
    }
}
