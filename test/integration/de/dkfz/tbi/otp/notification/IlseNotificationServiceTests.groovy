package de.dkfz.tbi.otp.notification

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.ngsdata.*
import org.junit.*
import org.junit.rules.*

class IlseNotificationServiceTests {

    public static final String META_DATA_FILE_ENDING = "_fastq.tsv"

    IlseNotificationService ilseNotificationService
    ConfigService configService
    LsdfFilesService lsdfFilesService

    public File dir

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()

    @After
    void tearDown() {
        TestCase.removeMetaClass(ConfigService, configService)
        TestCase.removeMetaClass(LsdfFilesService, lsdfFilesService)
        TestCase.removeMetaClass(IlseNotificationService, ilseNotificationService)
    }

    @Test
    void testCreateIlseNotificationForIlseIds_WhenNoIlseIdsDefined_ShouldFail() {
        TestCase.shouldFailWithMessageContaining(AssertionError, 'ILSe IDs not defined') {
            ilseNotificationService.createIlseNotificationForIlseIds(null)
        }
    }

    @Test
    void testCreateIlseNotificationForIlseIds_WhenIlseIdNotConsistOfNumbers_ShouldFail() {
        List<String> ilseIds = ["abcd"]

        TestCase.shouldFailWithMessageContaining(AssertionError, 'An ILSe ID can just consist of numbers') {
            ilseNotificationService.createIlseNotificationForIlseIds(ilseIds)
        }
    }

    @Test
    void testCreateIlseNotificationForIlseIds_WhenIlseFolderDoesNotExist_ShouldFail() {
        List<String> ilseIds = ["1234"]

        TestCase.shouldFailWithMessageContaining(AssertionError, "No Folder for ILSe ${ilseIds.first()} can be found") {
            ilseNotificationService.createIlseNotificationForIlseIds(ilseIds)
        }
    }

    @Test
    void testCreateIlseNotificationForIlseIds_WhenAllFine_ShouldReturnNotificationMessage() {
        createTestDirectory()

        List<String> ilseIds = ["1234"]

        Run run = DomainFactory.createRun()

        List<String> seqTypes = ['seqType1', 'seqType2']
        List<String> pathes = ["path1", "path2"]
        List<String> samples = ['sample1', 'sample2']

        Project project = DomainFactory.createProject()

        Individual individual = DomainFactory.createIndividual(project: project)

        samples.each { String sampleName ->
            Sample sample = DomainFactory.createSample(individual: individual)
            DomainFactory.createSampleIdentifier([name: sampleName, sample: sample])
        }

        File runFolder = new File(dir, run.name)
        assert runFolder.mkdir()

        lsdfFilesService.metaClass.getIlseFolder = { String s ->
            return dir
        }

        ilseNotificationService.metaClass.findMetaFileInRunFolder = { File f ->
            return createMetaDataFileInFolder(runFolder, 'meta_data_file_1')
        }

        ilseNotificationService.metaClass.parseMetaFileContentForProperties = { String s, Map m ->
            return [samples: samples, seqTypes: seqTypes]
        }

        ilseNotificationService.metaClass.getPathsToSeqTypesForRunAndProject = { Run r, Project p ->
            return pathes
        }

        String expectedMessage = """
New run ${run.name} with ${seqTypes.join(', ')} is installed and is ready for the analysis.
Data are available in the directory:
${pathes.join('\n')}

Samples:
${samples.join('\n')}
"""

        assert expectedMessage == ilseNotificationService.createIlseNotificationForIlseIds(ilseIds)
    }

    @Test
    void testCreateIlseNotificationForIlseIds_WhenSeveralProjectsForSamples_ShouldFail() {
        createTestDirectory()

        List<String> ilseIds = ["1234"]

        Run run = DomainFactory.createRun()

        List<String> seqTypes = ['seqType1', 'seqType2']
        List<String> pathes = ["path1", "path2"]
        List<String> samples = ['sample1', 'sample2']

        samples.each { String sampleName ->
            Project project = DomainFactory.createProject()
            Individual individual = DomainFactory.createIndividual(project: project)
            Sample sample = DomainFactory.createSample(individual: individual)
            DomainFactory.createSampleIdentifier([name: sampleName, sample: sample])
        }

        File runFolder = new File(dir, run.name)
        assert runFolder.mkdir()

        lsdfFilesService.metaClass.getIlseFolder = { String s ->
            return dir
        }

        ilseNotificationService.metaClass.findMetaFileInRunFolder = { File f ->
            return createMetaDataFileInFolder(runFolder, 'meta_data_file_1')
        }

        ilseNotificationService.metaClass.parseMetaFileContentForProperties = { String s, Map m ->
            return [samples: samples, seqTypes: seqTypes]
        }

        ilseNotificationService.metaClass.getPathsToSeqTypesForRunAndProject = { Run r, Project p ->
            return pathes
        }


        TestCase.shouldFailWithMessageContaining(AssertionError, "contains samples of more than one project, what is currently not supported.") {
            ilseNotificationService.createIlseNotificationForIlseIds(ilseIds)
        }
    }

