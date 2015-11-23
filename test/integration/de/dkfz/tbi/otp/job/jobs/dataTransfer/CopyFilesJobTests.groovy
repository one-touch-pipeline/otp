package de.dkfz.tbi.otp.job.jobs.dataTransfer

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.job.jobs.WatchdogJob
import de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys
import de.dkfz.tbi.otp.job.processing.ExecutionHelperService
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.utils.CollectionUtils
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.Realm.OperationType
import org.springframework.beans.factory.annotation.Autowired

import static junit.framework.TestCase.assertEquals
import static junit.framework.TestCase.assertNotNull


class CopyFilesJobTests {

    @Autowired
    CopyFilesJob job

    TestData testData

    final static String PBS_ID = "1234"


    @After
    void tearDown() {
        testData = null
        TestCase.removeMetaClass(ProcessedMergedBamFileService, job.processedMergedBamFileService)
        TestCase.removeMetaClass(ExecutionService, job.executionService)
        TestCase.removeMetaClass(ExecutionHelperService, job.executionHelperService)
        TestCase.removeMetaClass(RunProcessingService, job.runProcessingService)
        TestCase.removeMetaClass(LsdfFilesService, job.lsdfFilesService)
        TestCase.removeMetaClass(CopyFilesJob, job)
        AbstractMergedBamFileService.metaClass = null
    }


    @Test
    void testExecute_RunCanNotBeFound_IllegalArgumentException() {
        job.metaClass.getProcessParameterValue = { -> "-1" }
        shouldFail(IllegalArgumentException) {job.execute()}
    }

    @Test
    void testExecute_NoDataFileAndOneBamFileFound_CheckForInfoFileCreation() {
        Run run = Run.build()
        Realm realm = Realm.build(operationType: Realm.OperationType.DATA_MANAGEMENT)
        ProcessedMergedBamFile processedMergedBamFile = DomainFactory.createProcessedMergedBamFile()
        processedMergedBamFile.project.realmName = realm.name

        job.metaClass.getProcessParameterValue = { -> run.id.toString() }
        job.metaClass.processedMergedBamFilesForRun = { Run run2 -> assert run2 == run; return [processedMergedBamFile] }

        AbstractMergedBamFileService.metaClass.static.destinationDirectory = { ProcessedMergedBamFile pmbf -> TestCase.uniqueNonExistentPath.path }

        job.metaClass.addOutputParameter = { String parameterName, String ids ->
            assert ids == WatchdogJob.SKIP_WATCHDOG
        }

        job.executionService.metaClass.executeCommand = { Realm realm1, String command ->
            assert command.contains("A new lane is currently in progress for this sample")
        }

        job.execute()
    }

    @Test
    void testExecute_NoBamFilesFound_DataFilesHaveToBeLinked_PBSListIsEmpty() {
        Map paths = prepareTestExecute(true)

        job.executionService.metaClass.executeCommand = {Realm realm1, String command ->
            assert command.contains("ln -s ${paths.initialPath} ${paths.finalPath}")
            return '0\n'
        }

        job.metaClass.addOutputParameter = { String parameterName, String ids ->
            assert ids == WatchdogJob.SKIP_WATCHDOG
        }

        job.execute()
    }

    @Test
    void testExecute_DataFilesHaveToBeCopied() {
        Map paths = prepareTestExecute(false)

        mockServicesForFileCopyingAndRunService(paths)
    }

    @Test
    void testExecute_BamDataFileHaveToBeCopied() {
        Map paths = prepareTestExecute(false, true)

        mockServicesForFileCopyingAndRunService(paths)
    }



    private void mockServicesForFileCopyingAndRunService(Map paths) {
        job.executionHelperService.metaClass.sendScript = { Realm realm1, String text, String jobIdentifier ->
            assert text.contains("cp ${paths.initialPath} ${paths.finalPath};chmod 440 ${paths.finalPath}")
            return PBS_ID
        }

        job.metaClass.addOutputParameter = { String parameterName, String ids ->
            if (parameterName == JobParameterKeys.PBS_ID_LIST) {
                assert ids == PBS_ID
            } else if (parameterName == JobParameterKeys.REALM) {
                assert ids == CollectionUtils.exactlyOneElement(Realm.list()).id.toString()
            }
        }

        job.execute()
    }

    private Map prepareTestExecute(boolean linkedExternally, boolean createAlignBamFile = false) {
        Run run = Run.build()
        Realm realm = Realm.build(operationType: OperationType.DATA_MANAGEMENT)
        SeqTrack seqTrack = SeqTrack.build(linkedExternally: linkedExternally)
        seqTrack.project.realmName = realm.name
        DataFile dataFile
        if (createAlignBamFile) {
            dataFile = DataFile.build(
                run: run,
                seqTrack: null,
                alignmentLog: AlignmentLog.build(seqTrack: seqTrack),
                project: seqTrack.project,
                fileType : FileType.buildLazy(type: FileType.Type.ALIGNMENT)
                )
        } else {
            dataFile = DataFile.build(run: run, seqTrack: seqTrack, project: seqTrack.project)
        }
        dataFile.runSegment = DomainFactory.createRunSegment(run: run)
        assert dataFile.save(failOnError: true, flush: true)

        job.metaClass.getProcessParameterValue = { -> run.id.toString() }
        job.runProcessingService.metaClass.dataFilesForProcessing = { Run run2 -> assert run2 == run; [dataFile] }

        return [
                initialPath: job.lsdfFilesService.getFileInitialPath(dataFile),
                finalPath: job.lsdfFilesService.getFileFinalPath(dataFile),
        ]
    }


    private ProcessedMergedBamFile createMergedBamFileForRun(Run run, Sample sample) {
        SeqTrack seqTrack = testData.createSeqTrack([run: run, sample: sample])
        assert seqTrack.save()

        AlignmentPass alignmentPass = testData.createAlignmentPass([seqTrack: seqTrack])
        assert alignmentPass.save()

        ProcessedBamFile processedBamFile = testData.createProcessedBamFile([alignmentPass: alignmentPass])
        assert processedBamFile.save()

        MergingSet mergingSet = testData.createMergingSet([
            identifier: MergingSet.nextIdentifier(alignmentPass.workPackage),
            mergingWorkPackage: alignmentPass.workPackage,
            status: MergingSet.State.NEEDS_PROCESSING
        ])
        assertNotNull(mergingSet.save([flush: true]))

        MergingPass mergingPass = testData.createMergingPass([mergingSet: mergingSet])
        assertNotNull(mergingPass.save([flush: true]))

        ProcessedMergedBamFile processedMergedBamFile = TestData.createProcessedMergedBamFile([
            mergingPass: mergingPass,
            fileOperationStatus: FileOperationStatus.PROCESSED,
            ])
        assert processedMergedBamFile.save()
    }
}
