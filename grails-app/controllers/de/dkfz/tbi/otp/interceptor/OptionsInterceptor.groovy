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
package de.dkfz.tbi.otp.interceptor

import groovy.transform.CompileStatic

import de.dkfz.tbi.otp.config.InstanceLogo
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService

@CompileStatic
class OptionsInterceptor {
    ProcessingOptionService processingOptionService

    OptionsInterceptor() {
        matchAll()
    }

    @Override
    boolean before() { true }

    @Override
    boolean after() {
        if (model != null) {
            model.contactDataOperatedBy = processingOptionService.findOptionAsString(ProcessingOption.OptionName.GUI_CONTACT_DATA_OPERATED_BY)
            model.contactDataSupportEmail = processingOptionService.findOptionAsString(ProcessingOption.OptionName.GUI_CONTACT_DATA_SUPPORT_EMAIL)
            model.contactDataPersonInCharge = processingOptionService.findOptionAsString(ProcessingOption.OptionName.GUI_CONTACT_DATA_PERSON_IN_CHARGE)
            model.contactDataPostalAddress = processingOptionService.findOptionAsString(ProcessingOption.OptionName.GUI_CONTACT_DATA_POSTAL_ADDRESS)
            model.piwikUrl = processingOptionService.findOptionAsString(ProcessingOption.OptionName.GUI_TRACKING_PIWIK_URL)
            model.piwikSiteId = processingOptionService.findOptionAsInteger(ProcessingOption.OptionName.GUI_TRACKING_SITE_ID)
            model.piwikEnabled = processingOptionService.findOptionAsBoolean(ProcessingOption.OptionName.GUI_TRACKING_ENABLED)
            model.showPartners = processingOptionService.findOptionAsBoolean(ProcessingOption.OptionName.GUI_SHOW_PARTNERS)

            InstanceLogo logo = InstanceLogo.NONE
            try {
                logo = InstanceLogo.valueOf(processingOptionService.findOptionAsString(ProcessingOption.OptionName.GUI_LOGO))
            } catch (IllegalArgumentException e) { }
            model.logo = logo.fileName

            String faqLink
            try {
                faqLink = processingOptionService.findOptionAsString(ProcessingOption.OptionName.NOTIFICATION_TEMPLATE_FAQ_LINK)
            } catch (IllegalArgumentException e) { }
            model.faqLink = faqLink

            // this file is provided by the gradle-git-properties gradle plugin
            String resourceName = "git.properties"
            ClassLoader loader = Thread.currentThread().getContextClassLoader()
            Properties props = new Properties()
            InputStream resourceStream
            try  {
                resourceStream = loader.getResourceAsStream(resourceName)
                props.load(resourceStream)
            } finally {
                resourceStream?.close()
            }

            String hash = props['git.commit.id.abbrev'] as String
            String version = props['git.tags'] as String

            if (version) {
                version = "Version ${version.substring(1)}"
            } else {
                String branch = props['git.branch'] as String
                version = "Branch ${branch}"
            }
            model.version = "${version} (${hash})"

            model.uriWithParams = "${request.forwardURI - request.contextPath}?${request.queryString}"
        }
        true
    }

    @Override
    void afterView() {
        // no-op
    }
}
