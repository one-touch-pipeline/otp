package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.CollectionUtils

/**
 */
class SeqPlatformModelLabelService {

    /**
     * Returns the SeqPlatformModelLabel when its name or alias contains the nameOrAlias input.
     * Returns null if no SeqPlatformModelLabel could be found.
     */
    public static SeqPlatformModelLabel findSeqPlatformModelLabelByNameOrAlias(String nameOrAlias) {
        assert nameOrAlias: "the input 'nameOrAlias' is null"

        SeqPlatformModelLabel seqPlatformModelLabel = SeqPlatformModelLabel.findByName(nameOrAlias)

        if (seqPlatformModelLabel) {
            return seqPlatformModelLabel
        }

        return CollectionUtils.atMostOneElement(SeqPlatformModelLabel.list().findAll {
            it.alias?.contains(nameOrAlias)
        })
    }


    public static SeqPlatformModelLabel createNewSeqPlatformModelLabel(String name, List<String> aliases = []) {
        assert name : "the input name must not be null"
        assert !findSeqPlatformModelLabelByNameOrAlias(name) : "the SeqPlatformModelLabel exists already"

        SeqPlatformModelLabel seqPlatformModelLabel =  new SeqPlatformModelLabel(
                name: name,
                alias: aliases
        )
        assert seqPlatformModelLabel.save(flush: true)
        return seqPlatformModelLabel
    }


    public static SeqPlatformModelLabel addNewAliasToSeqPlatformModelLabel(String platformNameOrAlias, String alias) {
        assert platformNameOrAlias : "the input platformNameOrAlias must not be null"
        assert alias : "the input alias must not be null"

        SeqPlatformModelLabel seqPlatformModelLabel = findSeqPlatformModelLabelByNameOrAlias(platformNameOrAlias)
        assert seqPlatformModelLabel : "No SeqPlatformModelLabel with name or alias ${platformNameOrAlias} exists"
        assert !seqPlatformModelLabel.alias.contains(alias) : "the alias was already created"
        seqPlatformModelLabel.alias.add(alias)
        assert seqPlatformModelLabel.save(flush: true)
        return seqPlatformModelLabel
    }
}
