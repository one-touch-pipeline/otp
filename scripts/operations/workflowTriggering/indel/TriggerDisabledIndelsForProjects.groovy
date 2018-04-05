import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.utils.logging.*

println "\n\ntrigger disabled indel projects: "
def samplePairs = SamplePair.withCriteria {
    mergingWorkPackage1 {
        sample {
            individual {
                project {
                    'in'('name', [
                            ''
                    ])
                }
            }
        }
    }
    eq('indelProcessingStatus', SamplePair.ProcessingStatus.DISABLED)
}


//show all
samplePairs.each { println "${it}" }
println samplePairs.size()

/*
LogThreadLocal.withThreadLog(System.out, {
    SamplePair.withTransaction {
        samplePairs.each {
            it.indelProcessingStatus = SamplePair.ProcessingStatus.NEEDS_PROCESSING
            println it.save(flush: true, failOnError: true)
        }
    }
})
println samplePairs.size()
// */
