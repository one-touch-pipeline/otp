package de.dkfz.tbi.otp.job.jobs.bam

import grails.test.spock.IntegrationSpec
import org.codehaus.groovy.grails.support.PersistenceContextInterceptor

import de.dkfz.tbi.otp.dataprocessing.ImportProcess
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.job.jobs.importExternallyMergedBam.ImportExternallyMergedBamStartJob
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.DomainFactory

class ImportExternallyMergedBamStartJobIntegrationSpec extends IntegrationSpec {

    PersistenceContextInterceptor persistenceInterceptor

    def "execute sets state of importProcess on STARTED"() {
        given:
        JobExecutionPlan plan = DomainFactory.createJobExecutionPlan(enabled: true)
        ImportProcess importProcess = new ImportProcess(
                externallyProcessedMergedBamFiles: [
                        DomainFactory.createExternallyProcessedMergedBamFile(),
                        DomainFactory.createExternallyProcessedMergedBamFile()
                ]
        ).save()
        ImportExternallyMergedBamStartJob importExternallyMergedBamStartJob = new ImportExternallyMergedBamStartJob()
        importExternallyMergedBamStartJob.schedulerService = Mock(SchedulerService) {
            1 * createProcess(_, _, _) >> null
            _ * isActive() >> true
        }
        importExternallyMergedBamStartJob.optionService = new ProcessingOptionService()
        importExternallyMergedBamStartJob.setJobExecutionPlan(plan)
        importExternallyMergedBamStartJob.persistenceInterceptor = persistenceInterceptor

        when:
        importExternallyMergedBamStartJob.execute()

        then:
        importProcess.state == ImportProcess.State.STARTED
    }
}
