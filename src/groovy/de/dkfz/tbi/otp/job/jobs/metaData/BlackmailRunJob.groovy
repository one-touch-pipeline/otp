package de.dkfz.tbi.otp.job.jobs.metaData

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.ngsdata.Run

class BlackmailRunJob extends AbstractEndStateAwareJobImpl {

    @Override
    public void execute() throws Exception {
        long runId = Long.parseLong(getProcessParameterValue())
        Run run = Run.get(runId)
        if (checkRun(run)) {
            run.blacklisted = false
            succeed()
        } else {
            run.blacklisted = true
            fail()
        }
        run.save(flush: true)
    }

    boolean checkRun(Run run) {
        List<String> blackList = [
            "110218_HWUSI-EAS451_00020_FC_62YJKAAXX",
            "110325_SN750_0056_BB0244ABXX",
            "110325_SN750_0055_AB022NABXX",
            "110616_SN750_0075_BC018AACXX",
        ]
        if (blackList.contains(run.name)) {
            return false
        }
        return true
    }
}
