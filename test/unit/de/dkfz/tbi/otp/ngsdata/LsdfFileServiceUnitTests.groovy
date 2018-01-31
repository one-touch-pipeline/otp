package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.*
import grails.buildtestdata.mixin.*
import org.codehaus.groovy.control.io.*
import org.junit.*
import org.junit.rules.*

@Build([Realm])
class LsdfFileServiceUnitTests {

    LsdfFilesService service

    @Before
    void setUp() {
        service = new LsdfFilesService()
    }

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder()

    @Test
    void testFileReadableAndNotEmptyMethods_AllCorrect() {
        File file = tempFolder.newFile()
        file << "content"
        assert LsdfFilesService.isFileReadableAndNotEmpty(file)
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)
    }

    @Test
    void testFileReadableAndNotEmptyMethods_IsNotAbsolute() {
        File file = new File("testFile.txt")
        assert shouldFail(AssertionError, {LsdfFilesService.isFileReadableAndNotEmpty(file)}) =~ /(?i)isAbsolute/
        assert shouldFail(AssertionError, {LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)}) =~ /(?i)isAbsolute/
    }

    @Test
    void testFileReadableAndNotEmptyMethods_DoesNotExist() {
        //file must be absolute to make sure that the test fails the 'exists?' assertion
        File file = new File(tempFolder.newFolder(), "testFile.txt")
        assert !LsdfFilesService.isFileReadableAndNotEmpty(file)
        assert shouldFail(AssertionError, {LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)}) =~ /(?i)not found\./
    }

    @Test
    void testFileReadableAndNotEmptyMethods_IsNotAFile() {
        File file = tempFolder.newFolder()
        assert !LsdfFilesService.isFileReadableAndNotEmpty(file)
        assert shouldFail(AssertionError, {LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)}) =~ /(?i)isFile/
    }

    @Test
    void testFileReadableAndNotEmptyMethods_CanNotRead() {
        File file = tempFolder.newFile()
        file << "content"
        try {
            file.setReadable(false)
            assert !LsdfFilesService.isFileReadableAndNotEmpty(file)
            assert shouldFail(AssertionError, {LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)}) =~ /(?i)canRead/
        } finally {
            file.setReadable(true)
        }
    }

    @Test
    void testFileReadableAndNotEmptyMethods_IsEmpty() {
        File file = tempFolder.newFile()
        assert !LsdfFilesService.isFileReadableAndNotEmpty(file)
        assert shouldFail(AssertionError, {LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)}) =~ /(?i)length/
    }

    @Test
    void testDeleteFile() {
        testDeleteMethod(tempFolder.newFile(), { LsdfFilesService service, Realm realm, File file ->
            service.deleteFile(realm, file)
        })
    }

    @Test
    void testDeleteDirectory() {
        testDeleteMethod(tempFolder.newFolder(), { LsdfFilesService service, Realm realm, File file ->
            service.deleteDirectory(realm, file)
        })
    }

    @Test
    void test_deleteFilesRecursive_shouldBeFine() {
        Realm realm = DomainFactory.createRealm()
        service.executionService = [
                executeCommand: {Realm realm2, String command->
                    ProcessHelperService.executeAndAssertExitCodeAndErrorOutAndReturnStdout(command)
                }
                ] as ExecutionService
        service.createClusterScriptService = new CreateClusterScriptService()

        List<File> files = [
                tempFolder.newFolder(),
                tempFolder.newFile(),
        ]

        service.deleteFilesRecursive(realm, files)

        files.each {
            assert !it.exists()
        }
    }

    @Test
    void test_deleteFilesRecursive_FilesOrDirectoriesIsEmpty_shouldDoNothing() {
        Realm realm = DomainFactory.createRealm()
        service.executionService = [
                executeCommand: {Realm realm2, String command->
                    assert false: 'Should not be called'
                }
        ] as ExecutionService
        service.createClusterScriptService = new CreateClusterScriptService()

        service.deleteFilesRecursive(realm, [])
    }

    @Test
    void test_deleteFilesRecursive_noRealm_shouldThrowException() {
        TestCase.shouldFailWithMessageContaining(AssertionError, 'realm may not be null.') {
            service.deleteFilesRecursive(null, [tempFolder.newFolder()])
        }
    }

    @Test
    void test_deleteFilesRecursive_FilesOrDirectoriesIsNull_shouldThrowException() {
        Realm realm = DomainFactory.createRealm()
        TestCase.shouldFailWithMessageContaining(AssertionError, 'filesOrDirectories may not be null.') {
            service.deleteFilesRecursive(realm, null)
        }
    }



    @Test
    void test_deleteFilesRecursive_deletionFail_shouldThrowException() {
        final String MESSAGE = HelperUtils.uniqueString
        Realm realm = DomainFactory.createRealm()
        service.executionService = [
                executeCommand: {Realm realm2, String command->
                    assert false: MESSAGE
                }
        ] as ExecutionService
        service.createClusterScriptService = new CreateClusterScriptService()

        List<File> files = [
                tempFolder.newFolder(),
                tempFolder.newFile(),
        ]
        TestCase.shouldFailWithMessageContaining(AssertionError, MESSAGE) {
            service.deleteFilesRecursive(realm, files)
        }

        files.each {
            assert it.exists()
        }
    }

    @Test
    void testGetIlseFolder_nullInput_shouldFail() {
        TestCase.shouldFailWithMessageContaining(AssertionError, 'assert ilseId =~ /^\\d{4,6}$/') {
            service.getIlseFolder(null)
        }
    }

    @Test
    void testGetIlseFolder_emptyInput_shouldFail() {
        TestCase.shouldFailWithMessageContaining(AssertionError, 'assert ilseId =~ /^\\d{4,6}$/') {
            service.getIlseFolder('')
        }
    }


    @Test
    void testGetIlseFolder_inputHas3Digits_shouldFail() {
        TestCase.shouldFailWithMessageContaining(AssertionError, 'assert ilseId =~ /^\\d{4,6}$/') {
            service.getIlseFolder('123')
        }
    }

    @Test
    void testGetIlseFolder_inputHas4Digits_shouldReturnCorrectFile() {
        File expected = new File("${LsdfFilesService.SEQ_CENTER_INBOX_PATH}/core/001/001234")

        assert expected == service.getIlseFolder('1234')
    }

    @Test
    void testGetIlseFolder_inputHas6Digits_shouldReturnCorrectFile() {
        File expected = new File("${LsdfFilesService.SEQ_CENTER_INBOX_PATH}/core/123/123456")

        assert expected == service.getIlseFolder('123456')
    }

    @Test
    void testGetIlseFolder_inputHas7Digits_shouldFail() {
        TestCase.shouldFailWithMessageContaining(AssertionError, 'assert ilseId =~ /^\\d{4,6}$/') {
            service.getIlseFolder('1234567')
        }
    }

    @Test
    void testGetIlseFolder_inputContainsNonDigits_shouldFail() {
        TestCase.shouldFailWithMessageContaining(AssertionError, 'assert ilseId =~ /^\\d{4,6}$/') {
            service.getIlseFolder('12a3')
        }
    }


    private void testDeleteMethod(File file, Closure call) {
        Realm realm = new Realm()
        LsdfFilesService service = createInstanceWithMockedExecuteCommand(realm)

        LogThreadLocal.withThreadLog(new NullWriter()) {
            call(service, realm, file)
        }

        assert !file.exists()
    }

    private LsdfFilesService createInstanceWithMockedExecuteCommand(Realm expectedRealm) {
        LsdfFilesService service = new LsdfFilesService()
        service.executionService = [
                executeCommand: { Realm realm, String command ->
                    assert realm.is(expectedRealm)
                    return ['bash', '-c', command].execute().getText()
                }
        ] as ExecutionService
        return service
    }
}
