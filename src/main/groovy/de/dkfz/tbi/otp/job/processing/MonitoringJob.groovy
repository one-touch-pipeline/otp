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

import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.job.scheduler.OldClusterJobMonitor
import de.dkfz.tbi.otp.job.scheduler.Scheduler

/**
 * Interface for {@link Job}s which are long running. An example is to
 * keep track of jobs running on a cluster. The interface is needed
 * in communication with the {@link OldClusterJobMonitor} as this service
 * invokes methods on this interface to notify the Job about state changes
 * of the tracked job on the cluster.
 *
 * A Job implementing this interface behaves different in the Scheduler.
 * When the execute() method finishes the end check is NOT performed.
 * Instead the Job is supposed to invoke the end check itself on the
 * SchedulerService.
 */
interface MonitoringJob extends EndStateAwareJob {

    /**
     * Callback to inform that the tracked cluster job finished on the Realm.
     *
     * <p>
     * An implementing class should use this method to have custom code
     * to handle the finishing of one cluster job.
     *
     * <p>
     * <strong>This method shall return quickly. If it triggers a time consuming operation, it shall
     * perform that operation asynchronously on a different thread by calling
     * {@link Scheduler#doOnOtherThread(Job, Closure)}.</strong>
     *
     * <p>
     * <strong>This method may be called on a different thread and with a different persistence context than the
     * {@link #execute()} method or other invocations of this method.</strong> So do not share domain objects between
     * method invocations.
     *
     * <p>
     * If this method throws an exception, this MonitoringJob will be marked as failed and finished.
     */
    void finished(ClusterJob finishedClusterJob)
}
