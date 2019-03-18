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

import grails.test.spock.IntegrationSpec

class SampleTypePerProjectIntegrationSpec extends IntegrationSpec {
    Project project2
    Project project1
    SampleType sampleType1
    SampleType sampleType2
    Sample sampleProject1Type1
    Sample sampleProject1Type2
    Sample sampleProject2Type1
    Sample sampleProject2Type2

    void setupSpec() {
        DomainFactory.createAllAlignableSeqTypes()
    }

    void cleanupSpec() {
        SeqType.findAll()*.delete(flush: true)
    }

    void setup() {
        // create cross-matrix of two projects and two sample types
        // (slightly involved because project is only linked via individual)
        project1 = DomainFactory.createProject()
        project2 = DomainFactory.createProject()
        sampleType1 = DomainFactory.createSampleType()
        sampleType2 = DomainFactory.createSampleType()

        sampleProject1Type1 = DomainFactory.createSample([
                individual: DomainFactory.createIndividual([project: project1]),
                sampleType: sampleType1,
        ])
        sampleProject1Type2 = DomainFactory.createSample([
                individual: DomainFactory.createIndividual([project: project1]),
                sampleType: sampleType2,
        ])
        sampleProject2Type1 = DomainFactory.createSample([
                individual: DomainFactory.createIndividual([project: project2]),
                sampleType: sampleType1,
        ])
        sampleProject2Type2 = DomainFactory.createSample([
                individual: DomainFactory.createIndividual([project: project2]),
                sampleType: sampleType2,
        ])
    }
}
