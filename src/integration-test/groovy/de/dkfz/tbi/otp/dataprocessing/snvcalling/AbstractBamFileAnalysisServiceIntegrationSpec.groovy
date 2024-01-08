/*
 * Copyright 2011-2020 The OTP authors
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
import spock.lang.*

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.QcTrafficLightStatus
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

import static de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair.ProcessingStatus

@Rollback
@Integration
class AbstractBamFileAnalysisServiceIntegrationSpec extends Specification {

    SamplePair samplePair1
    ConfigPerProjectAndSeqType roddyConfig1
    AbstractBamFile bamFile1
    AbstractBamFile bamFile2

    @Shared
    SnvCallingService snvCallingService

    @Shared
    IndelCallingService indelCallingService

    @Shared
    AceseqService aceseqService

    @Shared
    SophiaService sophiaService

    @Shared
    RunYapsaService runYapsaService

    void setupData() {
        Map map = DomainFactory.createProcessableSamplePair()

        samplePair1 = map.samplePair
        bamFile1 = map.bamFile1
        bamFile2 = map.bamFile2
        roddyConfig1 = map.roddyConfig

        DomainFactory.createAllAnalysableSeqTypes()

        DomainFactory.createReferenceGenomeAndAnalysisProcessingOptions()
    }

    void setupDataExtended(String processingStatus, Closure pipeline, QcTrafficLightStatus qc) {
        setupData()

        samplePair1."${processingStatus}" = ProcessingStatus.NEEDS_PROCESSING
        if (processingStatus == "aceseqProcessingStatus") {
            samplePair1.sophiaProcessingStatus = ProcessingStatus.NO_PROCESSING_NEEDED
            DomainFactory.createSophiaInstance(samplePair1)
        }
        assert samplePair1.save(flush: true)

        AbstractBamFile bamFile = samplePair1.mergingWorkPackage1.bamFileInProjectFolder
        bamFile.comment = DomainFactory.createComment()
        bamFile.qcTrafficLightStatus = qc
        bamFile.save(flush: true)

        DomainFactory.createRoddyWorkflowConfig(
                seqType: samplePair1.seqType,
                project: samplePair1.project,
                pipeline: pipeline()
        )
        DomainFactory.createProcessingOptionLazy([
                name   : ProcessingOption.OptionName.PIPELINE_ACESEQ_REFERENCE_GENOME,
                type   : null,
                project: null,
                value  : samplePair1.mergingWorkPackage1.referenceGenome.name,
        ])
        DomainFactory.createProcessingOptionLazy([
                name   : ProcessingOption.OptionName.PIPELINE_SOPHIA_REFERENCE_GENOME,
                type   : null,
                project: null,
                value  : samplePair1.mergingWorkPackage1.referenceGenome.name,
        ])
    }

    @Unroll
    void "samplePairForProcessing shouldn't find anything for wrong referenceGenome"() {
        given:
        setupData()
        samplePair1."${processingStatus}" = ProcessingStatus.NEEDS_PROCESSING
        assert samplePair1.save(flush: true)
        Pipeline pipeline1 = pipeline()
        Map configProperties = [
                project : samplePair1.project,
                pipeline: pipeline1,
        ]
        if (pipeline1.usesRoddy()) {
            DomainFactory.createRoddyWorkflowConfig(configProperties + [
                    seqType: samplePair1.seqType,
            ])
        } else if (pipeline1.name == Pipeline.Name.RUN_YAPSA) {
            DomainFactory.createRunYapsaConfig(configProperties)
        } else {
            throw new UnsupportedOperationException("cannot figure out which workflow config to create")
        }

        expect:
        service().samplePairForProcessing(ProcessingPriority.NORMAL) == null

        where:
        processingStatus           | pipeline                                       | service                  | optionName
        "sophiaProcessingStatus"   | { DomainFactory.createSophiaPipelineLazy() }   | { this.sophiaService }   | ProcessingOption.OptionName.PIPELINE_SOPHIA_REFERENCE_GENOME
        "aceseqProcessingStatus"   | { DomainFactory.createAceseqPipelineLazy() }   | { this.aceseqService }   | ProcessingOption.OptionName.PIPELINE_ACESEQ_REFERENCE_GENOME
        "runYapsaProcessingStatus" | { DomainFactory.createRunYapsaPipelineLazy() } | { this.runYapsaService } | ProcessingOption.OptionName.PIPELINE_RUNYAPSA_REFERENCE_GENOME
    }

    @Unroll
    void "samplePairForProcessing should return a sample pair when qc of bam file is okay"() {
        given:
        setupDataExtended(processingStatus, pipeline, qc)

        expect:
        samplePair1 == service().samplePairForProcessing(ProcessingPriority.NORMAL)

        where:
        processingStatus         | pipeline                                     | service                 | qc
        "indelProcessingStatus"  | { DomainFactory.createIndelPipelineLazy() }  | { indelCallingService } | AbstractBamFile.QcTrafficLightStatus.ACCEPTED
        "sophiaProcessingStatus" | { DomainFactory.createSophiaPipelineLazy() } | { sophiaService }       | AbstractBamFile.QcTrafficLightStatus.ACCEPTED
        "aceseqProcessingStatus" | { DomainFactory.createAceseqPipelineLazy() } | { aceseqService }       | AbstractBamFile.QcTrafficLightStatus.ACCEPTED
        "indelProcessingStatus"  | { DomainFactory.createIndelPipelineLazy() }  | { indelCallingService } | AbstractBamFile.QcTrafficLightStatus.QC_PASSED
        "sophiaProcessingStatus" | { DomainFactory.createSophiaPipelineLazy() } | { sophiaService }       | AbstractBamFile.QcTrafficLightStatus.QC_PASSED
        "aceseqProcessingStatus" | { DomainFactory.createAceseqPipelineLazy() } | { aceseqService }       | AbstractBamFile.QcTrafficLightStatus.QC_PASSED
    }

    @Unroll
    void "samplePairForProcessing should not return a sample pair when project is archived"() {
        given:
        setupDataExtended(processingStatus, pipeline, AbstractBamFile.QcTrafficLightStatus.ACCEPTED)
        samplePair1.project.archived = true
        samplePair1.project.save(flush: true)

        expect:
        !service().samplePairForProcessing(ProcessingPriority.NORMAL)

        where:
        processingStatus         | pipeline                                     | service
        "indelProcessingStatus"  | { DomainFactory.createIndelPipelineLazy() }  | { indelCallingService }
        "sophiaProcessingStatus" | { DomainFactory.createSophiaPipelineLazy() } | { sophiaService }
        "aceseqProcessingStatus" | { DomainFactory.createAceseqPipelineLazy() } | { aceseqService }
    }
}
