/*
 * Copyright 2011-2024 The OTP authors
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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.springframework.security.access.AccessDeniedException
import spock.lang.Shared
import spock.lang.Specification

import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.CollectionUtils

@Rollback
@Integration
class SeqTypeServiceIntegrationSpec extends Specification implements UserAndRoles {

    SeqTypeService seqTypeService

    @Shared
    List<SeqType> alignableSeqTypes

    SeqTrack seqTrack

    void setupData() {
        createUserAndRoles()
        alignableSeqTypes = DomainFactory.createAllAlignableSeqTypes()
        seqTrack = DomainFactory.createSeqTrack()
        addUserWithReadAccessToProject(CollectionUtils.atMostOneElement(User.findAllByUsername(USER)), seqTrack.project)
    }

    void "test testAlignableSeqTypesByProject with different users, when no seqType is connected to the project"() {
        given:
        setupData()
        List<SeqType> seqTypes = []

        when:
        seqTypes = doWithAuth(user) {
            seqTypeService.alignableSeqTypesByProject(seqTrack.project)
        }

        then:
        0 == seqTypes.size()

        where:
        user     | _
        ADMIN    | _
        OPERATOR | _
        USER     | _
    }

    void "test testAlignableSeqTypesByProject with different users, when one seqType is connected to the project"() {
        given:
        setupData()
        List<SeqType> seqTypes = []
        DomainFactory.createSeqTrack(sample: seqTrack.sample)
        DomainFactory.createSeqTrack(sample: seqTrack.sample, seqType: alignableSeqTypes[0])
        DomainFactory.createSeqTrack(sample: seqTrack.sample, seqType: alignableSeqTypes[0])

        when:
        seqTypes = doWithAuth(user) {
            seqTypeService.alignableSeqTypesByProject(seqTrack.project)
        }

        then:
        1 == seqTypes.size()

        where:
        user     | _
        ADMIN    | _
        OPERATOR | _
        USER     | _
    }

    void "test testAlignableSeqTypesByProject with different users, when two seqTypes are connected to the project"() {
        given:
        setupData()
        List<SeqType> seqTypes = []
        DomainFactory.createSeqTrack(sample: seqTrack.sample)
        DomainFactory.createSeqTrack(sample: seqTrack.sample, seqType: alignableSeqTypes[0])
        DomainFactory.createSeqTrack(sample: seqTrack.sample, seqType: alignableSeqTypes[0])
        DomainFactory.createSeqTrack(sample: seqTrack.sample, seqType: alignableSeqTypes[1])
        DomainFactory.createSeqTrack(sample: seqTrack.sample, seqType: alignableSeqTypes[1])

        when:
        seqTypes = doWithAuth(user) {
            seqTypeService.alignableSeqTypesByProject(seqTrack.project)
        }

        then:
        2 == seqTypes.size()

        where:
        user     | _
        ADMIN    | _
        OPERATOR | _
        USER     | _
    }

    void "test testAlignableSeqTypesByProject with user that has no access to the project, throws exception"()  {
        given:
        setupData()

        when:
        doWithAuth(TESTUSER) {
            seqTypeService.alignableSeqTypesByProject(seqTrack.project)
        }

        then:
        thrown(AccessDeniedException)
    }
}
