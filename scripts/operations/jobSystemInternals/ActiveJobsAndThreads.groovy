import de.dkfz.tbi.otp.job.processing.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*
println "SCHEDULER ACTIVE"
println ctx.schedulerService.active
println ""
println "SCHEDULER QUEUE"
ctx.schedulerService.queue.each {
    println it
}
println ""
println "RUNNING JOBS"
ctx.schedulerService.getRunning().each {
    println "${it}\t(ProcessingStep ID ${it.processingStep.id}, ${it.processingStep.process.jobExecutionPlan.name} on ${atMostOneElement(ProcessParameter.findAllByProcess(it.processingStep.process))?.toObject()}"
}
println ""
println "EXECUTOR SERVICE\nQueue size: ${ctx.executorService.executor.queue.size()}\n"
println "THREADS"
Thread.getAllStackTraces().each {
    println "${it.key} (ID ${it.key.id})"
    it.value.each {
        println "  ${it}"
    }
}
"EOF"
