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

import groovy.util.logging.Slf4j

import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier

/**
 * Base class for jobs which maybe submit cluster jobs, wait for them to finish, and then validate their results.
 */
@Slf4j
abstract class AbstractMaybeSubmitWaitValidateJob extends AbstractMultiJob {

    @Override
    protected final NextAction execute(final Collection<? extends ClusterJobIdentifier> finishedClusterJobs) throws Throwable {
        if (finishedClusterJobs) {
            Map<ClusterJobIdentifier, String> failedClusterJobs = failedOrNotFinishedClusterJobs(finishedClusterJobs)
            if (failedClusterJobs.isEmpty()) {
                log.info "All ${finishedClusterJobs.size()} cluster jobs have finished successfully."
            } else {
                throw new RuntimeException(createExceptionString(failedClusterJobs, finishedClusterJobs))
            }
            validate()
            return NextAction.SUCCEED
        }
        return maybeSubmit()
    }

    protected String createExceptionString(Map<ClusterJobIdentifier, String> failedClusterJobs,
                                           Collection<? extends ClusterJobIdentifier> finishedClusterJobs) {
        Comparator sortByClusterJobId = { ClusterJobIdentifier identifier1, ClusterJobIdentifier identifier2 ->
            identifier1.clusterJobId <=> identifier2.clusterJobId
        } as Comparator

        String failedJobsOfTotalJobs = "${failedClusterJobs.size()} of ${finishedClusterJobs.size()} cluster jobs failed:"

        List<String> failedClusterJobsDetails = []
        failedClusterJobs.sort(sortByClusterJobId).collect { ClusterJobIdentifier clusterJobIdentifier, String reason ->
            failedClusterJobsDetails.add("${clusterJobIdentifier}: " +
                    "${reason}\n${"Log file: ${ClusterJob.getByClusterJobIdentifier(clusterJobIdentifier, processingStep).jobLog}"}\n")
        }

        return "\n${failedJobsOfTotalJobs}\n\n${failedClusterJobsDetails.join("\n")}"
    }

    @Override
    Collection<ClusterJob> failedOrNotFinishedClusterJobs() {
        Collection<ClusterJob> clusterJobs = ClusterJob.findAllByProcessingStep(processingStep)
        if (!clusterJobs) {
            throw new RuntimeException("No ClusterJobs found for ${processingStep}")
        }

        return failedOrNotFinishedClusterJobs(
                ClusterJobIdentifier.asClusterJobIdentifierList(clusterJobs)
        ).collect { ClusterJobIdentifier identifier, String error ->
            return ClusterJob.getByClusterJobIdentifier(identifier, processingStep)
        }
    }

    /**
     * Returns all failed or not finished ClusterJobs
     */
    protected abstract Map<ClusterJobIdentifier, String> failedOrNotFinishedClusterJobs(Collection<? extends ClusterJobIdentifier> finishedClusterJobs)
            throws Throwable

    /**
     * Called when the job is started.
     *
     * @return What should be done next. If cluster jobs were submitted during the execution of this method, the method
     * must return {@link NextAction#WAIT_FOR_CLUSTER_JOBS}, otherwise it must return {@link NextAction#SUCCEED}.
     * In case of {@link NextAction#WAIT_FOR_CLUSTER_JOBS}, the job system will notify this job about the cluster jobs
     * having finished by calling the {@link #validate()} method.
     *
     * @throws Throwable Throwing will make this job fail.
     */
    protected abstract NextAction maybeSubmit() throws Throwable

    /**
     * Called when <em>all</em> of the cluster jobs which were submitted by {@link #maybeSubmit()} have finished
     * <em>successfully</em>.
     *
     * <p>
     * After this method returns, this job will succeed and finish.
     *
     * <p>
     * This method must not submit any cluster jobs. If you need more flexibility, subclass {@link AbstractMultiJob}
     * directly.
     *
     * <p>
     * <strong>TODO: As long as OTP-1026 is not resolved, this method must return quickly.</strong>
     *
     * <p>
     * <strong>This method may be called on a different thread, with a different persistence context and on another
     * instance of the implementing class than the {@link #maybeSubmit()} method.</strong> So:
     * <ul>
     *     <li>Do not share domain objects between the methods.</li>
     *     <li>Do not rely on instance variables for sharing information between the methods.</li>
     * </ul>
     *
     * @throws Throwable Throwing will make this job fail.
     */
    protected abstract void validate() throws Throwable
}
