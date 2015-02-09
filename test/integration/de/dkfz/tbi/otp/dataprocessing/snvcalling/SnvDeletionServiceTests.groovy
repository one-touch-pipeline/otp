
package de.dkfz.tbi.otp.dataprocessing.snvcalling

import grails.test.mixin.*

import org.junit.After
import org.junit.Before
import org.junit.Test

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*


class SnvDeletionServiceTests {

    SnvDeletionService snvDeletionService

    SnvCallingInstanceTestData testdata
    SnvCallingInstance instance

    @Before
    void setUp() {
        testdata = new SnvCallingInstanceTestData()
        testdata.createSnvObjects()
        instance = testdata.createSnvCallingInstance()
        assert instance.save()
        Realm realm = DomainFactory.createRealmDataManagementDKFZ()
        assert realm.save()
        createAllJobResults(instance)
    }

    @After
    void tearDown() {
        snvDeletionService = null
        testdata = null
        instance = null
    }

    void createAllJobResults(SnvCallingInstance instance) {
        SnvJobResult callingResult = testdata.createAndSaveSnvJobResult(instance, SnvCallingStep.CALLING)
        SnvJobResult annotationResult = testdata.createAndSaveSnvJobResult(instance, SnvCallingStep.SNV_ANNOTATION, callingResult)
        SnvJobResult deepAnnotationResult = testdata.createAndSaveSnvJobResult(instance, SnvCallingStep.SNV_DEEPANNOTATION, annotationResult)
        SnvJobResult filterResult = testdata.createAndSaveSnvJobResult(instance, SnvCallingStep.FILTER_VCF, deepAnnotationResult)
    }


    @Test(expected=AssertionError)
    public void testDeleteForProcessedMergedBamFile_shouldFail_NoBamFile() {
        snvDeletionService.deleteForProcessedMergedBamFile(null)
    }

    @Test(expected=RuntimeException)
    public void testDeleteForProcessedMergedBamFile_shouldFail_InstanceInProgress() {
        snvDeletionService.deleteForProcessedMergedBamFile(testdata.bamFileControl)
    }

    @Test
    public void testDeleteForProcessedMergedBamFile_InstanceFinishedForControlTumor1() {
        instance.processingState = SnvProcessingStates.FINISHED
        File directory = instance.samplePair.getSamplePairPath().getAbsoluteDataManagementPath()

        List<File> directories = snvDeletionService.deleteForProcessedMergedBamFile(testdata.bamFileControl)
        assert []== SnvJobResult.list()
        assert []== SnvCallingInstance.list()
        assert [testdata.samplePair2]== SamplePair.list()
        assert [directory] == directories
    }

    @Test
    public void testDeleteForProcessedMergedBamFile_InstanceFinishedForControlTumor1AndControlTumor2() {
        instance.processingState = SnvProcessingStates.FINISHED
        instance = testdata.createSnvCallingInstance([
            sampleType1BamFile: testdata.bamFileTumor2,
            processingState: SnvProcessingStates.FINISHED,
            samplePair: testdata.samplePair2,
        ])
        assert instance.save()
        createAllJobResults(instance)

        List<File> directories = snvDeletionService.deleteForProcessedMergedBamFile(testdata.bamFileControl)
        assert []== SnvJobResult.list()
        assert []== SnvCallingInstance.list()
        assert []== SamplePair.list()
        assert 2 == directories.size()
    }


    @Test
    public void testDeleteForProcessedMergedBamFile_MultipleInstancesFinishedForControlTumor1() {
        instance.processingState = SnvProcessingStates.FINISHED
        instance = testdata.createSnvCallingInstance([
            processingState: SnvProcessingStates.FINISHED,
            instanceName: 'instance2',

        ])
        assert instance.save()
        createAllJobResults(instance)

        List<File> directories = snvDeletionService.deleteForProcessedMergedBamFile(testdata.bamFileControl)
        assert []== SnvJobResult.list()
        assert []== SnvCallingInstance.list()
        assert [testdata.samplePair2]== SamplePair.list()
        assert 1 == directories.size()
    }

    @Test
    public void testDeleteForProcessedMergedBamFile_InstanceFinishedForControlTumor1AndControlTumor2_DeleteTumor1() {
        instance.processingState = SnvProcessingStates.FINISHED
        instance = testdata.createSnvCallingInstance([
            sampleType1BamFile: testdata.bamFileTumor2,
            processingState: SnvProcessingStates.FINISHED,
            samplePair: testdata.samplePair2,
        ])
        assert instance.save()
        createAllJobResults(instance)

        List<File> directories = snvDeletionService.deleteForProcessedMergedBamFile(testdata.bamFileTumor)
        assert 4== SnvJobResult.list().size()
        assert [instance]== SnvCallingInstance.list()
        assert [testdata.samplePair2]== SamplePair.list()
        assert 1 == directories.size()
    }

    @Test
    public void testDeleteForProcessedMergedBamFile_InstanceFinishedForControlTumor1AndControlTumor2_DeleteTumor1_tumor2InProgress() {
        instance.processingState = SnvProcessingStates.FINISHED
        instance = testdata.createSnvCallingInstance([
            sampleType1BamFile: testdata.bamFileTumor2,
            samplePair: testdata.samplePair2,
        ])
        assert instance.save()
        createAllJobResults(instance)

        List<File> directories = snvDeletionService.deleteForProcessedMergedBamFile(testdata.bamFileTumor)
        assert 4== SnvJobResult.list().size()
        assert [instance]== SnvCallingInstance.list()
        assert [testdata.samplePair2]== SamplePair.list()
        assert 1 == directories.size()
    }

    @Test
    public void testDeleteForProcessedMergedBamFile_InstanceFailedForControlTumor1() {
        SnvJobResult.list().each {
            it.withdrawn = true
            assert it.save(flush: true)
        }
        instance.processingState = SnvProcessingStates.FAILED
        File directory = instance.samplePair.getSamplePairPath().getAbsoluteDataManagementPath()

        List<File> directories = snvDeletionService.deleteForProcessedMergedBamFile(testdata.bamFileControl)
        assert []== SnvJobResult.list()
        assert []== SnvCallingInstance.list()
        assert [testdata.samplePair2]== SamplePair.list()
        assert [directory] == directories
    }


}
