package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.spock.*
import spock.lang.*

class SnvCallingServiceIntegrationSpec extends IntegrationSpec {

    final static String ARBITRARY_INSTANCE_NAME = '2014-08-25_15h32'
    final static double COVERAGE_TOO_LOW = 20.0

    SamplePair samplePair1

    ConfigPerProjectAndSeqType roddyConfig1
    AbstractMergedBamFile bamFile1_1
    AbstractMergedBamFile bamFile2_1

    SnvCallingService snvCallingService

    def setup() {
        def map = DomainFactory.createProcessableSamplePair()

        samplePair1 = map.samplePair
        bamFile1_1 = map.bamFile1
        bamFile2_1 = map.bamFile2
        roddyConfig1 = map.roddyConfig

        DomainFactory.createAllAnalysableSeqTypes()
    }

    @Unroll
    void "samplePairForProcessing when config has wrong #property"() {
        given:
        if (property == "project") {
            roddyConfig1.project = DomainFactory.createProject(name: "otherProject", dirName: "tmp")
        } else {
            roddyConfig1.seqType = DomainFactory.createExomeSeqType()
        }
        assert roddyConfig1.save(flush: true)

        expect:
        null == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY)

        where:
        property << ["project", "seqType"]
    }

    void "samplePairForProcessing when config is obsolete"() {
        given:
        roddyConfig1.obsoleteDate = new Date()
        assert roddyConfig1.save(flush: true)

        expect:
        null == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY)
    }

    void "samplePairForProcessing when the snvCallingInstance is already in progress"() {
        given:
        DomainFactory.createRoddySnvCallingInstance(
                instanceName: ARBITRARY_INSTANCE_NAME,
                samplePair: samplePair1,
                config: roddyConfig1,
                sampleType1BamFile: bamFile1_1,
                sampleType2BamFile: bamFile2_1
        )

        expect:
        null == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY)
    }


    void "samplePairForProcessing when a snvCallingInstance already finished"() {
        given:
        DomainFactory.createRoddySnvCallingInstance(
                instanceName: ARBITRARY_INSTANCE_NAME,
                samplePair: samplePair1,
                config: roddyConfig1,
                sampleType1BamFile: bamFile1_1,
                sampleType2BamFile: bamFile2_1,
                processingState: AnalysisProcessingStates.FINISHED
        )

        expect:
        samplePair1 == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY)
    }

    void "samplePairForProcessing when other samplePair inProcess"() {
        given:
        def map2 = DomainFactory.createProcessableSamplePair()
        SamplePair samplePair2 = map2.samplePair

        DomainFactory.createRoddySnvCallingInstance(
                instanceName: ARBITRARY_INSTANCE_NAME,
                samplePair: samplePair2,
                config: roddyConfig1,
                sampleType1BamFile: map2.bamFile1,
                sampleType2BamFile: map2.bamFile2
        )

        expect:
        samplePair1.individual != samplePair2.individual
        samplePair1 == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY)
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
        samplePair1 == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY)

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
        null == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY)

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
        null == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY)

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
        null == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY)

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
        null == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY)

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
        null == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY)
    }


    void "samplePairForProcessing when for both bam Files the coverage is too low"() {
        given:
        [bamFile1_1, bamFile2_1].each {
            it.coverage = COVERAGE_TOO_LOW
            it.save(flush: true)
        }

        expect:
        null == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY)
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
        null == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY)

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
        samplePair1 == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY)

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
        null == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY)

        where:
        number << [1, 2]
    }


    void "samplePairForProcessing when check if the order correct"() {
        given:
        DomainFactory.createProcessableSamplePair()

        expect:
        samplePair1 == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY)
    }


    void "samplePairForProcessing ensure that FastTrack is processed first"() {
        given:
        SamplePair samplePairFastTrack = DomainFactory.createProcessableSamplePair().samplePair
        Project project = samplePairFastTrack.project
        project.processingPriority = ProcessingPriority.FAST_TRACK_PRIORITY
        assert project.save(flush: true)

        expect:
        samplePairFastTrack == snvCallingService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY)
    }


    void "samplePairForProcessing, make sure that min processing priority is taken into account"() {
        expect:
        null == snvCallingService.samplePairForProcessing(ProcessingPriority.FAST_TRACK_PRIORITY)
    }


    void "validateInputBamFiles, when all okay, return without exception"() {
        given:
        RoddySnvCallingInstance snvCallingInstance = DomainFactory.createRoddySnvCallingInstance(
                instanceName: ARBITRARY_INSTANCE_NAME,
                samplePair: samplePair1,
                config: roddyConfig1,
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
        RoddySnvCallingInstance instance = new RoddySnvCallingInstance([
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
