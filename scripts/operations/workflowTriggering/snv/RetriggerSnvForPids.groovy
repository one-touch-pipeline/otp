import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.utils.logging.*

println "\n\nretrigger snv for pids: "
def samplePairs = SamplePair.withCriteria {
    mergingWorkPackage1 {
        sample {
            individual {
                'in'('pid', [

                ])
            }
        }
    }
}


//show all
samplePairs.each { println "${it}" }
println samplePairs.size()

/*
LogThreadLocal.withThreadLog(System.out, {
    SamplePair.withTransaction {
        samplePairs.each { SamplePair samplePair ->
            AbstractSnvCallingInstance.findAllBySamplePair(samplePair).each { AbstractSnvCallingInstance instance ->
                if (instance.processingState == AnalysisProcessingStates.IN_PROGRESS) {
                    instance.withdrawn = true
                    instance.save(flush: true, failOnError: true)
                }
            }

            samplePair.snvProcessingStatus = SamplePair.ProcessingStatus.NEEDS_PROCESSING
            println samplePair.save(flush: true, failOnError: true)
        }
    }
})
println samplePairs.size()
// */
