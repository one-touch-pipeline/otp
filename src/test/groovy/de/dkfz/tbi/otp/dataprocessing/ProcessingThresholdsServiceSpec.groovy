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
package de.dkfz.tbi.otp.dataprocessing

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

class ProcessingThresholdsServiceSpec extends Specification implements DataTest, DomainFactoryCore {
    private static final Integer LANES = 1
    private static final Double COVERAGE = null

    @Override
    Class[] getDomainClassesToMock() {
        return [
                Individual,
                ProcessingThresholds,
                Project,
                Sample,
                SampleType,
                SeqType,
        ]
    }

    void "test createUpdateOrDelete, test create"() {
        given:
        ProcessingThresholds processingThresholds
        Project project = createProject()
        SampleType sampleType = createSampleType()
        SeqType seqType = createSeqType()

        ProcessingThresholdsService service = new ProcessingThresholdsService()

        when:
        processingThresholds = service.createUpdateOrDelete(project, sampleType, seqType, LANES, COVERAGE)

        then:
        processingThresholds.project == project
        processingThresholds.sampleType == sampleType
        processingThresholds.seqType == seqType
        processingThresholds.numberOfLanes == LANES
        processingThresholds.coverage == COVERAGE
    }

    void "test createUpdateOrDelete, test update"() {
        given:
        ProcessingThresholds processingThresholds = DomainFactory.createProcessingThresholds()
        ProcessingThresholdsService service = new ProcessingThresholdsService()

        expect:
        processingThresholds.numberOfLanes != LANES
        processingThresholds.coverage != COVERAGE

        when:
        service.createUpdateOrDelete(processingThresholds.project, processingThresholds.sampleType, processingThresholds.seqType, 1, COVERAGE)

        then:
        processingThresholds.numberOfLanes == LANES
        processingThresholds.coverage == COVERAGE
    }

    void "test createUpdateOrDelete, test delete"() {
        given:
        ProcessingThresholds processingThresholds = DomainFactory.createProcessingThresholds()
        ProcessingThresholdsService service = new ProcessingThresholdsService()

        when:
        service.createUpdateOrDelete(processingThresholds.project, processingThresholds.sampleType, processingThresholds.seqType, null, null)

        then:
        ProcessingThresholds.all.size() == 0
    }
}
