package de.dkfz.tbi.otp.job.jobs.fileSystemConsistency

import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl
import de.dkfz.tbi.otp.job.processing.ProcessParameter
import de.dkfz.tbi.otp.fileSystemConsistency.ConsistencyCheck
import org.springframework.stereotype.Component
import org.springframework.context.annotation.Scope
import org.springframework.scheduling.annotation.Scheduled

@Component("dataFileStatusStartJob")
@Scope("singleton")
class DataFileStatusStartJob extends AbstractStartJobImpl {

    @Scheduled(cron="0 0 1 * * *")
    void execute() {
        ConsistencyCheck consistencyCheck = new ConsistencyCheck()
        consistencyCheck.save(flush: true)
        createProcess(new ProcessParameter(
                value: consistencyCheck.id.toString(),
                className: consistencyCheck.class.name)
        )
        log.debug "FileSystemConsistencyWorkflow: job started"
    }
}
