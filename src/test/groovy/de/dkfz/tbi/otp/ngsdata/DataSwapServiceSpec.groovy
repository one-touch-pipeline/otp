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
package de.dkfz.tbi.otp.ngsdata

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

class DataSwapServiceSpec extends Specification implements DataTest, ServiceUnitTest<DataSwapService> {

    @Override
    Class[] getDomainClassesToMock() {
        []
    }

    void "completeOmittedNewValuesAndLog, valid keys, keys are used as values"() {
        given:
        Map<String, String> swapMap = [
                ("key1"): "",
                ("key2"): "key2_value",
                ("key3"): "",
        ]

        when:
        service.completeOmittedNewValuesAndLog(swapMap, "label", new StringBuilder())

        then:
        swapMap["key1"] == "key1"
        swapMap["key2"] == "key2_value"
        swapMap["key3"] == "key3"
    }

    void "completeOmittedNewValuesAndLog, empty key"() {
        given:
        Map<String, String> swapMap = [
                (""): "value",
        ]

        when:
        service.completeOmittedNewValuesAndLog(swapMap, "label", new StringBuilder())

        then:
        swapMap[""] == "value"
    }

    void "completeOmittedNewValuesAndLog, builds valid log"() {
        given:
        StringBuilder log = new StringBuilder()
        Map<String, String> swapMap = [
                ("key1"): "",
                ("key2"): "key2_value",
                (""): "key3_value",
        ]

        when:
        service.completeOmittedNewValuesAndLog(swapMap, "label", log)

        then:
        log.toString() == """
  swapping label:
    - key1 --> key1
    - key2 --> key2_value
    -  --> key3_value"""
    }
}
