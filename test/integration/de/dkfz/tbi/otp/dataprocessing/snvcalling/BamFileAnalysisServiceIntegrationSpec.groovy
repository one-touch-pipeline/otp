package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import grails.test.spock.*
import spock.lang.*

import static de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair.*

class BamFileAnalysisServiceIntegrationSpec extends IntegrationSpec {

    final static String ARBITRARY_INSTANCE_NAME = '2014-08-25_15h32'
    final static double COVERAGE_TOO_LOW = 20.0

    SamplePair samplePair1
    SnvConfig snvConfig1
    ConfigPerProject roddyConfig1
    ExternalScript script1
    ExternalScript joinScript
    AbstractMergedBamFile bamFile1_1
    AbstractMergedBamFile bamFile2_1

    SnvCallingService snvCallingService
    IndelCallingService indelCallingService
    AceseqService aceseqService
    SophiaService sophiaService

    def setup() {
        def map = DomainFactory.createProcessableSamplePair()

        samplePair1 = map.samplePair
        bamFile1_1 = map.bamFile1
        bamFile2_1 = map.bamFile2
        snvConfig1 = map.snvConfig
        script1 = map.script
        joinScript = map.joinScript
        roddyConfig1 = map.roddyConfig

        DomainFactory.createAllAnalysableSeqTypes()
    }


    @Unroll
    void "samplePairForProcessing for SnvCalling returns samplePair"() {
        given:

        samplePair1.snvProcessingStatus = ProcessingStatus.NEEDS_PROCESSING
        assert samplePair1.save(flush: true)

        expect:
        samplePair1 == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY, config)

