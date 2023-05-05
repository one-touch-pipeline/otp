/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing.snvcalling

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.SophiaService
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

@Rollback
@Integration
class SophiaServiceIntegrationSpec extends Specification {

    SophiaService sophiaService

    void setupData() {
        DomainFactory.createAllAnalysableSeqTypes()
    }

    @Unroll
    void "samplePairForProcessing, for Sophia pipeline, only EPMBF available with #property is #value, should not return any bam file"() {
        given:
        setupData()

        RoddyBamFile.list().each {
            it.withdrawn = true
            assert it.save(flush: true)
        }
        DomainFactory.createSamplePairWithExternallyProcessedBamFiles(true, [(property): value])

        expect:
        !sophiaService.samplePairForProcessing(ProcessingPriority.NORMAL)

        where:
        property             | value
        'coverage'           | 5
        'insertSizeFile'     | null
        'maximumReadLength'  | null
    }

    @Unroll
    void "samplePairForProcessing, for Sophia pipeline, only EPMBF available with #property is #value, should return new instance"() {
        given:
        setupData()

        RoddyBamFile.list().each {
            it.withdrawn = true
            assert it.save(flush: true)
        }
        SamplePair samplePair = DomainFactory.createSamplePairWithExternallyProcessedBamFiles(true, [(property): value])

        DomainFactory.createExternallyProcessedBamFileQualityAssessment(samplePair.mergingWorkPackage1.processableBamFileInProjectFolder)
        DomainFactory.createExternallyProcessedBamFileQualityAssessment(samplePair.mergingWorkPackage2.processableBamFileInProjectFolder)

        expect:
        samplePair == sophiaService.samplePairForProcessing(ProcessingPriority.NORMAL)

        where:
        property             | value
        'coverage'           | 30
        'coverage'           | null
        'insertSizeFile'     | 'insertSize.txt'
        'maximumReadLength'  | 5
        'maximumReadLength'  | 200
    }
}
