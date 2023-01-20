/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.job.processing

import grails.gorm.transactions.Transactional
import groovy.transform.Synchronized
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.roddy.execution.jobs.BatchEuphoriaJobManager
import de.dkfz.roddy.execution.jobs.JobManagerOptions
import de.dkfz.roddy.execution.jobs.cluster.lsf.LSFJobManager
import de.dkfz.roddy.execution.jobs.cluster.pbs.PBSJobManager
import de.dkfz.roddy.execution.jobs.cluster.slurm.SlurmJobManager
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.ngsdata.Realm

import java.time.Duration

@Transactional
class ClusterJobManagerFactoryService {
    @Autowired
    RemoteShellHelper remoteShellHelper
    ConfigService configService

    private final Map<Realm, BatchEuphoriaJobManager> managerPerRealm = [:]

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
                    .setUserIdForJobQueries(configService.sshUser)
                    .setTrackOnlyStartedJobs(false)
                    .setUserMask("027")
                    .setPassEnvironment(true) // module system
                    .setTimeZoneId(configService.timeZoneId)
                    .build()
            BEExecutionServiceAdapter bEExecutionServiceAdapter = new BEExecutionServiceAdapter(remoteShellHelper, realm)

            switch (realm.jobScheduler) {
                case Realm.JobScheduler.PBS:
                    manager = new PBSJobManager(bEExecutionServiceAdapter, jobManagerParameters)
                    break
                case Realm.JobScheduler.LSF:
                    manager = new LSFJobManager(bEExecutionServiceAdapter, jobManagerParameters)
                    break
                case Realm.JobScheduler.SLURM:
                    manager = new SlurmJobManager(bEExecutionServiceAdapter, jobManagerParameters)
                    break
                default:
                    throw new IllegalArgumentException("Unsupported cluster job scheduler \"${realm.jobScheduler}\"")
            }
            managerPerRealm[realm] = manager
        }
        return manager
    }
}
