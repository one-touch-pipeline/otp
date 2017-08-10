package de.dkfz.tbi.otp.ngsdata

import grails.buildtestdata.mixin.Build
import de.dkfz.tbi.TestCase
import org.junit.Ignore
import org.junit.Test
@Ignore("OTP-2607")
@Build([SeqPlatform, SeqPlatformGroup, SeqPlatformModelLabel, SequencingKitLabel])
class SeqPlatformServiceUnitTests {

    final String PLATFORM_NAME = "platform_name"

    @Test
    void testCreateNewSeqPlatform_SeqPlatformNameIsNull_shouldFail() {
        TestCase.shouldFail(AssertionError){
            SeqPlatformService.createNewSeqPlatform(null)
        }
    }

    @Test
    void testCreateNewSeqPlatform_OnlyNameIsProvided_AllFine() {
        SeqPlatform seqPlatform = SeqPlatformService.createNewSeqPlatform(PLATFORM_NAME)
        assert seqPlatform.name == PLATFORM_NAME
        assert seqPlatform.getSeqPlatformGroup(null) == null
        assert seqPlatform.seqPlatformModelLabel == null
        assert seqPlatform.sequencingKitLabel == null
    }

    @Test
    void testCreateNewSeqPlatform_NameAndSeqPlatformGroupAreProvided_AllFine() {
        SeqPlatformGroup seqPlatformGroup = SeqPlatformGroup.build()
        SeqPlatform seqPlatform = SeqPlatformService.createNewSeqPlatform(
                PLATFORM_NAME,
                seqPlatformGroup
        )
        assert seqPlatform.name == PLATFORM_NAME
        assert seqPlatform.getSeqPlatformGroup(null) == seqPlatformGroup
        assert seqPlatform.seqPlatformModelLabel == null
        assert seqPlatform.sequencingKitLabel == null
    }

    @Test
    void testCreateNewSeqPlatform_NameAndSeqPlatformGroupAndSeqPlatformModelLabelAreProvided_AllFine() {
        SeqPlatformGroup seqPlatformGroup = SeqPlatformGroup.build()
        SeqPlatformModelLabel seqPlatformModelLabel = SeqPlatformModelLabel.build()
        SeqPlatform seqPlatform = SeqPlatformService.createNewSeqPlatform(
                PLATFORM_NAME,
                seqPlatformGroup,
                seqPlatformModelLabel
        )
        assert seqPlatform.name == PLATFORM_NAME
        assert seqPlatform.getSeqPlatformGroup(null) == seqPlatformGroup
        assert seqPlatform.seqPlatformModelLabel == seqPlatformModelLabel
        assert seqPlatform.sequencingKitLabel == null
    }

    @Test
    void testCreateNewSeqPlatform_EverythingIsProvided_AllFine() {
        SeqPlatformGroup seqPlatformGroup = SeqPlatformGroup.build()
        SeqPlatformModelLabel seqPlatformModelLabel = SeqPlatformModelLabel.build()
        SequencingKitLabel sequencingKitLabel = SequencingKitLabel.build()
        SeqPlatform seqPlatform = SeqPlatformService.createNewSeqPlatform(
                PLATFORM_NAME,
                seqPlatformGroup,
                seqPlatformModelLabel,
                sequencingKitLabel
        )
        assert seqPlatform.name == PLATFORM_NAME
        assert seqPlatform.getSeqPlatformGroup(null) == seqPlatformGroup
        assert seqPlatform.seqPlatformModelLabel == seqPlatformModelLabel
        assert seqPlatform.sequencingKitLabel == sequencingKitLabel
    }
}
