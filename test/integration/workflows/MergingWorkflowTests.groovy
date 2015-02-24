package workflows

import org.joda.time.Duration

import de.dkfz.tbi.otp.utils.CollectionUtils

import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*
import static org.junit.Assert.*
import grails.test.mixin.*
import grails.test.mixin.domain.*
import grails.test.mixin.support.*
import grails.util.Environment
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.QaProcessingStatus
import de.dkfz.tbi.otp.filehandling.FileNames
import de.dkfz.tbi.otp.job.jobs.merging.MergingStartJob
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.FileType.Type
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry.Classification
import de.dkfz.tbi.otp.testing.GroovyScriptAwareIntegrationTest
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils

/**
 * Preparation for execution: see src/docs/guide/devel/testing/workflowTesting.gdoc
 */
class MergingWorkflowTests extends AbstractWorkflowTest {

    ProcessingOptionService processingOptionService

    ProcessedBamFileService processedBamFileService

    ProcessedMergedBamFileService processedMergedBamFileService

    ChecksumFileService checksumFileService

    LsdfFilesService lsdfFilesService

    ExecutionService executionService

    MergingStartJob mergingStartJob

    // The scheduler needs to access the created objects while the test is being executed
    boolean transactional = false

    // TODO want to get rid of this hardcoded.. idea: maybe calculating from the walltime of the cluster jobs.. -> OTP-570/OTP-672
    final Duration TIMEOUT = Duration.standardMinutes(40)

    // TODO This paths should be obtained from somewhere else..  maybe from ~/.otp.properties, but I am hardcoding for now.. -> OTP-570/OTP-672
    String basePath = 'WORKFLOW_ROOT/MergingWorkflow'
    String rootPath = "${basePath}/root_path"
    String processingRootPath = "${basePath}/processing_root_path"
    String loggingRootPath = "${basePath}/logging_root_path"

    // this file is alrady available in the file system in the MergingWorkflowTest folder
    String inputSingleLaneAlingment = "${basePath}/inputFile/blood_runName_s_laneId_PAIRED.sorted.bam"

    String pidDir = "${processingRootPath}/otp_test_project/results_per_pid/654321"
    String alignmentDir = "${pidDir}/alignment//testname1_123/pass0"
    String singleLaneBamFile = "${alignmentDir}/tumor_testname1_s_123_PAIRED.sorted.bam"

    ProcessedBamFile processedBamFile

    /**
     * Realm necessary to cleanup folder structure
     */
    Realm realm

    void setUp() {

        def paths = [
            rootPath: "${rootPath}",
            processingRootPath: "${processingRootPath}",
            programsRootPath: '/',
            loggingRootPath: loggingRootPath,
        ]

        createUserAndRoles()

        TestData testData = new TestData()
        testData.createObjects()

        // Realms for testing on DKFZ
        testData.realm.delete(flush: true)
        realm = DomainFactory.createRealmDataManagementDKFZ(paths)
        assertNotNull(realm.save(flush: true))

        realm = DomainFactory.createRealmDataProcessingDKFZ(paths)
        assertNotNull(realm.save(flush: true))

        DataFile dataFile1 = testData.createDataFile([fileName: "dataFile1"])
        assertNotNull(dataFile1.save([flush: true, failOnError: true]))

        DataFile dataFile2 = testData.createDataFile([fileName: "dataFile2"])
        assertNotNull(dataFile2.save([flush: true, failOnError: true]))

        AlignmentPass alignmentPass = testData.createAlignmentPass()
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
        String cmdCleanUp = cleanUpTestFoldersCommand()
        String cmdBuildDirStructure = "mkdir -p ${alignmentDir}; mkdir -p ${loggingRootPath}/log/status/"
        String cmdCopyFile = "rsync ${inputSingleLaneAlingment} ${singleLaneBamFile}"
        // Call "sync" to block termination of script until I/O is done
        executionService.executeCommand(realm, "${cmdCleanUp}; ${cmdBuildDirStructure} && ${cmdCopyFile} && sync")
        checkFiles([singleLaneBamFile])
    }


    /**
     * Returns a comand to clean up the rootPath and processingRootPath
     * @return Command to clean up used folders
     */
    String cleanUpTestFoldersCommand() {
        return "rm -rf ${rootPath}/* ${processingRootPath}/* ${loggingRootPath}/*"
        /* When testing on BioQuant, there is no write access. You have to replace the
         * above line by something like 'return "true"' */
    }

    /**
     * Helper to see logs at console ( besides seeing at the reports in the end)
     * @msg Message to be shown
     */
    void println(String msg) {
        log.debug(msg)
        System.out.println(msg)
    }

    void tearDown() {
        //executionService.executeCommand(realm, cleanUpTestFoldersCommand())
        realm = null
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
            run("scripts/workflows/MergingWorkflow.groovy")
        }

        SpringSecurityUtils.doWithAuth("admin") {
            processingOptionService.createOrUpdate(
                    "PBS_mergingJob",
                    "DKFZ",
                    null,
                    '{"-l": {nodes: "1:ppn=6:lsdf", walltime: "00:15:00", mem: "50g"}}',
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
}
