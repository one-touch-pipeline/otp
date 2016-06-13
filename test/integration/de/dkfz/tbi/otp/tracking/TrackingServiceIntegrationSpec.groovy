package de.dkfz.tbi.otp.tracking

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.TrackingService.SamplePairDiscovery
import grails.test.spock.*

import static de.dkfz.tbi.otp.tracking.ProcessingStatus.WorkflowProcessingStatus.*

class TrackingServiceIntegrationSpec extends IntegrationSpec {

    TrackingService trackingService = new TrackingService()

    void "getAlignmentAndDownstreamProcessingStatus, No ST, returns NOTHING_DONE_WONT_DO"() {
        expect:
        NOTHING_DONE_WONT_DO == trackingService.getAlignmentAndDownstreamProcessingStatus([] as Set, new SamplePairDiscovery()).alignmentProcessingStatus
    }

    void "getAlignmentAndDownstreamProcessingStatus, 1 ST not alignable, returns NOTHING_DONE_WONT_DO"() {
        given:
        Set<SeqTrack> seqTracks = [DomainFactory.createSeqTrackWithOneDataFile([:], [fileWithdrawn: true])]

        expect:
        NOTHING_DONE_WONT_DO == trackingService.getAlignmentAndDownstreamProcessingStatus(seqTracks, new SamplePairDiscovery()).alignmentProcessingStatus
    }

    void "getAlignmentAndDownstreamProcessingStatus, 1 MWP ALL_DONE, returns ALL_DONE"() {
        given:
        AbstractMergedBamFile bamFile = createBamFileInProjectFolder(DomainFactory.PROCESSED_BAM_FILE_PROPERTIES)

        Set<SeqTrack> seqTracks = bamFile.containedSeqTracks

        expect:
        ALL_DONE == trackingService.getAlignmentAndDownstreamProcessingStatus(seqTracks, new SamplePairDiscovery()).alignmentProcessingStatus
    }

    void "getAlignmentAndDownstreamProcessingStatus, 1 MWP NOTHING_DONE_MIGHT_DO, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        AbstractMergedBamFile bamFile = createBamFileInProjectFolder(DomainFactory.PROCESSED_BAM_FILE_PROPERTIES)

        Set<SeqTrack> seqTracks = [DomainFactory.createSeqTrackWithDataFiles(bamFile.mergingWorkPackage)]

