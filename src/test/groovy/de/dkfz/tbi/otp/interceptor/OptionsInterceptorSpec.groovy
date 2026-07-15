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
package de.dkfz.tbi.otp.interceptor

import grails.testing.web.interceptor.InterceptorUnitTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.config.InstanceLogo
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.security.SecurityService
import de.dkfz.tbi.otp.workflowExecution.WorkflowSystemService

class OptionsInterceptorSpec extends Specification implements InterceptorUnitTest<OptionsInterceptor> {

    private void mockServices(boolean administrativeUser, boolean autoImportEnabled, boolean workflowSystemEnabled) {
        interceptor.securityService = Mock(SecurityService) {
            hasCurrentUserAdministrativeRoles() >> administrativeUser
        }
        interceptor.workflowSystemService = Mock(WorkflowSystemService) {
            isEnabled() >> workflowSystemEnabled
        }
        interceptor.configService = Mock(ConfigService) {
            getOidcEnabled() >> false
        }
        interceptor.processingOptionService = Mock(ProcessingOptionService) {
            findOptionAsBoolean(ProcessingOption.OptionName.TICKET_SYSTEM_AUTO_IMPORT_ENABLED) >> autoImportEnabled
            findOptionAsString(ProcessingOption.OptionName.GUI_LOGO) >> InstanceLogo.NONE.name()
            _ * findOptionAsString(_) >> ""
            _ * findOptionAsBoolean(_) >> false
            _ * findOptionAsInteger(_) >> 0
        }
    }

    @Unroll
    void "Interceptor after function: banner visibility flags reflect administrative role (#administrativeUser), auto import (#autoImportEnabled) and workflow system (#workflowSystemEnabled)"() {
        given:
        mockServices(administrativeUser, autoImportEnabled, workflowSystemEnabled)

        when:
        withRequest(controller: "home")
        interceptor.model = [:]
        interceptor.after()

        then:
        interceptor.model.autoImportDisabledBannerVisible == expectedAutoImportBanner
        interceptor.model.workflowSystemDisabledBannerVisible == expectedWorkflowSystemBanner

        where:
        administrativeUser | autoImportEnabled | workflowSystemEnabled || expectedAutoImportBanner | expectedWorkflowSystemBanner
        true               | false             | false                 || true                     | true
        true               | true              | true                  || false                    | false
        true               | false             | true                  || true                     | false
        true               | true              | false                 || false                    | true
        false              | false             | false                 || false                    | false
    }
}
