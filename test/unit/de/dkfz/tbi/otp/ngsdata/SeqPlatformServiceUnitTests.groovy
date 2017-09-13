package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.*
import grails.buildtestdata.mixin.*
import org.junit.*

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
        assert seqPlatform.seqPlatformModelLabel == null
        assert seqPlatform.sequencingKitLabel == null
    }

    @Test
    void testCreateNewSeqPlatform_NameAndSeqPlatformGroupAreProvided_AllFine() {
        SeqPlatform seqPlatform = SeqPlatformService.createNewSeqPlatform(
                PLATFORM_NAME,
        )
        assert seqPlatform.name == PLATFORM_NAME
        assert seqPlatform.seqPlatformModelLabel == null
        assert seqPlatform.sequencingKitLabel == null
    }

    @Test
    void testCreateNewSeqPlatform_NameAndSeqPlatformGroupAndSeqPlatformModelLabelAreProvided_AllFine() {
        SeqPlatformModelLabel seqPlatformModelLabel = SeqPlatformModelLabel.build()
        SeqPlatform seqPlatform = SeqPlatformService.createNewSeqPlatform(
                PLATFORM_NAME,
                seqPlatformModelLabel
        )
        assert seqPlatform.name == PLATFORM_NAME
        assert seqPlatform.seqPlatformModelLabel == seqPlatformModelLabel
        assert seqPlatform.sequencingKitLabel == null
    }

    @Test
    void testCreateNewSeqPlatform_EverythingIsProvided_AllFine() {
        SeqPlatformModelLabel seqPlatformModelLabel = SeqPlatformModelLabel.build()
        SequencingKitLabel sequencingKitLabel = SequencingKitLabel.build()
        SeqPlatform seqPlatform = SeqPlatformService.createNewSeqPlatform(
                PLATFORM_NAME,
                seqPlatformModelLabel,
                sequencingKitLabel
        )
        assert seqPlatform.name == PLATFORM_NAME
        assert seqPlatform.seqPlatformModelLabel == seqPlatformModelLabel
        assert seqPlatform.sequencingKitLabel == sequencingKitLabel
    }
}
