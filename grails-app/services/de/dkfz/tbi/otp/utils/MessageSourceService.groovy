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

import grails.gorm.transactions.Transactional
import groovy.text.SimpleTemplateEngine
import org.grails.spring.context.support.PluginAwareResourceBundleMessageSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.context.NoSuchMessageException
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.lang.Nullable
import org.springframework.validation.ObjectError

@Transactional
class MessageSourceService {

    @Autowired
    PluginAwareResourceBundleMessageSource messageSource

    MessageSource getMessageSourceInstance() {
        return messageSource
    }

    String createMessage(String templateName) {
        assert templateName
        return messageSourceInstance.getMessage(templateName, [].toArray(), LocaleContextHolder.locale)
    }

    String createMessage(String templateName, Map properties) {
        assert templateName
        return new SimpleTemplateEngine().createTemplate(createMessage(templateName)).make(properties).toString()
    }

    String getMessage(String templateName, @Nullable Object[] args) {
        assert templateName
        return messageSource.getMessage(templateName, args, LocaleContextHolder.locale)
    }

    String createError(ObjectError error) {
        assert error
        String text
        try {
            text = messageSourceInstance.getMessage(error, LocaleContextHolder.locale)
        } catch (NoSuchMessageException ignored) {
            text = error.codes[0]
        }
        return text
    }
}
