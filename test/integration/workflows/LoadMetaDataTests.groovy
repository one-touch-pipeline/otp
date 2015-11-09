package workflows

import de.dkfz.tbi.otp.dataprocessing.AlignmentPass
import de.dkfz.tbi.otp.dataprocessing.AlignmentPass.AlignmentState
import de.dkfz.tbi.otp.job.processing.ExecutionState
import de.dkfz.tbi.otp.job.processing.ProcessingStepUpdate
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.SampleType.SpecificReferenceGenome
import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin
import org.joda.time.Duration
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement
import static org.junit.Assert.*

class LoadMetaDataTests extends WorkflowTestCase {

    // the String "UNKNOWN" is used instead of the enum, because that is how it appears in external input files
    final String UNKNOWN_VERIFIED_VALUE_FROM_METADATA_FILE = "UNKNOWN"

    LsdfFilesService lsdfFilesService


    // files to be processed by the tests
    String fastqR1Filepath
    String fastqR2Filepath

    String barcode = "GATCGA"
    String fastqR1Filename1 = "example_${barcode}_file_L001_R1.fastq.gz"
    String fastqR2Filename1 = "example_${barcode}_file_L001_R2.fastq.gz"
    String fastqR1Filename2 = "example_${barcode}_file_L002_R1.fastq.gz"
    String fastqR2Filename2 = "example_${barcode}_file_L002_R2.fastq.gz"
    String fastqR1Filename3 = "example_${barcode}_file_L003_R1.fastq.gz"
    String fastqR2Filename3 = "example_${barcode}_file_L003_R2.fastq.gz"
    String runName = "130312_D00133_0018_ADTWTJACXX"
    String runDate = "2013-03-12"
    String metaDataFilepath

    String seqCenterName = "DKFZ"
    String sampleID = "SampleIdentifier"
    String projectName = "TheProject"
    String softwareToolName = "CASAVA"
    String softwareToolVersion = "1.8.2"
    String pipeLineVersion = "${softwareToolName}-${softwareToolVersion}"
    String libraryLayout = SeqType.LIBRARYLAYOUT_PAIRED
    String instrumentPlatform = "Illumina"
    String instrumentModel = "HiSeq2000"
    String sequencingKitName = "sequencingKit"
    String ilseId = "1234"

    String laneNoKit = "1"
    String laneNoKitId = "2"
    String laneNoUnknown = "3"

    String baseCount = "8781211000"
    String readCount = "87812110"
    String cycleCount = "101"
    String insertSize = "162"

