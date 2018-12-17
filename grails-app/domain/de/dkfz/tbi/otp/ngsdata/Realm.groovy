package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.utils.*


class Realm implements Entity, Serializable {

    enum JobScheduler {
        LSF,
        PBS,

        static Realm.JobScheduler findByName(String name) {
            return values().find {
                it.name() == name
            }
        }
    }

    String name

    JobScheduler jobScheduler
    String host                         // job submission host name
    int port                            // job submission host port
    int timeout
    String defaultJobSubmissionOptions  // default options for job submission

    static constraints = {
        defaultJobSubmissionOptions validator: {
            ClusterJobSubmissionOptionsService.validateJsonString(it)
        }
    }

    @Override
    String toString() {
        return "Realm ${id} ${name}"
    }
}
