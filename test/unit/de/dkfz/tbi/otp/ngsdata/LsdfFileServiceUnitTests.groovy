package de.dkfz.tbi.otp.ngsdata

import org.codehaus.groovy.control.io.NullWriter

import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import grails.test.mixin.TestFor
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@TestFor(LsdfFilesService)
class LsdfFileServiceUnitTests {

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
        assert shouldFail(AssertionError, {LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)}) =~ /(?i)exists/
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
