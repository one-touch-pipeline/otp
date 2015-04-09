/**
 * Script to manually create a new merging set.
 * The script "CheckMergingSet" can be used to find the needed bam file ids.
 */

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.*

//the merging work package to use
MergingWorkpackage mwp = MergingSet.get(    ).mergingWorkPackage,
//the ids of the bam files to use in the merging set
List<Long> bamFileIds = [    ]


//---------------------------


//intern helper
MergingSet createNewMergingSetAndAssignBamFiles(MergingWorkPackage workPackage, Collection<Long> bamFileIds) {
    List<AbstractBamFile> bamFiles = bamFileIds.collect {AbstractBamFile.get(it)}

    MergingSet mergingSet = new MergingSet(identifier: MergingSet.nextIdentifier(workPackage), mergingWorkPackage: workPackage)
    ctx.mergingSetService.assertSave(mergingSet)
    bamFiles.each {AbstractBamFile bamFile ->
        MergingSetAssignment mergingAssigment = new MergingSetAssignment(mergingSet: mergingSet, bamFile: bamFile)
        ctx.mergingSetService.assertSave(mergingAssigment)
        ctx.abstractBamFileService.assignedToMergingSet(bamFile)
    }
    mergingSet.status = MergingSet.State.NEEDS_PROCESSING
    ctx.mergingSetService.assertSave(mergingSet)
    println "created a new mergingSet: ${mergingSet}"
    return mergingSet
}

MergingSet.withTransaction {
    MergingSet mergingSet = createNewMergingSetAndAssignBamFiles(
      mwp, bamFileIds.collect {id as Long})

    ctx.abstractBamFileService.findByMergingSet(mergingSet).each { println "${it}" + (it instanceof ProcessedBamFile ? "  ${it.alignmentPass}" : "") }
    println "\n\n"
}
println ""
