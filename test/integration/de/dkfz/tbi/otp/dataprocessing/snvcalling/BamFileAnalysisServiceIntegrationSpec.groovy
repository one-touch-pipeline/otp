package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.spock.*
import spock.lang.*

import static de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair.*

class BamFileAnalysisServiceIntegrationSpec extends IntegrationSpec {

    SamplePair samplePair1
    ConfigPerProjectAndSeqType roddyConfig1
    AbstractMergedBamFile bamFile1_1
    AbstractMergedBamFile bamFile2_1

    SnvCallingService snvCallingService
    IndelCallingService indelCallingService
    AceseqService aceseqService
    SophiaService sophiaService
    RunYapsaService runYapsaService

    def setup() {
        def map = DomainFactory.createProcessableSamplePair()

        samplePair1 = map.samplePair
        bamFile1_1 = map.bamFile1
        bamFile2_1 = map.bamFile2
        roddyConfig1 = map.roddyConfig

        DomainFactory.createAllAnalysableSeqTypes()
    }


    @Unroll
    void "samplePairForProcessing shouldn't find anything for wrong referenceGenome"() {
        given:
        samplePair1."${processingStatus}" = ProcessingStatus.NEEDS_PROCESSING
        assert samplePair1.save(flush: true)
        Pipeline pipeline1 = pipeline()
        Map configProperties = [
                project: samplePair1.project,
                pipeline:  pipeline1,
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

        DomainFactory.createProcessingOptionLazy([
                name: optionName,
                type: null,
                project: null,
                value: 'foobar',
        ])

        expect:
        null == service.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY)

        where:
        processingStatus            | pipeline                                       | service               | optionName
        "sophiaProcessingStatus"    | { DomainFactory.createSophiaPipelineLazy() }   | this.sophiaService    | ProcessingOption.OptionName.PIPELINE_SOPHIA_REFERENCE_GENOME
        "aceseqProcessingStatus"    | { DomainFactory.createAceseqPipelineLazy() }   | this.aceseqService    | ProcessingOption.OptionName.PIPELINE_ACESEQ_REFERENCE_GENOME
        "runYapsaProcessingStatus"  | { DomainFactory.createRunYapsaPipelineLazy() } | this.runYapsaService  | ProcessingOption.OptionName.PIPELINE_RUNYAPSA_REFERENCE_GENOME

    }

    @Unroll
    void "samplePairForProcessing should not return a sample pair when qc of bam file is too bad"() {
        given:
        samplePair1."${processingStatus}" = ProcessingStatus.NEEDS_PROCESSING
        if (processingStatus == "aceseqProcessingStatus") {
            samplePair1.sophiaProcessingStatus = ProcessingStatus.NO_PROCESSING_NEEDED
            DomainFactory.createSophiaInstance(samplePair1)
        }
        assert samplePair1.save(flush: true)

        AbstractMergedBamFile bamFile = samplePair1.mergingWorkPackage1.bamFileInProjectFolder
        bamFile.comment = DomainFactory.createComment()
        bamFile.qcTrafficLightStatus = qc
        bamFile.save(flush: true)

        DomainFactory.createRoddyWorkflowConfig(
                seqType: samplePair1.seqType,
                project: samplePair1.project,
                pipeline: pipeline()
        )
        DomainFactory.createProcessingOptionLazy([
                name: ProcessingOption.OptionName.PIPELINE_ACESEQ_REFERENCE_GENOME,
                type: null,
                project: null,
                value: samplePair1.mergingWorkPackage1.referenceGenome.name,
        ])
        DomainFactory.createProcessingOptionLazy([
                name: ProcessingOption.OptionName.PIPELINE_SOPHIA_REFERENCE_GENOME,
                type: null,
                project: null,
                value: samplePair1.mergingWorkPackage1.referenceGenome.name,
        ])

        expect:
        !service.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY)

