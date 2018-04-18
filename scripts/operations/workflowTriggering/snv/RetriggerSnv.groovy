import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.utils.logging.*

println "\n\n retrigger snv: "
def snvCallingInstances = RoddySnvCallingInstance.withCriteria {
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

            it.samplePair.snvProcessingStatus = SamplePair.ProcessingStatus.NEEDS_PROCESSING
            println it.samplePair.save(flush: true, failOnError: true)
        }
    }
})
println snvCallingInstances.size()
// */
