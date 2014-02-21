package workflows

import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*
import static org.junit.Assert.*
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.jobs.metaData.MetaDataStartJob
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.FileType.Type
import de.dkfz.tbi.otp.testing.GroovyScriptAwareIntegrationTest

/**
 * Currently only a test for exome data exist
 */

/**
 * To run this workflow test the preparation steps described in the documentation (grails doc) have to be followed.
 * Additional preparations
 * The two files:
 * - fastqR1Filepath
 * - fastqR2Filepath
 * have to be created before the test can be started
 */
class LoadMetaDataTests extends GroovyScriptAwareIntegrationTest {

    // The scheduler needs to access the created objects while the test is being executed
    boolean transactional = false
    // TODO ( jira: OTP-566)  want to get rid of this hardcoded.. idea: maybe calculating from the walltime of the cluster jobs..
    int SLEEPING_TIME_IN_MINUTES = 10

    // the String "UNKNOWN" is used instead of the enum, because that is how it appears in external input files
    final String UNKNOWN_VERIFIED_VALUE_FROM_METADATA_FILE = "UNKNOWN"

    LsdfFilesService lsdfFilesService
    ExecutionService executionService

    MetaDataStartJob metaDataStartJob

    // TODO This paths should be obtained from somewhere else..  maybe from .otpproperties, but I am hardcoding for now..
    String baseDir = "WORKFLOW_ROOT/"
    // TODO change this to be dependent of the user
    String myBaseDir = "${baseDir}/MetaDataWorkflow"
    String rootPath = "${myBaseDir}/root_path/"
    String processingRootPath = "${myBaseDir}/processing_root_path/"
    String testDataDir = "${baseDir}/files/"
    String ftpDir = "${rootPath}/ftp/"

    // files to be processed by the tests
    String fastqR1Filepath = "${testDataDir}/35-3B_NoIndex_L007_R1_complete_filtered.fastq.gz"
    String fastqR2Filepath = "${testDataDir}/35-3B_NoIndex_L007_R2_complete_filtered.fastq.gz"

    String barcode = "GATCGA"
    String fastqR1Filename1 = "example_${barcode}_fileR1_1.fastq.gz"
    String fastqR2Filename1 = "example_${barcode}_fileR2_1.fastq.gz"
    String fastqR1Filename2 = "example_${barcode}_fileR1_2.fastq.gz"
    String fastqR2Filename2 = "example_${barcode}_fileR2_2.fastq.gz"
    String fastqR1Filename3 = "example_${barcode}_fileR1_3.fastq.gz"
    String fastqR2Filename3 = "example_${barcode}_fileR2_3.fastq.gz"
    String runName = "130312_D00133_0018_ADTWTJACXX"
    String runDate = "2013-03-12"
    String metaDataFilepath = "${ftpDir}/${runName}/${runName}.fastq.tsv"

    Realm realm
    String realmProgramsRootPath = "/"

    String seqCenterName = "TheSequencingCenter"
    String sampleID = "SampleIdentifier"
    String projectName = "TheProject"
    String softwareToolName = "CASAVA"
    String softwareToolVersion = "1.8.2"
    String pipeLineVersion = "${softwareToolName}-${softwareToolVersion}"
    String libraryLayout = "PAIRED"
    String instrumentPlatform = "Illumina"
    String instrumentModel = "HiSeq2000"

    String laneNoKit = "1"
    String laneNoKitId = "2"
    String laneNoUnknown = "3"

    String baseCount = "8781211000"
    String readCount = "87812110"
    String cycleCount = "101"
    String insertSize = "162"

    private String md5sum(String filepath) {
        String cmdMd5sum = "md5sum ${filepath}"
        String output = executionService.executeCommand(realm, cmdMd5sum)
        String md5sum = output.split().first()
        return md5sum
    }

    private Map<MetaDataColumn, String> metaDataDefault() {
        Map<MetaDataColumn, String> emptyMetaData = MetaDataColumn.values().collectEntries {
            [(it): ""]
        }
        Map<MetaDataColumn, String> metaDataDefault = new HashMap(emptyMetaData)
        metaDataDefault.putAll([
            (MetaDataColumn.CENTER_NAME): seqCenterName,
            (MetaDataColumn.RUN_ID): runName,
            (MetaDataColumn.RUN_DATE): runDate,
            (MetaDataColumn.BASE_COUNT): baseCount,
            (MetaDataColumn.READ_COUNT): readCount,
            (MetaDataColumn.CYCLE_COUNT): cycleCount,
            (MetaDataColumn.SAMPLE_ID): sampleID,
            (MetaDataColumn.INSTRUMENT_PLATFORM): instrumentPlatform,
            (MetaDataColumn.INSTRUMENT_MODEL): instrumentModel,
            (MetaDataColumn.PIPELINE_VERSION): pipeLineVersion,
            (MetaDataColumn.INSERT_SIZE): insertSize,
            (MetaDataColumn.LIBRARY_LAYOUT): libraryLayout,
            (MetaDataColumn.WITHDRAWN): "0"])
        return metaDataDefault
    }

