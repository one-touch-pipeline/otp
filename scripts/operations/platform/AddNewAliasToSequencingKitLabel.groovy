import de.dkfz.tbi.otp.ngsdata.*


String sequencingKitLabelNameOrAlias = "..."

List<String> aliases = []

aliases.each { String alias ->
    //SequencingKitLabelService.addNewAliasToSequencingKitLabel(sequencingKitLabelNameOrAlias, alias)
}

println SequencingKitLabelService.findSequencingKitLabelByNameOrAlias(sequencingKitLabelNameOrAlias).alias