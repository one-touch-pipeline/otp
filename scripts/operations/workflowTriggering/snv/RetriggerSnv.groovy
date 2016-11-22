import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

println "\n\n retrigger snv: "
def snvCallingInstances = SnvCallingInstance.withCriteria {
    'in'("id", [
            // ids of SnvCallingInstances

    ] as long[])
}

//show all
snvCallingInstances.each { println "${it}" }
println snvCallingInstances.size()

/*
LogThreadLocal.withThreadLog(System.out, {
    SamplePair.withTransaction {
        snvCallingInstances.each {
            if (it.processingState == AnalysisProcessingStates.IN_PROGRESS) {
                SnvJobResult.findAllBySnvCallingInstance(it)*.withdraw()
                it.withdrawn = true
                it.save(flush: true, failOnError: true)
            }

            it.samplePair.processingStatus = SamplePair.ProcessingStatus.NEEDS_PROCESSING
            println it.samplePair.save(flush: true, failOnError: true)
        }
    }
})
println snvCallingInstances.size()
// */
