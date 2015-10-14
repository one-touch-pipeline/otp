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

    @Test
    void test_listLibraryPreparationKitWithAliases_oneDataWithoutSynonym() {
        LibraryPreparationKit libraryPreparationKit = LibraryPreparationKit.build()

        def list = metaDataFieldsService.listLibraryPreparationKitWithAliases()
        assert [[libraryPreparationKit, []]] == list

    }

    @Test
    void test_listLibraryPreparationKitWithAliases_noData() {
        def list = metaDataFieldsService.listLibraryPreparationKitWithAliases()
        assert [] == list
    }

    @Test
    void test_listLibraryPreparationKitWithAliases_oneDataWithOneSynonym() {
        LibraryPreparationKit libraryPreparationKit = LibraryPreparationKit.build()
        LibraryPreparationKitSynonym libraryPreparationKitSynonym = LibraryPreparationKitSynonym.build(
                libraryPreparationKit: libraryPreparationKit
                )
        def list = metaDataFieldsService.listLibraryPreparationKitWithAliases()

        assert [[libraryPreparationKit, [libraryPreparationKitSynonym]]] == list
    }

    @Test
    void test_listLibraryPreparationKitWithAliases_multipleData() {
        LibraryPreparationKit libraryPreparationKit1 = LibraryPreparationKit.build()
        LibraryPreparationKit libraryPreparationKit2 = LibraryPreparationKit.build()
        LibraryPreparationKit libraryPreparationKit3 = LibraryPreparationKit.build()

        def list = metaDataFieldsService.listLibraryPreparationKitWithAliases()
        assert [[libraryPreparationKit1, []], [libraryPreparationKit2, []], [libraryPreparationKit3,[]]] == list
    }

    @Test
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


    @Test
    void test_listAntibodyTarget_oneData() {
        AntibodyTarget antibodyTarget = AntibodyTarget.build()

        def list = metaDataFieldsService.listAntibodyTarget()
        assert 1 == list.size()
        assert antibodyTarget == list[0]
    }

    @Test
    void test_listAntibodyTarget_noData() {
        def list = metaDataFieldsService.listAntibodyTarget()
        assert [] == list
    }

    @Test
    void test_listAntibodyTarget_multipleData() {
        AntibodyTarget antibodyTarget1 = DomainFactory.createAntibodyTarget()
        AntibodyTarget antibodyTarget2 = DomainFactory.createAntibodyTarget()
        AntibodyTarget antibodyTarget3 = DomainFactory.createAntibodyTarget()
        def list = metaDataFieldsService.listAntibodyTarget()
        assert 3 == list.size()
        assert list.contains(antibodyTarget1)
        assert list.contains(antibodyTarget2)
        assert list.contains(antibodyTarget3)
    }


    @Test
    void test_listSeqCenter_oneData() {
        SeqCenter seqCenter = SeqCenter.build()
        def list = metaDataFieldsService.listSeqCenter()
        assert 1 == list.size()
        assert seqCenter == list[0]
    }

    @Test
    void test_listSeqCenter_noData() {
        def list = metaDataFieldsService.listSeqCenter()
        assert [] == list
    }

    @Test
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



    @Test
    void test_listPlatforms_oneData() {
        SeqPlatform seqPlatform = SeqPlatform.build(seqPlatformModelLabel: null)

        def list = metaDataFieldsService.listPlatforms()
        assert [seqPlatform] == list

    }

    @Test
    void test_listPlatforms_noData() {
        def list = metaDataFieldsService.listPlatforms()
        assert [] == list
    }

    @Test
    void test_listPlatforms_multipleData() {
        SeqPlatform seqPlatform1 = SeqPlatform.build()
        SeqPlatform seqPlatform2 = SeqPlatform.build()
        SeqPlatform seqPlatform3 = SeqPlatform.build()

        def list = metaDataFieldsService.listPlatforms()
        assert [seqPlatform1, seqPlatform2, seqPlatform3] as Set == list as Set
    }



    @Test
    void test_listSeqType_oneData() {
        SeqType seqType = SeqType.build()
        def list = metaDataFieldsService.listSeqType()
        assert 1 == list.size()
        assert seqType == list[0]
    }

    @Test
    void test_listSeqType_noData() {
        def list = metaDataFieldsService.listSeqType()
        assert [] == list
    }

    @Test
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

