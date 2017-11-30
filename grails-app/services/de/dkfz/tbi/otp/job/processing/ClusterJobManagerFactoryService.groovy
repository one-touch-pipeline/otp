package de.dkfz.tbi.otp.job.processing

import de.dkfz.roddy.execution.jobs.*
import de.dkfz.roddy.execution.jobs.cluster.lsf.*
import de.dkfz.roddy.execution.jobs.cluster.pbs.*
import de.dkfz.tbi.otp.ngsdata.*
import groovy.transform.*

import java.time.*

@CompileStatic
class ClusterJobManagerFactoryService {
    ExecutionService executionService

    private Map<RealmAndUser, BatchEuphoriaJobManager> managerPerRealm = [:]

    BatchEuphoriaJobManager getJobManager(Realm realm) {
        BatchEuphoriaJobManager manager = managerPerRealm[new RealmAndUser(realm, realm.unixUser)]

        if (manager == null) {
            JobManagerOptions jobManagerParameters = JobManagerOptions.create()
                    .setStrictMode(true)
                    .setCreateDaemon(false)
                    .setUpdateInterval(Duration.ZERO)
                    .setUserIdForJobQueries(realm.unixUser)
                    .setTrackUserJobsOnly(true)
                    .setTrackOnlyStartedJobs(false)
                    .setUserMask("027")
                    .build()

            if (realm.jobScheduler == Realm.JobScheduler.PBS) {
                manager = new PBSJobManager(new BEExecutionServiceAdapter(executionService, realm, realm.unixUser), jobManagerParameters)
            } else if (realm.jobScheduler == Realm.JobScheduler.LSF) {
                manager = new LSFJobManager(new BEExecutionServiceAdapter(executionService, realm, realm.unixUser), jobManagerParameters)
            }  else {
                throw new Exception("Unsupported cluster job scheduler")
            }
            managerPerRealm[new RealmAndUser(realm, realm.unixUser)] = manager
        }
        return manager
    }
}
