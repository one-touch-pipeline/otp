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
package de.dkfz.tbi.otp

import grails.util.BuildSettings
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.StaticMessageSource

import de.dkfz.tbi.otp.utils.MessageSourceService

@SuppressWarnings('JavaIoPackageAccess')
class TestMessageSourceService extends MessageSourceService {

    static Locale defaultLocale = LocaleContextHolder.locale

    StaticMessageSource staticMessageSource = new StaticMessageSource()

    static Properties parseMessagePropertiesFile() {
        File i18nFile = new File(BuildSettings.BASE_DIR, "grails-app/i18n/messages.properties")
        Properties properties = new Properties()
        properties.load(new FileInputStream(i18nFile as File))
        return properties
    }

    TestMessageSourceService() {
        parseMessagePropertiesFile().each { key, value ->
            staticMessageSource.addMessage(key.toString(), defaultLocale, value.toString())
        }
    }

    @Override
    MessageSource getMessageSourceInstance() {
        return staticMessageSource
    }
}
