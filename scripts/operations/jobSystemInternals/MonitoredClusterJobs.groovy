/**
 * Lists the cluster jobs which are currently being monitored by the {@link ClusterJobMonitoringService}.
 */

import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*


ClusterJobMonitoringService clusterJobMonitoringService = ctx.clusterJobMonitoringService

clusterJobMonitoringService.queuedJobs.each { Job otpJob, clusterJobs ->
    ProcessingStep processingStep = otpJob.processingStep
    Process process = processingStep.process
    println "ProcessingStep ${processingStep.id}: ${processingStep.jobDefinition.name} on ${atMostOneElement(ProcessParameter.findAllByProcess(process))?.toObject()}"
    clusterJobs.each {
        println "  ${it}"
    }
}
''
