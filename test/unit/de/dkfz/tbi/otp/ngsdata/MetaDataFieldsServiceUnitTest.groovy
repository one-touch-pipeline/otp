package de.dkfz.tbi.otp.ngsdata

import grails.buildtestdata.mixin.Build
import grails.test.mixin.*
import grails.test.mixin.services.ServiceUnitTestMixin
import grails.test.mixin.support.*
import org.junit.*

@TestFor(MetaDataFieldsService)
@Build([
    AntibodyTarget,
    ExomeEnrichmentKit,
    ExomeEnrichmentKitSynonym,
    SeqCenter,
    SeqPlatformModelIdentifier,
    SeqPlatform,
    SeqType,
])
class MetaDataFieldsServiceUnitTest {

    MetaDataFieldsService metaDataFieldsService

    @Before
    void setup() {
        metaDataFieldsService = new MetaDataFieldsService()
    }

    void test_listExomeEnrichmentKitWithAliases_oneDataWithoutSynonym() {
        ExomeEnrichmentKit exomeEnrichmentKit = ExomeEnrichmentKit.build()

        def list = metaDataFieldsService.listExomeEnrichmentKitWithAliases()
        assert [[exomeEnrichmentKit, []]] == list

    }

    void test_listExomeEnrichmentKitWithAliases_noData() {
        def list = metaDataFieldsService.listExomeEnrichmentKitWithAliases()
        assert [] == list
    }

    void test_listExomeEnrichmentKitWithAliases_oneDataWithOneSynonym() {
        ExomeEnrichmentKit exomeEnrichmentKit = ExomeEnrichmentKit.build()
        ExomeEnrichmentKitSynonym exomeEnrichmentKitSynonym = ExomeEnrichmentKitSynonym.build(
                exomeEnrichmentKit: exomeEnrichmentKit
                )
        def list = metaDataFieldsService.listExomeEnrichmentKitWithAliases()

        assert [[exomeEnrichmentKit, [exomeEnrichmentKitSynonym]]] == list
    }

    void test_listExomeEnrichmentKitWithAliases_multipleData() {
        ExomeEnrichmentKit exomeEnrichmentKit1 = ExomeEnrichmentKit.build()
        ExomeEnrichmentKit exomeEnrichmentKit2 = ExomeEnrichmentKit.build()
        ExomeEnrichmentKit exomeEnrichmentKit3 = ExomeEnrichmentKit.build()

        def list = metaDataFieldsService.listExomeEnrichmentKitWithAliases()
        assert [[exomeEnrichmentKit1, []], [exomeEnrichmentKit2, []], [exomeEnrichmentKit3,[]]] == list
    }

    void test_listExomeEnrichmentKitWithAliases_oneDataWithMultipleSynonym() {
        ExomeEnrichmentKit exomeEnrichmentKit = ExomeEnrichmentKit.build()
        ExomeEnrichmentKitSynonym exomeEnrichmentKitSynonym1 = ExomeEnrichmentKitSynonym.build(
                exomeEnrichmentKit: exomeEnrichmentKit
                )
        ExomeEnrichmentKitSynonym exomeEnrichmentKitSynonym2 = ExomeEnrichmentKitSynonym.build(
                exomeEnrichmentKit: exomeEnrichmentKit
                )
        ExomeEnrichmentKitSynonym exomeEnrichmentKitSynonym3 = ExomeEnrichmentKitSynonym.build(
                exomeEnrichmentKit: exomeEnrichmentKit
                )

        def list = metaDataFieldsService.listExomeEnrichmentKitWithAliases()
        assert [[exomeEnrichmentKit, [exomeEnrichmentKitSynonym1, exomeEnrichmentKitSynonym2, exomeEnrichmentKitSynonym3]]] == list
    }


    void test_listAntibodyTarget_oneData() {
        AntibodyTarget antibodyTarget = AntibodyTarget.build()

        def list = metaDataFieldsService.listAntibodyTarget()
        assert 1 == list.size()
        assert antibodyTarget == list[0]
    }

    void test_listAntibodyTarget_noData() {
        def list = metaDataFieldsService.listAntibodyTarget()
        assert [] == list
    }

