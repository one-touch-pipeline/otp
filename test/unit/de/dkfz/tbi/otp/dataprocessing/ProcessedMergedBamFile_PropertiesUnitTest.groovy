package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

import grails.test.mixin.Mock
import org.junit.After
import org.junit.Before
import org.junit.Test

@Mock([
        LibraryPreparationKit,
        MergingWorkPackage,
        MergingPass,
        MergingSet,
        SeqPlatform,
        SeqPlatformGroup,
        SeqCenter,
        SeqType,
        SeqTrack,
        SampleType,
        Pipeline,
        ProjectCategory,
        Project,
        ProcessedMergedBamFile,
        Individual,
        Sample,
        SoftwareTool,
        ReferenceGenome,
        RunSegment,
        FileType,
        DataFile,
        Run,
        Realm,
])
class ProcessedMergedBamFile_PropertiesUnitTest {

    static final short PROCESSING_PRIORITY = 1

    SampleType sampleType
    Sample sample
    SeqType seqType
    Individual individual
    Project project
    ReferenceGenome referenceGenome
    MergingWorkPackage workPackage
    ProcessedMergedBamFile bamFile

    @Before
    void setUp() {
        sampleType = DomainFactory.createSampleType()
        project = DomainFactory.createProject(processingPriority: PROCESSING_PRIORITY)
        individual = DomainFactory.createIndividual(project: project)
        sample = DomainFactory.createSample(sampleType: sampleType, individual: individual)
        referenceGenome = DomainFactory.createReferenceGenome()
        seqType = DomainFactory.createSeqType()
        workPackage = DomainFactory.createMergingWorkPackage(sample: sample, referenceGenome: referenceGenome, seqType: seqType, pipeline: DomainFactory.createDefaultOtpPipeline())
        MergingSet mergingSet = DomainFactory.createMergingSet(mergingWorkPackage: workPackage)
        MergingPass mergingPass = DomainFactory.createMergingPass(mergingSet: mergingSet)
        bamFile = DomainFactory.createProcessedMergedBamFileWithoutProcessedBamFile(mergingPass, [:], false)
    }

    @After
    void tearDown() {
        sample = null
        sampleType = null
        seqType = null
        individual = null
        project = null
        referenceGenome = null
        workPackage = null
        bamFile = null
    }

    @Test
    void testGetMergingWorkPackage() {
        assert workPackage == bamFile.mergingWorkPackage
    }

    @Test
    void testGetProject() {
        assert project == bamFile.project
    }

    @Test
    void testGetProcessingPriority() {
        assert PROCESSING_PRIORITY == bamFile.processingPriority
    }

    @Test
    void testGetIndividual() {
        assert individual == bamFile.individual
    }

    @Test
    void testGetSample() {
        assert sample == bamFile.sample
    }

    @Test
    void testGetSampleType() {
        assert sampleType == bamFile.sampleType
    }

    @Test
    void testGetSeqType() {
        assert seqType == bamFile.seqType
    }

    @Test
    void testGetReferenceGenome() {
        assert referenceGenome == bamFile.referenceGenome
    }
}