    private Map<MetaDataColumn, String> metaData(Map<MetaDataColumn, String> metaDataToOverride) {
        assertTrue((MetaDataColumn.values() as List).containsAll(metaDataToOverride.keySet()))
        Map<MetaDataColumn, String> metaData = metaDataDefault()
        metaData.putAll(metaDataToOverride)
        return metaData
    }

    private List<MetaDataColumn> metaDataColumns() {
        return MetaDataColumn.values() as List<MetaDataColumn>
    }

    private String metaDataTableHeader(List<MetaDataColumn> metaDataColumns = metaDataColumns()) {
        return metaDataColumns.join("\t") + "\n"
    }

    private String metaDataTableEntry(Map<MetaDataColumn, String> metaData, List<MetaDataColumn> metaDataColumns = metaDataColumns()) {
        List<String> values = []
        metaDataColumns.each { MetaDataColumn column ->
            values << metaData[column]
        }
        return values.join("\t") + "\n"
    }

    @Before
    void setUp() {
        // Setup logic here
        Map paths = [
            rootPath: rootPath,
            processingRootPath: processingRootPath,
            programsRootPath: realmProgramsRootPath,
        ]

        realm = DomainFactory.createRealmDataManagementDKFZ(paths).save(flush: true)
        realm = DomainFactory.createRealmDataProcessingDKFZ(paths).save(flush: true)

        FileType fileType = new FileType()
        fileType.type = FileType.Type.SEQUENCE
        fileType.subType = "fastq"
        fileType.vbpPath = "/sequence/"
        fileType.signature = ".fastq"
        assertNotNull(fileType.save(flush: true))

        SampleType sampleType = new SampleType()
        sampleType.name = "some sample type"
        assertNotNull(sampleType.save(flush: true))

        Project project = new Project()
        project.name = projectName
        project.dirName = projectName
        project.realmName = realm.name
        assertNotNull(project.save(flush: true))

        Individual individual = new Individual()
        individual.pid = "1234"
        individual.mockPid = "abcd"
        individual.mockFullName = "mockFullName"
        individual.type = Individual.Type.REAL
        individual.project = project
        assertNotNull(individual.save(flush: true))

        Sample sample = new Sample()
        sample.individual = individual
        sample.sampleType = sampleType
        assertNotNull(sample.save(flush: true))

        SampleIdentifier sampleIdentifier = new SampleIdentifier()
        sampleIdentifier.name = sampleID
        sampleIdentifier.sample = sample
        assertNotNull(sampleIdentifier.save(flush: true))

        SoftwareTool softwareTool = new SoftwareTool()
        softwareTool.programName = softwareToolName
        softwareTool.programVersion = softwareToolVersion
        softwareTool.type = SoftwareTool.Type.BASECALLING
        assertNotNull(softwareTool.save(flush: true))

        SoftwareToolIdentifier softwareToolIdentifier = new SoftwareToolIdentifier()
        softwareToolIdentifier.name = pipeLineVersion
        softwareToolIdentifier.softwareTool = softwareTool
        assertNotNull(softwareToolIdentifier.save(flush: true))

        SeqPlatform seqPlatform = new SeqPlatform()
        seqPlatform.name = instrumentPlatform
        seqPlatform.model = instrumentModel
        assertNotNull(seqPlatform.save(flush: true))

        SeqPlatformModelIdentifier seqPlatformModelIdentifier = new SeqPlatformModelIdentifier()
        seqPlatformModelIdentifier.name = instrumentModel
        seqPlatformModelIdentifier.seqPlatform = seqPlatform
        assertNotNull(seqPlatformModelIdentifier.save(flush: true))

        SeqCenter seqCenter = new SeqCenter(
                        name: seqCenterName,
                        dirName: seqCenterName
                        )
        assertNotNull(seqCenter.save([flush: true, failOnError: true]))

        Run run = new Run()
        run.name = runName
        run.seqCenter = seqCenter
        run.seqPlatform = seqPlatform
        run.storageRealm = Run.StorageRealm.DKFZ
        assertNotNull(run.save([flush: true, failOnError: true]))

        RunSegment runSegment = new RunSegment()
        runSegment.initialFormat = RunSegment.DataFormat.FILES_IN_DIRECTORY
        runSegment.currentFormat = RunSegment.DataFormat.FILES_IN_DIRECTORY
        runSegment.dataPath = ftpDir
        runSegment.filesStatus = RunSegment.FilesStatus.NEEDS_INSTALLATION
        runSegment.mdPath = ftpDir
        runSegment.run = run
        assertNotNull(runSegment.save([flush: true, failOnError: true]))
    }