        where:
        processingStatus         | pipeline                                     | service                   | qc
        "indelProcessingStatus"  | { DomainFactory.createIndelPipelineLazy() }  | this.indelCallingService  | AbstractMergedBamFile.QcTrafficLightStatus.REJECTED
        "sophiaProcessingStatus" | { DomainFactory.createSophiaPipelineLazy() } | this.sophiaService        | AbstractMergedBamFile.QcTrafficLightStatus.REJECTED
        "aceseqProcessingStatus" | { DomainFactory.createAceseqPipelineLazy() } | this.aceseqService        | AbstractMergedBamFile.QcTrafficLightStatus.REJECTED
        "indelProcessingStatus"  | { DomainFactory.createIndelPipelineLazy() }  | this.indelCallingService  | AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED
        "sophiaProcessingStatus" | { DomainFactory.createSophiaPipelineLazy() } | this.sophiaService        | AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED
        "aceseqProcessingStatus" | { DomainFactory.createAceseqPipelineLazy() } | this.aceseqService        | AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED
    }


    @Unroll
    void "samplePairForProcessing should return a sample pair when qc of bam file is okay"() {
        given:
        samplePair1."${processingStatus}" = ProcessingStatus.NEEDS_PROCESSING
        if (processingStatus == "aceseqProcessingStatus") {
            samplePair1.sophiaProcessingStatus = ProcessingStatus.NO_PROCESSING_NEEDED
            DomainFactory.createSophiaInstance(samplePair1)
        }
        assert samplePair1.save(flush: true)

        AbstractMergedBamFile bamFile = samplePair1.mergingWorkPackage1.bamFileInProjectFolder
        if (qc == AbstractMergedBamFile.QcTrafficLightStatus.ACCEPTED) {
            bamFile.comment = DomainFactory.createComment()
        }
        bamFile.qcTrafficLightStatus = qc
        bamFile.save(flush: true)

        DomainFactory.createRoddyWorkflowConfig(
                seqType: samplePair1.seqType,
                project: samplePair1.project,
                pipeline: pipeline()
        )
        DomainFactory.createProcessingOptionLazy([
                name: ProcessingOption.OptionName.PIPELINE_ACESEQ_REFERENCE_GENOME,
                type: null,
                project: null,
                value: samplePair1.mergingWorkPackage1.referenceGenome.name,
        ])
        DomainFactory.createProcessingOptionLazy([
                name: ProcessingOption.OptionName.PIPELINE_SOPHIA_REFERENCE_GENOME,
                type: null,
                project: null,
                value: samplePair1.mergingWorkPackage1.referenceGenome.name,
        ])

        expect:
        samplePair1 == service.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY)

        where:
        processingStatus         | pipeline                                     | service                   | qc
        "indelProcessingStatus"  | { DomainFactory.createIndelPipelineLazy() }  | this.indelCallingService  | AbstractMergedBamFile.QcTrafficLightStatus.ACCEPTED
        "sophiaProcessingStatus" | { DomainFactory.createSophiaPipelineLazy() } | this.sophiaService        | AbstractMergedBamFile.QcTrafficLightStatus.ACCEPTED
        "aceseqProcessingStatus" | { DomainFactory.createAceseqPipelineLazy() } | this.aceseqService        | AbstractMergedBamFile.QcTrafficLightStatus.ACCEPTED
        "indelProcessingStatus"  | { DomainFactory.createIndelPipelineLazy() }  | this.indelCallingService  | AbstractMergedBamFile.QcTrafficLightStatus.QC_PASSED
        "sophiaProcessingStatus" | { DomainFactory.createSophiaPipelineLazy() } | this.sophiaService        | AbstractMergedBamFile.QcTrafficLightStatus.QC_PASSED
        "aceseqProcessingStatus" | { DomainFactory.createAceseqPipelineLazy() } | this.aceseqService        | AbstractMergedBamFile.QcTrafficLightStatus.QC_PASSED
    }
}
