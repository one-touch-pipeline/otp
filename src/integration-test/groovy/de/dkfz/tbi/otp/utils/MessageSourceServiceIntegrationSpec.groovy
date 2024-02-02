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
package de.dkfz.tbi.otp.utils

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.grails.spring.context.support.PluginAwareResourceBundleMessageSource
import org.springframework.context.NoSuchMessageException
import spock.lang.Specification

@Rollback
@Integration
class MessageSourceServiceIntegrationSpec extends Specification {

    MessageSourceService messageSourceService

    void setupMessageSourceService() {
        messageSourceService = new MessageSourceService()
        messageSourceService.messageSource = Mock(PluginAwareResourceBundleMessageSource)
    }

    void "createMessage, when template is null, throw assert"() {
        given:
        setupMessageSourceService()

        when:
        messageSourceService.createMessage(null, [:])

        then:
        AssertionError e = thrown()
        e.message.contains('assert templateName')
    }

    void "createMessage, template does not exist, throws exception"() {
        given:
        setupMessageSourceService()

        when:
        messageSourceService.createMessage("non.existent.template")

        then:
        thrown(NoSuchMessageException)
    }

    @SuppressWarnings("GStringExpressionWithinString")
    void "createMessage, when template exist, return notification text"() {
        given:
        setupMessageSourceService()
        String templateName = "test.message.name"
        messageSourceService = new MessageSourceService(
                messageSource: Mock(PluginAwareResourceBundleMessageSource) {
                    _ * getMessageInternal(templateName, [], _) >> 'static text ${arg1} ${arg2} more static text'
                }
        )

        when:
        String message = messageSourceService.createMessage(templateName, [arg1: 'argOne', arg2: 'argTwo'])

        then:
        message == 'static text argOne argTwo more static text'
    }
}
