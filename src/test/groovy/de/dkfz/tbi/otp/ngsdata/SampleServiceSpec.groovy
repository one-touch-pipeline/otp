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
import spock.lang.Shared
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.project.Project

class SampleServiceSpec extends Specification implements DataTest, DomainFactoryCore {
    @Shared
    SampleService sampleService

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

    void setupSpec() {
        sampleService = new SampleService()
    }

    void "test getSamples, with one sample"() {
        given:
        Individual individual = createIndividual()

        Sample sample1 = new Sample(
                individual: individual,
                sampleType: createSampleType(name: "name1")
        )
        assert sample1.save(flush: true)

        expect:
        [sample1] == sampleService.getSamplesByIndividual(individual)
    }

    void "test getSamples, with multiple samples"() {
        given:
        Individual individual = createIndividual()

        Sample sample1 = createSample([individual: individual])
        Sample sample2 = createSample([individual: individual])

        expect:
        [sample1, sample2] == sampleService.getSamplesByIndividual(individual)
    }
}
