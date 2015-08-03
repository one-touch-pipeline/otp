import de.dkfz.tbi.otp.dataprocessing.snvcalling.*

println "\n\n retrigger snv: "
def snvCallingInstances = SnvCallingInstance.withCriteria {
    'in'("id", [
            // ids of SnvCallingInstances

    ] as long[] )
}

//show all
snvCallingInstances.each { println "${it}" }
println snvCallingInstances.size()

/*
SamplePair.withTransaction {
    snvCallingInstances.each {
        if (it.processingState == SnvProcessingStates.IN_PROGRESS) {
            SnvJobResult.findAllBySnvCallingInstance(it)*.makeWithdrawn()
            it.processingState = SnvProcessingStates.FAILED
            it.save(flush: true, failOnError: true)
        }

        it.samplePair.processingStatus = SamplePair.ProcessingStatus.NEEDS_PROCESSING
        println it.samplePair.save(flush: true, failOnError: true)
    }
}
println snvCallingInstances.size()
// */