        where:
        config << [SnvConfig, RoddyWorkflowConfig]

    }

    @Unroll
    void "samplePairForProcessing for Instance #processingStatus returns samplePair"() {
        given:
        samplePair1."${processingStatus}" = ProcessingStatus.NEEDS_PROCESSING
        if (processingStatus == "aceseqProcessingStatus") {
            samplePair1.sophiaProcessingStatus = ProcessingStatus.NO_PROCESSING_NEEDED
            DomainFactory.createSophiaInstance(samplePair1)
        }
        assert samplePair1.save(flush: true)
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
        samplePair1 == service.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY, RoddyWorkflowConfig)

        where:
        processingStatus         | pipeline                                     | service
        "indelProcessingStatus"  | { DomainFactory.createIndelPipelineLazy() }  | this.indelCallingService
        "sophiaProcessingStatus" | { DomainFactory.createSophiaPipelineLazy() } | this.sophiaService
        "aceseqProcessingStatus" | { DomainFactory.createAceseqPipelineLazy() } | this.aceseqService
    }

    @Unroll
    void "samplePairForProcessing, wrong referenceGenome"() {
        given:
        samplePair1."${processingStatus}" = ProcessingStatus.NEEDS_PROCESSING
        assert samplePair1.save(flush: true)
        DomainFactory.createRoddyWorkflowConfig(
                seqType: samplePair1.seqType,
                project: samplePair1.project,
                pipeline:  pipeline()
        )
        DomainFactory.createProcessingOptionLazy([
                name: optionName,
                type: null,
                project: null,
                value: 'foobar'
        ])

        expect:
        null == service.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY, RoddyWorkflowConfig)

        where:
        processingStatus         | pipeline                                     | service            | optionName
        "sophiaProcessingStatus" | { DomainFactory.createSophiaPipelineLazy() } | this.sophiaService | ProcessingOption.OptionName.PIPELINE_SOPHIA_REFERENCE_GENOME
        "aceseqProcessingStatus" | { DomainFactory.createAceseqPipelineLazy() } | this.aceseqService | ProcessingOption.OptionName.PIPELINE_ACESEQ_REFERENCE_GENOME

    }

    @Unroll
    void "samplePairForProcessing does not return a sample pair since qc of bam file is too bad"() {
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
        !service.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY, RoddyWorkflowConfig)

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
    void "samplePairForProcessing returns a sample pair since qc of bam file is okey"() {
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
        samplePair1 == service.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY, RoddyWorkflowConfig)

        where:
        processingStatus         | pipeline                                     | service                   | qc
        "indelProcessingStatus"  | { DomainFactory.createIndelPipelineLazy() }  | this.indelCallingService  | AbstractMergedBamFile.QcTrafficLightStatus.ACCEPTED
        "sophiaProcessingStatus" | { DomainFactory.createSophiaPipelineLazy() } | this.sophiaService        | AbstractMergedBamFile.QcTrafficLightStatus.ACCEPTED
        "aceseqProcessingStatus" | { DomainFactory.createAceseqPipelineLazy() } | this.aceseqService        | AbstractMergedBamFile.QcTrafficLightStatus.ACCEPTED
        "indelProcessingStatus"  | { DomainFactory.createIndelPipelineLazy() }  | this.indelCallingService  | AbstractMergedBamFile.QcTrafficLightStatus.QC_PASSED
        "sophiaProcessingStatus" | { DomainFactory.createSophiaPipelineLazy() } | this.sophiaService        | AbstractMergedBamFile.QcTrafficLightStatus.QC_PASSED
        "aceseqProcessingStatus" | { DomainFactory.createAceseqPipelineLazy() } | this.aceseqService        | AbstractMergedBamFile.QcTrafficLightStatus.QC_PASSED
    }


    void "samplePairForProcessing when #status is NEEDS_PROCESSING but the #service is requested"() {
        given:
        samplePair1[status] = ProcessingStatus.NEEDS_PROCESSING
        samplePair1[otherStatus] = ProcessingStatus.NO_PROCESSING_NEEDED
        assert samplePair1.save(flush: true)
        DomainFactory.createRoddyWorkflowConfig(
                seqType: samplePair1.seqType,
                project: samplePair1.project,
                pipeline: DomainFactory.createIndelPipelineLazy()
        )

        expect:
        if (service == "INDEL") {
            assert null == indelCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY, RoddyWorkflowConfig)
        } else {
            assert null == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY, SnvConfig)
        }


        where:
        status                  | otherStatus             | service
        "indelProcessingStatus" | "snvProcessingStatus"   | "SNV"
        "snvProcessingStatus"   | "indelProcessingStatus" | "INDEL"

    }


    @Unroll
    void "samplePairForProcessing when processing status #processingStatus"() {
        given:
        samplePair1.snvProcessingStatus = processingStatus
        assert samplePair1.save(flush: true)

        expect:
        null == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY, SnvConfig)

        where:
        processingStatus << [ProcessingStatus.NO_PROCESSING_NEEDED, ProcessingStatus.DISABLED]
    }


    @Unroll
    void "samplePairForProcessing when config has wrong #property"() {
        given:
        if (property == "project") {
            snvConfig1.project = DomainFactory.createProject(name: "otherProject", dirName: "tmp")
        } else {
            snvConfig1.seqType = DomainFactory.createExomeSeqType()
        }
        assert snvConfig1.save(flush: true)

        expect:
        null == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY, SnvConfig)

        where:
        property << ["project", "seqType"]
    }

    void "samplePairForProcessing when config is obsolete"() {
        given:
        snvConfig1.obsoleteDate = new Date()
        assert snvConfig1.save(flush: true)

        expect:
        null == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY, SnvConfig)
    }

    void "samplePairForProcessing when no external script exists"() {
        given:
        script1.scriptVersion = "v8"
        assert script1.save(flush: true)
        joinScript.scriptVersion = "v8"
        assert joinScript.save(flush: true)

        expect:
        null == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY, SnvConfig)
    }


    void "samplePairForProcessing when the snvCallingInstance is already in progress"() {
        given:
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvCallingInstance(
                instanceName: ARBITRARY_INSTANCE_NAME,
                samplePair: samplePair1,
                config: snvConfig1,
                sampleType1BamFile: bamFile1_1,
                sampleType2BamFile: bamFile2_1
        )
        snvCallingInstance.save()

        expect:
        null == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY, SnvConfig)
    }


    void "samplePairForProcessing when a snvCallingInstance already finished"() {
        given:
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvCallingInstance(
                instanceName: ARBITRARY_INSTANCE_NAME,
                samplePair: samplePair1,
                config: snvConfig1,
                sampleType1BamFile: bamFile1_1,
                sampleType2BamFile: bamFile2_1,
                processingState: AnalysisProcessingStates.FINISHED
        )
        snvCallingInstance.save()

        expect:
        samplePair1 == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY, SnvConfig)
    }


    void "samplePairForProcessing when the snvCalling failed"() {
        given:
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvCallingInstance(
                instanceName: ARBITRARY_INSTANCE_NAME,
                samplePair: samplePair1,
                config: snvConfig1,
                sampleType1BamFile: bamFile1_1,
                sampleType2BamFile: bamFile2_1,
                processingState: AnalysisProcessingStates.IN_PROGRESS,
        )
        assert snvCallingInstance.save(flush: true)


        DomainFactory.createSnvJobResult(
                snvCallingInstance: snvCallingInstance,
                externalScript: script1,
                chromosomeJoinExternalScript: joinScript,
        )
        snvCallingService.markSnvCallingInstanceAsFailed(snvCallingInstance, [SnvCallingStep.CALLING])

        expect:
        samplePair1 == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY, SnvConfig)
    }


    void "samplePairForProcessing when other samplePair inProcess"() {
        given:
        def map2 = DomainFactory.createProcessableSamplePair()
        SamplePair samplePair2 = map2.samplePair

        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvCallingInstance(
                instanceName: ARBITRARY_INSTANCE_NAME,
                samplePair: samplePair2,
                config: snvConfig1,
                sampleType1BamFile: map2.bamFile1,
                sampleType2BamFile: map2.bamFile2
        )
        snvCallingInstance.save()

        expect:
        samplePair1.individual != samplePair2.individual
        samplePair1 == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY, SnvConfig)
    }


    @Unroll
    void "samplePairForProcessing when bamFile#number does not contain all seqTracks"() {
        given:
        if (number == 1) {
            DomainFactory.createSeqTrackWithDataFiles(bamFile1_1.mergingWorkPackage)
        } else {
            DomainFactory.createSeqTrackWithDataFiles(bamFile2_1.mergingWorkPackage)
        }

        expect:
        samplePair1 == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY, SnvConfig)

        where:
        number << [1, 2]

    }


    @Unroll
    void "samplePairForProcessing when no samplepair for bamFile#number exists"() {
        given:
        MergingWorkPackage withSamplePair
        MergingWorkPackage withoutSamplePair
        if (number == 1) {
            withSamplePair = samplePair1.mergingWorkPackage2
            withoutSamplePair = samplePair1.mergingWorkPackage1
        } else {
            withSamplePair = samplePair1.mergingWorkPackage1
            withoutSamplePair = samplePair1.mergingWorkPackage2
        }

        MergingWorkPackage otherMwp = DomainFactory.createMergingWorkPackage(withSamplePair)
        DomainFactory.createSampleTypePerProject(project: samplePair1.project, sampleType: otherMwp.sampleType, category: SampleType.Category.DISEASE)
        DomainFactory.createSamplePair(otherMwp, withoutSamplePair)
        samplePair1.delete(flush: true)

        expect:
        null == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY, SnvConfig)

        where:
        number << [1, 2]
    }


    @Unroll
    void "samplePairForProcessing when bamFile#number is still in progress"() {
        given:
        AbstractMergedBamFile bamFileInProgress = (number == 1) ? bamFile1_1 : bamFile2_1

        bamFileInProgress.md5sum = null
        bamFileInProgress.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.INPROGRESS
        assert bamFileInProgress.save(flush: true)

        expect:
        null == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY, SnvConfig)

        where:
        number << [1, 2]
    }


    @Unroll
    void "samplePairForProcessing when for bamFile#number the coverage is too low"() {
        given:
        AbstractMergedBamFile problematicBamFile = (number == 1) ? bamFile1_1 : bamFile2_1
        problematicBamFile.coverage = COVERAGE_TOO_LOW
        assert problematicBamFile.save(flush: true)

        expect:
        null == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY, SnvConfig)

        where:
        number << [1, 2]
    }


    @Unroll
    void "samplePairForProcessing when for bamFile#number the number of lanes is too low"() {
        given:
        AbstractMergedBamFile problematicBamFile = (number == 1) ? bamFile1_1 : bamFile2_1
        ProcessingThresholds thresholds = ProcessingThresholds.findBySeqType(problematicBamFile.seqType)
        thresholds.numberOfLanes = 5
        assert thresholds.save(flush: true)

        expect:
        null == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY, SnvConfig)

        where:
        number << [1, 2]
    }


    void "samplePairForProcessing when for both bam Files the number of lanes is too low"() {
        given:
        ProcessingThresholds.findByProject(samplePair1.project).each {
            it.numberOfLanes = 5
            it.save(flush: true)
        }

        expect:
        null == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY, SnvConfig)
    }


    void "samplePairForProcessing when for both bam Files the coverage is too low"() {
        given:
        [bamFile1_1, bamFile2_1].each {
            it.coverage = COVERAGE_TOO_LOW
            it.save(flush: true)
        }

        expect:
        null == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY, SnvConfig)
    }


    @Unroll
    void "samplePairForProcessing when for bamFile#number no threshold exists"() {
        given:
        Project otherProject = DomainFactory.createProject()
        AbstractMergedBamFile problematicBamFile = (number == 1) ? bamFile1_1 : bamFile2_1
        ProcessingThresholds thresholds = ProcessingThresholds.findBySeqType(problematicBamFile.seqType)
        thresholds.project = otherProject
        assert thresholds.save(flush: true)

        expect:
        null == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY, SnvConfig)

        where:
        number << [1, 2]
    }


    @Unroll
    void "samplePairForProcessing when for bamFile#number the processing threshold #property is null"() {
        given:
        AbstractMergedBamFile bamFile = (number == 1) ? bamFile1_1 : bamFile2_1
        ProcessingThresholds thresholds = ProcessingThresholds.findBySeqType(bamFile.seqType)
        thresholds[property] = null
        if (property == "coverage") {
            thresholds.numberOfLanes = 1
        }
        assert thresholds.save(flush: true)

        expect:
        samplePair1 == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY, SnvConfig)

        where:
        number | property
        1      | "coverage"
        2      | "coverage"
        1      | "numberOfLanes"
        2      | "numberOfLanes"
    }


    @Unroll
    void "samplePairForProcessing when bamFile#number is withdrawn"() {
        given:
        AbstractMergedBamFile problematicBamFile = (number == 1) ? bamFile1_1 : bamFile2_1
        problematicBamFile.withdrawn = true
        assert problematicBamFile.save(flush: true)

        expect:
        null == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY, SnvConfig)

        where:
        number << [1, 2]
    }


    void "samplePairForProcessing when check if the order correct"() {
        given:
        DomainFactory.createProcessableSamplePair()

        expect:
        samplePair1 == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY, SnvConfig)
    }


    void "samplePairForProcessing ensure that FastTrack is processed first"() {
        given:
        SamplePair samplePairFastTrack = DomainFactory.createProcessableSamplePair().samplePair
        Project project = samplePairFastTrack.project
        project.processingPriority = ProcessingPriority.FAST_TRACK_PRIORITY
        assert project.save(flush: true)

        expect:
        samplePairFastTrack == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY, SnvConfig)
    }


    void "samplePairForProcessing, make sure that min processing priority is taken into account"() {
        expect:
        null == snvCallingService.samplePairForProcessing(ProcessingPriority.FAST_TRACK_PRIORITY, SnvConfig)
    }


    void "samplePairForProcessing, for Sophia pipeline, only PMBF available, should not return any bam file"() {
        given:

        RoddyBamFile.list().each {
            it.withdrawn = true
            assert it.save(flush: true)
        }
        DomainFactory.createSamplePairWithProcessedMergedBamFiles()

        expect:
        !sophiaService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY, RoddyWorkflowConfig)
    }

    @Unroll
    void "samplePairForProcessing, for Sophia pipeline, only EPMBF available with #property is #value, should not return any bam file"() {
        given:
        RoddyBamFile.list().each {
            it.withdrawn = true
            assert it.save(flush: true)
        }
        DomainFactory.createSamplePairWithExternalProcessedMergedBamFiles(true, [(property): value])


        expect:
        !sophiaService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY, RoddyWorkflowConfig)

        where:
        property             | value
        'coverage'           | 5
        'insertSizeFile'     | null
        'meanSequenceLength' | null
    }

    @Unroll
    void "samplePairForProcessing, for Sophia pipeline, only EPMBF available with #property is #value, should return new instance"() {
        given:
        RoddyBamFile.list().each {
            it.withdrawn = true
            assert it.save(flush: true)
        }
        SamplePair samplePair = DomainFactory.createSamplePairWithExternalProcessedMergedBamFiles(true, [(property): value])

        expect:
        samplePair == sophiaService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY, RoddyWorkflowConfig)

        where:
        property             | value
        'coverage'           | 30
        'coverage'           | null
        'insertSizeFile'     | 'insertSize.txt'
        'meanSequenceLength' | 5
        'meanSequenceLength' | 200
    }

    void "samplePairForProcessing, for Aceseq pipeline, when sophia has not run, should not return SamplePair"() {
        given:
        prepareSophiaForAceseqBase()

        expect:
        null == aceseqService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY, RoddyWorkflowConfig)
    }

    void "samplePairForProcessing, for Aceseq pipeline, when last sophia instance is running and not withdrawn and an older finish exist, should not return SamplePair"() {
        given:
        prepareSophiaForAceseq([processingState: AnalysisProcessingStates.FINISHED, withdrawn: false], [processingState: AnalysisProcessingStates.IN_PROGRESS, withdrawn: false])

        expect:
        null == aceseqService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY, RoddyWorkflowConfig)
    }

    void "samplePairForProcessing, for Aceseq pipeline, when last sophia instance is running and withdrawn and an older finish exist, should return SamplePair"() {
        given:
        prepareSophiaForAceseq([processingState: AnalysisProcessingStates.FINISHED, withdrawn: false], [processingState: AnalysisProcessingStates.IN_PROGRESS, withdrawn: true])

        expect:
        samplePair1 == aceseqService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY, RoddyWorkflowConfig)
    }



    void "samplePairForProcessing, for Aceseq pipeline, when all sophia instances are withdrawn, should not return SamplePair"() {
        given:
        prepareSophiaForAceseq([processingState: AnalysisProcessingStates.FINISHED, withdrawn: true], [processingState: AnalysisProcessingStates.FINISHED, withdrawn: true])

        expect:
        null == aceseqService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY, RoddyWorkflowConfig)
    }

    void "samplePairForProcessing, for ACEseq pipeline, coverage is not high enough, should not return SamplePair"() {
        given:
        prepareSophiaForAceseq([:], [:])
        DomainFactory.createProcessingOptionLazy([
                name: OptionName.PIPELINE_MIN_COVERAGE,
                type: Pipeline.Type.ACESEQ.toString(),
                project: null,
                value: "40",
        ])

        expect:
        !aceseqService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY, RoddyWorkflowConfig)
    }


    private void prepareSophiaForAceseqBase() {
        samplePair1.sophiaProcessingStatus = ProcessingStatus.NO_PROCESSING_NEEDED
        samplePair1.save(flush: true)
        DomainFactory.createProcessingOptionLazy([
                name   : OptionName.PIPELINE_ACESEQ_REFERENCE_GENOME,
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
                processingState: AnalysisProcessingStates.FINISHED,
                withdrawn: false,
                sampleType1BamFile: bamFile1_1,
                sampleType2BamFile: bamFile2_1,
        ]

        DomainFactory.createSophiaInstance(samplePair1, defaultMap + propertiesSophia1)
        DomainFactory.createSophiaInstance(samplePair1, defaultMap + propertiesSophia2)
    }


    void "validateInputBamFiles, when all okay, return without exception"() {
        given:
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvCallingInstance(
                instanceName: ARBITRARY_INSTANCE_NAME,
                samplePair: samplePair1,
                config: snvConfig1,
                sampleType1BamFile: bamFile1_1,
                sampleType2BamFile: bamFile2_1
        )
        snvCallingInstance.save()

        snvCallingService.abstractMergedBamFileService = Mock(AbstractMergedBamFileService) {
            2 * getExistingBamFilePath(_) >> TestCase.uniqueNonExistentPath
        }

        when:
        snvCallingService.validateInputBamFiles(snvCallingInstance)

        then:
        noExceptionThrown()
    }


    void "validateInputBamFiles, when path throw an exception, throw a new runtime exception"() {
        given:
        SnvCallingInstance instance = new SnvCallingInstance([
                sampleType1BamFile: new RoddyBamFile(),
                sampleType2BamFile: new RoddyBamFile(),
        ])
        snvCallingService.abstractMergedBamFileService = Mock(AbstractMergedBamFileService) {
            2 * getExistingBamFilePath(_) >> TestCase.uniqueNonExistentPath >> { assert false }
        }

        when:
        snvCallingService.validateInputBamFiles(instance)

        then:
        RuntimeException e = thrown()
        e.message.contains('The input BAM files have changed on the file system while this job processed them.')
    }
}
