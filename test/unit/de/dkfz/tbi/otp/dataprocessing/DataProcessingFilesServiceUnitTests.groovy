package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.TestConstants
import de.dkfz.tbi.otp.dataprocessing.DataProcessingFilesService.OutputDirectories
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.Realm.OperationType
import de.dkfz.tbi.otp.utils.CheckedLogger
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

//In the test the semantic "null as Type" is used to get grails to use a specific overloaded method signutare.
@TestMixin(GrailsUnitTestMixin)
@Build([
    Individual,
    ProcessedBamFile,
    Project,
    Realm,
    ReferenceGenome,
])
class DataProcessingFilesServiceUnitTests {

    private final static String TEST_FILE_NAME = "testFile.txt"
    private final static String TEST_FILE_CONTENT = "test file content"
    private final static int TEST_FILE_CONTENT_LENGTH = TEST_FILE_CONTENT.length()
    private final static String PASS_TYPE_NAME = "passTypeName"
    private final static long TIME = 60 * 60 * 1000



    CheckedLogger checkedLogger
    DataProcessingFilesService dataProcessingFilesService
    Realm realm
    Project project
    ProcessedBamFile processedBamFile

    File dir
    File file

    File processingDir
    File finalDir
    File processingFile
    File finalFile

    Object passService
    Date createdBefore
    List<Object> passes
    long freedBytes
    Closure passClosure

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()

    void setUp() {
        realm = Realm.build()

        dataProcessingFilesService = new DataProcessingFilesService()
        dataProcessingFilesService.lsdfFilesService = [
            deleteDirectory: {final Realm realm, final File directory ->
                assert directory.directory
                assert directory.delete()
            },
            deleteFile: {final Realm realm, final File file ->
                assert file.file
                assert file.delete()
            },
        ] as LsdfFilesService
        dataProcessingFilesService.configService = [
            getRealmDataProcessing: {realm}
        ] as ConfigService

        checkedLogger = new CheckedLogger()
        LogThreadLocal.setThreadLog(checkedLogger)
    }

    @After
    void tearDown() {
        dataProcessingFilesService = null
        realm = null
        if (dir && dir.exists()){
            assert dir.deleteDir()
            assert !dir.exists()
        }

        dir = null
        file = null

        processingDir = null
        finalDir = null
        processingFile = null
        finalFile = null

        passService = null
        createdBefore = null
        passes = null
        passClosure = null


        LogThreadLocal.removeThreadLog()
        checkedLogger.assertAllMessagesConsumed()
        checkedLogger = null
    }



    void createTestDirectory() {
        tmpDir.create()
        dir = tmpDir.newFolder(realm.processingRootPath)
        if (dir.exists()) {
            dir.deleteDir()
        }
        assert dir.mkdirs()
    }

    void createTestFile() {
        if (!dir) {
            createTestDirectory()
        }
        file = createTestFile(dir)
    }

    File createTestFile(File directoy, String fileName = TEST_FILE_NAME, String content = TEST_FILE_CONTENT) {
        assert directoy.exists() || directoy.mkdirs()
        File file = new File(directoy, fileName)
        assert !file.exists()
        file << content
        assert file.exists()
        return file
    }

    private void prepareCheckConsistencyWithDatabaseForDeletion() {
        createTestFile()
        processedBamFile = ProcessedBamFile.build([
            fileExists: true,
            fileSize: TEST_FILE_CONTENT_LENGTH,
            dateFromFileSystem: new Date(file.lastModified()),
        ])
        processedBamFile.alignmentPass.metaClass.isLatestPass = {true} //mock method because createCritera make problems
    }

    private File[] prepareDeleteProcessingFiles(int additionalFiles = 0) {
        prepareCheckConsistencyWithDatabaseForDeletion()
        List<File> files = []
        for (int i = 0 ; i < additionalFiles ; i++) {
            files << createTestFile(dir, "additionalTest${i}.txt")
        }
        dataProcessingFilesService.metaClass.checkConsistencyWithDatabaseForDeletion = {final def dbFile, final File fsFile -> true} //mock method
        return files as File[]
    }

    private void createCheckConsistencyData() {
        createTestDirectory()
        processingDir = new File(dir, "processing")
        assert processingDir.mkdir()
        finalDir = new File(dir, "final")
        assert finalDir.mkdir()
        processingFile = createTestFile(processingDir)
        finalFile = createTestFile(finalDir)
    }

