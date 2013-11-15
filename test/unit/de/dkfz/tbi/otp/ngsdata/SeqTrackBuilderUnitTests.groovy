package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*
import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*
import de.dkfz.tbi.otp.ngsdata.SeqTrack.DataProcessingState
import de.dkfz.tbi.otp.ngsdata.SeqTrack.QualityEncoding

@TestMixin(GrailsUnitTestMixin)
@Mock([SeqTrack, ExomeSeqTrack])
class SeqTrackBuilderUnitTests {

    void testCreate() {
        SeqTrackBuilder builder = new SeqTrackBuilder(
                        "lane",
                        new Run(),
                        new Sample(),
                        new SeqType(),
                        new SeqPlatform(),
                        new SoftwareTool()
                        )
        builder.setHasFinalBam(true).setHasOriginalBam(true).setUsingOriginalBam(true)
        builder.setnBasePairs(5).setnReads(6).setInsertSize(7)
        builder.setQualityEncoding(QualityEncoding.ILLUMINA).setAlignmentState(DataProcessingState.FINISHED).setFastqcState(DataProcessingState.IN_PROGRESS)

        SeqTrack seqTrack = builder.create()
        assertNotNull(seqTrack)
        assertEquals(SeqTrack.class, seqTrack.getClass())
        assertEquals("lane", seqTrack.laneId)
        assertEquals(true, seqTrack.hasFinalBam)
        assertEquals(true, seqTrack.hasOriginalBam)
        assertEquals(true, seqTrack.usingOriginalBam)
        assertEquals(5, seqTrack.nBasePairs)
        assertEquals(6, seqTrack.nReads)
        assertEquals(7, seqTrack.insertSize)
        assertNotNull(seqTrack.run)
        assertNotNull(seqTrack.sample)
        assertNotNull(seqTrack.seqType)
        assertNotNull(seqTrack.seqPlatform)
        assertNotNull(seqTrack.pipelineVersion)
        assertEquals(QualityEncoding.ILLUMINA, seqTrack.qualityEncoding)
        assertEquals(DataProcessingState.FINISHED, seqTrack.alignmentState)
        assertEquals(DataProcessingState.IN_PROGRESS, seqTrack.fastqcState)
    }

    void testCreateNoLane() {
        shouldFail(IllegalArgumentException.class) {
            new SeqTrackBuilder(
                            null,
                            new Run(),
                            new Sample(),
                            new SeqType(),
                            new SeqPlatform(),
                            new SoftwareTool()
                            )
        }
    }

    void testCreateNoRun() {
        shouldFail(IllegalArgumentException.class) {
            new SeqTrackBuilder(
                            "lane",
                            null,
                            new Sample(),
                            new SeqType(),
                            new SeqPlatform(),
                            new SoftwareTool()
                            )
        }
    }

    void testCreateNoSample() {
        shouldFail(IllegalArgumentException.class) {
            new SeqTrackBuilder(
                            "lane",
                            new Run(),
                            null,
                            new SeqType(),
                            new SeqPlatform(),
                            new SoftwareTool()
                            )
        }
    }

    void testCreateNoSeqType() {
        shouldFail(IllegalArgumentException.class) {
            new SeqTrackBuilder(
                            "lane",
                            new Run(),
                            new Sample(),
                            null,
                            new SeqPlatform(),
                            new SoftwareTool()
                            )
        }
    }

    void testCreateNoSeqPlatform() {
        shouldFail(IllegalArgumentException.class) {
            new SeqTrackBuilder(
                            "lane",
                            new Run(),
                            new Sample(),
                            new SeqType(),
                            null,
                            new SoftwareTool()
                            )
        }
    }

    void testCreateNoSoftwareTool() {
        shouldFail(IllegalArgumentException.class) {
            new SeqTrackBuilder(
                            "lane",
                            new Run(),
                            new Sample(),
                            new SeqType(),
                            new SeqPlatform(),
                            null
                            )
        }
    }

