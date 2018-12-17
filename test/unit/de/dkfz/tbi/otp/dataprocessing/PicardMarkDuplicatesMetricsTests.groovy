package de.dkfz.tbi.otp.dataprocessing

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.junit.Assert
import org.junit.Test

@TestFor(PicardMarkDuplicatesMetrics)
@Mock([MockAbstractBamFile])
class PicardMarkDuplicatesMetricsTests {



    @Test
    void testMetricsClassEmpty() {
        AbstractBamFile bamFile = new MockAbstractBamFile()
        bamFile.save(flush: true)

        PicardMarkDuplicatesMetrics picardMarkDuplicatesMetrics = new PicardMarkDuplicatesMetrics(
                        library: "testlibrary",
                        abstractBamFile: bamFile
                        )
        Assert.assertFalse(picardMarkDuplicatesMetrics.validate())
    }


    @Test
    void testLibraryEmpty() {
        AbstractBamFile bamFile = new MockAbstractBamFile()
        bamFile.save(flush: true)

        PicardMarkDuplicatesMetrics picardMarkDuplicatesMetrics = new PicardMarkDuplicatesMetrics(
                        metricsClass: "testmetricsClass",
                        abstractBamFile: bamFile
                        )
        Assert.assertFalse(picardMarkDuplicatesMetrics.validate())
    }


    @Test
    void testNoBamFile() {
        PicardMarkDuplicatesMetrics picardMarkDuplicatesMetrics = new PicardMarkDuplicatesMetrics(
                        metricsClass: "testmetricsClass",
                        library: "testlibrary",
                        )
        Assert.assertFalse(picardMarkDuplicatesMetrics.validate())
    }


    @Test
    void testAllCorrect() {
        AbstractBamFile bamFile = new MockAbstractBamFile()
        bamFile.save(flush: true)

        PicardMarkDuplicatesMetrics picardMarkDuplicatesMetrics = new PicardMarkDuplicatesMetrics(
                        metricsClass: "testmetricsClass",
                        library: "testlibrary",
                        abstractBamFile: bamFile
                        )
        Assert.assertTrue(picardMarkDuplicatesMetrics.validate())
        picardMarkDuplicatesMetrics.save(flush: true)
    }
}

