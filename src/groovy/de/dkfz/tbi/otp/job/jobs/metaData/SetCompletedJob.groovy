package de.dkfz.tbi.otp.job.jobs.metaData

import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.beans.factory.annotation.Autowired


class SetCompletedJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    RunProcessingService runProcessingService

    @Override
    public void execute() throws Exception {
        long runId = Long.parseLong(getProcessParameterValue())
        Run run = Run.get(runId)
        setStorageRealm(run)
        runProcessingService.setMetaDataComplete(run)
        run.save(flush: true)
        succeed()
    }

    private void setStorageRealm(Run run) {
        run.storageRealm = Run.StorageRealm.DKFZ
    }

}
