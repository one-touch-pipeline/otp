// reTrigger merged qa for specified id(s)

// retrigger "merged qa" workflow for given ids

import de.dkfz.tbi.otp.dataprocessing.*

println "\n\n create merged qa: "
List<ProcessedMergedBamFile> d = ProcessedMergedBamFile.withCriteria {
    'in'('id',[

     ] as long[])
}


//show all
d.each { println "${it}" }
println d.size()

/*
d.each {
    it.qualityAssessmentStatus = AbstractBamFile.QaProcessingStatus.NOT_STARTED
    println it.save(flush: true)
}
println d.size()
// */
''
