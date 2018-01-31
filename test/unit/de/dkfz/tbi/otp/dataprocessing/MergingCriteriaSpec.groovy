package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.mixin.*
import spock.lang.*

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
                libPrepKit: true,
        ])
    }

    void "test that for Exome data LibPrepKit must be true, should fail when it is false"() {
        given:
        MergingCriteria mergingCriteria = DomainFactory.createMergingCriteriaLazy()
        SeqType seqType = DomainFactory.createExomeSeqType()

        when:
        mergingCriteria.libPrepKit = false
        mergingCriteria.seqType = seqType

        then:
        TestCase.assertValidateError(mergingCriteria, "libPrepKit", "In case of Exome data, the libraryPreparationKit must be part of the MergingCriteria", false)
    }

    void "test that for WGBS data LibPrepKit must be false"() {
        expect:
        DomainFactory.createMergingCriteriaLazy([
                seqType   : DomainFactory.createWholeGenomeBisulfiteSeqType(),
                libPrepKit: false,
        ])
    }

    void "test that for WGBS data LibPrepKit must be false, should fail when it is true"() {
        given:
        MergingCriteria mergingCriteria = DomainFactory.createMergingCriteriaLazy()
        SeqType seqType = DomainFactory.createWholeGenomeBisulfiteSeqType()

        when:
        mergingCriteria.libPrepKit = true
        mergingCriteria.seqType = seqType

        then:
        TestCase.assertValidateError(mergingCriteria, "libPrepKit", "In case of WGBS data, the libraryPreparationKit must not be part of the MergingCriteria", true)
    }
}
