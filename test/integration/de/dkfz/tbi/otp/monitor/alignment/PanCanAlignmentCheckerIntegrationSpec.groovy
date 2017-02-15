package de.dkfz.tbi.otp.monitor.alignment

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.monitor.*
import de.dkfz.tbi.otp.ngsdata.*

class PanCanAlignmentCheckerIntegrationSpec extends AbstractRoddyAlignmentCheckerIntegrationSpec {

    @Override
    AbstractRoddyAlignmentChecker createRoddyAlignmentChecker() {
        return new PanCanAlignmentChecker()
    }

    @Override
    Pipeline createPipeLine() {
        return DomainFactory.createPanCanPipeline()
    }

    @Override
    Pipeline createPipeLineForCrosschecking() {
        return DomainFactory.createRoddyRnaPipeline()
    }


    void "workflowName, should return PanCanWorkflow"() {
        expect:
        'PanCanWorkflow' == createRoddyAlignmentChecker().getWorkflowName()
    }

    void "pipeLineName, should return PANCAN_ALIGNMENT"() {
        expect:
        Pipeline.Name.PANCAN_ALIGNMENT == createRoddyAlignmentChecker().getPipeLineName()
    }

    void "seqTypes, should return WGS and WES"() {
        given:
        List<SeqType> seqTypes = [
                DomainFactory.createWholeGenomeSeqType(),
                DomainFactory.createExomeSeqType(),
        ]

        expect:
        TestCase.assertContainSame(seqTypes, createRoddyAlignmentChecker().getSeqTypes())
    }


    void "filter, when seqTracks given, then create output for filtered seqTracks and return the others"() {
        given:
        MonitorOutputCollector output = Mock(MonitorOutputCollector)
        checker = Spy(PanCanAlignmentChecker)

        SeqTrack wgs = DomainFactory.createSeqTrack(seqType: DomainFactory.createWholeGenomeSeqType())

        SeqTrack wesFine = createExomeSeqTrack(true, true)

        SeqTrack wesNoLibraryPreparationKit = createExomeSeqTrack(false, false)

        SeqTrack wesNoBedFile = createExomeSeqTrack(true, false)

        List<SeqTrack> seqTracks = [
                wgs,
                wesFine,
                wesNoLibraryPreparationKit,
                wesNoBedFile,
        ]

        List<SeqTrack> expectedSeqTracks = [
                wgs,
                wesFine,
        ]

        when:
        List<RoddyBamFile> result = checker.filter(seqTracks, output)

        then:
        1 * output.showList(PanCanAlignmentChecker.HEADER_EXOME_WITHOUT_LIBRARY_PREPERATION_KIT, [wesNoLibraryPreparationKit])

        then:
        1 * output.showList(PanCanAlignmentChecker.HEADER_EXOME_NO_BEDFILE, [wesNoBedFile])

        then:
        expectedSeqTracks == result
    }


    private ExomeSeqTrack createExomeSeqTrack(boolean createLibraryPreperationKit, boolean createBedFile) {
        SeqTrack exomeSeqTrack = DomainFactory.createExomeSeqTrack([
                libraryPreparationKit: createLibraryPreperationKit ? DomainFactory.createLibraryPreparationKit() : null,
                kitInfoReliability   : createLibraryPreperationKit ? InformationReliability.KNOWN : InformationReliability.UNKNOWN_UNVERIFIED,
        ])
        DomainFactory.createReferenceGenomeProjectSeqType([
                project: exomeSeqTrack.project,
                seqType: exomeSeqTrack.seqType,
        ])
        if (createBedFile) {
            DomainFactory.createBedFile([
                    referenceGenome      : exomeSeqTrack.configuredReferenceGenome,
                    libraryPreparationKit: exomeSeqTrack.libraryPreparationKit,
            ])
        }
        return exomeSeqTrack
    }
}
