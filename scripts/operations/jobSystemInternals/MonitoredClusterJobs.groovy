/**
 * Lists the cluster jobs which are currently being monitored by the {@link PbsMonitorService}.
 */

import static de.dkfz.tbi.otp.utils.CollectionUtils.*
import de.dkfz.tbi.otp.job.processing.*

ctx.pbsMonitorService.queuedJobs.each { Job otpJob, clusterJobs ->
    ProcessingStep processingStep = otpJob.processingStep
    Process process = processingStep.process
    println "ProcessingStep ${processingStep.id} of ${process.jobExecutionPlan.name} on ${atMostOneElement(ProcessParameter.findAllByProcess(process))?.toObject()}"
    clusterJobs.each {
        println "  ${it}"
    }
}
''
