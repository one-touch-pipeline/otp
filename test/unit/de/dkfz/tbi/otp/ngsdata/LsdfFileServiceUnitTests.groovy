package de.dkfz.tbi.otp.ngsdata

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import de.dkfz.tbi.otp.utils.WaitingFileUtils

class LsdfFileServiceUnitTests extends GroovyTestCase {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder()

    @Before
    void before() {
        WaitingFileUtils.defaultTimeoutMillis = 0L
    }

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
}
