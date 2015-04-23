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


class CopyFilesJobTests extends GroovyTestCase {

    CopyFilesJob job
    TestData testData

    final static String PBS_ID = "1234"

    @Before
    void setUp() {
        job = new CopyFilesJob()
        job.executionHelperService = new ExecutionHelperService()
        job.runProcessingService = new RunProcessingService()
        job.processedMergedBamFileService = new ProcessedMergedBamFileService()
        job.lsdfFilesService = new LsdfFilesService()
        job.configService = new ConfigService()
        job.executionService = new ExecutionService()
    }

    @After
    void tearDown() {
        job = null
        testData = null
    }


    @Test
    void testProcessedMergedBamFilesForRun() {

        shouldFail(IllegalArgumentException.class, { job.processedMergedBamFilesForRun(null)})

        testData = new TestData()
        testData.createObjects()
        Run run1 = testData.run

        assertEquals(0, job.processedMergedBamFilesForRun(run1).size())

        ProcessedMergedBamFile pmbf1 = createMergedBamFileForRun(run1, testData.sample)

        assertEquals(1, job.processedMergedBamFilesForRun(run1).size())

        final Run run2 = testData.createRun("run2")
        assertNotNull(run2.save(flush: true))
        assertEquals(0, job.processedMergedBamFilesForRun(run2).size())

        ProcessedMergedBamFile pmbf2 = createMergedBamFileForRun(run1, testData.sample)
        assertEquals(2, job.processedMergedBamFilesForRun(run1).size())
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
        ProcessedMergedBamFile processedMergedBamFile = ProcessedMergedBamFile.build()
        processedMergedBamFile.project.realmName = realm.name

        job.metaClass.getProcessParameterValue = { -> run.id.toString() }
        job.metaClass.processedMergedBamFilesForRun = { Run run2 -> assert run2 == run; return [processedMergedBamFile] }
        job.processedMergedBamFileService.metaClass.destinationDirectory = { ProcessedMergedBamFile pmbf -> TestCase.uniqueNonExistentPath.path }
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
                seqTrack: null,
                alignmentLog: AlignmentLog.build(seqTrack: seqTrack),
                project: seqTrack.project,
                fileType : FileType.buildLazy(type: FileType.Type.ALIGNMENT)
                )
        } else {
            dataFile = DataFile.build(seqTrack: seqTrack, project: seqTrack.project)
        }

        job.metaClass.getProcessParameterValue = { -> run.id.toString() }
        job.runProcessingService.metaClass.dataFilesForProcessing = { Run run2 -> assert run2 == run; [dataFile] }
        String initialPath = TestCase.uniqueNonExistentPath.path
        String finalPath = TestCase.uniqueNonExistentPath.path
        job.lsdfFilesService.metaClass.getFileInitialPath = { DataFile file -> assert file == dataFile; return initialPath }
        job.lsdfFilesService.metaClass.getFileFinalPath = { DataFile file -> assert file == dataFile; return finalPath }

        return [
                initialPath: initialPath,
                finalPath: finalPath,
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

        ProcessedMergedBamFile processedMergedBamFile = testData.createProcessedMergedBamFile([
            mergingPass: mergingPass,
            fileOperationStatus: FileOperationStatus.PROCESSED
            ])
        assert processedMergedBamFile.save()
    }
}
