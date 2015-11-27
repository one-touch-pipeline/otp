package workflows

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.QaProcessingStatus
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin
import org.joda.time.Duration
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

import static de.dkfz.tbi.otp.dataprocessing.Workflow.Name.DEFAULT_OTP
import static de.dkfz.tbi.otp.ngsdata.Individual.Type.REAL
import static de.dkfz.tbi.otp.ngsdata.SoftwareTool.Type.ALIGNMENT
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull

@TestMixin(IntegrationTestMixin)
class MergingWorkflowTests extends WorkflowTestCase {

    ProcessingOptionService processingOptionService

    ProcessedBamFileService processedBamFileService

    ProcessedMergedBamFileService processedMergedBamFileService

    ChecksumFileService checksumFileService

    LsdfFilesService lsdfFilesService


    String inputSingleLaneAlingment

    String pidDir
    String alignmentDir
    String singleLaneBamFile

    ProcessedBamFile processedBamFile

    @Before
    void setUp() {
        // this file is already available in the file system in the MergingWorkflowTest folder
        inputSingleLaneAlingment = "${getWorkflowDirectory().absolutePath}/inputFile/blood_runName_s_laneId_PAIRED.sorted.bam"

        pidDir = "${processingRootPath}/otp_test_project/results_per_pid/654321"
        alignmentDir = "${pidDir}/alignment//testname1_123/pass0"
        singleLaneBamFile = "${alignmentDir}/tumor_testname1_s_123_PAIRED.sorted.bam"

        TestData testData = new TestData()

        Project project = new Project(
                name: "otp_test_project",
                dirName: "otp_test_project",
                realmName: realm.name,
                alignmentDeciderBeanName: DefaultOtpAlignmentDecider.getClass().name
        )
        assert project.save(flush: true)

        Individual individual = new Individual(
                pid: "654321",
                project: project,
                mockPid: "654321",
                mockFullName: "654321",
                type: REAL,
        )
        assert individual.save(flush: true)

        SampleType sampleType = new SampleType(
                name: "TUMOR",
        )
        assert sampleType.save(flush: true)

        Sample sample = new Sample(
                individual: individual,
                sampleType:sampleType,
        )
        assert sample.save(flush: true)

        SeqType seqType = new SeqType(
                libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED,
                name: SeqTypeNames.WHOLE_GENOME.seqTypeName,
                dirName: 'whole_genome',
        )
        assert seqType.save(flush: true)

        SeqCenter seqCenter = new SeqCenter(
                name: 'seqCenter',
                dirName: 'seqCenterDirName',
        )
        assert seqCenter.save(flush: true)

        SeqPlatformGroup seqPlatformGroup = new SeqPlatformGroup(name: 'seqPlatformGroup')
        assert seqPlatformGroup.save(flush: true)

        SeqPlatform seqPlatform = new SeqPlatform(
                name: 'seqPlatformName',
                seqPlatformGroup: seqPlatformGroup,
        )
        assert seqPlatform.save(flush: true)

        Run run = new Run(
                name: "testname1",
                seqCenter: seqCenter,
                seqPlatform: seqPlatform,
        )
        assert run.save(flush: true)

        SoftwareTool softwareTool = new SoftwareTool(
                programName: 'softwaretool',
                type: ALIGNMENT,
        )
        assert softwareTool.save(flush: true)

        SeqTrack seqTrack = new SeqTrack(
                sample: sample,
                seqType: seqType,
                laneId: "123",
                run: run,
                seqPlatform: seqPlatform,
                pipelineVersion: softwareTool
        )
        assert seqTrack.save(flush: true)

        DataFile dataFile1 = testData.createDataFile([fileName: "dataFile1"])
        assertNotNull(dataFile1.save([flush: true, failOnError: true]))

        DataFile dataFile2 = testData.createDataFile([fileName: "dataFile2"])
        assertNotNull(dataFile2.save([flush: true, failOnError: true]))

        ReferenceGenome referenceGenome = new ReferenceGenome(
                name: 'refGenome',
                path: 'reference_genome',
                fileNamePrefix: 'chr',
                length: 100,
                lengthWithoutN: 100,
                lengthRefChromosomes: 100,
                lengthRefChromosomesWithoutN: 100,
        )
        assert referenceGenome.save(flush: true)

        Workflow workflow = new Workflow(
                name: DEFAULT_OTP,
                type: Workflow.Type.ALIGNMENT,
        )
        assert workflow.save(flush: true)

        MergingWorkPackage mergingWorkPackage = new MergingWorkPackage(
                sample: sample,
                seqType: seqType,
                seqPlatformGroup: seqPlatformGroup,
                referenceGenome: referenceGenome,
                workflow: workflow,
                needsProcessing: false,
        )
        assert mergingWorkPackage.save(flush: true)

        AlignmentPass alignmentPass = testData.createAlignmentPass(seqTrack: seqTrack)
        assertNotNull(alignmentPass.save([flush: true, failOnError: true]))

        processedBamFile = testData.createProcessedBamFile([
                        alignmentPass: alignmentPass,
                        type: BamType.SORTED,
                        fileSize: new File(inputSingleLaneAlingment).length(),
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

        SpringSecurityUtils.doWithAuth("admin") {
            processingOptionService.createOrUpdate(
                    "picardJavaSetting",
                    null,
                    null,
                    'export JAVA_OPTIONS=-Xmx8G',
                    "Java options for Picard"
            )
        }

        MergingSet mergingSet = CollectionUtils.exactlyOneElement(MergingSet.createCriteria().list {
            eq("status", MergingSet.State.DECLARED)
            order("id", "asc")
        })

        mergingSet.status = MergingSet.State.NEEDS_PROCESSING
        assert mergingSet.save(flush: true, failOnError: true)

        execute()

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
    List<String> getWorkflowScripts() {
        return ["scripts/workflows/MergingWorkflow.groovy"]
    }

    @Override
    Duration getTimeout() {
        Duration.standardMinutes(40)
    }
}
