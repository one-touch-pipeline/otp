/**
 * Helper to show the (single and recursively merged) bam files of a merged bam file.
 * It use as input the id of a merging set.
 * It goes throw all PMBF of the set and shows the used PBF and PMBF recursive.
 */

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils;
import de.dkfz.tbi.otp.dataprocessing.*


//the merging set to check
def mergingSet = MergingSet.get(   )
assert mergingSet != null

//recursive helper for display content
def display = { display, bamFile, space = "" ->
  println "${space}PMBF ${bamFile} withdrawn ${bamFile.withdrawn}"
  println "${space}  MergingPass ${bamFile.mergingPass}"
  println "${space}    MergingSet ${bamFile.mergingSet}"
  ctx.abstractBamFileService.findByProcessedMergedBamFile(bamFile).each {
    if (it instanceof ProcessedMergedBamFile) {
      display(display, it, "      ${space}")
    } else {
      println "${space}      BAM file ${it} withdrawn ${it.withdrawn}"
    }
  }
}

MergingPass mergingPass = MergingPass.findAllByMergingSet(mergingSet).find{it.latestPass}
display(display, CollectionUtils.exactlyOneElement(ProcessedMergedBamFile.findAllByMergingPass(mergingPass)))
println ""