    void test_listAntibodyTarget_multipleData() {
        AntibodyTarget antibodyTarget1 = AntibodyTarget.build()
        AntibodyTarget antibodyTarget2 = AntibodyTarget.build()
        AntibodyTarget antibodyTarget3 = AntibodyTarget.build()
        def list = metaDataFieldsService.listAntibodyTarget()
        assert 3 == list.size()
        assert list.contains(antibodyTarget1)
        assert list.contains(antibodyTarget2)
        assert list.contains(antibodyTarget3)
    }


    void test_listSeqCenter_oneData() {
        SeqCenter seqCenter = SeqCenter.build()
        def list = metaDataFieldsService.listSeqCenter()
        assert 1 == list.size()
        assert seqCenter == list[0]
    }

    void test_listSeqCenter_noData() {
        def list = metaDataFieldsService.listSeqCenter()
        assert [] == list
    }

    void test_listSeqCenter_multipleData() {
        SeqCenter seqCenter1 = SeqCenter.build()
        SeqCenter seqCenter2 = SeqCenter.build()
        SeqCenter seqCenter3 = SeqCenter.build()
        def list = metaDataFieldsService.listSeqCenter()
        assert 3 == list.size()
        assert list.contains(seqCenter1)
        assert list.contains(seqCenter2)
        assert list.contains(seqCenter3)
    }


    void test_listSeqPlatformWithAliases_oneDataWithoutSynonym() {
        SeqPlatform seqPlatform = SeqPlatform.build()

        def list = metaDataFieldsService.listPlatformAndIdentifier()
        assert [[seqPlatform, []]] == list

    }

    void test_listSeqPlatformWithAliases_noData() {
        def list = metaDataFieldsService.listPlatformAndIdentifier()
        assert [] == list
    }

    void test_listSeqPlatformWithAliases_oneDataWithOneSynonym() {
        SeqPlatform seqPlatform = SeqPlatform.build()
        SeqPlatformModelIdentifier seqPlatformModelIdentifier = SeqPlatformModelIdentifier.build(
                seqPlatform: seqPlatform
                )
        def list = metaDataFieldsService.listPlatformAndIdentifier()

        assert [[seqPlatform, [seqPlatformModelIdentifier]]] == list
    }

    void test_listSeqPlatformWithAliases_multipleData() {
        SeqPlatform seqPlatform1 = SeqPlatform.build()
        SeqPlatform seqPlatform2 = SeqPlatform.build()
        SeqPlatform seqPlatform3 = SeqPlatform.build()

        def list = metaDataFieldsService.listPlatformAndIdentifier()
        assert [[seqPlatform1, []], [seqPlatform2, []], [seqPlatform3,[]]] == list
    }

    void test_listSeqPlatformWithAliases_oneDataWithMultipleSynonym() {
        SeqPlatform seqPlatform = SeqPlatform.build()
        SeqPlatformModelIdentifier seqPlatformModelIdentifier1 = SeqPlatformModelIdentifier.build(
                seqPlatform: seqPlatform,
                )
        SeqPlatformModelIdentifier seqPlatformModelIdentifier2 = SeqPlatformModelIdentifier.build(
                seqPlatform: seqPlatform
                )
        SeqPlatformModelIdentifier seqPlatformModelIdentifier3 = SeqPlatformModelIdentifier.build(
                seqPlatform: seqPlatform
                )

        def list = metaDataFieldsService.listPlatformAndIdentifier()
        assert [[seqPlatform, [seqPlatformModelIdentifier1, seqPlatformModelIdentifier2, seqPlatformModelIdentifier3]]] == list
    }

    void test_listSeqType_oneData() {
        SeqType seqType = SeqType.build()
        def list = metaDataFieldsService.listSeqType()
        assert 1 == list.size()
        assert seqType == list[0]
    }

    void test_listSeqType_noData() {
        def list = metaDataFieldsService.listSeqType()
        assert [] == list
    }

    void test_listSeqType_multipleData() {
        SeqType seqType1 = SeqType.build()
        SeqType seqType2 = SeqType.build()
        SeqType seqType3 = SeqType.build()
        def list = metaDataFieldsService.listSeqType()
        assert 3 == list.size()
        assert list.contains(seqType1)
        assert list.contains(seqType2)
        assert list.contains(seqType3)
    }

}

