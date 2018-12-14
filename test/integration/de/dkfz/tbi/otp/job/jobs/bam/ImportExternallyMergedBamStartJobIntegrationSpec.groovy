package de.dkfz.tbi.otp.job.jobs.bam

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.jobs.importExternallyMergedBam.*
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.scheduler.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.spock.*
import org.codehaus.groovy.grails.support.*

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
