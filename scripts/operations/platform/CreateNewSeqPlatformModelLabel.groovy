import de.dkfz.tbi.otp.ngsdata.*

String seqPlatformModelLabelName = "..."
//alias can be empty
List<String> aliases = []

//SeqPlatformModelLabelService.createNewSeqPlatformModelLabel(seqPlatformModelLabelName, aliases)

println SeqPlatformModelLabelService.findSeqPlatformModelLabelByNameOrAlias(seqPlatformModelLabelName)