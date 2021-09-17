/*
 * Copyright 2011-2021 The OTP authors
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
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.dataprocessing.snvcalling.RoddySnvCallingInstance
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.ProcessOutput
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.*
import java.util.concurrent.Semaphore
import java.util.regex.Matcher
import java.util.regex.Pattern

@Transactional
class RoddyExecutionService {

    @Autowired
    ClusterJobService clusterJobService
    @Autowired
    ConfigService configService
    @Autowired
    ProcessingOptionService processingOptionService
    @Autowired
    RemoteShellHelper remoteShellHelper

    static final String NO_STARTED_JOBS_MESSAGE = '\nThere were no started jobs, the execution directory will be removed.\n'

    // suppressed because breaking the line would break the pattern
    @SuppressWarnings("LineLength")
    static final Pattern RODDY_EXECUTION_STORE_DIRECTORY_PATTERN = Pattern.compile(/(?:^|\n)Creating\sthe\sfollowing\sexecution\sdirectory\sto\sstore\sinformation\sabout\sthis\sprocess:\s*\n\s*(\/.*\/${RoddySnvCallingInstance.RODDY_EXECUTION_DIR_PATTERN})(?:\n|$)/)

    // Example:
    // Running job r150428_104246480_stds_snvCallingMetaScript => 3504988
    static final Pattern RODDY_OUTPUT_PATTERN = Pattern.compile(/^\s*(?:Running|Rerun)\sjob\s(.*_(\S+))\s=>\s(\S+)\s*$/)

    private Semaphore numberOfRoddyProcesses

    void clearRoddyExecutionStoreDirectory(RoddyResult roddyResult) {
        if (roddyResult.roddyExecutionDirectoryNames && !roddyResult.workDirectory.exists()) {
            roddyResult.roddyExecutionDirectoryNames.clear()
            roddyResult.save(flush: true)
        }
    }

    ProcessOutput execute(String cmd, Realm realm) {
        assert cmd
        assert realm

        numberOfRoddyProcesses = numberOfRoddyProcesses ?:
                new Semaphore(processingOptionService.findOptionAsInteger(ProcessingOption.OptionName.MAXIMUM_EXECUTED_RODDY_PROCESSES), true)

        ProcessOutput output
        numberOfRoddyProcesses.acquire()
        try {
            output = remoteShellHelper.executeCommandReturnProcessOutput(realm, cmd).assertExitCodeZero()
        } finally {
            numberOfRoddyProcesses.release()
        }
        if (output.stderr.contains("java.lang.OutOfMemoryError")) {
            throw new RoddyException('An out of memory error occurred when executing Roddy.')
        } else if (output.stderr.contains("An uncaught error occurred during a run. SEVERE")) {
            throw new RoddyException('An unexpected error occurred when executing Roddy.')
        }
        return output
    }

    Collection<ClusterJob> createClusterJobObjects(RoddyResult roddyResult, ProcessOutput roddyOutput, WorkflowStep workflowStep = null,
                                                   @Deprecated ProcessingStep processingStep = null) {
        assert roddyResult
        assert roddyOutput
        assert (workflowStep == null) ^ (processingStep == null)

        Collection<ClusterJob> submittedClusterJobs = []
        roddyOutput.stdout.eachLine {
            if (it.trim().isEmpty()) {
                return //skip empty lines
            }
            Matcher m = it =~ RODDY_OUTPUT_PATTERN
            if (m.matches()) {
                String jobName = m.group(1)
                String jobClass = m.group(2)
                String jobId = m.group(3)

                if (!jobId.matches(/[1-9]\d*/)) {
                    throw new RoddyException("'${jobId}' is not a valid job ID.")
                }

                ClusterJob clusterJob
                if (processingStep) {
                    clusterJob = clusterJobService.createClusterJob(
                            roddyResult.project.realm, jobId, configService.getSshUser(), processingStep, roddyResult.getSeqType(), jobName, jobClass
                    )
                } else {
                    clusterJob = clusterJobService.createClusterJob(
                            roddyResult.project.realm, jobId, configService.getSshUser(), workflowStep, jobName, jobClass
                    )
                }
                submittedClusterJobs.add(clusterJob)
            }
        }
        assert submittedClusterJobs.empty == roddyOutput.stderr.contains(NO_STARTED_JOBS_MESSAGE)
        return submittedClusterJobs
    }

    void saveRoddyExecutionStoreDirectory(RoddyResult roddyResult, String roddyOutput, FileSystem fileSystem) {
        assert roddyResult

        Path directory = parseRoddyExecutionStoreDirectoryFromRoddyOutput(roddyOutput, fileSystem)
        assert directory.parent == fileSystem.getPath(roddyResult.workExecutionStoreDirectory.absolutePath)
        FileService.waitUntilExists(directory)
        assert Files.isDirectory(directory)

        roddyResult.roddyExecutionDirectoryNames.add(directory.fileName.toString())

        assert roddyResult.roddyExecutionDirectoryNames.last() == roddyResult.roddyExecutionDirectoryNames.max()
        assert roddyResult.save(flush: true)
    }

    private Path parseRoddyExecutionStoreDirectoryFromRoddyOutput(String roddyOutput, FileSystem fileSystem) {
        Matcher m = roddyOutput =~ RODDY_EXECUTION_STORE_DIRECTORY_PATTERN
        if (m.find()) {
            Path directory = fileSystem.getPath(m.group(1))
            assert !m.find()
            return directory
        } else {
            throw new RoddyException("Roddy output contains no information about output directories")
        }
    }
}
