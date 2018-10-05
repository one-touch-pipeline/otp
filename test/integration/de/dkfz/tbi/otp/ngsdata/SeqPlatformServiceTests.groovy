package de.dkfz.tbi.otp.ngsdata

import org.junit.*

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.utils.CheckedLogger
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal


class SeqPlatformServiceTests {

    static final String PLATFORM_NAME = 'Some platform name'

    SeqPlatformService seqPlatformService

    CheckedLogger checkedLogger

    @Before
    void setUp() {
        checkedLogger = new CheckedLogger()
        LogThreadLocal.setThreadLog(checkedLogger)
    }

    @After
    void tearDown() {
        LogThreadLocal.removeThreadLog()
        checkedLogger.assertAllMessagesConsumed()
        checkedLogger = null
    }


    private static List createDataFor_findForNameAndModelAndSequencingKit() {
        final String OTHER_PLATFORM_NAME = 'Some other platform name'
        SeqPlatformModelLabel seqPlatformModelLabel = DomainFactory.createSeqPlatformModelLabel()
        SeqPlatformModelLabel seqPlatformModelLabel2 = DomainFactory.createSeqPlatformModelLabel()
        SequencingKitLabel sequencingKitLabel = DomainFactory.createSequencingKitLabel()
        SequencingKitLabel sequencingKitLabel2 = DomainFactory.createSequencingKitLabel()
        [
            PLATFORM_NAME,
            OTHER_PLATFORM_NAME,
        ].each { name ->
            [
                seqPlatformModelLabel,
                seqPlatformModelLabel2,
                null,
            ].each {model ->
                [
                    sequencingKitLabel,
                    sequencingKitLabel2,
                    null,
                ].each { kit ->
                    DomainFactory.createSeqPlatformWithSeqPlatformGroup([
                        name: name,
                        seqPlatformModelLabel: model,
                        sequencingKitLabel: kit,
                    ])
                }
            }
        }
        return [
            seqPlatformModelLabel,
            sequencingKitLabel,
        ]
    }


    @Test
    void test_findForNameAndModelAndSequencingKit_shouldReturnSeqplatform_PlatformNameAndModelAndKitGiven() {
        def (model, kit) = createDataFor_findForNameAndModelAndSequencingKit()

        SeqPlatform seqPlatform = seqPlatformService.findForNameAndModelAndSequencingKit(PLATFORM_NAME, model, kit)
        assert seqPlatform
        assert PLATFORM_NAME == seqPlatform.name
        assert model == seqPlatform.seqPlatformModelLabel
        assert kit == seqPlatform.sequencingKitLabel
    }

    @Test
    void test_findForNameAndModelAndSequencingKit_shouldReturnSeqplatform_PlatformNameAndModelGivenAndKitIsNull() {
        SeqPlatformModelLabel model = createDataFor_findForNameAndModelAndSequencingKit()[0] as SeqPlatformModelLabel

        SeqPlatform seqPlatform = seqPlatformService.findForNameAndModelAndSequencingKit(PLATFORM_NAME, model, null)
        assert seqPlatform
        assert PLATFORM_NAME == seqPlatform.name
        assert model == seqPlatform.seqPlatformModelLabel
        assert null == seqPlatform.sequencingKitLabel
    }

    @Test
    void test_findForNameAndModelAndSequencingKit_shouldReturnSeqplatform_PlatformNameAndKitGivenAndModelIsNull() {
        SequencingKitLabel kit = createDataFor_findForNameAndModelAndSequencingKit()[1] as SequencingKitLabel

        SeqPlatform seqPlatform = seqPlatformService.findForNameAndModelAndSequencingKit(PLATFORM_NAME, null, kit)
        assert seqPlatform
        assert PLATFORM_NAME == seqPlatform.name
        assert null == seqPlatform.seqPlatformModelLabel
        assert kit == seqPlatform.sequencingKitLabel
    }

    @Test
    void test_findForNameAndModelAndSequencingKit_shouldReturnSeqplatform_PlatformNameGivenAndModelAndKitIsNull() {
        createDataFor_findForNameAndModelAndSequencingKit()

        SeqPlatform seqPlatform = seqPlatformService.findForNameAndModelAndSequencingKit(PLATFORM_NAME, null, null)
        assert seqPlatform
        assert PLATFORM_NAME == seqPlatform.name
        assert null == seqPlatform.seqPlatformModelLabel
        assert null == seqPlatform.sequencingKitLabel
    }

    @Test
    void test_findForNameAndModelAndSequencingKit_shouldReturnSeqplatform_PlatformNameInOtherCaseAndModelAndKitGiven() {
        def (model, kit) = createDataFor_findForNameAndModelAndSequencingKit()

        SeqPlatform seqPlatform = seqPlatformService.findForNameAndModelAndSequencingKit(PLATFORM_NAME, model, kit)
        assert seqPlatform
        assert PLATFORM_NAME == seqPlatform.name
        assert model == seqPlatform.seqPlatformModelLabel
        assert kit == seqPlatform.sequencingKitLabel
    }

    @Test
    void test_findForNameAndModelAndSequencingKit_shouldReturnNull_UnknownPlatformNameGiven() {
        def (model, kit) = createDataFor_findForNameAndModelAndSequencingKit()

        SeqPlatform seqPlatform = seqPlatformService.findForNameAndModelAndSequencingKit('Some other name', model, kit)
        assert null == seqPlatform
    }

    @Test
    void test_findForNameAndModelAndSequencingKit_shouldReturnNull_UnknownModelGiven() {
        SequencingKitLabel kit = createDataFor_findForNameAndModelAndSequencingKit()[1] as SequencingKitLabel

        SeqPlatform seqPlatform = seqPlatformService.findForNameAndModelAndSequencingKit(PLATFORM_NAME, SeqPlatformModelLabel.build(), kit)
        assert null == seqPlatform
    }

    @Test
    void test_findForNameAndModelAndSequencingKit_shouldReturnNull_UnknownKitGiven() {
        SeqPlatformModelLabel model = createDataFor_findForNameAndModelAndSequencingKit()[0] as SeqPlatformModelLabel

        SeqPlatform seqPlatform = seqPlatformService.findForNameAndModelAndSequencingKit(PLATFORM_NAME, model, SequencingKitLabel.build())
        assert null == seqPlatform
    }

    @Test
    void test_findForNameAndModelAndSequencingKit_shouldFail_PlatformNameIsNull() {
        def (model, kit) = createDataFor_findForNameAndModelAndSequencingKit()

        TestCase.shouldFail(AssertionError) {
            seqPlatformService.findForNameAndModelAndSequencingKit(null, model, kit)
        }
    }

    @Test
    void test_findForNameAndModelAndSequencingKit_shouldFail_PlatformNameIsEmpty() {
        def (model, kit) = createDataFor_findForNameAndModelAndSequencingKit()

        TestCase.shouldFail(AssertionError) {
            seqPlatformService.findForNameAndModelAndSequencingKit('', model, kit)
        }
    }

    @Test
    void testCreateNewSeqPlatform_SeqPlatformExistsAlready_shouldFail() {
        DomainFactory.createSeqPlatformWithSeqPlatformGroup(
                name: PLATFORM_NAME,
                seqPlatformGroups: null,
                seqPlatformModelLabel: null,
                sequencingKitLabel: null,
        )
        TestCase.shouldFail(AssertionError) {
            SeqPlatformService.createNewSeqPlatform(PLATFORM_NAME)
        }
    }
}
