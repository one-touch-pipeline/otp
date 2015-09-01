import de.dkfz.tbi.otp.dataprocessing.snvcalling.*

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
SamplePair.withTransaction {
    samplePairs.each {
        it.processingStatus = SamplePair.ProcessingStatus.NEEDS_PROCESSING
        println it.save(flush: true, failOnError: true)
    }
}
println samplePairs.size()
// */
