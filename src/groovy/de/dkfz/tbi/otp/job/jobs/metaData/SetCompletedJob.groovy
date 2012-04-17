package de.dkfz.tbi.otp.job.jobs.metaData

import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.ngsdata.Run
import de.dkfz.tbi.otp.ngsdata.DataFile

class SetCompletedJob extends AbstractEndStateAwareJobImpl {

    @Override
    public void execute() throws Exception {
        long runId = Long.parseLong(getProcessParameterValue())
        Run run = Run.get(runId)
        setRealm(run)
        run.complete = true
        run.save(flush: true)
        succeed()
    }

    private void setRealm(Run run) {
        Set<String> hosts = new HashSet<String>()
        List<DataFile> files = DataFile.findAllByRun(run)
        for(DataFile file in files) {
            if (file.project) {
                hosts << file.project.realm.name
            }
        }
        switch(hosts.size()) {
            case 0:
                return
            case 1: 
                String[] array= hosts.toArray()
                String rr = array[0].toUpperCase()
                run.realm = Run.Realm."${rr}"
                return
            default:
                run.realm = Run.Realm.MIXED
        }
    }

}
