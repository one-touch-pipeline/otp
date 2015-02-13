
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
        File directoryInstance = instance.getSnvInstancePath().getAbsoluteDataManagementPath()
        File directorySamplePair = instance.samplePair.getSamplePairPath().getAbsoluteDataManagementPath()

        List<File> directories = snvDeletionService.deleteForProcessedMergedBamFile(testdata.bamFileControl)
        assert []== SnvJobResult.list()
        assert []== SnvCallingInstance.list()
        assert [testdata.samplePair2]== SamplePair.list()
        assert 2 == directories.size()
        assert [directoryInstance, directorySamplePair] == directories
    }

    @Test
    public void testDeleteForProcessedMergedBamFile_InstanceFinishedForControlTumor1AndControlTumor2() {
        instance.processingState = SnvProcessingStates.FINISHED
        SnvCallingInstance instance2 = testdata.createSnvCallingInstance([
            sampleType1BamFile: testdata.bamFileTumor2,
            processingState: SnvProcessingStates.FINISHED,
            samplePair: testdata.samplePair2,
        ])
        assert instance2.save()
        createAllJobResults(instance2)
        File directoryInstance1 = instance.getSnvInstancePath().getAbsoluteDataManagementPath()
        File directoryInstance2 = instance2.getSnvInstancePath().getAbsoluteDataManagementPath()
        File directorySamplePair1 = instance.samplePair.getSamplePairPath().getAbsoluteDataManagementPath()
        File directorySamplePair2 = instance2.samplePair.getSamplePairPath().getAbsoluteDataManagementPath()

        List<File> directories = snvDeletionService.deleteForProcessedMergedBamFile(testdata.bamFileControl)
        assert []== SnvJobResult.list()
        assert []== SnvCallingInstance.list()
        assert []== SamplePair.list()
        assert 4 == directories.size()
        assert [directoryInstance1, directoryInstance2, directorySamplePair1, directorySamplePair2] == directories

    }


    @Test
    public void testDeleteForProcessedMergedBamFile_MultipleInstancesFinishedForControlTumor1() {
        instance.processingState = SnvProcessingStates.FINISHED
        SnvCallingInstance instance2 = testdata.createSnvCallingInstance([
            processingState: SnvProcessingStates.FINISHED,
            instanceName: 'instance2',

        ])
        assert instance2.save()
        createAllJobResults(instance2)
        File directoryInstance1 = instance.getSnvInstancePath().getAbsoluteDataManagementPath()
        File directoryInstance2 = instance2.getSnvInstancePath().getAbsoluteDataManagementPath()
        File directorySamplePair = instance.samplePair.getSamplePairPath().getAbsoluteDataManagementPath()

        List<File> directories = snvDeletionService.deleteForProcessedMergedBamFile(testdata.bamFileControl)
        assert []== SnvJobResult.list()
        assert []== SnvCallingInstance.list()
        assert [testdata.samplePair2]== SamplePair.list()
        assert 3 == directories.size()
        assert [directoryInstance1, directoryInstance2, directorySamplePair] == directories
    }

    @Test
    public void testDeleteForProcessedMergedBamFile_InstanceFinishedForControlTumor1AndControlTumor2_DeleteTumor1() {
        instance.processingState = SnvProcessingStates.FINISHED
        SnvCallingInstance instance2 = testdata.createSnvCallingInstance([
            sampleType1BamFile: testdata.bamFileTumor2,
            processingState: SnvProcessingStates.FINISHED,
            samplePair: testdata.samplePair2,
        ])
        assert instance2.save()
        createAllJobResults(instance2)
        File directoryInstance = instance.getSnvInstancePath().getAbsoluteDataManagementPath()
        File directorySamplePair = instance.samplePair.getSamplePairPath().getAbsoluteDataManagementPath()

        List<File> directories = snvDeletionService.deleteForProcessedMergedBamFile(testdata.bamFileTumor)
        assert 4 == SnvJobResult.list().size()
        assert [instance2]== SnvCallingInstance.list()
        assert [testdata.samplePair2]== SamplePair.list()
        assert 2 == directories.size()
        assert [directoryInstance, directorySamplePair] == directories
    }

    @Test
    public void testDeleteForProcessedMergedBamFile_InstanceFinishedForControlTumor1AndControlTumor2_DeleteTumor1_tumor2InProgress() {
        instance.processingState = SnvProcessingStates.FINISHED
        SnvCallingInstance instance2 = testdata.createSnvCallingInstance([
            sampleType1BamFile: testdata.bamFileTumor2,
            samplePair: testdata.samplePair2,
        ])
        assert instance2.save()
        createAllJobResults(instance2)
        File directoryInstance = instance.getSnvInstancePath().getAbsoluteDataManagementPath()
        File directorySamplePair = instance.samplePair.getSamplePairPath().getAbsoluteDataManagementPath()

        List<File> directories = snvDeletionService.deleteForProcessedMergedBamFile(testdata.bamFileTumor)
        assert 4== SnvJobResult.list().size()
        assert [instance2]== SnvCallingInstance.list()
        assert [testdata.samplePair2]== SamplePair.list()
        assert 2 == directories.size()
        assert [directoryInstance, directorySamplePair] == directories
    }

    @Test
    public void testDeleteForProcessedMergedBamFile_InstanceFailedForControlTumor1() {
        SnvJobResult.list().each {
            it.withdrawn = true
            assert it.save(flush: true)
        }
        instance.processingState = SnvProcessingStates.FAILED
        File directoryInstance = instance.getSnvInstancePath().getAbsoluteDataManagementPath()
        File directorySamplePair = instance.samplePair.getSamplePairPath().getAbsoluteDataManagementPath()

        List<File> directories = snvDeletionService.deleteForProcessedMergedBamFile(testdata.bamFileControl)
        assert []== SnvJobResult.list()
        assert []== SnvCallingInstance.list()
        assert [testdata.samplePair2]== SamplePair.list()
        assert 2 == directories.size()
        assert [directoryInstance, directorySamplePair] == directories
    }



    @Test
    public void testDeleteForProcessedMergedBamFile_InstanceFinishedForControlTumor1_TwoBamFiles_DeleteOnlyOneInstance() {
        instance.processingState = SnvProcessingStates.FINISHED
        ProcessedMergedBamFile bamFile2 = testdata.createProcessedMergedBamFile(testdata.bamFileTumor.individual, testdata.bamFileTumor.seqType)
        bamFile2.mergingSet.mergingWorkPackage = testdata.bamFileTumor.mergingWorkPackage
        bamFile2.mergingSet.identifier = 1
        assert bamFile2.mergingSet.save(flush: true)
        assert bamFile2.save(flush: true)
        SnvCallingInstance instance2 = testdata.createSnvCallingInstance([
            sampleType1BamFile: bamFile2,
            samplePair: testdata.samplePair1,
            instanceName: 'test2',
            processingState: SnvProcessingStates.FINISHED,
        ])
        assert instance2.save()
        createAllJobResults(instance2)
        File directoryInstance1 = instance.getSnvInstancePath().getAbsoluteDataManagementPath()
        File directoryInstance2 = instance2.getSnvInstancePath().getAbsoluteDataManagementPath()
        File directorySamplePair = instance.samplePair.getSamplePairPath().getAbsoluteDataManagementPath()


        List<File> directories = snvDeletionService.deleteForProcessedMergedBamFile(testdata.bamFileTumor)
        assert 4 == SnvJobResult.list().size()
        assert [instance2] == SnvCallingInstance.list()
        assert [testdata.samplePair1, testdata.samplePair2] == SamplePair.list()
        assert 1 == directories.size()
        assert [directoryInstance1] == directories
    }

    @Test
    public void testDeleteForProcessedMergedBamFile_InstanceFinishedForControlTumor1_TwoBamFiles_DeleteBoth() {
        instance.processingState = SnvProcessingStates.FINISHED
        ProcessedMergedBamFile bamFile2 = testdata.createProcessedMergedBamFile(testdata.bamFileTumor.individual, testdata.bamFileTumor.seqType)
        bamFile2.mergingSet.mergingWorkPackage = testdata.bamFileTumor.mergingWorkPackage
        bamFile2.mergingSet.identifier = 1
        assert bamFile2.mergingSet.save(flush: true)
        assert bamFile2.save(flush: true)
        SnvCallingInstance instance2 = testdata.createSnvCallingInstance([
            sampleType1BamFile: bamFile2,
            samplePair: testdata.samplePair1,
            instanceName: 'test2',
            processingState: SnvProcessingStates.FINISHED,
        ])
        assert instance2.save()
        createAllJobResults(instance2)
        File directoryInstance1 = instance.getSnvInstancePath().getAbsoluteDataManagementPath()
        File directoryInstance2 = instance2.getSnvInstancePath().getAbsoluteDataManagementPath()
        File directorySamplePair = instance.samplePair.getSamplePairPath().getAbsoluteDataManagementPath()


        List<File> directories1 = snvDeletionService.deleteForProcessedMergedBamFile(testdata.bamFileTumor)
        List<File> directories2 = snvDeletionService.deleteForProcessedMergedBamFile(bamFile2)
        assert 0 == SnvJobResult.list().size()
        assert [] == SnvCallingInstance.list()
        assert [testdata.samplePair2] == SamplePair.list()
        assert 1 == directories1.size()
        assert 2 == directories2.size()
        assert [directoryInstance1] == directories1
        assert [directoryInstance2, directorySamplePair] == directories2
    }


}
