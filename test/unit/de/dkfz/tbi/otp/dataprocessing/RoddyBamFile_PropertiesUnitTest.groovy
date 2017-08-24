package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.mixin.*
import org.junit.*

@Mock([
        DataFile,
        FileType,
        Individual,
        LibraryPreparationKit,
        MergingCriteria,
        MergingWorkPackage,
        Pipeline,
        Project,
        ProjectCategory,
        ReferenceGenome,
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
class RoddyBamFile_PropertiesUnitTest {

    static final short PROCESSING_PRIORITY = 1

    SampleType sampleType
    Sample sample
    SeqType seqType
    Individual individual
    Project project
    ReferenceGenome referenceGenome
    MergingWorkPackage workPackage
    Pipeline pipeline
    RoddyBamFile bamFile

    @Before
    void setUp() {
        sampleType = DomainFactory.createSampleType()
        project = DomainFactory.createProject(processingPriority: PROCESSING_PRIORITY)
        individual = DomainFactory.createIndividual(project: project)
        sample = DomainFactory.createSample(sampleType: sampleType, individual: individual)
        referenceGenome = DomainFactory.createReferenceGenome()
        seqType = DomainFactory.createSeqType()
        pipeline = DomainFactory.createPanCanPipeline()
        workPackage = DomainFactory.createMergingWorkPackage(
                sample:          sample,
                referenceGenome: referenceGenome,
                seqType:         seqType,
                pipeline:        pipeline,
        )
        bamFile = DomainFactory.createRoddyBamFile(workPackage: workPackage)
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
        pipeline = null
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
