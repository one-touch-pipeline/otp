package de.dkfz.tbi.otp.monitor.alignment

import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.roddyRna.RoddyRnaFactory
import de.dkfz.tbi.otp.monitor.MonitorOutputCollector
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqTrack

class AllRoddyAlignmentsCheckerIntegrationSpec extends Specification implements RoddyRnaFactory {


    void "handle, if SeqTracks given, then return finished RoddyBamFile, call Roddy alignments workflows with correct seqTracks and output SeqTypes not supported by any workflow"() {
        given:
        MonitorOutputCollector output = Mock(MonitorOutputCollector)
        AllRoddyAlignmentsChecker checker = new AllRoddyAlignmentsChecker()

        SeqTrack wrongSeqType = DomainFactory.createSeqTrack()

        List<SeqTrack> panCanSeqTracks = DomainFactory.createRoddyAlignableSeqTypes().collect {
            DomainFactory.createSeqTrack(
                    DomainFactory.createMergingWorkPackage(
                            seqType: it,
                            pipeline: DomainFactory.createPanCanPipeline(),
                    )
            )
        }
        RnaRoddyBamFile rnaRoddyBamFile = createBamFile([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
        ])

        List<SeqTrack> seqTracks = [
                wrongSeqType,
                panCanSeqTracks,
                rnaRoddyBamFile.containedSeqTracks,
        ].flatten()

        List<RoddyBamFile> finishedRoddyBamFiles = [rnaRoddyBamFile]

        when:
        List<RoddyBamFile> result = checker.handle(seqTracks, output)

        then:
        1 * output.showUniqueList(AllRoddyAlignmentsChecker.HEADER_NOT_SUPPORTED_SEQTYPES, [wrongSeqType], _)

        then:
        1 * output.showWorkflow('PanCanWorkflow')
        1 * output.showWorkflow('WgbsAlignmentWorkflow')
        1 * output.showWorkflow('RnaAlignmentWorkflow')
        0 * output.showWorkflow(_)

        then:
        finishedRoddyBamFiles == result
    }

    void "handle, if no SeqTracks given, then return empty list and do not create output"() {
        MonitorOutputCollector output = Mock(MonitorOutputCollector)
        AllRoddyAlignmentsChecker checker = new AllRoddyAlignmentsChecker()

        when:
        List<RoddyBamFile> result = checker.handle([], output)

        then:
        0 * output._

        then:
        [] == result
    }

}
