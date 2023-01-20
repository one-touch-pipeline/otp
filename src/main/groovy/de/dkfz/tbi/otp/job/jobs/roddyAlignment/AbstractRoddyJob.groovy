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
package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import groovy.transform.CompileDynamic
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.roddy.JobStateLogFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.ProcessOutput

import java.nio.file.FileSystem

import static de.dkfz.tbi.otp.utils.logging.LogThreadLocal.threadLog

/**
 * class for roddy jobs that handle failed or not finished cluster jobs, analyse them and provide
 * information about their failure for {@link AbstractMaybeSubmitWaitValidateJob}
 */
@CompileDynamic
abstract class AbstractRoddyJob<R extends RoddyResult> extends AbstractMaybeSubmitWaitValidateJob {

    @Autowired
    ConfigService configService

    @Autowired
    ClusterJobSchedulerService clusterJobSchedulerService

    @Autowired
    FileSystemService fileSystemService

    @Autowired
    RoddyExecutionService roddyExecutionService

    @Override
    @SuppressWarnings("ThrowRuntimeException") // ignored: will be removed with the old workflow system
    protected final NextAction maybeSubmit() throws Throwable {
        Realm.withTransaction {
            final RoddyResult roddyResult = refreshedProcessParameterObject
            final Realm realm = roddyResult.project.realm
            String cmd = prepareAndReturnWorkflowSpecificCommand(roddyResult, realm)

            ProcessOutput output = roddyExecutionService.execute(cmd, realm)

            Collection<ClusterJob> submittedClusterJobs = roddyExecutionService.createClusterJobObjects(roddyResult, output, null, processingStep)

            if (submittedClusterJobs) {
                FileSystem fs = fileSystemService.getRemoteFileSystem(roddyResult.project.realm)
                roddyExecutionService.saveRoddyExecutionStoreDirectory(roddyResult, output.stderr, fs)
                submittedClusterJobs.each {
                    clusterJobSchedulerService.retrieveAndSaveJobInformationAfterJobStarted(it)
                    threadLog?.info("Log file: ${it.jobLog}")
                }
                return NextAction.WAIT_FOR_CLUSTER_JOBS
            }
            threadLog?.info 'Roddy has not submitted any cluster jobs. Running validate().'
            try {
                validate()
            } catch (Throwable t) {
                throw new RuntimeException('validate() failed after Roddy has not submitted any cluster jobs.', t)
            }
            return NextAction.SUCCEED
        }
    }

    protected abstract String prepareAndReturnWorkflowSpecificCommand(R roddyResult, Realm realm) throws Throwable

    @Override
    protected void validate() throws Throwable {
        Realm.withTransaction {
            final RoddyResult roddyResultObject = refreshedProcessParameterObject
            validate(roddyResultObject)
        }
    }

    protected abstract void validate(R roddyResultObject) throws Throwable

    @Override
    protected Map<ClusterJobIdentifier, String> failedOrNotFinishedClusterJobs(
            Collection<? extends ClusterJobIdentifier> finishedClusterJobs) throws Throwable {
        RoddyResult roddyResult = refreshedProcessParameterObject
        assert roddyResult

        // Roddy has started at least one cluster job, hence the jobStateLogFile must exist.
        File latestExecutionDirectory = roddyResult.latestWorkExecutionDirectory
        JobStateLogFile jobStateLogFile = JobStateLogFile.getInstance(latestExecutionDirectory)

        return analyseFinishedClusterJobs(finishedClusterJobs, jobStateLogFile)
    }

    Map<ClusterJobIdentifier, String> analyseFinishedClusterJobs(
            Collection<? extends ClusterJobIdentifier> finishedClusterJobs, JobStateLogFile jobStateLogFile) {
        Map<ClusterJobIdentifier, String> failedOrNotFinishedClusterJobs = [:]

        finishedClusterJobs.each {
            if (!jobStateLogFile.containsClusterJobId(it.clusterJobId)) {
                failedOrNotFinishedClusterJobs.put(it, "JobStateLogFile contains no information for this cluster job.")
            } else if (!jobStateLogFile.isClusterJobFinishedSuccessfully(it.clusterJobId)) {
                failedOrNotFinishedClusterJobs.put(it, "Status code: ${jobStateLogFile.getPropertyFromLatestLogFileEntry(it.clusterJobId, "statusCode")}")
            }
        }
        return failedOrNotFinishedClusterJobs
    }
}
