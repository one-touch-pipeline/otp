package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*
import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.ngsdata.SeqTrack.DataProcessingState
import de.dkfz.tbi.otp.ngsdata.SeqTrack.QualityEncoding

@TestMixin(GrailsUnitTestMixin)
@Mock([SeqTrack, ExomeSeqTrack, ChipSeqSeqTrack])
class SeqTrackBuilderUnitTests {

    final String LANE_ID = "lane"
    final String ANTIBODY_TARGET_NAME = "antibodyTargetName"
    final String ANTIBODY_NAME = "antibodyName"
    final AntibodyTarget ANTIBODY_TARGET = new AntibodyTarget(name: ANTIBODY_TARGET_NAME)
    final long N_BASE_PAIRS = 5
    final long N_READS = 6
    final int INSERT_SIZE = 7

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
        builder.setQualityEncoding(QualityEncoding.ILLUMINA).setFastqcState(DataProcessingState.IN_PROGRESS)

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
        builder.setQualityEncoding(QualityEncoding.ILLUMINA).setFastqcState(DataProcessingState.IN_PROGRESS)

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
        assertEquals(DataProcessingState.IN_PROGRESS, seqTrack.fastqcState)
        assertEquals(InformationReliability.UNKNOWN_UNVERIFIED, seqTrack.kitInfoReliability)
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
        builder.setQualityEncoding(QualityEncoding.ILLUMINA).setFastqcState(DataProcessingState.IN_PROGRESS)
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
        assertEquals(DataProcessingState.IN_PROGRESS, seqTrack.fastqcState)
        assertEquals(InformationReliability.KNOWN, seqTrack.kitInfoReliability)
        assertNotNull(seqTrack.exomeEnrichmentKit)
    }

    void testCreateExomeWithKitInfoReliabilityIsUnknown() {
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
        builder.setQualityEncoding(QualityEncoding.ILLUMINA)setFastqcState(DataProcessingState.IN_PROGRESS)
        builder.setInformationReliability(InformationReliability.UNKNOWN_VERIFIED)

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
        assertEquals(DataProcessingState.IN_PROGRESS, seqTrack.fastqcState)
        assertEquals(InformationReliability.UNKNOWN_VERIFIED, seqTrack.kitInfoReliability)
        assertNull(seqTrack.exomeEnrichmentKit)
    }

    void testCreateExomeWithKitInfoReliabilityIsUnknownUnverified() {
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
        builder.setQualityEncoding(QualityEncoding.ILLUMINA).setFastqcState(DataProcessingState.IN_PROGRESS)
        builder.setInformationReliability(InformationReliability.UNKNOWN_UNVERIFIED)

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
        assertEquals(DataProcessingState.IN_PROGRESS, seqTrack.fastqcState)
        assertEquals(InformationReliability.UNKNOWN_UNVERIFIED, seqTrack.kitInfoReliability)
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
            builder.setQualityEncoding(QualityEncoding.ILLUMINA).setFastqcState(DataProcessingState.IN_PROGRESS)
            builder.setExomeEnrichmentKit(null)
            builder.create()
        }
    }

    void testCreateExomeWithKitInfoReliabilityIsKnownAndNoExomeEnrichmentKit() {
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
            builder.setQualityEncoding(QualityEncoding.ILLUMINA).setFastqcState(DataProcessingState.IN_PROGRESS)
            builder.setInformationReliability(InformationReliability.KNOWN)
            builder.create()
        }
    }

    void testCreateChipSeqWithAntibodyTargetAndNoAntibody() {
        SeqTrackBuilder builder =  new SeqTrackBuilder(
                        LANE_ID,
                        new Run(),
                        new Sample(),
                        new SeqType(name: SeqTypeNames.CHIP_SEQ.seqTypeName),
                        new SeqPlatform(),
                        new SoftwareTool()
                        )
        builder.setHasFinalBam(true).setHasOriginalBam(true).setUsingOriginalBam(true)
        builder.setnBasePairs(N_BASE_PAIRS).setnReads(N_READS).setInsertSize(INSERT_SIZE)
        builder.setQualityEncoding(QualityEncoding.ILLUMINA).setFastqcState(DataProcessingState.IN_PROGRESS)
        builder.setAntibodyTarget(ANTIBODY_TARGET)

        SeqTrack seqTrack = builder.create()
        assertNotNull(seqTrack)
        assertEquals(ChipSeqSeqTrack.class, seqTrack.getClass())
        assertEquals(LANE_ID, seqTrack.laneId)
        assertEquals(true, seqTrack.hasFinalBam)
        assertEquals(true, seqTrack.hasOriginalBam)
        assertEquals(true, seqTrack.usingOriginalBam)
        assertEquals(N_BASE_PAIRS, seqTrack.nBasePairs)
        assertEquals(N_READS, seqTrack.nReads)
        assertEquals(INSERT_SIZE, seqTrack.insertSize)
        assertNotNull(seqTrack.run)
        assertNotNull(seqTrack.sample)
        assertNotNull(seqTrack.seqType)
        assertNotNull(seqTrack.seqPlatform)
        assertNotNull(seqTrack.pipelineVersion)
        assertEquals(QualityEncoding.ILLUMINA, seqTrack.qualityEncoding)
        assertEquals(DataProcessingState.IN_PROGRESS, seqTrack.fastqcState)
        assertEquals(ANTIBODY_TARGET, seqTrack.antibodyTarget)
        assertNull(seqTrack.antibody)
    }

    void testCreateChipSeqWithAntibodyTargetAndAntibody() {
        SeqTrackBuilder builder =  new SeqTrackBuilder(
                        LANE_ID,
                        new Run(),
                        new Sample(),
                        new SeqType(name: SeqTypeNames.CHIP_SEQ.seqTypeName),
                        new SeqPlatform(),
                        new SoftwareTool()
                        )
        builder.setHasFinalBam(true).setHasOriginalBam(true).setUsingOriginalBam(true)
        builder.setnBasePairs(N_BASE_PAIRS).setnReads(N_READS).setInsertSize(INSERT_SIZE)
        builder.setQualityEncoding(QualityEncoding.ILLUMINA).setFastqcState(DataProcessingState.IN_PROGRESS)
        builder.setAntibodyTarget(ANTIBODY_TARGET)
        builder.setAntibody(ANTIBODY_NAME)

        SeqTrack seqTrack = builder.create()
        assertNotNull(seqTrack)
        assertEquals(ChipSeqSeqTrack.class, seqTrack.getClass())
        assertEquals(LANE_ID, seqTrack.laneId)
        assertEquals(true, seqTrack.hasFinalBam)
        assertEquals(true, seqTrack.hasOriginalBam)
        assertEquals(true, seqTrack.usingOriginalBam)
        assertEquals(N_BASE_PAIRS, seqTrack.nBasePairs)
        assertEquals(N_READS, seqTrack.nReads)
        assertEquals(INSERT_SIZE, seqTrack.insertSize)
        assertNotNull(seqTrack.run)
        assertNotNull(seqTrack.sample)
        assertNotNull(seqTrack.seqType)
        assertNotNull(seqTrack.seqPlatform)
        assertNotNull(seqTrack.pipelineVersion)
        assertEquals(QualityEncoding.ILLUMINA, seqTrack.qualityEncoding)
        assertEquals(DataProcessingState.IN_PROGRESS, seqTrack.fastqcState)
        assertEquals(ANTIBODY_TARGET, seqTrack.antibodyTarget)
        assertEquals(ANTIBODY_NAME, seqTrack.antibody)
    }

    void testCreateChipSeqWithNoAntibodyTarget() {
        shouldFail(IllegalArgumentException.class) {
            SeqTrackBuilder builder =  new SeqTrackBuilder(
                            LANE_ID,
                            new Run(),
                            new Sample(),
                            new SeqType(name: SeqTypeNames.CHIP_SEQ.seqTypeName),
                            new SeqPlatform(),
                            new SoftwareTool()
                            )
            builder.setHasFinalBam(true).setHasOriginalBam(true).setUsingOriginalBam(true)
            builder.setnBasePairs(N_BASE_PAIRS).setnReads(N_READS).setInsertSize(INSERT_SIZE)
            builder.setQualityEncoding(QualityEncoding.ILLUMINA).setFastqcState(DataProcessingState.IN_PROGRESS)
            builder.create()
        }
    }
}
