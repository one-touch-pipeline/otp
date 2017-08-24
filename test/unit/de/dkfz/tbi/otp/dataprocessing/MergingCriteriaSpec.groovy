package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.mixin.*
import spock.lang.*

@Mock([
        MergingCriteria,
        Project,
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
}
