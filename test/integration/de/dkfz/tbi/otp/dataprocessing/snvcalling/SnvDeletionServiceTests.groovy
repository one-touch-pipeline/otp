
package de.dkfz.tbi.otp.dataprocessing.snvcalling

import org.junit.After
import org.junit.Before
import org.junit.Test

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*


class SnvDeletionServiceTests {

    SnvDeletionService snvDeletionService

    SnvCallingInstanceTestData testdata
    SnvCallingInstance instance
    ProcessedMergedBamFile bamFileTumor2
    SamplePair samplePair2

    @Before
    void setUp() {
        testdata = new SnvCallingInstanceTestData()
        testdata.createSnvObjects(TestCase.uniqueNonExistentPath)
        instance = testdata.createSnvCallingInstance()
        assert instance.save()
        Realm realm = DomainFactory.createRealmDataManagementDKFZ()
        assert realm.save()
        createAllJobResults(instance)
        (bamFileTumor2, samplePair2) = testdata.createDisease(testdata.bamFileControl.mergingWorkPackage)
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
    public void testDeleteForAbstractMergedBamFile_shouldFail_NoBamFile() {
        snvDeletionService.deleteForAbstractMergedBamFile(null)
    }

    @Test(expected=RuntimeException)
    public void testDeleteForAbstractMergedBamFile_shouldFail_InstanceInProgress() {
        snvDeletionService.deleteForAbstractMergedBamFile(testdata.bamFileControl)
    }

    @Test
    public void testDeleteForAbstractMergedBamFile_InstanceFinishedForControlTumor1() {
        instance.processingState = AnalysisProcessingStates.FINISHED
        File directoryInstance = instance.getSnvInstancePath().getAbsoluteDataManagementPath()
        File directorySamplePair = instance.samplePair.getSnvSamplePairPath().getAbsoluteDataManagementPath()

        List<File> directories = snvDeletionService.deleteForAbstractMergedBamFile(testdata.bamFileControl)
        assert []== SnvJobResult.list()
        assert []== SnvCallingInstance.list()
        assert [samplePair2]== SamplePair.list()
        assert 2 == directories.size()
        assert [directoryInstance, directorySamplePair] == directories
    }

    @Test
    public void testDeleteForAbstractMergedBamFile_InstanceFinishedForControlTumor1AndControlTumor2() {
        instance.processingState = AnalysisProcessingStates.FINISHED
        SnvCallingInstance instance2 = testdata.createSnvCallingInstance([
            sampleType1BamFile: bamFileTumor2,
            processingState: AnalysisProcessingStates.FINISHED,
            samplePair: samplePair2,
        ])
        assert instance2.save()
        createAllJobResults(instance2)
        File directoryInstance1 = instance.getSnvInstancePath().getAbsoluteDataManagementPath()
        File directoryInstance2 = instance2.getSnvInstancePath().getAbsoluteDataManagementPath()
        File directorySamplePair1 = instance.samplePair.getSnvSamplePairPath().getAbsoluteDataManagementPath()
        File directorySamplePair2 = instance2.samplePair.getSnvSamplePairPath().getAbsoluteDataManagementPath()

        List<File> directories = snvDeletionService.deleteForAbstractMergedBamFile(testdata.bamFileControl)
        assert []== SnvJobResult.list()
        assert []== SnvCallingInstance.list()
        assert []== SamplePair.list()
        assert 4 == directories.size()
        assert [directoryInstance1, directoryInstance2, directorySamplePair1, directorySamplePair2] == directories

    }


    @Test
    public void testDeleteForAbstractMergedBamFile_MultipleInstancesFinishedForControlTumor1() {
        instance.processingState = AnalysisProcessingStates.FINISHED
        SnvCallingInstance instance2 = testdata.createSnvCallingInstance([
            processingState: AnalysisProcessingStates.FINISHED,
            instanceName: 'instance2',

        ])
        assert instance2.save(flush: true)
        createAllJobResults(instance2)
        File directoryInstance1 = instance.getSnvInstancePath().getAbsoluteDataManagementPath()
        File directoryInstance2 = instance2.getSnvInstancePath().getAbsoluteDataManagementPath()
        File directorySamplePair = instance.samplePair.getSnvSamplePairPath().getAbsoluteDataManagementPath()

        List<File> directories = snvDeletionService.deleteForAbstractMergedBamFile(testdata.bamFileControl)
        assert []== SnvJobResult.list()
        assert []== SnvCallingInstance.list()
        assert [samplePair2]== SamplePair.list()
        assert 3 == directories.size()
        assert [directoryInstance1, directoryInstance2, directorySamplePair] == directories
    }

    @Test
    public void testDeleteForAbstractMergedBamFile_InstanceFinishedForControlTumor1AndControlTumor2_DeleteTumor1() {
        instance.processingState = AnalysisProcessingStates.FINISHED
        SnvCallingInstance instance2 = testdata.createSnvCallingInstance([
            sampleType1BamFile: bamFileTumor2,
            processingState: AnalysisProcessingStates.FINISHED,
            samplePair: samplePair2,
        ])
        assert instance2.save()
        createAllJobResults(instance2)
        File directoryInstance = instance.getSnvInstancePath().getAbsoluteDataManagementPath()
        File directorySamplePair = instance.samplePair.getSnvSamplePairPath().getAbsoluteDataManagementPath()

        List<File> directories = snvDeletionService.deleteForAbstractMergedBamFile(testdata.bamFileTumor)
        assert 4 == SnvJobResult.list().size()
        assert [instance2]== SnvCallingInstance.list()
        assert [samplePair2]== SamplePair.list()
        assert 2 == directories.size()
        assert [directoryInstance, directorySamplePair] == directories
    }

    @Test
    public void testDeleteForAbstractMergedBamFile_InstanceFinishedForControlTumor1AndControlTumor2_DeleteTumor1_tumor2InProgress() {
        instance.processingState = AnalysisProcessingStates.FINISHED
        SnvCallingInstance instance2 = testdata.createSnvCallingInstance([
            sampleType1BamFile: bamFileTumor2,
            samplePair: samplePair2,
        ])
        assert instance2.save()
        createAllJobResults(instance2)
        File directoryInstance = instance.getSnvInstancePath().getAbsoluteDataManagementPath()
        File directorySamplePair = instance.samplePair.getSnvSamplePairPath().getAbsoluteDataManagementPath()

        List<File> directories = snvDeletionService.deleteForAbstractMergedBamFile(testdata.bamFileTumor)
        assert 4== SnvJobResult.list().size()
        assert [instance2]== SnvCallingInstance.list()
        assert [samplePair2]== SamplePair.list()
        assert 2 == directories.size()
        assert [directoryInstance, directorySamplePair] == directories
    }

    @Test
    public void testDeleteForAbstractMergedBamFile_InstanceFailedForControlTumor1() {
        SnvJobResult.list().each {
            it.withdrawn = true
            assert it.save(flush: true)
        }
        instance.withdrawn = true
        File directoryInstance = instance.getSnvInstancePath().getAbsoluteDataManagementPath()
        File directorySamplePair = instance.samplePair.getSnvSamplePairPath().getAbsoluteDataManagementPath()

        List<File> directories = snvDeletionService.deleteForAbstractMergedBamFile(testdata.bamFileControl)
        assert []== SnvJobResult.list()
        assert []== SnvCallingInstance.list()
        assert [samplePair2]== SamplePair.list()
        assert 2 == directories.size()
        assert [directoryInstance, directorySamplePair] == directories
    }

    private SnvCallingInstance prepareForTwoBamFilesTests() {
        instance.processingState = AnalysisProcessingStates.FINISHED
        ProcessedMergedBamFile bamFile2 = DomainFactory.createProcessedMergedBamFile(
                testdata.bamFileTumor.mergingWorkPackage, DomainFactory.randomProcessedBamFileProperties)
        SnvCallingInstance instance2 = testdata.createSnvCallingInstance([
            sampleType1BamFile: bamFile2,
            samplePair: testdata.samplePair,
            instanceName: 'test2',
            processingState: AnalysisProcessingStates.FINISHED,
        ])
        assert instance2.save(flush: true)
        createAllJobResults(instance2)
        return instance2
    }

    @Test
    public void testDeleteForAbstractMergedBamFile_InstanceFinishedForControlTumor1_TwoBamFiles_DeleteOnlyOneInstance() {
        SnvCallingInstance instance2 = prepareForTwoBamFilesTests()
        File directoryInstance1 = instance.getSnvInstancePath().getAbsoluteDataManagementPath()
        File directoryInstance2 = instance2.getSnvInstancePath().getAbsoluteDataManagementPath()
        File directorySamplePair = instance.samplePair.getSnvSamplePairPath().getAbsoluteDataManagementPath()


        List<File> directories = snvDeletionService.deleteForAbstractMergedBamFile(testdata.bamFileTumor)
        assert 4 == SnvJobResult.list().size()
        assert [instance2] == SnvCallingInstance.list()
        assert [testdata.samplePair, samplePair2] == SamplePair.list()
        assert 1 == directories.size()
        assert [directoryInstance1] == directories
    }

    @Test
    public void testDeleteForAbstractMergedBamFile_InstanceFinishedForControlTumor1_TwoBamFiles_DeleteBoth() {
        SnvCallingInstance instance2 = prepareForTwoBamFilesTests()
        ProcessedMergedBamFile bamFile2 = instance2.sampleType1BamFile
        File directoryInstance1 = instance.getSnvInstancePath().getAbsoluteDataManagementPath()
        File directoryInstance2 = instance2.getSnvInstancePath().getAbsoluteDataManagementPath()
        File directorySamplePair = instance.samplePair.getSnvSamplePairPath().getAbsoluteDataManagementPath()


        List<File> directories1 = snvDeletionService.deleteForAbstractMergedBamFile(testdata.bamFileTumor)
        List<File> directories2 = snvDeletionService.deleteForAbstractMergedBamFile(bamFile2)
        assert 0 == SnvJobResult.list().size()
        assert [] == SnvCallingInstance.list()
        assert [samplePair2] == SamplePair.list()
        assert 1 == directories1.size()
        assert 2 == directories2.size()
        assert [directoryInstance1] == directories1
        assert [directoryInstance2, directorySamplePair] == directories2
    }
}
