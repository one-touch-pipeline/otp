package operations.workflowTriggering.alignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal


LogThreadLocal.withThreadLog(System.out, { SeqTrack.withTransaction {

    Collection<RoddyBamFile> roddyBamFiles = RoddyBamFile.withCriteria {
        'in'('id', [
                // RoddyBamFile IDs
                -1,

        ] as long[])
    }

    roddyBamFiles.each {
        println "${it}"
    }
    println roddyBamFiles.size()
    println '\n'

    /*
    roddyBamFiles.each {
        it.makeWithdrawn()
        it.workPackage.needsProcessing = true
        it.save(flush: true)
        println "restart ${it}"
    }
    //*/
}})
println ''
