package de.dkfz.tbi.otp.job.jobs.merging

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl

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
