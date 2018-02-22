package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.security.User
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.acl.AclUtilService
import org.junit.*
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.acls.domain.BasePermission

import de.dkfz.tbi.otp.testing.AbstractIntegrationTest


class SeqTypeServiceTest extends AbstractIntegrationTest {

    SeqTypeService seqTypeService

    AclUtilService aclUtilService

    @Before
    void setUp() {
        createUserAndRoles()
    }


    @Test
    void testAlignableSeqTypesByProject_admin_none() {
        DomainFactory.createAllAlignableSeqTypes()
        SeqTrack seqTrack = DomainFactory.createSeqTrack()

        SpringSecurityUtils.doWithAuth(ADMIN) {
            List<SeqType> seqtypes = seqTypeService.alignableSeqTypesByProject(seqTrack.project)
            assert 0 == seqtypes.size()
        }
    }


    @Test
    void testAlignableSeqTypesByProject_operator_none() {
        DomainFactory.createAllAlignableSeqTypes()
        SeqTrack seqTrack = DomainFactory.createSeqTrack()

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            List<SeqType> seqtypes = seqTypeService.alignableSeqTypesByProject(seqTrack.project)
            assert 0 == seqtypes.size()
        }
    }


    @Test
    void testAlignableSeqTypesByProject_normalUserWithAccess_none() {
        DomainFactory.createAllAlignableSeqTypes()
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        addUserWithReadAccessToProject(User.findByUsername(USER), seqTrack.project)
        SpringSecurityUtils.doWithAuth(USER) {
            List<SeqType> seqtypes = seqTypeService.alignableSeqTypesByProject(seqTrack.project)
            assert 0 == seqtypes.size()
        }
    }


    @Test
    void testAlignableSeqTypesByProject_normalUserWithoutAccess_none() {
        DomainFactory.createDefaultOtpAlignableSeqTypes()
        SeqTrack seqTrack = DomainFactory.createSeqTrack()

        SpringSecurityUtils.doWithAuth(USER) {
            shouldFail(AccessDeniedException) {
                seqTypeService.alignableSeqTypesByProject(seqTrack.project)
            }
        }
    }


    @Test
    void testAlignableSeqTypesByProject_operator_one() {
        List<SeqType> alignableSeqTypes = DomainFactory.createAllAlignableSeqTypes()
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        DomainFactory.createSeqTrack(sample: seqTrack.sample)
        DomainFactory.createSeqTrack(sample: seqTrack.sample, seqType: alignableSeqTypes[0])
        DomainFactory.createSeqTrack(sample: seqTrack.sample, seqType: alignableSeqTypes[0])

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            List<SeqType> seqtypes = seqTypeService.alignableSeqTypesByProject(seqTrack.project)
            assert 1 == seqtypes.size()
        }
    }


    @Test
    void testAlignableSeqTypesByProject_operator_two() {
        List<SeqType> alignableSeqTypes = DomainFactory.createAllAlignableSeqTypes()
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        DomainFactory.createSeqTrack(sample: seqTrack.sample)
        DomainFactory.createSeqTrack(sample: seqTrack.sample, seqType: alignableSeqTypes[0])
        DomainFactory.createSeqTrack(sample: seqTrack.sample, seqType: alignableSeqTypes[0])
        DomainFactory.createSeqTrack(sample: seqTrack.sample, seqType: alignableSeqTypes[1])
        DomainFactory.createSeqTrack(sample: seqTrack.sample, seqType: alignableSeqTypes[1])

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            List<SeqType> seqtypes = seqTypeService.alignableSeqTypesByProject(seqTrack.project)
            assert 2 == seqtypes.size()
        }
    }
}
