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

}

