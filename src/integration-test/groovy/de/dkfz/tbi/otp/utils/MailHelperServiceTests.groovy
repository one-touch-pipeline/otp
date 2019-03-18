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

package de.dkfz.tbi.otp.utils

import grails.plugin.mail.MailService
import org.junit.After
import org.junit.Test

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService

class MailHelperServiceTests {

    static final String SUBJECT = 'Test Subject'
    static final String BODY = 'Test Body'
    static final String RECIPIENT = 'a.b@c.d'

    MailHelperService mailHelperService

    ProcessingOptionService processingOptionService

    @After
    void tearDown() {
        TestCase.removeMetaClass(MailHelperService, mailHelperService)
        TestCase.removeMetaClass(MailService, mailHelperService.mailService)
    }

    @Test
    void testSendEmail() {
        mailHelperService.sendEmail(
                SUBJECT,
                BODY,
                RECIPIENT,
        )
    }

    @Test
    void testSendEmailWithMultipleRecipients() {
        mailHelperService.sendEmail(
                SUBJECT,
                BODY,
                [RECIPIENT, RECIPIENT],
        )
    }

    @Test
    void testSendEmail_subjectIsNull_shouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            mailHelperService.sendEmail(
                    null,
                    BODY,
                    RECIPIENT,
            )
        }.contains('assert emailSubject')
    }

    @Test
    void testSendEmail_bodyIsNull_shouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            mailHelperService.sendEmail(
                    SUBJECT,
                    null,
                    RECIPIENT,
            )
        }.contains('assert content')
    }

    @Test
    void testSendEmail_receiverIsNull_shouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            mailHelperService.sendEmail(
                    SUBJECT,
                    BODY,
                    null,
            )
        }.contains('assert recipient')
    }
}