        expect:
        NOTHING_DONE_MIGHT_DO == trackingService.getAlignmentAndDownstreamProcessingStatus(seqTracks, new SamplePairDiscovery()).alignmentProcessingStatus
    }

    void "getAlignmentAndDownstreamProcessingStatus, 1 ST not alignable, 1 MWP ALL_DONE, returns PARTLY_DONE_WONT_DO_MORE"() {
        given:
        AbstractMergedBamFile bamFile = createBamFileInProjectFolder(DomainFactory.PROCESSED_BAM_FILE_PROPERTIES)

        Set<SeqTrack> seqTracks = bamFile.containedSeqTracks + [DomainFactory.createSeqTrackWithDataFiles(bamFile.mergingWorkPackage, [:], [fileWithdrawn: true])]

        expect:
        PARTLY_DONE_WONT_DO_MORE == trackingService.getAlignmentAndDownstreamProcessingStatus(seqTracks, new SamplePairDiscovery()).alignmentProcessingStatus
    }

    void "getAlignmentAndDownstreamProcessingStatus, no MWP, returns NOTHING_DONE_WONT_DO"() {
        given:
        Set<SeqTrack> seqTracks = [DomainFactory.createSeqTrackWithOneDataFile()]

        expect:
        NOTHING_DONE_WONT_DO == trackingService.getAlignmentAndDownstreamProcessingStatus(seqTracks, new SamplePairDiscovery()).alignmentProcessingStatus
    }

    void "getAlignmentAndDownstreamProcessingStatus, 1 MWP in progress, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile()

        expect:
        NOTHING_DONE_MIGHT_DO == trackingService.getAlignmentAndDownstreamProcessingStatus(bamFile.containedSeqTracks, new SamplePairDiscovery()).alignmentProcessingStatus
    }

    void "getAlignmentAndDownstreamProcessingStatus, 2 MergingProperties, 1 MWP ALL_DONE, returns PARTLY_DONE_WONT_DO_MORE"() {
        given:
        AbstractMergedBamFile bamFile = createBamFileInProjectFolder(DomainFactory.PROCESSED_BAM_FILE_PROPERTIES)

        Set<SeqTrack> seqTracks = bamFile.containedSeqTracks + [DomainFactory.createSeqTrackWithOneDataFile()]

        expect:
        PARTLY_DONE_WONT_DO_MORE == trackingService.getAlignmentAndDownstreamProcessingStatus(seqTracks, new SamplePairDiscovery()).alignmentProcessingStatus
    }

    void "getAlignmentAndDownstreamProcessingStatus, 2 MergingProperties, 1 MWP in progress, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile()

        Set<SeqTrack> seqTracks = bamFile.containedSeqTracks + [DomainFactory.createSeqTrackWithOneDataFile()]

        expect:
        NOTHING_DONE_MIGHT_DO == trackingService.getAlignmentAndDownstreamProcessingStatus(seqTracks, new SamplePairDiscovery()).alignmentProcessingStatus
    }

    void "getAlignmentAndDownstreamProcessingStatus, 2 MergingProperties, 1 MWP NOTHING_DONE_MIGHT_DO, 1 MWP ALL_DONE, returns PARTLY_DONE_MIGHT_DO_MORE"() {
        given:
        AbstractMergedBamFile bamFile1 = createBamFileInProjectFolder(DomainFactory.PROCESSED_BAM_FILE_PROPERTIES)
        AbstractMergedBamFile bamFile2 = createBamFileInProjectFolder(DomainFactory.PROCESSED_BAM_FILE_PROPERTIES)

        Set<SeqTrack> seqTracks = bamFile1.containedSeqTracks + bamFile2.containedSeqTracks

        DomainFactory.createSeqTrackWithDataFiles(bamFile2.mergingWorkPackage)

        expect:
        PARTLY_DONE_MIGHT_DO_MORE == trackingService.getAlignmentAndDownstreamProcessingStatus(seqTracks, new SamplePairDiscovery()).alignmentProcessingStatus
    }

    void "getSnvProcessingStatus, no MWP, returns NOTHING_DONE_WONT_DO"() {
        expect:
        NOTHING_DONE_WONT_DO == trackingService.getSnvProcessingStatus([] as Set, false, new SamplePairDiscovery()).snvProcessingStatus
    }

    void "getSnvProcessingStatus, no SP, returns NOTHING_DONE_WONT_DO"() {
        given:
        Set<MergingWorkPackage> mergingWorkPackages = [DomainFactory.createMergingWorkPackage()]

        expect:
        NOTHING_DONE_WONT_DO == trackingService.getSnvProcessingStatus(mergingWorkPackages, false, new SamplePairDiscovery()).snvProcessingStatus
    }

    void "getSnvProcessingStatus, 1 SCI FINISHED, bamFileInProjectFolder set, returns ALL_DONE"() {
        given:
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvInstanceWithRoddyBamFiles(processingState: SnvProcessingStates.FINISHED)

        [1, 2].each {
            setBamFileInProjectFolder(snvCallingInstance."sampleType${it}BamFile")
        }

        Set<MergingWorkPackage> mergingWorkPackages = [snvCallingInstance.sampleType1BamFile.mergingWorkPackage]

        expect:
        ALL_DONE == trackingService.getSnvProcessingStatus(mergingWorkPackages, false, new SamplePairDiscovery()).snvProcessingStatus
    }

    void "getSnvProcessingStatus, 1 SCI not FINISHED, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvInstanceWithRoddyBamFiles()

        [1, 2].each {
            setBamFileInProjectFolder(snvCallingInstance."sampleType${it}BamFile")
        }

        Set<MergingWorkPackage> mergingWorkPackages = [snvCallingInstance.sampleType1BamFile.mergingWorkPackage]

        expect:
        NOTHING_DONE_MIGHT_DO == trackingService.getSnvProcessingStatus(mergingWorkPackages, false, new SamplePairDiscovery()).snvProcessingStatus
    }

    void "getSnvProcessingStatus, 1 SCI FINISHED, bamFileInProjectFolder unset, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvInstanceWithRoddyBamFiles(processingState: SnvProcessingStates.FINISHED)

        Set<MergingWorkPackage> mergingWorkPackages = [snvCallingInstance.sampleType1BamFile.mergingWorkPackage]

        expect:
        NOTHING_DONE_MIGHT_DO == trackingService.getSnvProcessingStatus(mergingWorkPackages, false, new SamplePairDiscovery()).snvProcessingStatus
    }

    void "getSnvProcessingStatus, no SCI, bamFileInProjectFolder set, no samplePairForSnvProcessing, returns NOTHING_DONE_WONT_DO"() {
        given:
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvInstanceWithRoddyBamFiles()

        [1, 2].each {
            setBamFileInProjectFolder(snvCallingInstance."sampleType${it}BamFile")
        }

        Set<MergingWorkPackage> mergingWorkPackages = [snvCallingInstance.sampleType1BamFile.mergingWorkPackage]

        snvCallingInstance.delete(flush: true)

        expect:
        NOTHING_DONE_WONT_DO == trackingService.getSnvProcessingStatus(mergingWorkPackages, false, new SamplePairDiscovery()).snvProcessingStatus
    }

    void "getSnvProcessingStatus, no SCI, bamFileInProjectFolder set, samplePairForSnvProcessing exists, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvInstanceWithRoddyBamFiles([:], [coverage: 2], [coverage: 2])

        DomainFactory.createSnvConfigForSnvCallingInstance(snvCallingInstance)

        [1, 2].each {
            setBamFileInProjectFolder(snvCallingInstance."sampleType${it}BamFile")
            DomainFactory.createProcessingThresholdsForBamFile(snvCallingInstance."sampleType${it}BamFile", [coverage: 1, numberOfLanes: null])
        }

        Set<MergingWorkPackage> mergingWorkPackages = [snvCallingInstance.sampleType1BamFile.mergingWorkPackage]

        snvCallingInstance.delete(flush: true)

        expect:
        NOTHING_DONE_MIGHT_DO == trackingService.getSnvProcessingStatus(mergingWorkPackages, false, new SamplePairDiscovery()).snvProcessingStatus
    }

    void "getSnvProcessingStatus, no SCI, bamFileInProjectFolder unset, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvInstanceWithRoddyBamFiles()

        Set<MergingWorkPackage> mergingWorkPackages = [snvCallingInstance.sampleType1BamFile.mergingWorkPackage]

        snvCallingInstance.delete(flush: true)

        expect:
        NOTHING_DONE_MIGHT_DO == trackingService.getSnvProcessingStatus(mergingWorkPackages, false, new SamplePairDiscovery()).snvProcessingStatus
    }

    void "getSnvProcessingStatus, 2 MWP, 1 SP ALL_DONE, returns PARTLY_DONE_WONT_DO_MORE"() {
        given:
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvInstanceWithRoddyBamFiles(processingState: SnvProcessingStates.FINISHED)

        [1, 2].each {
            setBamFileInProjectFolder(snvCallingInstance."sampleType${it}BamFile")
        }

        Set<MergingWorkPackage> mergingWorkPackages = [snvCallingInstance.sampleType1BamFile.mergingWorkPackage] + [DomainFactory.createMergingWorkPackage()]

        expect:
        PARTLY_DONE_WONT_DO_MORE == trackingService.getSnvProcessingStatus(mergingWorkPackages, false, new SamplePairDiscovery()).snvProcessingStatus
    }

    void "getSnvProcessingStatus, 2 MWP, 1 MWP without SP, 1 MWP MIGHT_DO_MORE, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvInstanceWithRoddyBamFiles(processingState: SnvProcessingStates.FINISHED)

        Set<MergingWorkPackage> mergingWorkPackages = [snvCallingInstance.sampleType1BamFile.mergingWorkPackage] + DomainFactory.createMergingWorkPackage()

        expect:
        NOTHING_DONE_MIGHT_DO == trackingService.getSnvProcessingStatus(mergingWorkPackages, false, new SamplePairDiscovery()).snvProcessingStatus
    }

    void "getSnvProcessingStatus, 2 MWP, 1 SP ALL_DONE, 1 SP MIGHT_DO_MORE, returns PARTLY_DONE_MIGHT_DO_MORE"() {
        given:
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvInstanceWithRoddyBamFiles(processingState: SnvProcessingStates.FINISHED)

        [1, 2].each {
            setBamFileInProjectFolder(snvCallingInstance."sampleType${it}BamFile")
        }

        SnvCallingInstance snvCallingInstance2 = DomainFactory.createSnvInstanceWithRoddyBamFiles(processingState: SnvProcessingStates.FINISHED)

        Set<MergingWorkPackage> mergingWorkPackages = [snvCallingInstance.sampleType1BamFile.mergingWorkPackage] + [snvCallingInstance2.sampleType1BamFile.mergingWorkPackage]

        expect:
        PARTLY_DONE_MIGHT_DO_MORE == trackingService.getSnvProcessingStatus(mergingWorkPackages, false, new SamplePairDiscovery()).snvProcessingStatus
    }

    private static AbstractMergedBamFile createBamFileInProjectFolder(Map bamFileProperties = [:]) {
        AbstractMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile(bamFileProperties)

        return setBamFileInProjectFolder(bamFile)
    }

    private static AbstractMergedBamFile setBamFileInProjectFolder (AbstractMergedBamFile bamFile) {
        bamFile.mergingWorkPackage.bamFileInProjectFolder = bamFile
        bamFile.mergingWorkPackage.save(flush: true)

        return bamFile
    }
}
