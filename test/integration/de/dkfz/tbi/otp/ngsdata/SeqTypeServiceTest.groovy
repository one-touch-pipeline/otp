package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.*

import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.grails.plugins.springsecurity.service.acl.AclUtilService
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.acls.domain.BasePermission

import de.dkfz.tbi.otp.testing.AbstractIntegrationTest


class SeqTypeServiceTest extends AbstractIntegrationTest {

    SeqTypeService seqTypeService

    AclUtilService aclUtilService



    void setUp() {
        createUserAndRoles()
    }



    void testAlignableSeqTypesByProject_admin_none() {
        DomainFactory.createAlignableSeqTypes()
        SeqTrack seqTrack = SeqTrack.build()

        SpringSecurityUtils.doWithAuth("admin") {
            List<SeqType> seqtypes = seqTypeService.alignableSeqTypesByProject(seqTrack.project)
            assert 0 == seqtypes.size()
        }
    }


    void testAlignableSeqTypesByProject_operator_none() {
        DomainFactory.createAlignableSeqTypes()
        SeqTrack seqTrack = SeqTrack.build()

        SpringSecurityUtils.doWithAuth("operator") {
            List<SeqType> seqtypes = seqTypeService.alignableSeqTypesByProject(seqTrack.project)
            assert 0 == seqtypes.size()
        }
    }


    void testAlignableSeqTypesByProject_normalUserWithAccess_none() {
        DomainFactory.createAlignableSeqTypes()
        SeqTrack seqTrack = SeqTrack.build()
        SpringSecurityUtils.doWithAuth("admin") {
            aclUtilService.addPermission(seqTrack.project, "user", BasePermission.READ)
        }

        SpringSecurityUtils.doWithAuth("user") {
            List<SeqType> seqtypes = seqTypeService.alignableSeqTypesByProject(seqTrack.project)
            assert 0 == seqtypes.size()
        }
    }


    void testAlignableSeqTypesByProject_normalUserWithoutAccess_none() {
        DomainFactory.createAlignableSeqTypes()
        SeqTrack seqTrack = SeqTrack.build()

        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                seqTypeService.alignableSeqTypesByProject(seqTrack.project)
            }
        }
    }


    void testAlignableSeqTypesByProject_operator_one() {
        List<SeqType> alignableSeqTypes = DomainFactory.createAlignableSeqTypes()
        SeqTrack seqTrack = SeqTrack.build()
        SeqTrack.build(sample: seqTrack.sample)
        SeqTrack.build(sample: seqTrack.sample, seqType: alignableSeqTypes[0])
        SeqTrack.build(sample: seqTrack.sample, seqType: alignableSeqTypes[0])

        SpringSecurityUtils.doWithAuth("operator") {
            List<SeqType> seqtypes = seqTypeService.alignableSeqTypesByProject(seqTrack.project)
            assert 1 == seqtypes.size()
        }
    }


    void testAlignableSeqTypesByProject_operator_two() {
        List<SeqType> alignableSeqTypes = DomainFactory.createAlignableSeqTypes()
        SeqTrack seqTrack = SeqTrack.build()
        SeqTrack.build(sample: seqTrack.sample)
        SeqTrack.build(sample: seqTrack.sample, seqType: alignableSeqTypes[0])
        SeqTrack.build(sample: seqTrack.sample, seqType: alignableSeqTypes[0])
        SeqTrack.build(sample: seqTrack.sample, seqType: alignableSeqTypes[1])
        SeqTrack.build(sample: seqTrack.sample, seqType: alignableSeqTypes[1])

        SpringSecurityUtils.doWithAuth("operator") {
            List<SeqType> seqtypes = seqTypeService.alignableSeqTypesByProject(seqTrack.project)
            assert 2 == seqtypes.size()
        }
    }
}
