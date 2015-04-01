package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import org.junit.Test


/**
 */
class AbstractMergedBamFileTest {

    @Test
    void testConstraints_allFine_succeeds() {
        DomainFactory.createProcessedMergedBamFile()
    }

    @Test
    void testConstraints_numberOfMergedLanesIsZero_validationShouldFail() {
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile()
        bamFile.numberOfMergedLanes = 0
        TestCase.assertValidateError(bamFile, "numberOfMergedLanes", "min.notmet", 0)
    }
}
