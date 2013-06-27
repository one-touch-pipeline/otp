package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*

import grails.test.mixin.*
import org.junit.*

/**
 * The DOM has been created long time ago.
 * Here only possibility to create and save DOM is tested
 * 
 *
 */
@TestFor(AbstractBamFile)
class AbstractBamFileTests {

    void testSave() {
        AbstractBamFile bamFile = new AbstractBamFile(
            type: AbstractBamFile.BamType.SORTED)
        Assert.assertTrue(bamFile.validate())
        bamFile.save(flush: true)
    }

    void testQualityControlIsNotNull() {
        AbstractBamFile bamFile = new AbstractBamFile(
            type: AbstractBamFile.BamType.SORTED)
        bamFile.qualityControl =  null
        Assert.assertFalse(bamFile.validate())
    }
}