    void testCreateExome() {
        SeqTrackBuilder builder = new SeqTrackBuilder(
                        "lane",
                        new Run(),
                        new Sample(),
                        new SeqType(name: SeqTypeNames.EXOME.seqTypeName),
                        new SeqPlatform(),
                        new SoftwareTool()
                        )
        builder.setHasFinalBam(true).setHasOriginalBam(true).setUsingOriginalBam(true)
        builder.setnBasePairs(5).setnReads(6).setInsertSize(7)
        builder.setQualityEncoding(QualityEncoding.ILLUMINA).setAlignmentState(DataProcessingState.FINISHED).setFastqcState(DataProcessingState.IN_PROGRESS)

        SeqTrack seqTrack = builder.create()
        assertNotNull(seqTrack)
        assertEquals(ExomeSeqTrack.class, seqTrack.getClass())
        assertEquals("lane", seqTrack.laneId)
        assertEquals(true, seqTrack.hasFinalBam)
        assertEquals(true, seqTrack.hasOriginalBam)
        assertEquals(true, seqTrack.usingOriginalBam)
        assertEquals(5, seqTrack.nBasePairs)
        assertEquals(6, seqTrack.nReads)
        assertEquals(7, seqTrack.insertSize)
        assertNotNull(seqTrack.run)
        assertNotNull(seqTrack.sample)
        assertNotNull(seqTrack.seqType)
        assertNotNull(seqTrack.seqPlatform)
        assertNotNull(seqTrack.pipelineVersion)
        assertEquals(QualityEncoding.ILLUMINA, seqTrack.qualityEncoding)
        assertEquals(DataProcessingState.FINISHED, seqTrack.alignmentState)
        assertEquals(DataProcessingState.IN_PROGRESS, seqTrack.fastqcState)
        assertEquals(ExomeSeqTrack.KitInfoState.LATER_TO_CHECK, seqTrack.kitInfoState)
        assertNull(seqTrack.exomeEnrichmentKit)
    }

    void testCreateExomeWithExomeEnrichmentKit() {
        SeqTrackBuilder builder = new SeqTrackBuilder(
                        "lane",
                        new Run(),
                        new Sample(),
                        new SeqType(name: SeqTypeNames.EXOME.seqTypeName),
                        new SeqPlatform(),
                        new SoftwareTool()
                        )
        builder.setHasFinalBam(true).setHasOriginalBam(true).setUsingOriginalBam(true)
        builder.setnBasePairs(5).setnReads(6).setInsertSize(7)
        builder.setQualityEncoding(QualityEncoding.ILLUMINA).setAlignmentState(DataProcessingState.FINISHED).setFastqcState(DataProcessingState.IN_PROGRESS)
        builder.setExomeEnrichmentKit(new ExomeEnrichmentKit())

        SeqTrack seqTrack = builder.create()
        assertNotNull(seqTrack)
        assertEquals(ExomeSeqTrack.class, seqTrack.getClass())
        assertEquals("lane", seqTrack.laneId)
        assertEquals(true, seqTrack.hasFinalBam)
        assertEquals(true, seqTrack.hasOriginalBam)
        assertEquals(true, seqTrack.usingOriginalBam)
        assertEquals(5, seqTrack.nBasePairs)
        assertEquals(6, seqTrack.nReads)
        assertEquals(7, seqTrack.insertSize)
        assertNotNull(seqTrack.run)
        assertNotNull(seqTrack.sample)
        assertNotNull(seqTrack.seqType)
        assertNotNull(seqTrack.seqPlatform)
        assertNotNull(seqTrack.pipelineVersion)
        assertEquals(QualityEncoding.ILLUMINA, seqTrack.qualityEncoding)
        assertEquals(DataProcessingState.FINISHED, seqTrack.alignmentState)
        assertEquals(DataProcessingState.IN_PROGRESS, seqTrack.fastqcState)
        assertEquals(ExomeSeqTrack.KitInfoState.KNOWN, seqTrack.kitInfoState)
        assertNotNull(seqTrack.exomeEnrichmentKit)
    }

    void testCreateExomeWithKitInfoStateIsUnknown() {
        SeqTrackBuilder builder = new SeqTrackBuilder(
                        "lane",
                        new Run(),
                        new Sample(),
                        new SeqType(name: SeqTypeNames.EXOME.seqTypeName),
                        new SeqPlatform(),
                        new SoftwareTool()
                        )
        builder.setHasFinalBam(true).setHasOriginalBam(true).setUsingOriginalBam(true)
        builder.setnBasePairs(5).setnReads(6).setInsertSize(7)
        builder.setQualityEncoding(QualityEncoding.ILLUMINA).setAlignmentState(DataProcessingState.FINISHED).setFastqcState(DataProcessingState.IN_PROGRESS)
        builder.setKitInfoState(ExomeSeqTrack.KitInfoState.UNKNOWN)

        SeqTrack seqTrack = builder.create()
        assertNotNull(seqTrack)
        assertEquals(ExomeSeqTrack.class, seqTrack.getClass())
        assertEquals("lane", seqTrack.laneId)
        assertEquals(true, seqTrack.hasFinalBam)
        assertEquals(true, seqTrack.hasOriginalBam)
        assertEquals(true, seqTrack.usingOriginalBam)
        assertEquals(5, seqTrack.nBasePairs)
        assertEquals(6, seqTrack.nReads)
        assertEquals(7, seqTrack.insertSize)
        assertNotNull(seqTrack.run)
        assertNotNull(seqTrack.sample)
        assertNotNull(seqTrack.seqType)
        assertNotNull(seqTrack.seqPlatform)
        assertNotNull(seqTrack.pipelineVersion)
        assertEquals(QualityEncoding.ILLUMINA, seqTrack.qualityEncoding)
        assertEquals(DataProcessingState.FINISHED, seqTrack.alignmentState)
        assertEquals(DataProcessingState.IN_PROGRESS, seqTrack.fastqcState)
        assertEquals(ExomeSeqTrack.KitInfoState.UNKNOWN, seqTrack.kitInfoState)
        assertNull(seqTrack.exomeEnrichmentKit)
    }

