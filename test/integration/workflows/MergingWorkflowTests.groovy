package workflows

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.QaProcessingStatus
import de.dkfz.tbi.otp.job.jobs.merging.MergingStartJob
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.joda.time.Duration
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull

class MergingWorkflowTests extends WorkflowTestCase {

    ProcessingOptionService processingOptionService

    ProcessedBamFileService processedBamFileService

    ProcessedMergedBamFileService processedMergedBamFileService

    ChecksumFileService checksumFileService

    LsdfFilesService lsdfFilesService

    MergingStartJob mergingStartJob


    // TODO want to get rid of this hardcoded.. idea: maybe calculating from the walltime of the cluster jobs.. -> OTP-570/OTP-672
    final Duration TIMEOUT = Duration.standardMinutes(40)

    String inputSingleLaneAlingment

    String pidDir
    String alignmentDir
    String singleLaneBamFile

    ProcessedBamFile processedBamFile

    @Before
    void setUp() {

        createUserAndRoles()

        // this file is already available in the file system in the MergingWorkflowTest folder
        inputSingleLaneAlingment = "${getWorkflowDirectory().absolutePath}/inputFile/blood_runName_s_laneId_PAIRED.sorted.bam"

        pidDir = "${processingRootPath}otp_test_project/results_per_pid/654321"
        alignmentDir = "${pidDir}/alignment//testname1_123/pass0"
        singleLaneBamFile = "${alignmentDir}/tumor_testname1_s_123_PAIRED.sorted.bam"

        TestData testData = new TestData()

        Project project = Project.build(
            name : "otp_test_project",
            dirName : "otp_test_project",
            realmName : realm.name,
        )

        Individual individual = Individual.build(
                pid: "654321",
                project: project,
        )

        SampleType sampleType = SampleType.build(
                name: "TUMOR",
        )

        Sample sample = Sample.build(
                individual: individual,
                sampleType:sampleType,
        )

        SeqType seqType = SeqType.build(
                libraryLayout: "PAIRED",
        )

        Run run = Run.build(
                name: "testname1",
        )

        SeqTrack seqTrack = SeqTrack.build(
                sample: sample,
                seqType: seqType,
                laneId: "123",
                run: run,
        )

        DataFile dataFile1 = testData.createDataFile([fileName: "dataFile1"])
        assertNotNull(dataFile1.save([flush: true, failOnError: true]))

        DataFile dataFile2 = testData.createDataFile([fileName: "dataFile2"])
        assertNotNull(dataFile2.save([flush: true, failOnError: true]))

        AlignmentPass alignmentPass = testData.createAlignmentPass(seqTrack: seqTrack)
        assertNotNull(alignmentPass.save([flush: true, failOnError: true]))

        processedBamFile = testData.createProcessedBamFile([
                        alignmentPass: alignmentPass,
                        type: BamType.SORTED,
                        status: AbstractBamFile.State.PROCESSED,
                        qualityAssessmentStatus: QaProcessingStatus.FINISHED
                        ])
        assertNotNull(processedBamFile.save([flush: true, failOnError: true]))

        MergingSet mergingSet = new MergingSet(
                identifier: 0,
                mergingWorkPackage: processedBamFile.mergingWorkPackage,
                status: MergingSet.State.DECLARED
        )
        assertNotNull(mergingSet.save([flush: true, failOnError: true]))

        MergingSetAssignment mergingSetAssignment = new MergingSetAssignment(
                mergingSet: mergingSet,
                bamFile: processedBamFile
        )
        assertNotNull(mergingSetAssignment.save([flush: true, failOnError: true]))
        /*
         * Setup directories and files for corresponding database objects
         */
        String cmdBuildDirStructure = "mkdir -p ${alignmentDir}"
        String cmdCopyFile = "rsync ${inputSingleLaneAlingment} ${singleLaneBamFile}"
        // Call "sync" to block termination of script until I/O is done
        executionService.executeCommand(realm, "${cmdBuildDirStructure} && ${cmdCopyFile} && sync")
        checkFiles([singleLaneBamFile])
    }


    /**
     * Helper to see logs at console ( besides seeing at the reports in the end)
     * @msg Message to be shown
     */
    void println(String msg) {
        log.debug(msg)
        System.out.println(msg)
    }


    /**
     * Test execution of the workflow without any processing options defined
     */
    @Ignore
    @Test
    void testExecutionWithoutProcessingOptions() {
        assertEquals(singleLaneBamFile, processedBamFileService.getFilePath(processedBamFile))
        // Import workflow from script file
        SpringSecurityUtils.doWithAuth("admin") {
            runScript("scripts/workflows/MergingWorkflow.groovy")
        }

        SpringSecurityUtils.doWithAuth("admin") {
            processingOptionService.createOrUpdate(
                    "PBS_mergingJob",
                    "DKFZ",
                    null,
                    '{"-l": {nodes: "1:ppn=1:lsdf", walltime: "00:15:00", mem: "100m"}}',
                    "merging job depending cluster option for dkfz"
            )

            processingOptionService.createOrUpdate(
                    "picardJavaSetting",
                    null,
                    null,
                    'export JAVA_OPTIONS=-Xmx8G',
                    "Java options for Picard"
            )
        }


        JobExecutionPlan jobExecutionPlan = CollectionUtils.exactlyOneElement(JobExecutionPlan.list())

        // TODO hack to be able to start the workflow -> OTP-570/OTP-672
        mergingStartJob.setJobExecutionPlan(jobExecutionPlan)
        MergingSet mergingSet = CollectionUtils.exactlyOneElement(MergingSet.createCriteria().list {
            eq("status", MergingSet.State.DECLARED)
            order("id", "asc")
        })

        mergingSet.status = MergingSet.State.NEEDS_PROCESSING
        assert mergingSet.save(flush: true, failOnError: true)

        waitUntilWorkflowFinishesWithoutFailure(TIMEOUT)

        mergingSet.refresh()

        ProcessedMergedBamFile processedMergedBamFile = CollectionUtils.exactlyOneElement(ProcessedMergedBamFile.list())
        assert processedMergedBamFile.mergingPass.mergingSet == mergingSet
        assertEquals(1, processedMergedBamFile.numberOfMergedLanes)
        assertEquals(MergingSet.State.PROCESSED, mergingSet.status)
        assertEquals(AbstractBamFile.QaProcessingStatus.NOT_STARTED, processedMergedBamFile.qualityAssessmentStatus)

        String mergedBamFile = processedMergedBamFileService.filePath(processedMergedBamFile)
        String baiFile = processedMergedBamFileService.filePathForBai(processedMergedBamFile)
        String metricsFile = processedMergedBamFileService.filePathForMetrics(processedMergedBamFile)
        String picardMd5 = checksumFileService.picardMd5FileName(mergedBamFile)

        checkFiles([mergedBamFile, baiFile, metricsFile, picardMd5])
    }


    void checkFiles(List paths) {
        paths.each {
            File file = new File(it)
            assert file.canRead()
            assert file.size() > 0
        }
    }

    @Override
    Runnable getStartJobRunnable() {
        new Runnable() {
            public void run() { mergingStartJob.execute() }
        }
    }
}
