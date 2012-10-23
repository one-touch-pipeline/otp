package de.dkfz.tbi.otp.job.jobs.examplePBS

import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.Realm
import org.springframework.beans.factory.annotation.Autowired

class SendSleepJob extends AbstractJobImpl {

    final int nJobs = 4

    @Autowired
    ExecutionService executionService

    @Override
    public void execute() throws Exception {
        String cmd = "date; sleep 600"
        Realm realm = Realm.list().get(0)
        List<String> pbsIDs = []
        for(int i=0; i<nJobs; i++) {
            pbsIDs.add(sendScript(realm, cmd))
        }
        addOutputParameter("__pbsIds", pbsIDs.join(","))
        addOutputParameter("__pbsRealm", realm.id.toString())
    }

    private String sendScript(Realm realm, String text) {
        String pbsResponse = executionService.executeJob(realm, text)
        List<String> extractedPbsIds = executionService.extractPbsIds(pbsResponse)
        if (extractedPbsIds.size() != 1) {
            log.debug "Number of PBS jobs is = ${extractedPbsIds.size()}"
        }
        return extractedPbsIds.get(0)
    }
}
