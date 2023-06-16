/*
 * Copyright 2011-2022 The OTP authors
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
import grails.core.GrailsApplication
import grails.util.Environment

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.config.PropertiesValidationService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.FastqImportInstanceService
import de.dkfz.tbi.otp.security.user.UserService
import de.dkfz.tbi.otp.workflow.shared.WorkflowException
import de.dkfz.tbi.otp.workflowExecution.WorkflowSystemService

class BootStrap {
    ConfigService configService
    GrailsApplication grailsApplication
    PropertiesValidationService propertiesValidationService
    SchedulerService schedulerService
    WorkflowSystemService workflowSystemService
    FastqImportInstanceService fastqImportInstanceService
    FileSystemService fileSystemService
    RemoteShellHelper remoteShellHelper

    def init = { servletContext ->
        // load the shutdown service
        grailsApplication.mainContext.getBean("shutdownService")

        propertiesValidationService.validateStartUpProperties()

        if ([Environment.PRODUCTION, Environment.DEVELOPMENT].contains(Environment.current)) {
            UserService.createFirstAdminUserIfNoUserExists()
        }

        fastqImportInstanceService.changeProcessToWait()

        if (configService.isJobSystemEnabled()) {
            log.info("JobSystem is enabled")

            // start the old workflow system (deprecated)
            schedulerService.startup()

            try {
                // start the new workflow system
                workflowSystemService.startWorkflowSystem()
            } catch (WorkflowException we) {
                log.error("Failed to start the workflow system.", we)
            }
        } else {
            log.info("JobSystem is disabled")
        }

        JSON.registerObjectMarshaller(Enum, { Enum e -> e.name() })
    }

    def destroy = {
        if (Environment.current == Environment.DEVELOPMENT) {
            //destroy file system to avoid problems on spring dev-tools restart
            fileSystemService.closeFileSystem()
            remoteShellHelper.closeFileSystem()
        }
    }
}
