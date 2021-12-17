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
import groovy.transform.CompileStatic

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.ngsdata.Realm

import java.util.regex.Pattern

import static de.dkfz.tbi.otp.utils.logging.LogThreadLocal.threadLog
import static org.springframework.util.Assert.notNull

/**
 * A service to construct paths and messages for logging the status of cluster jobs.
 */
@CompileStatic
@Transactional
class JobStatusLoggingService {

    ClusterJobManagerFactoryService clusterJobManagerFactoryService
    ConfigService configService

    final static LOGFILE_EXTENSION = '.log'
    final static STATUS_LOGGING_BASE_DIR = 'log/status'

    private String shellSnippetForClusterJobId(Realm realm) {
        return "\$(echo \${${clusterJobManagerFactoryService.getJobManager(realm).jobIdVariable}} | cut -d. -f1)"
    }

    /**
     * Get the base directory of the status log file of a workflow.
     *
     * @param processingStep the processing step of the job
     * @return the base directory of the status log file
     */
    String logFileBaseDir(ProcessingStep processingStep) {
        notNull processingStep, 'No processing step specified.'
        return "${configService.loggingRootPath}/${STATUS_LOGGING_BASE_DIR}"
    }

    /**
     * Get the location of the status log file of a workflow.
     *
     * @param realm the realm the job runs in
     * @param processingStep the processing step of the job
     * @param clusterJobId an optional cluster job ID. If <code>null</code>, shell code to retrieve the numeric part
     *          of the cluster job id is returned.
     *          (Read: pass the job ID except if the returned string is used in a cluster job shell script)
     * @return the location of the status log file
     */
    String constructLogFileLocation(Realm realm, ProcessingStep processingStep, String clusterJobId = null) {
        notNull realm, 'No realm specified.'
        String baseDir = logFileBaseDir(processingStep)
        String fileName = [
                "joblog",
                processingStep.process.id,
                clusterJobId ?: shellSnippetForClusterJobId(realm),
                realm.id,
        ].join("_")
        return "${baseDir}/${fileName}${LOGFILE_EXTENSION}"
    }

    /**
     * Constructs a logging message for the status logging from a processing step. The message is a comma-separated
     * list of elements, namely
     * <ul>
     * <li>the workflow name,</li>
     * <li>the (non-qualified) class name of the job,</li>
     * <li>the ID of the processing step, and</li>
     * <li>the cluster job ID.</li>
     * </ul>
     * The field of previous or next steps is empty if the processing step does not have previous or next steps.
     * The message is <strong>not</strong> terminated by a newline character.
     *
     * @param processingStep the {@link ProcessingStep} to construct the message from
     * @param clusterJobId an optional cluster job id. If <code>null</code>, shell code to retrieve the numeric part
     *          of the cluster job id is returned. (Read: To get a message usable for logging, do not provide it.)
     * @return a logging message
     */
    String constructMessage(Realm realm, ProcessingStep processingStep, String clusterJobId = null) {
        notNull processingStep, 'No processing step specified.'
        String message = [
                processingStep.jobDefinition.plan.name,
                processingStep.nonQualifiedJobClass,
                processingStep.id,
                clusterJobId ?: shellSnippetForClusterJobId(realm),
        ].join(',')
        return "${message}" as String
    }

    /**
     * Checks if cluster jobs have finished successfully.
     *
     * @param clusterJobs The cluster jobs to be checked.
     *
     * @return The jobs which have not completed successfully. This includes jobs which have failed and jobs which have
     *     not finished yet. Note that the returned ClusterJobIdentifier objects are <em>not</em> identical to ones
     *     passed in the argument.
     */
    Collection<ClusterJobIdentifier> failedOrNotFinishedClusterJobs(
            final ProcessingStep processingStep, final Collection<ClusterJobIdentifier> clusterJobs) {
        notNull processingStep
        notNull clusterJobs
        def invalidInput = clusterJobs.findAll({ it == null || it.realm == null || it.clusterJobId == null })
        assert invalidInput == []: "clusterJobs argument contains null values: ${invalidInput}"
        final Map<Realm, Collection<ClusterJobIdentifier>> clusterJobMap = (clusterJobs.groupBy({
            it.realm
        }).collectEntries { realm, clusterJob ->
            [(realm): clusterJob]
        } as Map<Realm, Collection<ClusterJobIdentifier>>)
        assert clusterJobMap.values().flatten().size() == clusterJobs.size()
        return failedOrNotFinishedClusterJobs(processingStep, clusterJobMap)
    }

    /**
     * Checks if cluster jobs have finished successfully.
     *
     * @param clusterJobMap The cluster jobs to be checked. Mapping from {@link Realm} to a collection of cluster
     *     job identifiers on that realm.
     *
     * @return The jobs which have not completed successfully. This includes jobs which have failed and jobs which have
     *     not finished yet.
     */
    Collection<ClusterJobIdentifier> failedOrNotFinishedClusterJobs(
            final ProcessingStep processingStep, final Map<Realm, Collection<ClusterJobIdentifier>> clusterJobMap) {
        notNull processingStep
        notNull clusterJobMap

        // OTP-967: try to figure out why processingStep sometimes returns NULL for .jobClass
        // lets try explicitly reconnecting it..
        processingStep.refresh()

        final Collection<ClusterJobIdentifier> failedOrNotFinishedClusterJobs = []
        clusterJobMap.each { Realm realm, Collection<ClusterJobIdentifier> clusterJobs ->
            notNull realm
            notNull clusterJobs
            clusterJobs.each {
                final File logFile = new File(constructLogFileLocation(realm, processingStep, it.clusterJobId))
                final String logFileText
                try {
                    logFileText = logFile.text
                } catch (final FileNotFoundException e) {
                    threadLog?.debug "Cluster job status log file ${logFile} not found."
                    logFileText = ''
                }
                notNull it
                final String expectedLogMessage = constructMessage(realm, processingStep, it.clusterJobId)
                if (!(logFileText =~ /(?:^|\s)${Pattern.quote(expectedLogMessage)}(?:$|\s)/)) {
                    threadLog?.debug "Did not find \"${expectedLogMessage}\" in ${logFile}."
                    failedOrNotFinishedClusterJobs.add(new ClusterJobIdentifier(it))
                }
            }
        }
        return failedOrNotFinishedClusterJobs
    }
}
