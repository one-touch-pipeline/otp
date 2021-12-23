/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.ngsdata

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.acl.AclUtilService
import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.junit.Test
import org.springframework.security.access.AccessDeniedException

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.CollectionUtils

@Rollback
@Integration
class SeqTypeServiceTest implements UserAndRoles {

    SeqTypeService seqTypeService

    AclUtilService aclUtilService

    void setupData() {
        createUserAndRoles()
    }

    @Test
    void testAlignableSeqTypesByProject_admin_none() {
        setupData()
        DomainFactory.createAllAlignableSeqTypes()
        SeqTrack seqTrack = DomainFactory.createSeqTrack()

        SpringSecurityUtils.doWithAuth(ADMIN) {
            List<SeqType> seqtypes = seqTypeService.alignableSeqTypesByProject(seqTrack.project)
            assert 0 == seqtypes.size()
        }
    }

    @Test
    void testAlignableSeqTypesByProject_operator_none() {
        setupData()
        DomainFactory.createAllAlignableSeqTypes()
        SeqTrack seqTrack = DomainFactory.createSeqTrack()

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            List<SeqType> seqtypes = seqTypeService.alignableSeqTypesByProject(seqTrack.project)
            assert 0 == seqtypes.size()
        }
    }

    @Test
    void testAlignableSeqTypesByProject_normalUserWithAccess_none() {
        setupData()
        DomainFactory.createAllAlignableSeqTypes()
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        addUserWithReadAccessToProject(CollectionUtils.atMostOneElement(User.findAllByUsername(USER)), seqTrack.project)
        SpringSecurityUtils.doWithAuth(USER) {
            List<SeqType> seqtypes = seqTypeService.alignableSeqTypesByProject(seqTrack.project)
            assert 0 == seqtypes.size()
        }
    }

    @Test
    void testAlignableSeqTypesByProject_normalUserWithoutAccess_none() {
        setupData()
        DomainFactory.createDefaultOtpAlignableSeqTypes()
        SeqTrack seqTrack = DomainFactory.createSeqTrack()

        SpringSecurityUtils.doWithAuth(USER) {
            TestCase.shouldFail(AccessDeniedException) {
                seqTypeService.alignableSeqTypesByProject(seqTrack.project)
            }
        }
    }

    @Test
    void testAlignableSeqTypesByProject_operator_one() {
        setupData()
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
        setupData()
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
