package de.dkfz.tbi.otp.job.processing

import groovy.transform.CompileStatic
import groovy.transform.Synchronized
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.roddy.execution.jobs.BatchEuphoriaJobManager
import de.dkfz.roddy.execution.jobs.JobManagerOptions
import de.dkfz.roddy.execution.jobs.cluster.lsf.LSFJobManager
import de.dkfz.roddy.execution.jobs.cluster.pbs.PBSJobManager
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.ngsdata.Realm

import java.time.Duration

@CompileStatic
class ClusterJobManagerFactoryService {
    @Autowired
    RemoteShellHelper remoteShellHelper
    ConfigService configService

    private Map<Realm, BatchEuphoriaJobManager> managerPerRealm = [:]

    BatchEuphoriaJobManager getJobManager(Realm realm) {
        BatchEuphoriaJobManager manager = managerPerRealm[realm]

        if (manager == null) {
            manager = createJobManager(realm)
        }
        return manager
    }

    @Synchronized
    private BatchEuphoriaJobManager createJobManager(Realm realm) {
        BatchEuphoriaJobManager manager = managerPerRealm[realm]
        if (!manager) {
            JobManagerOptions jobManagerParameters = JobManagerOptions.create()
                    .setCreateDaemon(false)
                    .setUpdateInterval(Duration.ZERO)
                    .setUserIdForJobQueries(configService.getSshUser())
                    .setTrackOnlyStartedJobs(false)
                    .setUserMask("027")
                    .setPassEnvironment(true) // module system
                    .setTimeZoneId(configService.getTimeZoneId())
                    .build()

            if (realm.jobScheduler == Realm.JobScheduler.PBS) {
                manager = new PBSJobManager(new BEExecutionServiceAdapter(remoteShellHelper, realm), jobManagerParameters)
            } else if (realm.jobScheduler == Realm.JobScheduler.LSF) {
                manager = new LSFJobManager(new BEExecutionServiceAdapter(remoteShellHelper, realm), jobManagerParameters)
            } else {
                throw new Exception("Unsupported cluster job scheduler")
            }
            managerPerRealm[realm] = manager
        }
        return manager
    }
}
