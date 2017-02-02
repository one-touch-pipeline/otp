package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import grails.test.mixin.*
import org.junit.*

@Mock([
        ProcessedMergedBamFile
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
        sampleType = new SampleType()
        project = new Project(processingPriority: PROCESSING_PRIORITY)
        individual = new Individual(project: project)
        sample = new Sample(sampleType: sampleType, individual: individual)
        referenceGenome = new ReferenceGenome()
        seqType = new SeqType()
        workPackage = new MergingWorkPackage(sample: sample, referenceGenome: referenceGenome, seqType: seqType, pipeline: new Pipeline())
        MergingSet mergingSet = new MergingSet(mergingWorkPackage: workPackage)
        MergingPass mergingPass = new MergingPass(mergingSet: mergingSet)
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
