package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*
import de.dkfz.tbi.otp.ngsdata.*


@TestFor(ProcessedMergedBamFileService)
class ProcessedMergedBamFileServiceUnitTests {

    @Test
    void testExomeEnrichmentKitCorrect() {
        ProcessedMergedBamFile mergedBamFile = new ProcessedMergedBamFile()
        Map input = createKitAndSingleLaneBamFiles(SeqTypeNames.EXOME.seqTypeName, ExomeSeqTrack)
        def abstractBamFileService = [
            findAllByProcessedMergedBamFile: { input.bamFiles }
        ] as AbstractBamFileService
        service.abstractBamFileService = abstractBamFileService
        assertEquals(input.kit, service.exomeEnrichmentKit(mergedBamFile))
    }

    @Test(expected = IllegalArgumentException)
    void testExomeEnrichmentKitNullInput() {
        service.exomeEnrichmentKit(null)
    }

    @Test(expected = IllegalArgumentException)
    void testExomeEnrichmentKitNotExomSeqType() {
        ProcessedMergedBamFile mergedBamFile = new ProcessedMergedBamFile()
        Map input = createKitAndSingleLaneBamFiles(SeqTypeNames.WHOLE_GENOME.seqTypeName, ExomeSeqTrack)
        def abstractBamFileService = [
            findAllByProcessedMergedBamFile: { input.bamFiles }
        ] as AbstractBamFileService
        service.abstractBamFileService = abstractBamFileService
        service.exomeEnrichmentKit(mergedBamFile)
    }

    @Test(expected = IllegalArgumentException)
    void testExomeEnrichmentKitNoSingleLandBamFiles() {
        ProcessedMergedBamFile mergedBamFile = new ProcessedMergedBamFile()
        Map input = createKitAndSingleLaneBamFiles(SeqTypeNames.EXOME.seqTypeName, ExomeSeqTrack)
        def abstractBamFileService = [
            findAllByProcessedMergedBamFile: { [] }
        ] as AbstractBamFileService
        service.abstractBamFileService = abstractBamFileService
        service.exomeEnrichmentKit(mergedBamFile)
    }

    @Test(expected = IllegalArgumentException)
    void testExomeEnrichmentKitDiffSeqTrackTypes() {
        ProcessedMergedBamFile mergedBamFile = new ProcessedMergedBamFile()
        Map input = createKitAndSingleLaneBamFiles(SeqTypeNames.EXOME.seqTypeName, ExomeSeqTrack)
        SeqTrack seqTrack = new SeqTrack()
        input.bamFiles[2].alignmentPass.seqTrack = seqTrack
        def abstractBamFileService = [
            findAllByProcessedMergedBamFile: { input.bamFiles }
        ] as AbstractBamFileService
        service.abstractBamFileService = abstractBamFileService
        service.exomeEnrichmentKit(mergedBamFile)
    }

    @Test(expected = IllegalArgumentException)
    void testExomeEnrichmentKitDiffKits() {
        ProcessedMergedBamFile mergedBamFile = new ProcessedMergedBamFile()
        ExomeEnrichmentKit kit = new ExomeEnrichmentKit(name: 'kit2')
        Map input = createKitAndSingleLaneBamFiles(SeqTypeNames.EXOME.seqTypeName, ExomeSeqTrack)
        input.bamFiles[2].alignmentPass.seqTrack.exomeEnrichmentKit = kit
        def abstractBamFileService = [
            findAllByProcessedMergedBamFile: { input.bamFiles }
        ] as AbstractBamFileService
        service.abstractBamFileService = abstractBamFileService
        service.exomeEnrichmentKit(mergedBamFile)
    }

    private Map createKitAndSingleLaneBamFiles(String seqTypeName, Class seqTypeClass) {
        assert seqTypeClass == SeqTrack || seqTypeClass == ExomeSeqTrack
        List bamFiles = []
        SeqType seqType = new SeqType()
        seqType.name = seqTypeName
        ExomeEnrichmentKit kit = new ExomeEnrichmentKit(name: 'kit1')
        Run run = new Run(name: 'run')
        3.times {
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
            bamFiles << bamFile
        }
        ProcessedMergedBamFileService.metaClass.seqType = { ProcessedMergedBamFile bamFile -> seqType }
        return [kit:kit, bamFiles:bamFiles]
    }
}
