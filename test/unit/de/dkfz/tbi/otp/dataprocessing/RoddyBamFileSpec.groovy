package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.mixin.*
import spock.lang.*

@Mock([
        DataFile,
        FileType,
        Individual,
        LibraryPreparationKit,
        MergingWorkPackage,
        Pipeline,
        ProcessingOption,
        Project,
        ProjectCategory,
        Realm,
        ReferenceGenome,
        ReferenceGenomeProjectSeqType,
        RoddyBamFile,
        RoddyWorkflowConfig,
        Run,
        RunSegment,
        Sample,
        SampleType,
        SeqCenter,
        SeqPlatform,
        SeqPlatformGroup,
        SeqTrack,
        SeqType,
        SoftwareTool,
])
class RoddyBamFileSpec extends Specification {


    RoddyBamFile roddyBamFile

    def setup() {
        roddyBamFile = DomainFactory.createRoddyBamFile()
        DomainFactory.createRealmDataManagement([name: roddyBamFile.project.realmName])
    }

    @Unroll
    void "test getFinalLibraryQAJsonFiles OneSeqTrack libraryName is #libraryName"() {
        given:
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator().next()
        seqTrack.libraryName = libraryName
        seqTrack.normalizedLibraryName = normalizedName
        assert seqTrack.save(flush: true)

        when:
        Map<String, File> result = roddyBamFile."get${finalOrWork}LibraryQAJsonFiles"()

        then:
        assert result.size() == 1
        assert result.containsKey(directoryName)
        assert result.get(directoryName).path.endsWith(path)

        where:
        finalOrWork | libraryName | normalizedName | directoryName || path
        "Work"      | null        | null           | "libNA"       || 'qualitycontrol/libNA/qualitycontrol.json'
        "Work"      | "library1"  | "1"            | "lib1"        || 'qualitycontrol/lib1/qualitycontrol.json'
        "Final"     | null        | null           | "libNA"       || 'qualitycontrol/libNA/qualitycontrol.json'
        "Final"     | "library1"  | "1"            | "lib1"        || 'qualitycontrol/lib1/qualitycontrol.json'
        "Final"     | "abc"       | "abc"          | "libabc"      || 'qualitycontrol/libabc/qualitycontrol.json'

    }


    @Unroll
    void "test getFinalLibraryQAJsonFiles MultipleSeqTrack libraryName is #libraryName"() {
        given:
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithDataFiles(roddyBamFile.workPackage as MergingWorkPackage, [
                libraryName          : libraryName,
                normalizedLibraryName: normalizedName
        ])
        roddyBamFile.seqTracks.add(seqTrack)
        roddyBamFile.numberOfMergedLanes = 2
        roddyBamFile.save(flush: true)

        when:
        Map<String, File> result = roddyBamFile."get${finalOrWork}LibraryQAJsonFiles"()

        then:
        assert result.size() == resultSize
        assert result.containsKey('libNA')
        assert result.containsKey(directoryName)

        where:
        finalOrWork | libraryName | normalizedName | directoryName || resultSize
        "Work"      | null        | null           | "libNA"       || 1
        "Work"      | "library1"  | "1"            | "lib1"        || 2
        "Final"     | null        | null           | "libNA"       || 1
        "Final"     | "library1"  | "1"            | "lib1"        || 2
    }


    void "getNumberOfReadsFromFastQc, when all fine, returns sum of all number of readsnumber of reads of all DataFiles of all SeqTracks of the RoddyBamFile"() {
        given:
        long numberOfReads1 = DomainFactory.counter++
        long numberOfReads2 = DomainFactory.counter++
        long numberOfReads3 = DomainFactory.counter++
        long expectedNumberOfReads = 2 * (numberOfReads1 + numberOfReads2 + numberOfReads3)

        roddyBamFile.numberOfMergedLanes = 3
        roddyBamFile.seqTracks = [
                DomainFactory.createSeqTrackWithDataFiles(roddyBamFile.mergingWorkPackage, [fastqcState: SeqTrack.DataProcessingState.FINISHED], [nReads: numberOfReads1]),
                DomainFactory.createSeqTrackWithDataFiles(roddyBamFile.mergingWorkPackage, [fastqcState: SeqTrack.DataProcessingState.FINISHED], [nReads: numberOfReads2]),
                DomainFactory.createSeqTrackWithDataFiles(roddyBamFile.mergingWorkPackage, [fastqcState: SeqTrack.DataProcessingState.FINISHED], [nReads: numberOfReads3]),
        ]
        assert roddyBamFile.save(flush: true)


        expect:
        expectedNumberOfReads == roddyBamFile.getNumberOfReadsFromFastQc()
    }


    void "getNumberOfReadsFromFastQc, when a fastqc workflow has not finished, throws exception"() {
        given:
        roddyBamFile.numberOfMergedLanes = 3
        roddyBamFile.seqTracks = [
                DomainFactory.createSeqTrackWithDataFiles(roddyBamFile.mergingWorkPackage, [fastqcState: SeqTrack.DataProcessingState.FINISHED], [nReads: DomainFactory.counter++]),
                DomainFactory.createSeqTrackWithDataFiles(roddyBamFile.mergingWorkPackage, [fastqcState: SeqTrack.DataProcessingState.IN_PROGRESS], [nReads: DomainFactory.counter++]),
                DomainFactory.createSeqTrackWithDataFiles(roddyBamFile.mergingWorkPackage, [fastqcState: SeqTrack.DataProcessingState.FINISHED], [nReads: DomainFactory.counter++]),
        ]
        assert roddyBamFile.save(flush: true)

        when:
        roddyBamFile.getNumberOfReadsFromFastQc()

        then:
        AssertionError e = thrown()
        e.message.contains('Not all Fastqc workflows of all seqtracks are finished')
    }


    void "getNumberOfReadsFromFastQc, when number of reads of a SeqTrack is null, throws exception"() {
        given:
        roddyBamFile.numberOfMergedLanes = 3
        roddyBamFile.seqTracks = [
                DomainFactory.createSeqTrackWithDataFiles(roddyBamFile.mergingWorkPackage, [fastqcState: SeqTrack.DataProcessingState.FINISHED], [nReads: DomainFactory.counter++]),
                DomainFactory.createSeqTrackWithDataFiles(roddyBamFile.mergingWorkPackage, [fastqcState: SeqTrack.DataProcessingState.FINISHED], [nReads: null]),
                DomainFactory.createSeqTrackWithDataFiles(roddyBamFile.mergingWorkPackage, [fastqcState: SeqTrack.DataProcessingState.FINISHED], [nReads: DomainFactory.counter++]),
        ]
        assert roddyBamFile.save(flush: true)

        when:
        roddyBamFile.getNumberOfReadsFromFastQc()

        then:
        AssertionError e = thrown()
        e.message.contains('At least one seqTrack has no value for number of reads')
    }
}
