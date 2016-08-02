//(Re)Trigger create merging set for specified ids

//Trigger "create merging set" workflow for all ProcessedBamfile given by id (also, if it was already created)
//Attention: If the object is already part of the last merging set, the merging workflow will fail for duplicate entries.

import de.dkfz.tbi.otp.dataprocessing.*

println "\n\n retrigger merging set: "
List<ProcessedBamFile> d = ProcessedBamFile.withCriteria {
    'in'("id", [
            // the ids




    ] as long[])
}

d.each { println "     ${it.alignmentPass.seqTrack.sample}" }

/*
d.each {
    it.status = AbstractBamFile.State.NEEDS_PROCESSING
    println it.save(flush: true)
}
// */
println d.size()
''
