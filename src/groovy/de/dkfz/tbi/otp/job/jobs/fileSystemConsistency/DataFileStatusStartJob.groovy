package de.dkfz.tbi.otp.job.jobs.fileSystemConsistency

import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl
import de.dkfz.tbi.otp.fileSystemConsistency.ConsistencyCheck
import org.springframework.stereotype.Component
import org.springframework.context.annotation.Scope

@Component("dataFileStatusStartJob")
@Scope("singleton")
class DataFileStatusStartJob extends AbstractStartJobImpl {

    //@Scheduled(cron="0 0 1 * * SAT")
    @Override
    void execute() {
        doWithPersistenceInterceptor {
            ConsistencyCheck consistencyCheck = new ConsistencyCheck()
            consistencyCheck.save(flush: true)
            createProcess(consistencyCheck)
            log.debug "FileSystemConsistencyWorkflow: job started"
        }
    }
}
