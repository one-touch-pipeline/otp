package de.dkfz.tbi.otp.ngsdata

import grails.buildtestdata.mixin.Build
import grails.test.mixin.*
import grails.test.mixin.services.ServiceUnitTestMixin
import grails.test.mixin.support.*
import org.junit.*

@TestFor(MetaDataFieldsService)
@Build([
    AntibodyTarget,
    LibraryPreparationKit,
    LibraryPreparationKitSynonym,
    SeqCenter,
    SeqPlatform,
    SeqType,
])
class MetaDataFieldsServiceUnitTest {

    MetaDataFieldsService metaDataFieldsService

    @Before
    void setup() {
        metaDataFieldsService = new MetaDataFieldsService()
    }

    void test_listLibraryPreparationKitWithAliases_oneDataWithoutSynonym() {
        LibraryPreparationKit libraryPreparationKit = LibraryPreparationKit.build()

        def list = metaDataFieldsService.listLibraryPreparationKitWithAliases()
        assert [[libraryPreparationKit, []]] == list

    }

    void test_listLibraryPreparationKitWithAliases_noData() {
        def list = metaDataFieldsService.listLibraryPreparationKitWithAliases()
        assert [] == list
    }

    void test_listLibraryPreparationKitWithAliases_oneDataWithOneSynonym() {
        LibraryPreparationKit libraryPreparationKit = LibraryPreparationKit.build()
        LibraryPreparationKitSynonym libraryPreparationKitSynonym = LibraryPreparationKitSynonym.build(
                libraryPreparationKit: libraryPreparationKit
                )
        def list = metaDataFieldsService.listLibraryPreparationKitWithAliases()

        assert [[libraryPreparationKit, [libraryPreparationKitSynonym]]] == list
    }

    void test_listLibraryPreparationKitWithAliases_multipleData() {
        LibraryPreparationKit libraryPreparationKit1 = LibraryPreparationKit.build()
        LibraryPreparationKit libraryPreparationKit2 = LibraryPreparationKit.build()
        LibraryPreparationKit libraryPreparationKit3 = LibraryPreparationKit.build()

        def list = metaDataFieldsService.listLibraryPreparationKitWithAliases()
        assert [[libraryPreparationKit1, []], [libraryPreparationKit2, []], [libraryPreparationKit3,[]]] == list
    }

    void test_listLibraryPreparationKitWithAliases_oneDataWithMultipleSynonym() {
        LibraryPreparationKit libraryPreparationKit = LibraryPreparationKit.build()
        LibraryPreparationKitSynonym libraryPreparationKitSynonym1 = LibraryPreparationKitSynonym.build(
                libraryPreparationKit: libraryPreparationKit
                )
        LibraryPreparationKitSynonym libraryPreparationKitSynonym2 = LibraryPreparationKitSynonym.build(
                libraryPreparationKit: libraryPreparationKit
                )
        LibraryPreparationKitSynonym libraryPreparationKitSynonym3 = LibraryPreparationKitSynonym.build(
                libraryPreparationKit: libraryPreparationKit
                )

        def list = metaDataFieldsService.listLibraryPreparationKitWithAliases()
        assert [[libraryPreparationKit, [libraryPreparationKitSynonym1, libraryPreparationKitSynonym2, libraryPreparationKitSynonym3]]] == list
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



    void test_listPlatforms_oneData() {
        SeqPlatform seqPlatform = SeqPlatform.build(seqPlatformModelLabel: null)

        def list = metaDataFieldsService.listPlatforms()
        assert [seqPlatform] == list

    }

    void test_listPlatforms_noData() {
        def list = metaDataFieldsService.listPlatforms()
        assert [] == list
    }

    void test_listPlatforms_multipleData() {
        SeqPlatform seqPlatform1 = SeqPlatform.build()
        SeqPlatform seqPlatform2 = SeqPlatform.build()
        SeqPlatform seqPlatform3 = SeqPlatform.build()

        def list = metaDataFieldsService.listPlatforms()
        assert [seqPlatform1, seqPlatform2, seqPlatform3] as Set == list as Set
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

