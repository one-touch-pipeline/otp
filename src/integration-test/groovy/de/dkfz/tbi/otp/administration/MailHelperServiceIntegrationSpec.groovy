/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.administration

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore

import java.time.ZonedDateTime

/**
 * {@link MailHelperService#deleteOldMails} relies on two things the unit test implementation of GORM does not provide:
 * the {@code all-delete-orphan} cascade from {@link Mail} to {@link Attachment}, and a flush that actually reaches the
 * database. Both only show up against a real hibernate session, so they are tested here instead of in
 * {@code MailHelperServiceSpec}.
 */
@Rollback
@Integration
class MailHelperServiceIntegrationSpec extends Specification implements DomainFactoryCore {

    static final int DELAY = 7

    @Autowired
    MailHelperService mailHelperService

    void setupData() {
        findOrCreateProcessingOption(name: ProcessingOption.OptionName.DELETE_OLD_MAILS_DELAY, value: DELAY.toString())
    }

    void "deleteOldMails, when a deleted mail had attachments, then the cascade deletes them as well"() {
        given:
        setupData()
        Mail mail = createMail([
                sendDateTime: ZonedDateTime.now().minusDays(DELAY + 1),
                state       : Mail.State.SENT,
        ])
        createAttachment([mail: mail])

        when:
        mailHelperService.deleteOldMails()

        then:
        Mail.count() == 0
        Attachment.count() == 0
    }

    void "deleteOldMails, when a mail is not old enough, then keep it and its attachments"() {
        given:
        setupData()
        Mail mailToDelete = createMail([
                sendDateTime: ZonedDateTime.now().minusDays(DELAY + 1),
                state       : Mail.State.SENT,
        ])
        createAttachment([mail: mailToDelete])
        Mail mailToKeep = createMail([
                sendDateTime: ZonedDateTime.now().minusDays(DELAY - 1),
                state       : Mail.State.SENT,
        ])
        Attachment attachmentToKeep = createAttachment([mail: mailToKeep])

        when:
        mailHelperService.deleteOldMails()

        then:
        TestCase.assertContainSame(Mail.list(), [mailToKeep])
        TestCase.assertContainSame(Attachment.list(), [attachmentToKeep])
    }
}
