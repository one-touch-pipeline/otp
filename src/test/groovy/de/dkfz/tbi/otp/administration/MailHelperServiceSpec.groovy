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
package de.dkfz.tbi.otp.administration

import grails.plugins.mail.MailService
import grails.testing.gorm.DataTest
import grails.validation.ValidationException
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.UserDomainFactory
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.workflowExecution.WorkflowSystemService

import java.time.ZoneId
import java.time.ZonedDateTime

class MailHelperServiceSpec extends Specification implements DataTest, DomainFactoryCore, UserDomainFactory, UserAndRoles {

    static final String SUBJECT = 'subject'
    static final String BODY = 'body'
    static final String SENDER = HelperUtils.randomEmail
    static final String REPLY_TO = HelperUtils.randomEmail
    static final String TICKET_EMAIL = HelperUtils.randomEmail
    static final String MAIL1 = HelperUtils.randomEmail
    static final String MAIL2 = HelperUtils.randomEmail
    static final String MAIL3 = HelperUtils.randomEmail
    static final String SENDER_SALUTATION = "OTP team"
    static final int DELAY = 7
    static final ZonedDateTime SEND_DATE_TIME = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault())

    MailHelperService mailHelperService

    TestConfigService configService

    @Override
    Class[] getDomainClassesToMock() {
        return [
                Mail,
                ProcessingOption,
        ]
    }

    void cleanup() {
        configService.clean()
    }

    void setupData() {
        findOrCreateProcessingOption(name: ProcessingOption.OptionName.EMAIL_SENDER, value: SENDER)
        findOrCreateProcessingOption(name: ProcessingOption.OptionName.EMAIL_REPLY_TO, value: REPLY_TO)
        findOrCreateProcessingOption(name: ProcessingOption.OptionName.EMAIL_TICKET_SYSTEM, value: TICKET_EMAIL)
        findOrCreateProcessingOption(name: ProcessingOption.OptionName.HELP_DESK_TEAM_NAME, value: SENDER_SALUTATION)
        findOrCreateProcessingOption(name: ProcessingOption.OptionName.DELETE_OLD_MAILS_DELAY, value: DELAY.toString())

        mailHelperService = new MailHelperService()
        mailHelperService.processingOptionService = new ProcessingOptionService()
        mailHelperService.configService = configService = new TestConfigService()
        mailHelperService.mailService = Mock(MailService)
    }

    void "fetchMailsInWaiting, when there are mails in waiting state, then return them"() {
        given:
        setupData()
        Mail mail1 = createMail([state: Mail.State.WAITING])
        Mail mail2 = createMail([state: Mail.State.WAITING])
        createMail([state: Mail.State.SENDING])
        createMail([state: Mail.State.FAILED])
        createMail([state: Mail.State.SENT])

        when:
        List<Mail> mails = mailHelperService.fetchMailsInWaiting()

        then:
        TestCase.assertContainSame(mails, [mail1, mail2])
    }

    @Unroll
    void "saveEmail, when everything is fine, save it in database"() {
        given:
        setupData()

        when:
        Mail mail = mailHelperService.saveMail(SUBJECT, BODY, to, cc, bcc)

        then:
        mail.to.size() + mail.cc.size() + mail.bcc.size() == result
        Mail.count() == 1
        mail.subject == SUBJECT
        mail.body == BODY

        where:
        to             | cc             | bcc            || result
        []             | []             | []             || 1
        [MAIL1]        | []             | []             || 2
        []             | [MAIL1]        | []             || 2
        []             | []             | [MAIL1]        || 2
        [MAIL1, MAIL2] | []             | []             || 3
        []             | [MAIL1, MAIL2] | []             || 3
        []             | []             | [MAIL1, MAIL2] || 3
        [MAIL1]        | [MAIL2]        | [MAIL3]        || 4
        [TICKET_EMAIL] | []             | []             || 1
        []             | [TICKET_EMAIL] | []             || 1
        []             | []             | [TICKET_EMAIL] || 2
    }

    @Unroll
    void "saveEmail, when the email is invalid, then throw ValidationException and don't save it in database"() {
        given:
        setupData()

        when:
        mailHelperService.saveMail(SUBJECT, BODY, to, cc, bcc)

        then:
        thrown(ValidationException)
        Mail.count() == 0

        where:
        to            | cc            | bcc
        ['ab']        | []            | []
        []            | ['ab']        | []
        []            | []            | ['ab']
        [MAIL1, 'cd'] | []            | []
        []            | [MAIL1, 'cd'] | []
        []            | []            | [MAIL1, 'cd']
    }

    @Unroll
    void "saveEmail, when subject or body are missing, then throw IllegalArgumentException and don't save it in database"() {
        given:
        setupData()

        when:
        mailHelperService.saveMail(subject, body)

        then:
        thrown(IllegalArgumentException)
        Mail.count() == 0

        where:
        subject | body
        null    | BODY
        ''      | BODY
        SUBJECT | null
        SUBJECT | ''
    }

    void "saveErrorMailInNewTransaction, should save error mail"() {
        given:
        setupData()

        when:
        Mail mail = mailHelperService.saveErrorMailInNewTransaction(SUBJECT, BODY)

        then:
        mail.subject == SUBJECT
        mail.body == BODY
        mail.to == [TICKET_EMAIL] as Set
        mail.cc == [] as Set
        mail.bcc == [] as Set
        mail.state == Mail.State.WAITING
    }

    void "getSenderName, should return correct sender salutation"() {
        given:
        setupData()

        mailHelperService.securityService = Mock(SecurityService)

        when:
        String sender = mailHelperService.senderName

        then:
        1 * mailHelperService.securityService.hasCurrentUserAdministrativeRoles() >> adminRole
        (adminRole ? 0 : 1) * mailHelperService.securityService.currentUser >> new User([realName: user])

        sender == resultName

        where:
        user     | adminRole || resultName
        OPERATOR | true      || SENDER_SALUTATION
        ADMIN    | true      || SENDER_SALUTATION
        TESTUSER | false     || TESTUSER
    }

    void "sendMailByScheduler, when mails are sent, then call expected processing option"() {
        given:
        setupData()
        String footer = 'footer'
        mailHelperService.processingOptionService = Mock(ProcessingOptionService)
        mailHelperService.workflowSystemService = Mock(WorkflowSystemService)
        configService.addOtpProperty(OtpProperty.CONFIG_EMAIL_ENABLED, mailSystemEnabled.toString())
        configService.fixClockTo(SEND_DATE_TIME)
        Mail mail = createMail([:], saveMail)

        when:
        mailHelperService.sendMailByScheduler(mail)

        then:
        1 * mailHelperService.workflowSystemService.enabled >> workflowSystemEnabled
        processingOptionCallCount * mailHelperService.processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_SENDER) >> SENDER
        processingOptionCallCount * mailHelperService.processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_REPLY_TO) >> REPLY_TO
        processingOptionCallCount * mailHelperService.processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_FOOTER) >> footer
        processingOptionCallCount * mailHelperService.mailService.sendMail(_)

        mail.sendDateTime == expectedDateTime
        mail.state == expectedState

        where:
        workflowSystemEnabled | mailSystemEnabled | saveMail || processingOptionCallCount | expectedState      | expectedDateTime
        true                  | true              | true     || 1                         | Mail.State.SENT    | SEND_DATE_TIME
        false                 | true              | true     || 0                         | Mail.State.WAITING | null
        true                  | false             | true     || 0                         | Mail.State.WAITING | null
        false                 | false             | true     || 0                         | Mail.State.WAITING | null
        true                  | true              | false    || 1                         | Mail.State.WAITING | null
        false                 | true              | false    || 0                         | Mail.State.WAITING | null
        true                  | false             | false    || 0                         | Mail.State.WAITING | null
        false                 | false             | false    || 0                         | Mail.State.WAITING | null
    }

    void "changeMailState, should change mail state and save it"() {
        given:
        setupData()
        Mail mail = createMail()

        when:
        mailHelperService.changeMailState(mail, Mail.State.SENT)

        then:
        mail.state == Mail.State.SENT
    }

    void "getTicketSystemMailAddress, should return correct ticket system mail address"() {
        given:
        setupData()

        when:
        String ticketSystemMailAddress = mailHelperService.ticketSystemMailAddress

        then:
        ticketSystemMailAddress == TICKET_EMAIL
    }

    void "getReplyToAddress, should return correct reply to address"() {
        given:
        setupData()

        when:
        String replyToAddress = mailHelperService.replyToAddress

        then:
        replyToAddress == REPLY_TO
    }

    void "deleteOldMails, should delete mail"() {
        given:
        setupData()

        ZonedDateTime sendDateTime1 = ZonedDateTime.now().minusDays(DELAY + 1)
        ZonedDateTime sendDateTime2 = ZonedDateTime.now().minusDays(DELAY - 1)

        (1..3).collect {
            createMail([
                    sendDateTime: sendDateTime1,
                    state       : Mail.State.SENT,
            ])
        }
        List<Mail> mailsToKeep = (1..3).collect {
            createMail([
                    sendDateTime: sendDateTime2,
                    state       : Mail.State.SENT,
            ])
        }

        when:
        mailHelperService.deleteOldMails()

        then:
        TestCase.assertContainSame(Mail.list(), mailsToKeep)
    }
}
