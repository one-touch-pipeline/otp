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

package de.dkfz.tbi.otp


import grails.converters.JSON
import grails.plugin.springsecurity.SecurityFilterPosition
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.util.Environment
import grails.core.GrailsApplication

import de.dkfz.odcf.audit.impl.DicomAuditLogger
import de.dkfz.odcf.audit.xml.layer.EventIdentification.EventOutcomeIndicator
import de.dkfz.tbi.otp.administration.UserService
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.config.PropertiesValidationService
import de.dkfz.tbi.otp.job.scheduler.SchedulerService

class BootStrap {
    ConfigService configService
    GrailsApplication grailsApplication
    PropertiesValidationService propertiesValidationService
    SchedulerService schedulerService

    def init = { servletContext ->
        // load the shutdown service
        grailsApplication.mainContext.getBean("shutdownService")

        propertiesValidationService.validateStartUpProperties()

        if (configService.isJobSystemEnabled()) {
            // startup the scheduler
            log.info("JobSystem is enabled")
            schedulerService.startup()
        } else {
            log.info("JobSystem is disabled")
        }

        if (Environment.isDevelopmentMode()) {
            // adds the backdoor filter allowing a developer to login without password only in development mode
            SpringSecurityUtils.clientRegisterFilter('backdoorFilter', SecurityFilterPosition.SECURITY_CONTEXT_FILTER.order + 10)
        }

        if ([Environment.PRODUCTION, Environment.DEVELOPMENT].contains(Environment.getCurrent())) {
            UserService.createFirstAdminUserIfNoUserExists()
        }

        JSON.registerObjectMarshaller(Enum, { Enum e -> e.name() })
        DicomAuditLogger.logActorStart(EventOutcomeIndicator.SUCCESS, ConfigService.getInstance().getDicomInstanceName())
    }

    def destroy = {
    }
}
