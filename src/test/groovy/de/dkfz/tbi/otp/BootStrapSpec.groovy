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

import grails.core.GrailsApplication
import grails.util.Environment
import org.springframework.context.ApplicationContext
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.config.PropertiesValidationService
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.FastqImportInstanceService
import de.dkfz.tbi.otp.workflow.bamImport.BamImportService
import de.dkfz.tbi.otp.workflow.shared.WorkflowException
import de.dkfz.tbi.otp.workflowExecution.WorkflowSystemService

class BootStrapSpec extends Specification {

    @Unroll
    void "init, when called, then call all expected methods"() {
        given:
        BootStrap bootStrap = new BootStrap()
        bootStrap.grailsApplication = Mock(GrailsApplication)
        ApplicationContext context = Mock(ApplicationContext)
        bootStrap.propertiesValidationService = Mock(PropertiesValidationService)
        bootStrap.fastqImportInstanceService = Mock(FastqImportInstanceService)
        bootStrap.bamImportService = Mock(BamImportService)
        bootStrap.configService = Mock(ConfigService)
        bootStrap.schedulerService = Mock(SchedulerService)
        bootStrap.workflowSystemService = Mock(WorkflowSystemService)

        when:
        bootStrap.init()

        then:
        1 * bootStrap.grailsApplication.mainContext >> context
        1 * context.getBean("shutdownService")
        1 * bootStrap.propertiesValidationService.validateStartUpProperties()
        1 * bootStrap.fastqImportInstanceService.changeProcessToWait()
        1 * bootStrap.bamImportService.changeProcessToWait()
        1 * bootStrap.configService.isJobSystemEnabled() >> jobSystemEnabled
        jobSystemCount * bootStrap.schedulerService.startup()
        jobSystemCount * bootStrap.workflowSystemService.startWorkflowSystem()

        where:
        jobSystemEnabled | jobSystemCount
        true             | 1
        false            | 0
    }

    @Unroll
    void "init, when startWorkflowSystem throws exception, then call all previous methods and error should be logged"() {
        given:
        BootStrap bootStrap = new BootStrap()
        bootStrap.grailsApplication = Mock(GrailsApplication)
        ApplicationContext context = Mock(ApplicationContext)
        bootStrap.propertiesValidationService = Mock(PropertiesValidationService)
        bootStrap.fastqImportInstanceService = Mock(FastqImportInstanceService)
        bootStrap.bamImportService = Mock(BamImportService)
        bootStrap.configService = Mock(ConfigService)
        bootStrap.schedulerService = Mock(SchedulerService)
        bootStrap.workflowSystemService = Mock(WorkflowSystemService)

        when:
        bootStrap.init()

        then:
        1 * bootStrap.grailsApplication.mainContext >> context
        1 * context.getBean("shutdownService")
        1 * bootStrap.propertiesValidationService.validateStartUpProperties()
        1 * bootStrap.fastqImportInstanceService.changeProcessToWait()
        1 * bootStrap.bamImportService.changeProcessToWait()
        1 * bootStrap.configService.isJobSystemEnabled() >> 1
        1 * bootStrap.schedulerService.startup()
        1 * bootStrap.workflowSystemService.startWorkflowSystem() >> {
            throw new WorkflowException("test")
        }
    }

    void "destroy, when called, then no exceptions should be thrown"() {
        given:
        BootStrap bootStrap = new BootStrap()

        when:
        bootStrap.destroy()

        then:
        noExceptionThrown()
        Environment.current == Environment.TEST
    }

}
