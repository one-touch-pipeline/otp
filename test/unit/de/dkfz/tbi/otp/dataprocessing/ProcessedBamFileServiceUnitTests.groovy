package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*
import de.dkfz.tbi.otp.ngsdata.*

@TestFor(ProcessedBamFileService)
@Mock([SeqType, ExomeEnrichmentKit, SeqTrack, ExomeSeqTrack, AlignmentPass, ProcessedBamFile])
class ProcessedBamFileServiceUnitTests {

    @Test
    void testExomeEnrichmentKitCorrect() {
        Map input = createKitAndBamFile(SeqTypeNames.EXOME.seqTypeName, ExomeSeqTrack)
        assertEquals(input.kit, service.exomeEnrichmentKit(input.bamFile))
    }

    @Test(expected = IllegalArgumentException)
    void testExomeEnrichmentKitNullInput() {
        service.exomeEnrichmentKit(null)
    }

    @Test(expected = IllegalArgumentException)
    void testExomeEnrichmentKitNotExonSeqType() {
        Map input = createKitAndBamFile(SeqTypeNames.WHOLE_GENOME.seqTypeName, ExomeSeqTrack)
        assertEquals(input.kit, service.exomeEnrichmentKit(input.bamFile))
    }

    @Test(expected = IllegalArgumentException)
    void testExomeEnrichmentKitWrongSeqTrackType() {
        Map input = createKitAndBamFile(SeqTypeNames.EXOME.seqTypeName, SeqTrack)
        assertEquals(input.kit, service.exomeEnrichmentKit(input.bamFile))
    }

    private Map createKitAndBamFile(String seqTypeName, Class seqTypeClass) {
        assert seqTypeClass == SeqTrack || seqTypeClass == ExomeSeqTrack
        SeqType seqType = new SeqType()
        seqType.name = seqTypeName
        ExomeEnrichmentKit kit = new ExomeEnrichmentKit()
        Run run = new Run(name: 'run')
        SeqTrack seqTrack
        if (seqTypeClass == SeqTrack) {
            seqTrack = new SeqTrack()
        }
        if (seqTypeClass == ExomeSeqTrack) {
            seqTrack = new ExomeSeqTrack(exomeEnrichmentKit: kit)
        }
        seqTrack.run = run
        seqTrack.seqType = seqType
        AlignmentPass pass = new AlignmentPass(seqTrack: seqTrack)
        ProcessedBamFile bamFile = new ProcessedBamFile(alignmentPass: pass)
        return [kit: kit, bamFile: bamFile]
    }
}