    @Test
    void testFindMetaFileInRunFolder_WhenNoMetaDataFileFound_ShouldFail() {
        createTestDirectory()

        TestCase.shouldFailWithMessageContaining(AssertionError, 'No meta-data file can be found under') {
            ilseNotificationService.findMetaFileInRunFolder(dir)
        }
    }

    @Test
    void testFindMetaFileInRunFolder_WhenSeveralMetaDataFilesExist_ShouldFail() {
        createTestDirectory()

        createMetaDataFileInFolder(dir, 'meta_data_file_1')
        createMetaDataFileInFolder(dir, 'meta_data_file_2')

        TestCase.shouldFailWithMessageContaining(AssertionError, 'At least one other file exists under') {
            ilseNotificationService.findMetaFileInRunFolder(dir)
        }
    }

    @Test
    void testFindMetaFileInRunFolder_WhenAllFine_ShouldReturnPathToMetaDataFile() {
        createTestDirectory()

        File metaFile = createMetaDataFileInFolder(dir, 'meta_data_file_1')

        assert metaFile == ilseNotificationService.findMetaFileInRunFolder(dir)
    }

    @Test
    void testParseMetaFileContentForProperties_WhenMetaDataFileIsEmpty_ShouldFail() {
        TestCase.shouldFailWithMessageContaining(AssertionError, 'Meta-Data File is empty') {
            ilseNotificationService.parseMetaFileContentForProperties("", ['property': 'header'])
        }
    }

    @Test
    void testParseMetaFileContentForProperties_WhenPropertyNotFound_ShouldFail() {
        String metaDataFile = "HEADER2\tHEADER3\nVALUE2\tVALUE3"

        TestCase.shouldFailWithMessageContaining(AssertionError, 'contains no information about') {
            ilseNotificationService.parseMetaFileContentForProperties(metaDataFile, ["property": "HEADER1"])
        }

    }

    @Test
    void testParseMetaFileContentForProperties_WhenAllFine_ShouldReturnMapOfParsedValues() {
        String metaDataFile = "HEADER1\tHEADER2\nVALUE1\tVALUE2"

        assert ["property": ["VALUE1"]] == ilseNotificationService.parseMetaFileContentForProperties(metaDataFile, ["property": "HEADER1"])
    }

    @Test
    void testGetPathsToSeqTypesForRunAndProject_WhenNoRun_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            ilseNotificationService.getPathsToSeqTypesForRunAndProject(null, DomainFactory.createProject())
        }
    }

    @Test
    void testGetPathsToSeqTypesForRunAndProject_WhenNoProject_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            ilseNotificationService.getPathsToSeqTypesForRunAndProject(DomainFactory.createRun(), null)
        }
    }

    @Test
    void testGetPathsToSeqTypesForRunAndProject_WhenNoDataFilesFoundForRunAndProject_ShouldFail() {
        TestCase.shouldFailWithMessageContaining(AssertionError, 'No Data Files can be found for') {
            ilseNotificationService.getPathsToSeqTypesForRunAndProject(DomainFactory.createRun(), DomainFactory.createProject())
        }
    }

    @Test
    void testGetPathsToSeqTypesForRunAndProject_WhenAllFine_ShouldReturnListOfPaths() {
        configService.metaClass.getProjectSequencePath = { Project p ->
            return "project"
        }

        lsdfFilesService.metaClass.seqTypeDirectory = { DataFile f ->
            return "seqType"
        }

        Project project = DomainFactory.createProject()

        Run run = DomainFactory.createRun()

        DataFile dataFile = DomainFactory.createDataFile([run: run, project: project])

        assert ["project/seqType/"] == ilseNotificationService.getPathsToSeqTypesForRunAndProject(run, project)
    }

    private File createTestDirectory() {
        dir = tmpDir.newFolder()
    }

    private static File createMetaDataFileInFolder(File directory, String name) {
        File metaFile = new File(directory, "${name}${META_DATA_FILE_ENDING}")
        assert metaFile.createNewFile()

        return metaFile
    }
}
