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
package de.dkfz.tbi.otp.ngsdata

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.project.Project

class IndividualSpec extends Specification implements DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                Individual,
                Project,
                Sample,
                SampleType,
                SeqType,
        ]
    }

    void "test validate"() {
        given:
        Individual individual = createIndividual()

        expect:
        individual.validate()
    }

    void "test validate, when pid is null"() {
        given:
        Individual individual = createIndividual()
        individual.pid = null

        expect:
        !individual.validate()
    }

    void "test validate, when type is null"() {
        given:
        Individual individual = createIndividual()
        individual.type = null

        expect:
        !individual.validate()
    }

    void "test validate, when project is null"() {
        given:
        Individual individual = createIndividual()
        individual.project = null

        expect:
        !individual.validate()
    }

    void "test validate, when pid is not unique"() {
        given:
        Individual individual1 = createIndividual([pid: 'pid'])
        assert individual1.validate()
        assert individual1.save(flush: true)
        Individual individual2 = createIndividual([pid: 'pid'], false)

        expect:
        !individual2.validate()
    }
}
