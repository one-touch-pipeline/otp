import de.dkfz.tbi.otp.ngsdata.*

String seqPlatformGroupName = "..."

//SeqPlatformGroupService.createNewSeqPlatformGroup(seqPlatformGroupName)

println SeqPlatformGroupService.findAllByName(seqPlatformGroupName)