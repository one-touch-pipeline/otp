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
package de.dkfz.tbi.otp.dataprocessing.snvcalling

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

@Rollback
@Integration
class AceSeqServiceIntegrationSpec extends Specification {

    SamplePair samplePair1
    ConfigPerProjectAndSeqType roddyConfig1
    AbstractBamFile bamFile1
    AbstractBamFile bamFile2

    AceseqService aceseqService

    void setupData() {
        Map map = DomainFactory.createProcessableSamplePair()

        samplePair1 = map.samplePair
        bamFile1 = map.bamFile1
        bamFile2 = map.bamFile2
        roddyConfig1 = map.roddyConfig

        DomainFactory.createAllAnalysableSeqTypes()
    }

    void "samplePairForProcessing, for Aceseq pipeline, when sophia has not run, should not return SamplePair"() {
        given:
        setupData()
        prepareSophiaForAceseqBase()

        expect:
        aceseqService.samplePairForProcessing(ProcessingPriority.NORMAL) == null
    }

    void "samplePairForProcessing, for Aceseq pipeline, when last sophia instance is running and not withdrawn and an older finish exist, should not return SamplePair"() {
        given:
        setupData()
        prepareSophiaForAceseq([processingState: AnalysisProcessingStates.FINISHED, withdrawn: false], [processingState: AnalysisProcessingStates.IN_PROGRESS, withdrawn: false])

        expect:
        aceseqService.samplePairForProcessing(ProcessingPriority.NORMAL) == null
    }

    void "samplePairForProcessing, for Aceseq pipeline, when last sophia instance is running and withdrawn and an older finish exist, should return SamplePair"() {
        given:
        setupData()
        prepareSophiaForAceseq([processingState: AnalysisProcessingStates.FINISHED, withdrawn: false], [processingState: AnalysisProcessingStates.IN_PROGRESS, withdrawn: true])

        expect:
        samplePair1 == aceseqService.samplePairForProcessing(ProcessingPriority.NORMAL)
    }

    void "samplePairForProcessing, for Aceseq pipeline, when all sophia instances are withdrawn, should not return SamplePair"() {
        given:
        setupData()
        prepareSophiaForAceseq([processingState: AnalysisProcessingStates.FINISHED, withdrawn: true], [processingState: AnalysisProcessingStates.FINISHED, withdrawn: true])

        expect:
        aceseqService.samplePairForProcessing(ProcessingPriority.NORMAL) == null
    }

    void "samplePairForProcessing, for ACEseq pipeline, coverage is not high enough, should not return SamplePair"() {
        given:
        setupData()
        prepareSophiaForAceseq([:], [:])
        DomainFactory.createProcessingOptionLazy([
                name   : ProcessingOption.OptionName.PIPELINE_MIN_COVERAGE,
                type   : Pipeline.Type.ACESEQ.toString(),
                project: null,
                value  : "40",
        ])

        expect:
        !aceseqService.samplePairForProcessing(ProcessingPriority.NORMAL)
    }

    private void prepareSophiaForAceseqBase() {
        samplePair1.sophiaProcessingStatus = SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED
        samplePair1.save(flush: true)
        DomainFactory.createProcessingOptionLazy([
                name   : ProcessingOption.OptionName.PIPELINE_ACESEQ_REFERENCE_GENOME,
                type   : null,
                project: null,
                value  : samplePair1.mergingWorkPackage1.referenceGenome.name,
        ])
        DomainFactory.createRoddyWorkflowConfig(
                seqType: samplePair1.seqType,
                project: samplePair1.project,
                pipeline: DomainFactory.createAceseqPipelineLazy(),
        )
    }

    private void prepareSophiaForAceseq(Map propertiesSophia1, Map propertiesSophia2) {
        prepareSophiaForAceseqBase()

        Map defaultMap = [
                processingState   : AnalysisProcessingStates.FINISHED,
                withdrawn         : false,
                sampleType1BamFile: bamFile1,
                sampleType2BamFile: bamFile2,
        ]

        DomainFactory.createSophiaInstance(samplePair1, defaultMap + propertiesSophia1)
        DomainFactory.createSophiaInstance(samplePair1, defaultMap + propertiesSophia2)
    }
}
