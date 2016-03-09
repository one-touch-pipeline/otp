package de.dkfz.tbi.otp.job.jobs.examplePBS

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.Realm

class SendSleepJob extends AbstractJobImpl {

    final int nJobs = 4

    @Autowired
    PbsService pbsService


    @Override
    public void execute() throws Exception {
        String cmd = "date; sleep 600"
        Realm realm = Realm.list().get(0)
        List<String> pbsIDs = []
        for(int i=0; i<nJobs; i++) {
            pbsIDs.add(pbsService.executeJob(realm, cmd))
        }
        addOutputParameter("__pbsIds", pbsIDs.join(","))
        addOutputParameter("__pbsRealm", realm.id.toString())
    }
}
