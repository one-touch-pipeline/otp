package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.*
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.*

@TestMixin(GrailsUnitTestMixin)
class FileNotReadableExceptionUnitTests {

    public void testFileNotReadableExceptionString() {
        FileNotReadableException e = new FileNotReadableException("tmp")
        assertEquals("can not read file: tmp", e.message)
    }

    public void testFileNotReadableExceptionStringParamIsNull() {
        String path = null
        FileNotReadableException e = new FileNotReadableException(path)
        assertEquals("can not read file: null", e.message)
    }

    public void testFileNotReadableExceptionFile() {
        FileNotReadableException e = new FileNotReadableException(new File("tmp"))
        assertEquals("can not read file: tmp", e.message)
    }

    public void testFileNotReadableExceptionFileParamIsNull() {
        File path = null
        FileNotReadableException e = new FileNotReadableException(path)
        assertEquals("can not read file: null", e.message)
    }
}
