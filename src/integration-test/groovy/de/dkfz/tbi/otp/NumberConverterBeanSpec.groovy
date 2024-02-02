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
package de.dkfz.tbi.otp

import grails.converters.JSON
import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.grails.web.json.JSONElement
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.AceseqQc

@Rollback
@Integration
class NumberConverterBeanSpec extends Specification {

    void "test conversion of double"() {
        given:
        String jsonString = """
{
"gender":"male",
"solutionPossible":"3",
"tcc":"0.5",
"goodnessOfFit":"0.904231625835189",
"ploidyFactor":"2.27",
"ploidy":"2",
}"""

        JSONElement json = JSON.parse(jsonString)

        when:
        AceseqQc qc = new AceseqQc(json)

        then:
        qc.tcc == 0.5d
    }
}