    @After
    void tearDown() {
        executionService.executeCommand(realm, cleanUpTestFoldersCommand())
    }

    // TODO  (jira: OTP-640) this ignore is here because of workflows tests are not transactional and so we cannot run multiple tests with clean database yet (We need to discovered best way to do it)
    // so at this moment only one test could be run at moment, all the others have to be commented
    @Ignore
    void testWholeGenomeMetadata() {
        String seqTypeName = SeqTypeNames.WHOLE_GENOME.seqTypeName
        createAndSaveSeqType(seqTypeName)

        String path = "${ftpDir}/${runName}"
        String softLinkFastqR1Filepath1 = "${path}/${fastqR1Filename1}"
        String softLinkFastqR2Filepath1 = "${path}/${fastqR2Filename1}"

        // Just to be sure the rootPath and the processingRootPath are clean for new test
        String cmdCleanUp = cleanUpTestFoldersCommand()
        String cmdBuildFileStructure = "mkdir -p ${path}"
        String cmdBuildSoftLinkToFileToBeProcessed = "ln -s ${fastqR1Filepath} ${softLinkFastqR1Filepath1}; ln -s ${fastqR2Filepath} ${softLinkFastqR2Filepath1}"

        StringBuffer sb = new StringBuffer()
        sb << metaDataTableHeader()
        sb << metaDataTableEntry(metaData([(MetaDataColumn.FASTQ_FILE): fastqR1Filename1, (MetaDataColumn.MD5): md5sum(fastqR1Filepath), (MetaDataColumn.LANE_NO): laneNoKit, (MetaDataColumn.SEQUENCING_TYPE): seqTypeName]))
        sb << metaDataTableEntry(metaData([(MetaDataColumn.FASTQ_FILE): fastqR2Filename1, (MetaDataColumn.MD5): md5sum(fastqR2Filepath), (MetaDataColumn.LANE_NO): laneNoKit, (MetaDataColumn.SEQUENCING_TYPE): seqTypeName]))
        String metaDataFile = sb.toString()

        String cmdCreateMetadataFile = "echo '${metaDataFile}' > ${metaDataFilepath}"
        executionService.executeCommand(realm, "${cmdCleanUp}; ${cmdBuildFileStructure}; ${cmdBuildSoftLinkToFileToBeProcessed}; ${cmdCreateMetadataFile}")
        run("scripts/MetaDataWorkflow.groovy")

        // there will be only one at the database
        JobExecutionPlan jobExecutionPlan = JobExecutionPlan.list()?.first()

        // TODO hack to be able to star the workflow
        metaDataStartJob.setJobExecutionPlan(jobExecutionPlan)

        boolean workflowFinishedSucessfully = waitUntilWorkflowIsOverOrTimeout(SLEEPING_TIME_IN_MINUTES)
        assertTrue(workflowFinishedSucessfully)
    }

