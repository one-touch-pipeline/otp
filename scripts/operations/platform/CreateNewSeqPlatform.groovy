import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils



String seqPlatformName = "..."
SeqPlatformGroup seqPlatformGroup = null
SeqPlatformModelLabel seqPlatformModelLabel = null
SequencingKitLabel sequencingKitLabel = null

/*
 * These values can stay empty or null.
 * To create a new SeqTrack it is not necessary to fill in the objects SeqPlatformGroup, SeqPlatformModelLabel, SequencingKitLabel.
 */
/*
String seqPlatformModelLabelNameOrAlias = "..."
seqPlatformModelLabel = SeqPlatformModelLabelService.findSeqPlatformModelLabelByNameOrAlias(seqPlatformModelLabelNameOrAlias)
println "seqPlatformModelLabel " + seqPlatformModelLabel
//*/

/*
String seqPlatformGroupName = "..."
seqPlatformGroup = CollectionUtils.exactlyOneElement(
    SeqPlatformGroup.findAllByName(seqPlatformGroupName)
)
println "seqPlatformGroup " + seqPlatformGroup
//*/

/*
String sequencingKitLabelNameOrAlias = "..."
sequencingKitLabel = SequencingKitLabelService.findSequencingKitLabelByNameOrAlias(sequencingKitLabelNameOrAlias)
println "sequencingKitLabel " + sequencingKitLabel
//*/



SeqPlatformService.createNewSeqPlatform(seqPlatformName, seqPlatformGroup,seqPlatformModelLabel, sequencingKitLabel)

println SeqPlatformService.findForNameAndModelAndSequencingKit(seqPlatformName, seqPlatformModelLabel, sequencingKitLabel)