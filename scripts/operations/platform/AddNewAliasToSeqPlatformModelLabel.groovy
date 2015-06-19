import de.dkfz.tbi.otp.ngsdata.*


String seqPlatformModelLabelNameOrAlias = "..."

List<String> aliases = []

aliases.each { String alias ->
    //SeqPlatformModelLabelService.addNewAliasToSeqPlatformModelLabel(seqPlatformModelLabelNameOrAlias, alias)
}

println SeqPlatformModelLabelService.findSeqPlatformModelLabelByNameOrAlias(seqPlatformModelLabelNameOrAlias).alias