    // TODO (jira: OTP-640) this ignore is here because of workflows tests are not transactional and so we cannot run multiple tests with clean database yet (We need to discovered best way to do it)
    // so at this moment only one test could be run at moment, all the others have to be commented
    @Ignore
    void testExomeMetadataNoEnrichmentKit() {
        String seqTypeName = SeqTypeNames.EXOME.seqTypeName
        createAndSaveSeqType(seqTypeName)

        run("scripts/ExomeEnrichmentKit/LoadExomeEnrichmentKits.groovy")
        List<ExomeEnrichmentKit> exomeEnrichmentKit = ExomeEnrichmentKit.list()
        assertFalse(exomeEnrichmentKit.isEmpty())

        String path = "${ftpDir}/${runName}"
        String softLinkFastqR1Filepath1 = "${path}/${fastqR1Filename1}"
        String softLinkFastqR2Filepath1 = "${path}/${fastqR2Filename1}"
        // Just to be sure the rootPath and the processingRootPath are clean for new test
        String cmdCleanUp = cleanUpTestFoldersCommand()
        String cmdBuildFileStructure = "mkdir -p ${path}"
        String cmdBuildSoftLinkToFileToBeProcessed = "ln -s ${fastqR1Filepath} ${softLinkFastqR1Filepath1}; ln -s ${fastqR2Filepath} ${softLinkFastqR2Filepath1}"

        List<MetaDataColumn> metaDataColumns = metaDataColumns()
        metaDataColumns.remove(MetaDataColumn.LIB_PREP_KIT)
        StringBuffer sb = new StringBuffer()
        sb << metaDataTableHeader(metaDataColumns)
        sb << metaDataTableEntry(metaData([(MetaDataColumn.FASTQ_FILE): fastqR1Filename1, (MetaDataColumn.MD5): md5sum(fastqR1Filepath), (MetaDataColumn.LANE_NO): laneNoKit, (MetaDataColumn.SEQUENCING_TYPE): seqTypeName]), metaDataColumns)
        sb << metaDataTableEntry(metaData([(MetaDataColumn.FASTQ_FILE): fastqR2Filename1, (MetaDataColumn.MD5): md5sum(fastqR2Filepath), (MetaDataColumn.LANE_NO): laneNoKit, (MetaDataColumn.SEQUENCING_TYPE): seqTypeName]), metaDataColumns)
        String metaDataFile = sb.toString()

        String cmdCreateMetadataFile = "echo '${metaDataFile}' > ${metaDataFilepath}"
        executionService.executeCommand(realm, "${cmdCleanUp}; ${cmdBuildFileStructure}; ${cmdBuildSoftLinkToFileToBeProcessed}; ${cmdCreateMetadataFile}")
        assertTrue(new File(metaDataFilepath).exists())
        run("scripts/MetaDataWorkflow.groovy")

        // there will be only one at the database
        JobExecutionPlan jobExecutionPlan = JobExecutionPlan.list()?.first()

        // TODO hack to be able to star the workflow
        metaDataStartJob.setJobExecutionPlan(jobExecutionPlan)

        boolean workflowFinishedSucessfully = waitUntilWorkflowIsOverOrTimeout(SLEEPING_TIME_IN_MINUTES)
        assertTrue(workflowFinishedSucessfully)
    }

