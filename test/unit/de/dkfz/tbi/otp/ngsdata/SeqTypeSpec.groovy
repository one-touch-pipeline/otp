package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.*
import grails.test.mixin.*
import org.junit.*
import spock.lang.Specification

@Mock([
        SeqType,
])
class SeqTypeSpec extends Specification {

    void "test get WGS Paired SeqType, no SeqType in DB, should fail"() {
        when:
        SeqType.wholeGenomePairedSeqType

        then:
        AssertionError e = thrown()
        e.message.contains('WGS PAIRED not found')
    }

    void "test get WGS Paired SeqType, no WGS Paired SeqType in DB, should fail"() {
        when:
        DomainFactory.createWholeGenomeSeqType(SeqType.LIBRARYLAYOUT_SINGLE)
        SeqType.wholeGenomePairedSeqType

        then:
        AssertionError e = thrown()
        e.message.contains('WGS PAIRED not found')
    }

    void "test get WGS Paired SeqType, All Fine"() {
        when:
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()

        then:
        seqType == SeqType.wholeGenomePairedSeqType
    }


    void "test get Exome Paired SeqType, no SeqType in DB, should fail"() {
        when:
        SeqType.exomePairedSeqType

        then:
        AssertionError e = thrown()
        e.message.contains('WES PAIRED not found')
    }

    void "test get Exome Paired SeqType, no Exome Paired SeqType in DB, should fail"() {
        when:
        DomainFactory.createExomeSeqType(SeqType.LIBRARYLAYOUT_SINGLE)
        SeqType.exomePairedSeqType

        then:
        AssertionError e = thrown()
        e.message.contains('WES PAIRED not found')
    }

    void "test get Exome Paired SeqType, AllFine"() {
        when:
        SeqType seqType = DomainFactory.createExomeSeqType()

        then:
        seqType == SeqType.exomePairedSeqType
    }

    void "test create SeqTypes with unique Name And LibraryLayout combination, all fine"() {
        when:
        DomainFactory.createSeqType(
                name         : "seqTypeName1",
                libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED,
                dirName      : "name1",
        )
        DomainFactory.createSeqType(
                name         : "seqTypeName1",
                libraryLayout: SeqType.LIBRARYLAYOUT_SINGLE,
                dirName      : "name1",
        )
        DomainFactory.createSeqType(
                name: "seqTypeName2",
                libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED,
        )

        then:
        SeqType.findAll().size() == 3
    }

    void "test create SeqTypes with non unique Name, dirName and LibraryLayout combination, should fail"() {
        when:
        DomainFactory.createSeqType(
                name         : "seqTypeName1",
                libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED,
                dirName      : "name1",
        )
        SeqType seqType = DomainFactory.createSeqType([
                name         : "seqTypeName1",
                libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED,
                dirName      : "name1",
        ], false)

        then:
        TestCase.assertAtLeastExpectedValidateError(seqType, "libraryLayout", "unique", LibraryLayout.PAIRED.name())
        TestCase.assertAtLeastExpectedValidateError(seqType, "dirName", "unique", "name1")
    }

    void "test create SeqTypes with non unique Name and different dirName and LibraryLayout combination, should fail"() {
        when:
        DomainFactory.createSeqType(
                name         : "seqTypeName1",
                libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED,
                dirName      : "name1",
        )
        SeqType seqType = DomainFactory.createSeqType([
                name         : "seqTypeName1",
                libraryLayout: SeqType.LIBRARYLAYOUT_SINGLE,
                dirName      : "name2",
        ], false)

        then:
        TestCase.assertValidateError(seqType, "dirName", "for same name and single cell, the dir name should be the same", "name2")
    }

    void "test create SeqTypes with unique Name, singleCell and LibraryLayout but same dirName combination, should fail"() {
        when:
        DomainFactory.createSeqType(
                name         : "seqTypeName1",
                libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED,
                singleCell   : true,
                dirName      : "name1",
        )
        SeqType seqType = DomainFactory.createSeqType([
                name         : "seqTypeName2",
                libraryLayout: SeqType.LIBRARYLAYOUT_SINGLE,
                singleCell   : false,
                dirName      : "name1",
        ], false)

        then:
        TestCase.assertValidateError(seqType, "dirName", "dir name constraint", "name1")
    }

    void "test create SeqType with non valid dirName, should fail"() {
        when:
        SeqType seqType = DomainFactory.createSeqType([
                name         : "seqTypeName1",
                libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED,
                dirName      : "name1\\",
        ], false)

        then:
        TestCase.assertValidateError(seqType, "dirName", "no valid path component", "name1\\")
    }
}
