package de.dkfz.tbi.otp.job.processing

import java.util.regex.Pattern

import static de.dkfz.tbi.otp.utils.logging.LogThreadLocal.getThreadLog
import static org.springframework.util.Assert.notNull
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifierImpl
import de.dkfz.tbi.otp.ngsdata.Realm

/**
 * A service to construct paths and messages for logging the status of PBS cluster jobs.
 *
 */
class JobStatusLoggingService {

    final static LOGFILE_EXTENSION = '.log'
    final static STATUS_LOGGING_BASE_DIR = 'log/status'

    final static SHELL_SNIPPET_GET_NUMERIC_PBS_ID = '$(echo $PBS_JOBID | cut -d. -f1)'

    /**
     * Get the base directory of the status log file of a workflow.
     *
     * @param realm the realm the job runs in
     * @param processingStep the processing step of the job
     * @return the base directory of the status log file
     */
    String logFileBaseDir(Realm realm, ProcessingStep processingStep) {
        notNull realm, 'No realm specified.'
        notNull processingStep, 'No processing step specified.'
        return "${realm.loggingRootPath}/${STATUS_LOGGING_BASE_DIR}"
    }

    /**
     * Get the location of the status log file of a workflow.
     *
     * @param realm the realm the job runs in
     * @param processingStep the processing step of the job
     * @return the location of the status log file
     */
    String logFileLocation(Realm realm, ProcessingStep processingStep) {
        String baseDir = logFileBaseDir(realm, processingStep)
        return "${baseDir}/joblog_${processingStep.process.id}_${realm.id}${LOGFILE_EXTENSION}"
    }

    /**
     * Constructs a logging message for the status logging from a processing step. The message is a comma-separated
     * list of elements, namely
     * <ul>
     * <li>the workflow name,</li>
     * <li>the (non-qualified) class name of the job,</li>
     * <li>the ID of the processing step, and</li>
     * <li>the PBS job ID.</li>
     * </ul>
     * The field of previous or next steps is empty if the processing step does not have previous or next steps.
     * The message is <strong>not</strong> terminated by a newline character.
     *
     * @param processingStep the {@link ProcessingStep} to construct the message from
     * @param pbdId an optional PBS job id. If <code>null</code>, shell code to retrieve the numeric part of the
     *          PBS job id is returned. (Read: To get a message usable for logging, do not provide it.)
     * @return a logging message
     */
    String constructMessage(ProcessingStep processingStep, String pbsId = null) {
        notNull processingStep, 'No processing step specified.'
        String message = [
            processingStep.jobDefinition.plan.name,
            processingStep.jobClass.split('\\.')[-1],
            processingStep.id,
            pbsId ?: SHELL_SNIPPET_GET_NUMERIC_PBS_ID,
        ].join(',')
        return "${message}" as String
    }


    /**
     * Checks if cluster jobs have finished successfully.
     *
     * @param clusterJobIds The IDs of the cluster jobs to be checked.
     *
     * @return The jobs which have not completed successfully. This includes jobs which have failed and jobs which have
     *     not finished yet.
     */
    public Collection<ClusterJobIdentifier> failedOrNotFinishedClusterJobs(
            final ProcessingStep processingStep, final Realm realm, final Collection<String> clusterJobIds) {
        notNull processingStep
        notNull realm
        notNull clusterJobIds
        return failedOrNotFinishedClusterJobs(processingStep, [(realm): clusterJobIds])
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
    public Collection<ClusterJobIdentifier> failedOrNotFinishedClusterJobs(
            final ProcessingStep processingStep, final Collection<ClusterJobIdentifier> clusterJobs) {
        notNull processingStep
        notNull clusterJobs
        def invalidInput = clusterJobs.findAll( { it == null || it.realm == null || it.clusterJobId == null } )
        assert invalidInput == [] : "clusterJobs argument contains null values: ${invalidInput}"
        final Map<Realm, Collection<String>> map = clusterJobs.groupBy( {it.realm} ).collectEntries {
                realm, clusterJob -> [(realm): clusterJob.clusterJobId] }
        assert map.values().sum { it.size() } == clusterJobs.size()
        return failedOrNotFinishedClusterJobs(processingStep, map)
    }


    /**
     * Checks if cluster jobs have finished successfully.
     *
     * @param clusterJobs The cluster jobs to be checked. Mapping from {@link Realm} to a collection of IDs of cluster
     *     jobs on that realm.
     *
     * @return The jobs which have not completed successfully. This includes jobs which have failed and jobs which have
     *     not finished yet.
     */
    public Collection<ClusterJobIdentifier> failedOrNotFinishedClusterJobs(
            final ProcessingStep processingStep, final Map<Realm, Collection<String>> clusterJobs) {
        notNull processingStep
        notNull clusterJobs

        // OTP-967: try to figure out why processingStep sometimes returns NULL for .jobClass
        // lets try explicitly reconnecting it..
        processingStep.refresh()

        final Collection<ClusterJobIdentifier> failedOrNotFinishedClusterJobs = []
        clusterJobs.each { Realm realm, Collection<String> clusterJobIds ->
            notNull realm
            notNull clusterJobIds
            final File logFile = new File(logFileLocation(realm, processingStep))
            final String logFileText
            try {
                logFileText = logFile.text
            } catch (final FileNotFoundException e) {
                threadLog?.debug "Cluster job status log file ${logFile} not found."
                logFileText = ''
            }
            clusterJobIds.each {
                notNull it
                final String expectedLogMessage = constructMessage(processingStep, it)
                if (!(logFileText =~ /(?:^|\s)${Pattern.quote(expectedLogMessage)}(?:$|\s)/)) {
                    threadLog?.debug "Did not find \"${expectedLogMessage}\" in ${logFile}."
                    failedOrNotFinishedClusterJobs.add(new ClusterJobIdentifierImpl(realm, it))
                }
            }
        }
        return failedOrNotFinishedClusterJobs
    }
}
