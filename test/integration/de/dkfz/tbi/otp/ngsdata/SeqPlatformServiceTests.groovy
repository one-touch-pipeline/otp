package de.dkfz.tbi.otp.ngsdata

import org.junit.*

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.utils.CheckedLogger
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal


class SeqPlatformServiceTests {

    static final String PLATFORM_NAME = 'Some platform name'
    static final String MODEL_NAME = 'Some model name'
    static final String KIT_NAME = 'Some kit name'



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



    private List createDataFor_findForNameAndModelAndSequencingKit() {
        final String OTHER_PLATFORM_NAME = 'Some other platform name'
        SeqPlatformModelLabel seqPlatformModelLabel = SeqPlatformModelLabel.build()
        SeqPlatformModelLabel seqPlatformModelLabel2 = SeqPlatformModelLabel.build()
        SequencingKitLabel sequencingKitLabel = SequencingKitLabel.build()
        SequencingKitLabel sequencingKitLabel2 = SequencingKitLabel.build()
        [
            PLATFORM_NAME,
            OTHER_PLATFORM_NAME
        ].each { name ->
            [
                seqPlatformModelLabel,
                seqPlatformModelLabel2,
                null
            ].each {model ->
                [
                    sequencingKitLabel,
                    sequencingKitLabel2,
                    null
                ].each {kit->
                    DomainFactory.createSeqPlatform([
                        name: name,
                        seqPlatformModelLabel: model,
                        sequencingKitLabel: kit,
                    ])
                }
            }
        }
        return [
            seqPlatformModelLabel,
            sequencingKitLabel
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
        def (model, kit) = createDataFor_findForNameAndModelAndSequencingKit()

        SeqPlatform seqPlatform = seqPlatformService.findForNameAndModelAndSequencingKit(PLATFORM_NAME, model, null)
        assert seqPlatform
        assert PLATFORM_NAME == seqPlatform.name
        assert model == seqPlatform.seqPlatformModelLabel
        assert null == seqPlatform.sequencingKitLabel
    }

    @Test
    void test_findForNameAndModelAndSequencingKit_shouldReturnSeqplatform_PlatformNameAndKitGivenAndModelIsNull() {
        def (model, kit) = createDataFor_findForNameAndModelAndSequencingKit()

        SeqPlatform seqPlatform = seqPlatformService.findForNameAndModelAndSequencingKit(PLATFORM_NAME, null, kit)
        assert seqPlatform
        assert PLATFORM_NAME == seqPlatform.name
        assert null == seqPlatform.seqPlatformModelLabel
        assert kit == seqPlatform.sequencingKitLabel
    }

    @Test
    void test_findForNameAndModelAndSequencingKit_shouldReturnSeqplatform_PlatformNameGivenAndModelAndKitIsNull() {
        def (model, kit) = createDataFor_findForNameAndModelAndSequencingKit()

        SeqPlatform seqPlatform = seqPlatformService.findForNameAndModelAndSequencingKit(PLATFORM_NAME, null, null)
        assert seqPlatform
        assert PLATFORM_NAME == seqPlatform.name
        assert null == seqPlatform.seqPlatformModelLabel
        assert null == seqPlatform.sequencingKitLabel
    }

    @Test
    void test_findForNameAndModelAndSequencingKit_shouldReturnSeqplatform_PlatformNameInOtherCaseAndModelAndKitGiven() {
        def (model, kit) = createDataFor_findForNameAndModelAndSequencingKit()
        String nameUpperCase = PLATFORM_NAME.toUpperCase()

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
        def (model, kit) = createDataFor_findForNameAndModelAndSequencingKit()

        SeqPlatform seqPlatform = seqPlatformService.findForNameAndModelAndSequencingKit(PLATFORM_NAME, SeqPlatformModelLabel.build(), kit)
        assert null == seqPlatform
    }

    @Test
    void test_findForNameAndModelAndSequencingKit_shouldReturnNull_UnknownKitGiven() {
        def (model, kit) = createDataFor_findForNameAndModelAndSequencingKit()

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



    private List createDataFor_validateSeqPlatform(Map map = [:], boolean createSecondDataFile = false) {
        Run run = Run.build()

        String platform = PLATFORM_NAME
        String model =  MODEL_NAME
        String kit = KIT_NAME

        if (map.containsKey('platform')) {
            platform = map.get('platform')
        }
        if (map.containsKey('model')) {
            model = map.get('model')
        }
        if (map.containsKey('kit')) {
            kit = map.get('kit')
        }

        MetaDataKey platformKey = MetaDataKey.build(name: MetaDataColumn.INSTRUMENT_PLATFORM.name())
        MetaDataKey modelKey = MetaDataKey.build(name: MetaDataColumn.INSTRUMENT_MODEL.name())
        MetaDataKey sequencingKit = MetaDataKey.build(name: MetaDataColumn.SEQUENCING_KIT.name())

        DataFile dataFile = DataFile.build(run: run)
        MetaDataEntry.build(dataFile: dataFile, key: platformKey, value: platform)
        MetaDataEntry.build(dataFile: dataFile, key: modelKey, value: model)
        MetaDataEntry.build(dataFile: dataFile, key: sequencingKit, value: kit)

        SeqPlatformModelLabel seqPlatformModelLabel = SeqPlatformModelLabel.build(name: MODEL_NAME)
        SequencingKitLabel sequencingKitLabel = SequencingKitLabel.build(name: KIT_NAME)
        DomainFactory.createSeqPlatform([
            name: PLATFORM_NAME,
            seqPlatformModelLabel: seqPlatformModelLabel,
            sequencingKitLabel: sequencingKitLabel,
        ])


        if (createSecondDataFile) {
            //second file with other platform
            dataFile = DataFile.build(run: run)
            MetaDataEntry.build(dataFile: dataFile, key: platformKey, value: platform)
            MetaDataEntry.build(dataFile: dataFile, key: modelKey, value: model)
            DomainFactory.createSeqPlatform([
                name: PLATFORM_NAME,
                seqPlatformModelLabel: seqPlatformModelLabel,
                sequencingKitLabel: null,
            ])
        }

        return [run, dataFile]
    }

    @Test
    void testCreateNewSeqPlatform_SeqPlatformExistsAlready_shouldFail() {
        DomainFactory.createSeqPlatform(
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
