/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.job.jobs.bam

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.ImportProcess
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.job.jobs.importExternallyMergedBam.ImportExternallyMergedBamStartJob
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.utils.SessionUtils

@Rollback
@Integration
class ImportExternallyMergedBamStartJobIntegrationSpec extends Specification {

    void "execute sets state of importProcess on STARTED"() {
        given:
        SessionUtils.metaClass.static.withNewSession = { Closure c -> c() }
        JobExecutionPlan plan = DomainFactory.createJobExecutionPlan(enabled: true)
        ImportProcess importProcess = new ImportProcess(
                externallyProcessedBamFiles: [
                        DomainFactory.createExternallyProcessedBamFile(),
                        DomainFactory.createExternallyProcessedBamFile()
                ]
        )
        importProcess.save(flush: true)
        ImportExternallyMergedBamStartJob importExternallyMergedBamStartJob = new ImportExternallyMergedBamStartJob()
        importExternallyMergedBamStartJob.schedulerService = Mock(SchedulerService) {
            1 * createProcess(_, _, _) >> null
            _ * isActive() >> true
        }

        importExternallyMergedBamStartJob.optionService = new ProcessingOptionService()
        importExternallyMergedBamStartJob.jobExecutionPlan = plan

        when:
        importExternallyMergedBamStartJob.execute()
        ImportProcess refreshedImportProcess = ImportProcess.get(importProcess.id)

        then:
        refreshedImportProcess.state == ImportProcess.State.STARTED

        cleanup:
        TestCase.removeMetaClass(SessionUtils)
    }
}
