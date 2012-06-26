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
        Set<String> hosts = new HashSet<String>()
        List<DataFile> files = DataFile.findAllByRun(run)
        for(DataFile file in files) {
            if (file.project) {
                hosts << file.project.realmName
            }
        }
        switch(hosts.size()) {
            case 0:
                return
            case 1: 
                String[] array= hosts.toArray()
                String rr = array[0].toUpperCase()
                run.storageRealm = Run.StorageRealm."${rr}"
                return
            default:
                run.storageRealm = Run.StorageRealm.MIXED
        }
    }

}
