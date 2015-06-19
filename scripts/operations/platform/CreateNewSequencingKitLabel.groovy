import de.dkfz.tbi.otp.ngsdata.*


String sequencingKitLabelName = "..."
//alias can be empty
List<String> aliases = []

//SequencingKitLabelService.createNewSequencingKitLabel(sequencingKitLabelName, aliases)

println SequencingKitLabelService.findSequencingKitLabelByNameOrAlias(sequencingKitLabelName)