    void testCreateExomeWithKitInfoStateIsLaterToCheck() {
        SeqTrackBuilder builder = new SeqTrackBuilder(
                        "lane",
                        new Run(),
                        new Sample(),
                        new SeqType(name: SeqTypeNames.EXOME.seqTypeName),
                        new SeqPlatform(),
                        new SoftwareTool()
                        )
        builder.setHasFinalBam(true).setHasOriginalBam(true).setUsingOriginalBam(true)
        builder.setnBasePairs(5).setnReads(6).setInsertSize(7)
        builder.setQualityEncoding(QualityEncoding.ILLUMINA).setAlignmentState(DataProcessingState.FINISHED).setFastqcState(DataProcessingState.IN_PROGRESS)
        builder.setKitInfoState(ExomeSeqTrack.KitInfoState.LATER_TO_CHECK)

        SeqTrack seqTrack = builder.create()
        assertNotNull(seqTrack)
        assertEquals(ExomeSeqTrack.class, seqTrack.getClass())
        assertEquals("lane", seqTrack.laneId)
        assertEquals(true, seqTrack.hasFinalBam)
        assertEquals(true, seqTrack.hasOriginalBam)
        assertEquals(true, seqTrack.usingOriginalBam)
        assertEquals(5, seqTrack.nBasePairs)
        assertEquals(6, seqTrack.nReads)
        assertEquals(7, seqTrack.insertSize)
        assertNotNull(seqTrack.run)
        assertNotNull(seqTrack.sample)
        assertNotNull(seqTrack.seqType)
        assertNotNull(seqTrack.seqPlatform)
        assertNotNull(seqTrack.pipelineVersion)
        assertEquals(QualityEncoding.ILLUMINA, seqTrack.qualityEncoding)
        assertEquals(DataProcessingState.FINISHED, seqTrack.alignmentState)
        assertEquals(DataProcessingState.IN_PROGRESS, seqTrack.fastqcState)
        assertEquals(ExomeSeqTrack.KitInfoState.LATER_TO_CHECK, seqTrack.kitInfoState)
        assertNull(seqTrack.exomeEnrichmentKit)
    }

    void testCreateExomeWithExomeEnrichmentKitIsNull() {
        shouldFail(IllegalArgumentException.class) {
            SeqTrackBuilder builder = new SeqTrackBuilder(
                            "lane",
                            new Run(),
                            new Sample(),
                            new SeqType(name: SeqTypeNames.EXOME.seqTypeName),
                            new SeqPlatform(),
                            new SoftwareTool()
                            )
            builder.setHasFinalBam(true).setHasOriginalBam(true).setUsingOriginalBam(true)
            builder.setnBasePairs(5).setnReads(6).setInsertSize(7)
            builder.setQualityEncoding(QualityEncoding.ILLUMINA).setAlignmentState(DataProcessingState.FINISHED).setFastqcState(DataProcessingState.IN_PROGRESS)
            builder.setExomeEnrichmentKit(null)
            builder.create()
        }
    }

    void testCreateExomeWithKitInfoStateIsKnownAndNoExomeEnrichmentKit() {
        shouldFail(IllegalArgumentException.class) {
            SeqTrackBuilder builder =  new SeqTrackBuilder(
                            "lane",
                            new Run(),
                            new Sample(),
                            new SeqType(name: SeqTypeNames.EXOME.seqTypeName),
                            new SeqPlatform(),
                            new SoftwareTool()
                            )
            builder.setHasFinalBam(true).setHasOriginalBam(true).setUsingOriginalBam(true)
            builder.setnBasePairs(5).setnReads(6).setInsertSize(7)
            builder.setQualityEncoding(QualityEncoding.ILLUMINA).setAlignmentState(DataProcessingState.FINISHED).setFastqcState(DataProcessingState.IN_PROGRESS)
            builder.setKitInfoState(ExomeSeqTrack.KitInfoState.KNOWN)
            builder.create()
        }
    }
}
