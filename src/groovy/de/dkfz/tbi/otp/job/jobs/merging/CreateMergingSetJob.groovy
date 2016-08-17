package de.dkfz.tbi.otp.job.jobs.merging

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component
@Scope("prototype")
@UseJobLog
class CreateMergingSetJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    MergingSetService mergingSetService

    @Override
    public void execute() throws Exception {
        long bamFileId = Long.parseLong(getProcessParameterValue())
        ProcessedBamFile bamFile = ProcessedBamFile.get(bamFileId)
        mergingSetService.createMergingSetForBamFile(bamFile)
        succeed()
    }
}