    ReferenceGenome referenceGenome

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
            (MetaDataColumn.WITHDRAWN): "0",
            (MetaDataColumn.SEQUENCING_KIT): sequencingKitName,
            (MetaDataColumn.ILSE_NO): ilseId,
        ])
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
        fastqR1Filepath = "${testDataDir}/35-3B_NoIndex_L007_R1_complete_filtered.fastq.gz"
        fastqR2Filepath = "${testDataDir}/35-3B_NoIndex_L007_R2_complete_filtered.fastq.gz"
        metaDataFilepath = "${ftpDir}/${runName}/${runName}.fastq.tsv"

        createDirectories([new File(ftpDir, runName)])
        String createFiles = "echo fastqR1Filepath > ${fastqR1Filepath}; echo fastqR2Filepath > ${fastqR2Filepath};"
        executionService.executeCommand(realm, "${createFiles}")

        FileType fileType = new FileType()
        fileType.type = FileType.Type.SEQUENCE
        fileType.subType = "fastq"
        fileType.vbpPath = "/sequence/"
        fileType.signature = ".fastq"
        assertNotNull(fileType.save(flush: true))

        SampleType sampleType = new SampleType()
        sampleType.name = "some_sample_type"
        sampleType.specificReferenceGenome = SpecificReferenceGenome.USE_PROJECT_DEFAULT
        assertNotNull(sampleType.save(flush: true))

        Project project = TestData.createProject()
        project.name = projectName
        project.dirName = projectName
        project.realmName = realm.name
        project.alignmentDeciderBeanName = 'defaultOtpAlignmentDecider'
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

        SeqPlatformModelLabel seqPlatformModelLabel = new SeqPlatformModelLabel(name: instrumentModel)
        assert seqPlatformModelLabel.save(flush: true)

        SequencingKitLabel sequencingKitLabel = new SequencingKitLabel(name: sequencingKitName)
        assert sequencingKitLabel.save(flush: true)

        SeqPlatformGroup seqPlatformGroup = new SeqPlatformGroup( name: "group1")
        assert seqPlatformGroup.save(flush: true)

        SeqPlatform seqPlatform = new SeqPlatform(
                name: instrumentPlatform,
                seqPlatformModelLabel: seqPlatformModelLabel,
                sequencingKitLabel: sequencingKitLabel,
                seqPlatformGroup: seqPlatformGroup
        )
        assert seqPlatform.save(flush: true)

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

        referenceGenome = TestData.createReferenceGenome()
        assert referenceGenome.save(flush: true)

        [
            createAndSaveSeqType(SeqTypeNames.WHOLE_GENOME.seqTypeName),
            createAndSaveSeqType(SeqTypeNames.EXOME.seqTypeName),
            createAndSaveSeqType(SeqTypeNames.CHIP_SEQ.seqTypeName),
        ].each {
            assert new ReferenceGenomeProjectSeqType(
                project: project,
                seqType: it,
                referenceGenome: referenceGenome,
            ).save(flush: true)
        }
    }


    @Test
    @Ignore
    void testWholeGenomeMetadata() {
        String seqTypeName = SeqTypeNames.WHOLE_GENOME.seqTypeName

        String path = "${ftpDir}/${runName}"
        String softLinkFastqR1Filepath1 = "${path}/${fastqR1Filename1}"
        String softLinkFastqR2Filepath1 = "${path}/${fastqR2Filename1}"

        String cmdBuildSoftLinkToFileToBeProcessed = "ln -s ${fastqR1Filepath} ${softLinkFastqR1Filepath1}; ln -s ${fastqR2Filepath} ${softLinkFastqR2Filepath1}"

        StringBuffer sb = new StringBuffer()
        sb << metaDataTableHeader()
        sb << metaDataTableEntry(metaData([(MetaDataColumn.FASTQ_FILE): fastqR1Filename1, (MetaDataColumn.MD5): md5sum(fastqR1Filepath), (MetaDataColumn.LANE_NO): laneNoKit, (MetaDataColumn.SEQUENCING_TYPE): seqTypeName]))
        sb << metaDataTableEntry(metaData([(MetaDataColumn.FASTQ_FILE): fastqR2Filename1, (MetaDataColumn.MD5): md5sum(fastqR2Filepath), (MetaDataColumn.LANE_NO): laneNoKit, (MetaDataColumn.SEQUENCING_TYPE): seqTypeName]))
        String metaDataFile = sb.toString()

        String cmdCreateMetadataFile = "echo '${metaDataFile}' > ${metaDataFilepath}"
        executionService.executeCommand(realm, "${cmdBuildSoftLinkToFileToBeProcessed}; ${cmdCreateMetadataFile}")

        assert AlignmentPass.count() == 0

        execute()

        assertAlignmentPassesAreNotStarted(1)
    }

    @Test
    @Ignore
    void testExomeMetadataNoLibraryPreparationKit() {
        String seqTypeName = SeqTypeNames.EXOME.seqTypeName

        runScript("scripts/LibraryPreparationKit/LoadLibraryPreparationKits.groovy")
        List<LibraryPreparationKit> libraryPreparationKits = LibraryPreparationKit.list()
        assertFalse(libraryPreparationKits.isEmpty())

        String path = "${ftpDir}/${runName}"
        String softLinkFastqR1Filepath1 = "${path}/${fastqR1Filename1}"
        String softLinkFastqR2Filepath1 = "${path}/${fastqR2Filename1}"
        String cmdBuildSoftLinkToFileToBeProcessed = "ln -s ${fastqR1Filepath} ${softLinkFastqR1Filepath1}; ln -s ${fastqR2Filepath} ${softLinkFastqR2Filepath1}"

        List<MetaDataColumn> metaDataColumns = metaDataColumns()
        metaDataColumns.remove(MetaDataColumn.LIB_PREP_KIT)
        StringBuffer sb = new StringBuffer()
        sb << metaDataTableHeader(metaDataColumns)
        sb << metaDataTableEntry(metaData([(MetaDataColumn.FASTQ_FILE): fastqR1Filename1, (MetaDataColumn.MD5): md5sum(fastqR1Filepath), (MetaDataColumn.LANE_NO): laneNoKit, (MetaDataColumn.SEQUENCING_TYPE): seqTypeName]), metaDataColumns)
        sb << metaDataTableEntry(metaData([(MetaDataColumn.FASTQ_FILE): fastqR2Filename1, (MetaDataColumn.MD5): md5sum(fastqR2Filepath), (MetaDataColumn.LANE_NO): laneNoKit, (MetaDataColumn.SEQUENCING_TYPE): seqTypeName]), metaDataColumns)
        String metaDataFile = sb.toString()

        String cmdCreateMetadataFile = "echo '${metaDataFile}' > ${metaDataFilepath}"
        executionService.executeCommand(realm, "${cmdBuildSoftLinkToFileToBeProcessed}; ${cmdCreateMetadataFile}")
        assertTrue(new File(metaDataFilepath).exists())

        assert AlignmentPass.count() == 0

        execute(1, false)

        assert exactlyOneElement(ProcessingStepUpdate.findAllByState(ExecutionState.FAILURE)).error.errorMessage.contains('Library preparation kit is not set')
        assert AlignmentPass.count() == 0
    }

    @Test
    @Ignore
    void testExomeMetadataWithLibraryPreparationKit() {
        String seqTypeName = SeqTypeNames.EXOME.seqTypeName

        runScript("scripts/LibraryPreparationKit/LoadLibraryPreparationKits.groovy")
        List<LibraryPreparationKit> libraryPreparationKits = LibraryPreparationKit.list()
        assertFalse(libraryPreparationKits.isEmpty())

        // this library PreparationKit was chosen to match the one at the script that is supposed to be executed together with the workflow
        String libraryPreparationKit = "Agilent SureSelect V3"
        String libraryPreparationKitIdentifier = "Agilent SureSelect V3 alias"

        LibraryPreparationKitSynonym libraryPreparationKitSynonym = new LibraryPreparationKitSynonym(
                        name: libraryPreparationKitIdentifier,
                        libraryPreparationKit: libraryPreparationKits.first())
        assertNotNull(libraryPreparationKitSynonym.save([flush: true]))

        assert new BedFile(
                referenceGenome: referenceGenome,
                libraryPreparationKit: libraryPreparationKits.first(),
                mergedTargetSize: 10l,
                fileName: "BedFileName",
                targetSize: 10l,
        ).save(flush: true)

        String path = "${ftpDir}/${runName}"
        String softLinkFastqR1Filepath1 = "${path}/${fastqR1Filename1}"
        String softLinkFastqR2Filepath1 = "${path}/${fastqR2Filename1}"
        String softLinkFastqR1Filepath2 = "${path}/${fastqR1Filename2}"
        String softLinkFastqR2Filepath2 = "${path}/${fastqR2Filename2}"
        String softLinkFastqR1Filepath3 = "${path}/${fastqR1Filename3}"
        String softLinkFastqR2Filepath3 = "${path}/${fastqR2Filename3}"

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
        executionService.executeCommand(realm, "${cmdBuildSoftLinkToFileToBeProcessed}; ${cmdCreateMetadataFile}")
        assertTrue(new File(metaDataFilepath).exists())

        assert AlignmentPass.count() == 0

        execute()

        assertAlignmentPassesAreNotStarted(2)
    }

    @Test
    @Ignore
    void testChipSeqMetadata() {
        String seqTypeName = SeqTypeNames.CHIP_SEQ.seqTypeName

        String path = "${ftpDir}/${runName}"
        String softLinkFastqR1Filepath = "${path}/${fastqR1Filename1}"
        String softLinkFastqR2Filepath = "${path}/${fastqR2Filename1}"

        String cmdBuildSoftLinkToFileToBeProcessed = "ln -s ${fastqR1Filepath} ${softLinkFastqR1Filepath}; ln -s ${fastqR2Filepath} ${softLinkFastqR2Filepath}"

        String ANTIBODY_TARGET_1 = "just4Test1"

        StringBuffer sb = new StringBuffer()
        sb << metaDataTableHeader()
        sb << metaDataTableEntry(metaData([(MetaDataColumn.FASTQ_FILE): fastqR1Filename1, (MetaDataColumn.MD5): md5sum(fastqR1Filepath), (MetaDataColumn.LANE_NO): laneNoKit, (MetaDataColumn.SEQUENCING_TYPE): seqTypeName, (MetaDataColumn.ANTIBODY_TARGET): ANTIBODY_TARGET_1]))
        sb << metaDataTableEntry(metaData([(MetaDataColumn.FASTQ_FILE): fastqR2Filename1, (MetaDataColumn.MD5): md5sum(fastqR2Filepath), (MetaDataColumn.LANE_NO): laneNoKit, (MetaDataColumn.SEQUENCING_TYPE): seqTypeName, (MetaDataColumn.ANTIBODY_TARGET): ANTIBODY_TARGET_1]))
        String metaDataFile = sb.toString()

        String cmdCreateMetadataFile = "echo '${metaDataFile}' > ${metaDataFilepath}"
        executionService.executeCommand(realm, "${cmdBuildSoftLinkToFileToBeProcessed}; ${cmdCreateMetadataFile}")

        AntibodyTarget.findOrSaveByName(ANTIBODY_TARGET_1)

        assert AlignmentPass.count() == 0

        execute()

        assert AlignmentPass.count() == 0
    }

    private SeqType createAndSaveSeqType(String seqTypeName) {
        SeqType seqType = new SeqType()
        seqType.name = seqTypeName
        seqType.libraryLayout = libraryLayout
        seqType.dirName = seqTypeName.replaceAll(/\s/, "")
        assertNotNull(seqType.save(flush: true))
        return seqType
    }

    private static void assertAlignmentPassesAreNotStarted(int expectedAlignmentPassCount) {
        Collection<AlignmentPass> alignmentPasses = AlignmentPass.list()
        assert alignmentPasses.size() == expectedAlignmentPassCount
        assert alignmentPasses.every { it.alignmentState == AlignmentState.NOT_STARTED }
    }


    /**
     * Helper to see logs at console ( besides seeing at the reports in the end)
     * @msg Message to be shown
     */
    void println(String msg) {
        log.debug(msg)
        System.out.println(msg)
    }

    @Override
    List<String> getWorkflowScripts() {
        return ["scripts/workflows/MetaDataWorkflow.groovy"]
    }

    @Override
    Duration getTimeout() {
        Duration.standardMinutes(2)
    }
}
