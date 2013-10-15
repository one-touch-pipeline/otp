package de.dkfz.tbi.ngstools.qualityAssessment

import javax.validation.*
import org.junit.*

class FileValidatorUnitTests {

    File file

    class Params {
        String name
        @FileCanRead
        String path
    }

    @Before
    void setUp() {
        file = new File("/tmp/test-file-validator")
        file << "some text"
    }

    @After
    public void tearDown() throws Exception {
        file.delete()
    }

    @Test(expected = ValidationException.class)
    void testPropertyFound() {
        Params invalidParams = new Params(name: "name", path: "path")
        FileValidator.validate(invalidParams)
    }

    @Test
    void testCorrect() {
        Params correctParams = new Params(name: "name", path: file.getAbsolutePath())
        FileValidator.validate(correctParams)
    }

    @Test(expected = ValidationException.class)
    void testCanNotRead() {
        file.delete()
        Params invalidParams = new Params(name: "name", path: file.getAbsolutePath())
        FileValidator.validate(invalidParams)
    }

    @Test(expected = ValidationException.class)
    void testFileSizeZero() {
        file.delete()
        file << ""
        Params invalidParams = new Params(name: "name", path: file.getAbsolutePath())
        FileValidator.validate(invalidParams)
    }

    @Test(expected = ValidationException.class)
    void testFileNameIsNull() {
        Params invalidParams = new Params(name: "name", path: null)
        FileValidator.validate(invalidParams)
    }

    @Test(expected = ValidationException.class)
    void testFileNameIsEmpty() {
        Params invalidParams = new Params(name: "name", path: "")
        FileValidator.validate(invalidParams)
    }

    @Test(expected = ValidationException.class)
    void testNotNormalFile() {
        Params invalidParams = new Params(name: "name", path: "/tmp")
        FileValidator.validate(invalidParams)
    }
}
