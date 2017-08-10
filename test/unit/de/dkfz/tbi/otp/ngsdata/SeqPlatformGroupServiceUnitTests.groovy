package de.dkfz.tbi.otp.ngsdata

import grails.buildtestdata.mixin.Build
import de.dkfz.tbi.TestCase
import org.junit.Ignore
import org.junit.Test

/**
 */
@Build([SeqPlatformGroup])
@Ignore("OTP-2607")
class SeqPlatformGroupServiceUnitTests {

    final String GROUP_NAME = "testGroup"

    @Test
    void testCreateNewSeqPlatformGroup_InputNameIsNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            SeqPlatformGroupService.createNewSeqPlatformGroup(null)

        }
    }

    @Test
    void testCreateNewSeqPlatformGroup_GroupExistsAlready_ShouldFail() {
        SeqPlatformGroup.build()
        TestCase.shouldFail(AssertionError) {
            SeqPlatformGroupService.createNewSeqPlatformGroup(GROUP_NAME)
        }
    }

    @Test
    void testCreateNewSeqPlatformGroup_AllFine() {
        assert SeqPlatformGroupService.createNewSeqPlatformGroup(GROUP_NAME) ==
                SeqPlatformGroup.findByName(GROUP_NAME)
    }
}
