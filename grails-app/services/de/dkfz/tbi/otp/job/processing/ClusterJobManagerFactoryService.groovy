package de.dkfz.tbi.otp.job.processing

import de.dkfz.roddy.execution.jobs.*
import de.dkfz.roddy.execution.jobs.cluster.lsf.*
import de.dkfz.roddy.execution.jobs.cluster.pbs.*
import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.ngsdata.*
import groovy.transform.*
import org.springframework.beans.factory.annotation.*

import java.time.*

@CompileStatic
class ClusterJobManagerFactoryService {
    @Autowired
    RemoteShellHelper remoteShellHelper
    ConfigService configService

    private Map<Realm, BatchEuphoriaJobManager> managerPerRealm = [:]

    BatchEuphoriaJobManager getJobManager(Realm realm) {
        BatchEuphoriaJobManager manager = managerPerRealm[realm]

        if (manager == null) {
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
            }  else {
                throw new Exception("Unsupported cluster job scheduler")
            }
            managerPerRealm[realm] = manager
        }
        return manager
    }
}
