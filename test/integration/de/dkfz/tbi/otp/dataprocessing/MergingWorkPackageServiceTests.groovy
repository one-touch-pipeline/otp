package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import org.junit.*

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage.MergingCriteria

class MergingWorkPackageServiceTests {

    MergingWorkPackageService mergingWorkPackageService

    @Test
    void testCreateWorkPackageAllCorrect() {
        MergingWorkPackage mergingWorkPackage = mergingWorkPackageService.createWorkPackage(ProcessedBamFile.build(), MergingCriteria.DEFAULT)
        assertNotNull(mergingWorkPackage)
    }

    @Test(expected = IllegalArgumentException)
    void testCreateWorkPackageBamFileNull() {
        mergingWorkPackageService.createWorkPackage(null, MergingCriteria.DEFAULT)
    }

    @Test(expected = IllegalArgumentException)
    void testCreateWorkPackageMergingCriteriaNull() {
        mergingWorkPackageService.createWorkPackage(ProcessedBamFile.build(), null)
    }
}
