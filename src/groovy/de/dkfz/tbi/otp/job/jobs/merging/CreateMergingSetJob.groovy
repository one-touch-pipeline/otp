package de.dkfz.tbi.otp.job.jobs.merging

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.MergingSetService;
import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import de.dkfz.tbi.otp.ngsdata.MetaDataService

class CreateMergingSetJob extends AbstractJobImpl {

    @Autowired
    MergingSetService mergingSetService

    @Override
    public void execute() throws Exception {
        long bamFileId = Long.parseLong(getProcessParameterValue())
        mergingSetService.createMergingSet(bamFileId)
    }
}