    // TODO (jira: OTP-640) this ignore is here because of workflows tests are not transactional and so we cannot run multiple tests with clean database yet (We need to discovered best way to do it)
    // so at this moment only one test could be run at moment, all the others have to be commented
    @Ignore
    void testExomeMetadataWithEnrichmentKit() {
        String seqTypeName = SeqTypeNames.EXOME.seqTypeName
        createAndSaveSeqType(seqTypeName)

        run("scripts/ExomeEnrichmentKit/LoadExomeEnrichmentKits.groovy")
        List<ExomeEnrichmentKit> exomeEnrichmentKit = ExomeEnrichmentKit.list()
        assertFalse(exomeEnrichmentKit.isEmpty())

        // this library PreparationKit was chosen to match the one at the script that is supposed to be executed together with the workflow
        String libraryPreparationKit = "Agilent SureSelect V3"
        String libraryPreparationKitIdentifier = "Agilent SureSelect V3 alias"

        ExomeEnrichmentKitSynonym exomeEnrichmentKitSynonym = new ExomeEnrichmentKitSynonym(
                        name: libraryPreparationKitIdentifier,
                        exomeEnrichmentKit: exomeEnrichmentKit.first())
        assertNotNull(exomeEnrichmentKitSynonym.save([flush: true]))

        String path = "${ftpDir}/${runName}"
        String softLinkFastqR1Filepath1 = "${path}/${fastqR1Filename1}"
        String softLinkFastqR2Filepath1 = "${path}/${fastqR2Filename1}"
        String softLinkFastqR1Filepath2 = "${path}/${fastqR1Filename2}"
        String softLinkFastqR2Filepath2 = "${path}/${fastqR2Filename2}"
        String softLinkFastqR1Filepath3 = "${path}/${fastqR1Filename3}"
        String softLinkFastqR2Filepath3 = "${path}/${fastqR2Filename3}"

        // Just to be sure the rootPath and the processingRootPath are clean for new test
        String cmdCleanUp = cleanUpTestFoldersCommand()
        String cmdBuildFileStructure = "mkdir -p ${path}"
        String cmdBuildSoftLinkToFileToBeProcessed = "ln -s ${fastqR1Filepath} ${softLinkFastqR1Filepath1}; ln -s ${fastqR2Filepath} ${softLinkFastqR2Filepath1}; ln -s ${fastqR1Filepath} ${softLinkFastqR1Filepath2}; ln -s ${fastqR2Filepath} ${softLinkFastqR2Filepath2}; ln -s ${fastqR1Filepath} ${softLinkFastqR1Filepath3}; ln -s ${fastqR2Filepath} ${softLinkFastqR2Filepath3} "

        StringBuffer sb = new StringBuffer()
        sb << metaDataTableHeader()
        sb << metaDataTableEntry(metaData([(MetaDataColumn.FASTQ_FILE): fastqR1Filename1, (MetaDataColumn.MD5): md5sum(fastqR1Filepath), (MetaDataColumn.LANE_NO): laneNoKit, (MetaDataColumn.SEQUENCING_TYPE): seqTypeName, (MetaDataColumn.LIB_PREP_KIT): libraryPreparationKit]))
        sb << metaDataTableEntry(metaData([(MetaDataColumn.FASTQ_FILE): fastqR2Filename1, (MetaDataColumn.MD5): md5sum(fastqR2Filepath), (MetaDataColumn.LANE_NO): laneNoKit, (MetaDataColumn.SEQUENCING_TYPE): seqTypeName, (MetaDataColumn.LIB_PREP_KIT): libraryPreparationKit]))
        sb << metaDataTableEntry(metaData([(MetaDataColumn.FASTQ_FILE): fastqR1Filename2, (MetaDataColumn.MD5): md5sum(fastqR1Filepath), (MetaDataColumn.LANE_NO): laneNoKitId, (MetaDataColumn.SEQUENCING_TYPE): seqTypeName, (MetaDataColumn.LIB_PREP_KIT): libraryPreparationKitIdentifier]))
        sb << metaDataTableEntry(metaData([(MetaDataColumn.FASTQ_FILE): fastqR2Filename2, (MetaDataColumn.MD5): md5sum(fastqR2Filepath), (MetaDataColumn.LANE_NO): laneNoKitId, (MetaDataColumn.SEQUENCING_TYPE): seqTypeName, (MetaDataColumn.LIB_PREP_KIT): libraryPreparationKitIdentifier]))
        sb << metaDataTableEntry(metaData([(MetaDataColumn.FASTQ_FILE): fastqR1Filename3, (MetaDataColumn.MD5): md5sum(fastqR1Filepath), (MetaDataColumn.LANE_NO): laneNoUnknown, (MetaDataColumn.SEQUENCING_TYPE): seqTypeName, (MetaDataColumn.LIB_PREP_KIT): UNKNOWN_VERIFIED_VALUE_FROM_METADATA_FILE]))
        sb << metaDataTableEntry(metaData([(MetaDataColumn.FASTQ_FILE): fastqR2Filename3, (MetaDataColumn.MD5): md5sum(fastqR2Filepath), (MetaDataColumn.LANE_NO): laneNoUnknown, (MetaDataColumn.SEQUENCING_TYPE): seqTypeName, (MetaDataColumn.LIB_PREP_KIT): UNKNOWN_VERIFIED_VALUE_FROM_METADATA_FILE]))
        String metaDataFile = sb.toString()

        String cmdCreateMetadataFile = "echo '${metaDataFile}' > ${metaDataFilepath}"
        executionService.executeCommand(realm, "${cmdCleanUp}; ${cmdBuildFileStructure}; ${cmdBuildSoftLinkToFileToBeProcessed}; ${cmdCreateMetadataFile}")
        assertTrue(new File(metaDataFilepath).exists())
        run("scripts/MetaDataWorkflow.groovy")

        // there will be only one at the database
        JobExecutionPlan jobExecutionPlan = JobExecutionPlan.list()?.first()

        // TODO hack to be able to star the workflow
        metaDataStartJob.setJobExecutionPlan(jobExecutionPlan)

        boolean workflowFinishedSucessfully = waitUntilWorkflowIsOverOrTimeout(SLEEPING_TIME_IN_MINUTES)
        assertTrue(workflowFinishedSucessfully)
    }

