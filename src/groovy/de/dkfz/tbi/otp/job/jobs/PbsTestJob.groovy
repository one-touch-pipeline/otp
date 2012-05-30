package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.PbsJob

/**
 * Very simple PbsTestJob, just printing something to sysout.
 * It returns three PBS Ids: "1", "2", "3"
 *
 */
class PbsTestJob extends AbstractJobImpl implements PbsJob {
    private Long realm
    @Override
    public void execute() throws Exception {
        println("Execute method of PbsTestJob called")
    }

    @Override
    public List<String> getPbsIds() {
        return ["1", "2", "3"]
    }

    public Long getRealm() {
        if (!realm) {
            // realm not set - try if there is a parameter
            for (Parameter param in inputParameters) {
                if (param.type.name == "__pbsRealm") {
                    return param.value as Long
                }
            }
        }
        return realm
    }

    public void setRealm(Long id) {
        realm = id
    }
}
