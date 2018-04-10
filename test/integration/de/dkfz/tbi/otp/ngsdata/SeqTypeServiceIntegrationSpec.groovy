package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.testing.*
import spock.lang.Specification
import spock.lang.Shared
import grails.plugin.springsecurity.*
import grails.plugin.springsecurity.acl.*
import org.springframework.security.access.*
import org.springframework.security.acls.domain.*
import org.springframework.security.access.AccessDeniedException

class SeqTypeServiceIntegrationSpec extends Specification implements UserAndRoles {

    SeqTypeService seqTypeService

    @Shared
    List<SeqType> alignableSeqTypes

    SeqTrack seqTrack

    void setupSpec() {
        alignableSeqTypes = DomainFactory.createAllAlignableSeqTypes()
    }

    void setup() {
        createUserAndRoles()
        seqTrack = DomainFactory.createSeqTrack()
        addUserToProject("user", seqTrack.project)
    }

    void cleanupSpec() {
        alignableSeqTypes*.delete(flush: true)
    }


    void "test testAlignableSeqTypesByProject with different users, when no seqType is connected to the project"() {
        given:
        List<SeqType> seqTypes = []

        when:
        SpringSecurityUtils.doWithAuth(user) {
            seqTypes = seqTypeService.alignableSeqTypesByProject(seqTrack.project)
        }

        then:
        0 == seqTypes.size()

        where:
        user       | _
        "admin"    | _
        "operator" | _
        "user"     | _
    }

    void "test testAlignableSeqTypesByProject with different users, when one seqType is connected to the project"() {
        given:
        List<SeqType> seqTypes = []
        DomainFactory.createSeqTrack(sample: seqTrack.sample)
        DomainFactory.createSeqTrack(sample: seqTrack.sample, seqType: alignableSeqTypes[0])
        DomainFactory.createSeqTrack(sample: seqTrack.sample, seqType: alignableSeqTypes[0])

        when:
        SpringSecurityUtils.doWithAuth(user) {
            seqTypes = seqTypeService.alignableSeqTypesByProject(seqTrack.project)
        }

        then:
        1 == seqTypes.size()

        where:
        user       | _
        "admin"    | _
        "operator" | _
        "user"     | _
    }

    void "test testAlignableSeqTypesByProject with different users, when two seqTypes are connected to the project"() {
        given:
        List<SeqType> seqTypes = []
        DomainFactory.createSeqTrack(sample: seqTrack.sample)
        DomainFactory.createSeqTrack(sample: seqTrack.sample, seqType: alignableSeqTypes[0])
        DomainFactory.createSeqTrack(sample: seqTrack.sample, seqType: alignableSeqTypes[0])
        DomainFactory.createSeqTrack(sample: seqTrack.sample, seqType: alignableSeqTypes[1])
        DomainFactory.createSeqTrack(sample: seqTrack.sample, seqType: alignableSeqTypes[1])

        when:
        SpringSecurityUtils.doWithAuth(user) {
            seqTypes = seqTypeService.alignableSeqTypesByProject(seqTrack.project)
        }

        then:
        2 == seqTypes.size()

        where:
        user       | _
        "admin"    | _
        "operator" | _
        "user"     | _
    }

    void "test testAlignableSeqTypesByProject with user that has no access to the project, throws exception"()  {
        when:
        SpringSecurityUtils.doWithAuth("testuser") {
            seqTypeService.alignableSeqTypesByProject(seqTrack.project)
        }

        then:
        thrown(AccessDeniedException)
    }
}
