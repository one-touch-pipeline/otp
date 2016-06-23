package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.mixin.Mock
import spock.lang.Specification
import spock.lang.Unroll

@Mock([
        DataFile,
        FileType,
        Individual,
        LibraryPreparationKit,
        MergingWorkPackage,
        Project,
        ProcessingOption,
        RoddyBamFile,
        ReferenceGenomeProjectSeqType,
        RoddyWorkflowConfig,
        Run,
        Sample,
        SampleType,
        SeqCenter,
        SeqPlatform,
        SeqPlatformGroup,
        SeqTrack,
        SeqType,
        SoftwareTool,
        Realm,
        ReferenceGenome,
        Pipeline,
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
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithDataFiles(roddyBamFile.workPackage, [
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
}