    // TODO (jira: OTP-640) this ignore is here because of workflows tests are not transactional and so we cannot run multiple tests with clean database yet (We need to discovered best way to do it)
    // so at this moment only one test could be run at moment, all the others have to be commented
    @Ignore
    void testChipSeqMetadata() {
        String seqTypeName = SeqTypeNames.CHIP_SEQ.seqTypeName
        createAndSaveSeqType(seqTypeName)

        String path = "${ftpDir}/${runName}"
        String softLinkFastqR1Filepath = "${path}/${fastqR1Filename1}"
        String softLinkFastqR2Filepath = "${path}/${fastqR2Filename1}"

        // Just to be sure the rootPath and the processingRootPath are clean for new test
        String cmdCleanUp = cleanUpTestFoldersCommand()
        String cmdBuildFileStructure = "mkdir -p ${path}"
        String cmdBuildSoftLinkToFileToBeProcessed = "ln -s ${fastqR1Filepath} ${softLinkFastqR1Filepath}; ln -s ${fastqR2Filepath} ${softLinkFastqR2Filepath}"

        String ANTIBODY_TARGET_1 = "just4Test1"

        StringBuffer sb = new StringBuffer()
        sb << metaDataTableHeader()
        sb << metaDataTableEntry(metaData([(MetaDataColumn.FASTQ_FILE): fastqR1Filename1, (MetaDataColumn.MD5): md5sum(fastqR1Filepath), (MetaDataColumn.LANE_NO): laneNoKit, (MetaDataColumn.SEQUENCING_TYPE): seqTypeName, (MetaDataColumn.ANTIBODY_TARGET): ANTIBODY_TARGET_1]))
        sb << metaDataTableEntry(metaData([(MetaDataColumn.FASTQ_FILE): fastqR2Filename1, (MetaDataColumn.MD5): md5sum(fastqR2Filepath), (MetaDataColumn.LANE_NO): laneNoKit, (MetaDataColumn.SEQUENCING_TYPE): seqTypeName, (MetaDataColumn.ANTIBODY_TARGET): ANTIBODY_TARGET_1]))
        String metaDataFile = sb.toString()

        String cmdCreateMetadataFile = "echo '${metaDataFile}' > ${metaDataFilepath}"
        executionService.executeCommand(realm, "${cmdCleanUp}; ${cmdBuildFileStructure}; ${cmdBuildSoftLinkToFileToBeProcessed}; ${cmdCreateMetadataFile}")

        AntibodyTarget.findOrSaveByName(ANTIBODY_TARGET_1)

        run("scripts/MetaDataWorkflow.groovy")

        // there will be only one at the database
        JobExecutionPlan jobExecutionPlan = JobExecutionPlan.list()?.first()

        // TODO hack to be able to star the workflow
        metaDataStartJob.setJobExecutionPlan(jobExecutionPlan)

        boolean workflowFinishedSucessfully = waitUntilWorkflowIsOverOrTimeout(SLEEPING_TIME_IN_MINUTES)
        assertTrue(workflowFinishedSucessfully)
    }

    private SeqType createAndSaveSeqType(String seqTypeName) {
        SeqType seqType = new SeqType()
        seqType.name = seqTypeName
        seqType.libraryLayout = libraryLayout
        seqType.dirName = seqTypeName
        assertNotNull(seqType.save(flush: true))
        return seqType
    }

    /**
     * Returns a comand to clean up the rootPath and processingRootPath
     * @return Command to clean up used folders
     */
    String cleanUpTestFoldersCommand() {
        return "rm -rf ${rootPath}/* ${processingRootPath}/*"
    }

    /**
     * Pauses the test until the workflow is finished or the timeout is reached
     * @return true if the process is finished, false otherwise
     */
    boolean waitUntilWorkflowIsOverOrTimeout(int timeout) {
        println "Started to wait (until workflow is over or timeout)"
        int timeCount = 0
        boolean finished = false
        while (!finished && (timeCount < timeout)) {
            println "waiting ... "
            timeCount++
            sleep(60000)
            finished = isProcessFinished()
        }
        return finished
    }

    // TODO maybe we can make this a sub class and put this method in parent..
    /**
     * Checks if the process created by the test is already finished and retrieves corresponding value
     * @return true if the process is finished, false otherwise
     */
    boolean isProcessFinished() {
        //TODO there should be not more than one .. can make assert to be sure
        List<Process> processes = Process.list()
        boolean finished = false
        if (processes.size() > 0) {
            Process process = processes.first()
            // Required otherwise will never detect the change..
            process.refresh()
            finished = process?.finished
        }
        return finished
    }

    /**
     * Helper to see logs at console ( besides seeing at the reports in the end)
     * @msg Message to be shown
     */
    void println(String msg) {
        log.debug(msg)
        System.out.println(msg)
    }
}
