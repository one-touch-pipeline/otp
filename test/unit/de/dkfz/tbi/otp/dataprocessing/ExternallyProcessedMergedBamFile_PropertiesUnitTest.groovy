package de.dkfz.tbi.otp.dataprocessing

import grails.test.mixin.Mock
import org.junit.Before
import org.junit.Test

import de.dkfz.tbi.otp.ngsdata.*

@Mock([
        ExternalMergingWorkPackage,
        ExternallyProcessedMergedBamFile,
        Individual,
        Sample,
        SampleType,
        SeqType,
        Project,
        Pipeline,
        SeqPlatformGroup,
        Realm,
        ReferenceGenome,
])
class ExternallyProcessedMergedBamFile_PropertiesUnitTest {

    static final short PROCESSING_PRIORITY = 1

    SampleType sampleType
    Sample sample
    SeqType seqType
    Individual individual
    Project project
    ReferenceGenome referenceGenome
    ExternallyProcessedMergedBamFile bamFile

    @Before
    void setUp() {
        sampleType = new SampleType()
        project = new Project(processingPriority: PROCESSING_PRIORITY)
        individual = new Individual(project: project)
        sample = new Sample(sampleType: sampleType, individual: individual)
        referenceGenome = new ReferenceGenome()
        seqType = new SeqType()
        ExternalMergingWorkPackage externalMergingWorkPackage = DomainFactory.createExternalMergingWorkPackage(referenceGenome: referenceGenome, sample: sample, seqType: seqType)
        bamFile = new ExternallyProcessedMergedBamFile(workPackage: externalMergingWorkPackage)
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
