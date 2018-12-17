package de.dkfz.tbi.otp.dataprocessing

import grails.test.mixin.Mock
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.*

@Mock([
        MergingCriteria,
        Project,
        Realm,
        SeqType,
])
class MergingCriteriaSpec extends Specification {

    void "test that for Exome data LibPrepKit must be true"() {
        expect:
        DomainFactory.createMergingCriteriaLazy([
                seqType: DomainFactory.createExomeSeqType(),
                useLibPrepKit: true,
        ])
    }

    void "test that for Exome data LibPrepKit must be true, should fail when it is false"() {
        given:
        MergingCriteria mergingCriteria = DomainFactory.createMergingCriteria()
        SeqType seqType = DomainFactory.createExomeSeqType()

        when:
        mergingCriteria.useLibPrepKit = false
        mergingCriteria.seqType = seqType

        then:
        TestCase.assertValidateError(mergingCriteria, "useLibPrepKit", "In case of Exome data, the libraryPreparationKit must be part of the MergingCriteria", false)
    }

    void "test that for WGBS data LibPrepKit must be false"() {
        expect:
        DomainFactory.createMergingCriteriaLazy([
                seqType   : DomainFactory.createWholeGenomeBisulfiteSeqType(),
                useLibPrepKit: false,
        ])
    }

    void "test that for WGBS data LibPrepKit must be false, should fail when it is true"() {
        given:
        MergingCriteria mergingCriteria = DomainFactory.createMergingCriteria()
        SeqType seqType = DomainFactory.createWholeGenomeBisulfiteSeqType()

        when:
        mergingCriteria.useLibPrepKit = true
        mergingCriteria.seqType = seqType

        then:
        TestCase.assertValidateError(mergingCriteria, "useLibPrepKit", "In case of WGBS data, the libraryPreparationKit must not be part of the MergingCriteria", true)
    }
}
