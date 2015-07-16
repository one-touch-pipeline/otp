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

    @Test
    void testFileNotReadableExceptionStringParamIsNull() {
        FileNotReadableException e = new FileNotReadableException(null as String)
        assertEquals("can not read file: null", e.message)
    }

    public void testFileNotReadableExceptionFile() {
        FileNotReadableException e = new FileNotReadableException(new File("tmp"))
        assertEquals("can not read file: tmp", e.message)
    }

    @Test
    void testFileNotReadableExceptionFileParamIsNull() {
        FileNotReadableException e = new FileNotReadableException(null as File)
        assertEquals("can not read file: null", e.message)
    }
}
