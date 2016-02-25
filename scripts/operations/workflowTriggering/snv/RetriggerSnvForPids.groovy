import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

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
        samplePairs.each {
            SnvCallingInstance.findAllBySamplePair(it).each {
                if (it.processingState == SnvProcessingStates.IN_PROGRESS) {
                    SnvJobResult.findAllBySnvCallingInstance(it)*.withdraw()
                    it.processingState = SnvProcessingStates.FAILED
                    it.save(flush: true, failOnError: true)
                }
            }

            it.processingStatus = SamplePair.ProcessingStatus.NEEDS_PROCESSING
            println it.save(flush: true, failOnError: true)
        }
    }
})
println samplePairs.size()
// */