    private void prepareDeleteOldProcessingFiles(boolean mayFileDelete = true) {
        passService = [
            mayProcessingFilesBeDeleted: {a, b -> return mayFileDelete},
            deleteProcessingFiles:  {a -> return TEST_FILE_CONTENT_LENGTH},
        ] as Object
        createdBefore = new Date()
        passes = ["pass 1", "pass 2", "pass 3"]
        freedBytes = passes.size() * TEST_FILE_CONTENT_LENGTH
        passClosure = {return passes}
    }



    void testDeleteProcessingDirectory_directoryAsFile() {
        project = Project.build()
        createTestDirectory()

        dataProcessingFilesService.deleteProcessingDirectory(project, dir)
        assert !dir.exists()
    }

    void testDeleteProcessingDirectory_directoryAsFile_projectIsNull() {
        createTestDirectory()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            dataProcessingFilesService.deleteProcessingDirectory(null as Project, dir)
        }
    }

    void testDeleteProcessingDirectory_directoryAsFile_directoryIsNull() {
        project = Project.build()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            dataProcessingFilesService.deleteProcessingDirectory(project, null as File)
        }
    }

    void testDeleteProcessingDirectory_directoryAsFile_directoryDoesNotExist() {
        project = Project.build()
        createTestDirectory()
        checkedLogger.addWarning("Directory has already been deleted: ${dir}.")
        assert dir.delete()

        dataProcessingFilesService.deleteProcessingDirectory(project, dir)
        assert !dir.exists()
    }

    void testDeleteProcessingDirectory_directoryAsFile_directoryIsNotEmpty() {
        project = Project.build()
        createTestFile()
        checkedLogger.addError("Directory ${dir} is not empty. Will not delete it.")
        dataProcessingFilesService.deleteProcessingDirectory(project, dir)
        assert dir.exists()
    }



    void testDeleteProcessingDirectory_directoryAsString() {
        project = Project.build()
        createTestDirectory()

        dataProcessingFilesService.deleteProcessingDirectory(project, dir as String)
        assert !dir.exists()
    }

    void testDeleteProcessingDirectory_directoryAsString_projectIsNull() {
        createTestDirectory()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            dataProcessingFilesService.deleteProcessingDirectory(null as Project, dir as String)
        }
    }

    void testDeleteProcessingDirectory_directoryAsString_directoryIsNull() {
        project = Project.build()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            dataProcessingFilesService.deleteProcessingDirectory(project, null as String)
        }
    }



    void testDeleteProcessingFile() {
        project = Project.build()
        createTestFile()

        assert TEST_FILE_CONTENT_LENGTH == dataProcessingFilesService.deleteProcessingFile(project, file)
        assert !file.exists()
    }

    void testDeleteProcessingFile_projectIsNull() {
        createTestFile()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            dataProcessingFilesService.deleteProcessingFile(null as Project, file)
        }
        assert file.exists()
    }

    void testDeleteProcessingFile_fileIsNull() {
        project = Project.build()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            dataProcessingFilesService.deleteProcessingFile(project, null as File)
        }
    }

    void testDeleteProcessingFile_fileDoesNotExist() {
        project = Project.build()
        createTestDirectory()
        file = new File("tmp.txt")
        checkedLogger.addWarning("File has already been deleted: ${file}.")

        assert 0 == dataProcessingFilesService.deleteProcessingFile(project, file)
    }



    void testDeleteProcessingFile_fileAsString() {
        project = Project.build()
        createTestFile()

        assert TEST_FILE_CONTENT_LENGTH == dataProcessingFilesService.deleteProcessingFile(project, file as String)
        assert !file.exists()
    }

    void testDeleteProcessingFile_fileAsStringAndProjectIsNull() {
        createTestFile()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            dataProcessingFilesService.deleteProcessingFile(null as Project, file as String)
        }
        assert file.exists()
    }

    void testDeleteProcessingFile_fileAsStringAndFileIsNull() {
        project = Project.build()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            dataProcessingFilesService.deleteProcessingFile(project, null as String)
        }
    }



    void testDeleteProcessingFiles_TypeProject() {
        project = Project.build()
        createTestFile()

        assert TEST_FILE_CONTENT_LENGTH == dataProcessingFilesService.deleteProcessingFiles(project, dir, [file.name])
        assert !file.exists()
    }

    void testDeleteProcessingFiles_NoProject() {
        createTestFile()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            dataProcessingFilesService.deleteProcessingFiles(null, dir, [file.name])
        }
        assert file.exists()
    }

    void testDeleteProcessingFiles_NoProcessingDirectory() {
        project = Project.build()
        createTestFile()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            dataProcessingFilesService.deleteProcessingFiles(project, null, [file.name])
        }
        assert file.exists()
    }

    void testDeleteProcessingFiles_FileNamesIsNull() {
        project = Project.build()
        createTestDirectory()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            dataProcessingFilesService.deleteProcessingFiles(project, dir, null)
        }
    }

    void testDeleteProcessingFiles_NoFiles() {
        project = Project.build()
        createTestDirectory()

        assert 0 == dataProcessingFilesService.deleteProcessingFiles(project, dir, [])
    }

    void testDeleteProcessingFiles_MultipleFiles() {
        project = Project.build()
        createTestFile()
        File file2 = createTestFile(dir, "test1")
        File file3 = createTestFile(dir, "test2")

        assert TEST_FILE_CONTENT_LENGTH * 3 == dataProcessingFilesService.deleteProcessingFiles(project, dir, [
            file.name,
            file2.name,
            file3.name
        ])
        assert !file.exists()
        assert !file2.exists()
        assert !file3.exists()
    }



    void testDeleteProcessingFilesAndDirectory_TypeProject() {
        project = Project.build()
        createTestFile()

        assert TEST_FILE_CONTENT_LENGTH == dataProcessingFilesService.deleteProcessingFilesAndDirectory(project, dir, [file.name])
        assert !file.exists()
        assert !dir.exists()
    }

    void testDeleteProcessingFilesAndDirectory_NoProject() {
        createTestFile()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            dataProcessingFilesService.deleteProcessingFilesAndDirectory(null, dir, [file.name])
        }
        assert file.exists()
    }

    void testDeleteProcessingFilesAndDirectory_NoProcessingDirectory() {
        project = Project.build()
        createTestFile()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            dataProcessingFilesService.deleteProcessingFilesAndDirectory(project, null, [file.name])
        }
        assert file.exists()
    }

    void testDeleteProcessingFilesAndDirectory_FileNamesIsNull() {
        project = Project.build()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            dataProcessingFilesService.deleteProcessingFilesAndDirectory(project, dir, null)
        }
    }

    void testDeleteProcessingFilesAndDirectory_NoFiles() {
        project = Project.build()
        createTestDirectory()

        assert 0 == dataProcessingFilesService.deleteProcessingFilesAndDirectory(project, dir, [])
        assert !dir.exists()
    }

    void testDeleteProcessingFilesAndDirectory_MultipleFiles() {
        project = Project.build()
        createTestFile()
        File file2 = createTestFile(dir, "test1")
        File file3 = createTestFile(dir, "test2")

        assert TEST_FILE_CONTENT_LENGTH * 3 == dataProcessingFilesService.deleteProcessingFilesAndDirectory(project, dir, [
            file.name,
            file2.name,
            file3.name
        ])
        assert !file.exists()
        assert !file2.exists()
        assert !file3.exists()
        assert !dir.exists()
    }


    void testDeleteProcessingFiles_TypeFile() {
        prepareDeleteProcessingFiles()

        assert TEST_FILE_CONTENT_LENGTH == dataProcessingFilesService.deleteProcessingFiles(processedBamFile, file)
        assert !processedBamFile.fileExists
        assert null != processedBamFile.deletionDate
        assert !file.exists()
    }

    void testDeleteProcessingFiles_withAdditionalFiles() {
        final int ADDITINAL_FILE_COUNT = 5
        File[] files = prepareDeleteProcessingFiles(ADDITINAL_FILE_COUNT)

        assert ((1 + ADDITINAL_FILE_COUNT) * TEST_FILE_CONTENT_LENGTH) == dataProcessingFilesService.deleteProcessingFiles(processedBamFile, file, files)
        assert !processedBamFile.fileExists
        assert null != processedBamFile.deletionDate
        assert !file.exists()
        files.each {
            assert !it.exists()
        }
    }

    void testDeleteProcessingFiles_dbFileIsNull() {
        prepareDeleteProcessingFiles()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            dataProcessingFilesService.deleteProcessingFiles(null, file)
        }
        assert processedBamFile.fileExists
        assert null == processedBamFile.deletionDate
        assert file.exists()
    }

    void testDeleteProcessingFiles_fsFileIsNull() {
        prepareDeleteProcessingFiles()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            dataProcessingFilesService.deleteProcessingFiles(processedBamFile, null)
        }
        assert processedBamFile.fileExists
        assert null == processedBamFile.deletionDate
        assert file.exists()
    }

    void testDeleteProcessingFiles_additionalFilesIsNull() {
        prepareDeleteProcessingFiles()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            dataProcessingFilesService.deleteProcessingFiles(processedBamFile, file, null)
        }
        assert processedBamFile.fileExists
        assert null == processedBamFile.deletionDate
        assert file.exists()
    }

    void testDeleteProcessingFiles_checkConsistencyWithDatabaseForDeletionIsFalse() {
        createTestFile()
        processedBamFile = ProcessedBamFile.build([
            fileExists: true
        ])
        dataProcessingFilesService.metaClass.checkConsistencyWithDatabaseForDeletion = {final def dbFile, final File fsFile -> false} //mock method

        assert 0 == dataProcessingFilesService.deleteProcessingFiles(processedBamFile, file)
        assert processedBamFile.fileExists
        assert null == processedBamFile.deletionDate
        assert file.exists()
    }



    void testCheckConsistencyWithFinalDestinationForDeletion() {
        createCheckConsistencyData()

        assert dataProcessingFilesService.checkConsistencyWithFinalDestinationForDeletion(processingDir, finalDir, [TEST_FILE_NAME])
    }

    void testCheckConsistencyWithFinalDestinationForDeletionMultipleFiles() {
        createCheckConsistencyData()
        List<String> files = [TEST_FILE_NAME]
        [1..5].each {
            files << createTestFile(processingDir, "additionalTest${it}.txt").name
            createTestFile(finalDir, "additionalTest${it}.txt")
        }

        assert dataProcessingFilesService.checkConsistencyWithFinalDestinationForDeletion(processingDir, finalDir, files)
    }

    void testCheckConsistencyWithFinalDestinationForDeletion_NoProcessingDirectory() {
        createCheckConsistencyData()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            dataProcessingFilesService.checkConsistencyWithFinalDestinationForDeletion(null, finalDir, [TEST_FILE_NAME])
        }
    }

    void testCheckConsistencyWithFinalDestinationForDeletion_NoFinalDestinationDirectory() {
        createCheckConsistencyData()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            dataProcessingFilesService.checkConsistencyWithFinalDestinationForDeletion(processingDir, null, [TEST_FILE_NAME])
        }
    }

    void testCheckConsistencyWithFinalDestinationForDeletion_NoFileNames() {
        createCheckConsistencyData()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            dataProcessingFilesService.checkConsistencyWithFinalDestinationForDeletion(processingDir, finalDir,null)
        }
    }

    void testCheckConsistencyWithFinalDestinationForDeletion_FileInProcessingDoesNotExist() {
        createCheckConsistencyData()
        processingFile.delete()

        assert dataProcessingFilesService.checkConsistencyWithFinalDestinationForDeletion(processingDir, finalDir, [TEST_FILE_NAME])
    }

    void testCheckConsistencyWithFinalDestinationForDeletion_FileInDestinationDoesNotExist() {
        createCheckConsistencyData()
        finalFile.delete()
        checkedLogger.addError("File does not exist: ${finalFile}. " +
                        "Expected it to be the same as ${processingFile} (${processingFile.size()} bytes, " +
                        "last modified ${new Date(processingFile.lastModified())})")

        assert !dataProcessingFilesService.checkConsistencyWithFinalDestinationForDeletion(processingDir, finalDir, [TEST_FILE_NAME])
    }

    void testCheckConsistencyWithFinalDestinationForDeletion_FileInDestinationHaveDifferentSize() {
        createCheckConsistencyData()
        processingFile << "More content"
        checkedLogger.addError("Files are different: " +
                        "${processingFile} (${processingFile.size()} bytes, last modified ${new Date(processingFile.lastModified())}) and " +
                        "${finalFile} (${finalFile.size()} bytes, last modified ${new Date(finalFile.lastModified())})")

        assert !dataProcessingFilesService.checkConsistencyWithFinalDestinationForDeletion(processingDir, finalDir, [TEST_FILE_NAME])
    }



    void testCheckConsistencyWithDatabaseForDeletion() {
        prepareCheckConsistencyWithDatabaseForDeletion()

        assert dataProcessingFilesService.checkConsistencyWithDatabaseForDeletion(processedBamFile, file)
    }

    void testCheckConsistencyWithDatabaseForDeletion_NoDBFile() {
        prepareCheckConsistencyWithDatabaseForDeletion()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            dataProcessingFilesService.checkConsistencyWithDatabaseForDeletion(null, file)
        }
    }

    void testCheckConsistencyWithDatabaseForDeletion_NoFsFile() {
        prepareCheckConsistencyWithDatabaseForDeletion()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            dataProcessingFilesService.checkConsistencyWithDatabaseForDeletion(processedBamFile, null)
        }
    }

    void testCheckConsistencyWithDatabaseForDeletion_CheckLogForFileExistIsFalse() {
        prepareCheckConsistencyWithDatabaseForDeletion()
        processedBamFile.fileExists = false
        checkedLogger.addWarning("fileExists is already false for ${processedBamFile} (${file}).")

        assert dataProcessingFilesService.checkConsistencyWithDatabaseForDeletion(processedBamFile, file)
    }

    void testCheckConsistencyWithDatabaseForDeletion_CheckLogForDeletionDateIsSet() {
        prepareCheckConsistencyWithDatabaseForDeletion()
        processedBamFile.deletionDate = new Date()
        checkedLogger.addWarning("deletionDate is already set (to ${processedBamFile.deletionDate}) for ${processedBamFile} (${file}).")

        assert dataProcessingFilesService.checkConsistencyWithDatabaseForDeletion(processedBamFile, file)
    }

    void testCheckConsistencyWithDatabaseForDeletion_CheckLogForFileDoesNotExist() {
        prepareCheckConsistencyWithDatabaseForDeletion()
        assert file.delete()
        checkedLogger.addError("File does not exist on the file system: ${processedBamFile} (${file}). Will not mark the file as deleted in the database.")

        assert !dataProcessingFilesService.checkConsistencyWithDatabaseForDeletion(processedBamFile, file)
    }

    void testCheckConsistencyWithDatabaseForDeletion_FileSizeIsInconsistent() {
        prepareCheckConsistencyWithDatabaseForDeletion()
        processedBamFile.fileSize = -1
        checkedLogger.addError("File size in database (${processedBamFile.fileSize}) and on the file system (${file.size()}) are different for ${processedBamFile} (${file}). Will not delete the file.")

        assert !dataProcessingFilesService.checkConsistencyWithDatabaseForDeletion(processedBamFile, file)
    }

    void testCheckConsistencyWithDatabaseForDeletion_DateIsInconsistent() {
        prepareCheckConsistencyWithDatabaseForDeletion()
        processedBamFile.dateFromFileSystem = new Date()
        checkedLogger.addError("File date in database (${processedBamFile.dateFromFileSystem}) and on the file system (${new Date(file.lastModified())}) are different for ${processedBamFile} (${file}). Will not delete the file.")

        assert !dataProcessingFilesService.checkConsistencyWithDatabaseForDeletion(processedBamFile, file)
    }



    void testDeleteOldProcessingFiles() {
        prepareDeleteOldProcessingFiles()

        checkedLogger.addInfo("Deleting processing files of ${PASS_TYPE_NAME} passes created before ${createdBefore}.")
        checkedLogger.addInfo("Found ${passes.size()} ${PASS_TYPE_NAME} passes for processing files deletion.")
        checkedLogger.addInfo("${freedBytes} bytes have been freed by deleting the processing files of ${passes.size()} ${PASS_TYPE_NAME} passes created before ${createdBefore}.")

        assert freedBytes == dataProcessingFilesService.deleteOldProcessingFiles(passService, PASS_TYPE_NAME, createdBefore, TIME, passClosure)
    }

    void testDeleteOldProcessingFiles_NoPassService() {
        prepareCheckConsistencyWithDatabaseForDeletion()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            dataProcessingFilesService.deleteOldProcessingFiles(null, PASS_TYPE_NAME, createdBefore, TIME, passClosure)
        }
    }

    void testDeleteOldProcessingFiles_NoPassTypeName() {
        prepareCheckConsistencyWithDatabaseForDeletion()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            dataProcessingFilesService.deleteOldProcessingFiles(passService, null, createdBefore, TIME, passClosure)
        }
    }

    void testDeleteOldProcessingFiles_NoCreatedBefore() {
        prepareCheckConsistencyWithDatabaseForDeletion()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            dataProcessingFilesService.deleteOldProcessingFiles(passService, PASS_TYPE_NAME, null, TIME, passClosure)
        }
    }

    void testDeleteOldProcessingFiles_NoPassFunc() {
        prepareCheckConsistencyWithDatabaseForDeletion()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            dataProcessingFilesService.deleteOldProcessingFiles(passService, PASS_TYPE_NAME, createdBefore, TIME, null)
        }
    }

    void testDeleteOldProcessingFiles_NoTimeLeft() {
        prepareDeleteOldProcessingFiles()
        long time = -10 //negative, since 0 has not worked.

        checkedLogger.addInfo("Deleting processing files of ${PASS_TYPE_NAME} passes created before ${createdBefore}.")
        checkedLogger.addInfo("Found ${passes.size()} ${PASS_TYPE_NAME} passes for processing files deletion.")
        checkedLogger.addInfo("Exiting because the maximum runtime (${time} ms) has elapsed.")
        checkedLogger.addInfo("0 bytes have been freed by deleting the processing files of 0 ${PASS_TYPE_NAME} passes created before ${createdBefore}.")

        assert 0 == dataProcessingFilesService.deleteOldProcessingFiles(passService, PASS_TYPE_NAME, createdBefore, time, passClosure)
    }

    void testDeleteOldProcessingFiles_Date() {
        prepareDeleteOldProcessingFiles(false)

        checkedLogger.addInfo("Deleting processing files of ${PASS_TYPE_NAME} passes created before ${createdBefore}.")
        checkedLogger.addInfo("Found ${passes.size()} ${PASS_TYPE_NAME} passes for processing files deletion.")
        passes.each {
            checkedLogger.addError("May not delete processing files of ${it}.")
        }
        checkedLogger.addInfo("0 bytes have been freed by deleting the processing files of 0 ${PASS_TYPE_NAME} passes created before ${createdBefore}.")

        assert 0 == dataProcessingFilesService.deleteOldProcessingFiles(passService, PASS_TYPE_NAME, createdBefore, TIME, passClosure)
    }

    @Test
    void testGetOutputDirectory() {
        Realm realm = Realm.build([
            operationType: OperationType.DATA_PROCESSING
        ])

        Project project = Project.build([
            name: "projectName",
            realmName: realm.name
        ]
        )

        Individual individual = Individual.build([
            project: project
        ])

        String pid = individual.pid
        String projectDir = project.dirName
        String realmDir = realm.processingRootPath
        //!dir || dir == OutputDirectories.BASE) ? "" : "${dir.toString().toLowerCase() -> postfix
        //${outputBaseDir}/results_per_pid/${individual.pid}/${postfix}"
        // rpPath = realm.processingRootPath
        //String pdName = project.dirName
        //return "${rpPath}/${pdName}"
        String expectedPath = "${realmDir}/${projectDir}/results_per_pid/${pid}/"
        String actualPath = dataProcessingFilesService.getOutputDirectory(individual, null)
        assertEquals(expectedPath, actualPath)

        expectedPath = "${realmDir}/${projectDir}/results_per_pid/${pid}/"
        actualPath = dataProcessingFilesService.getOutputDirectory(individual, OutputDirectories.BASE)
        assertEquals(expectedPath, actualPath)

        expectedPath = "${realmDir}/${projectDir}/results_per_pid/${pid}/alignment/"
        actualPath = dataProcessingFilesService.getOutputDirectory(individual, OutputDirectories.ALIGNMENT)
        assertEquals(expectedPath, actualPath)

        expectedPath = "${realmDir}/${projectDir}/results_per_pid/${pid}/fastx_qc/"
        actualPath = dataProcessingFilesService.getOutputDirectory(individual, OutputDirectories.FASTX_QC)
        assertEquals(expectedPath, actualPath)
    }
}
