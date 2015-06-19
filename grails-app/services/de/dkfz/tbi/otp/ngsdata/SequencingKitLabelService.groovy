package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.CollectionUtils

/**
 */
class SequencingKitLabelService {

    public static SequencingKitLabel findSequencingKitLabelByNameOrAlias(String nameOrAlias) {
        assert nameOrAlias: "the input 'nameOrAlias' is null"

        SequencingKitLabel sequencingKitLabel = SequencingKitLabel.findByName(nameOrAlias)

        if (sequencingKitLabel) {
            return sequencingKitLabel
        }

        return CollectionUtils.atMostOneElement(SequencingKitLabel.list().findAll {
            it.alias?.contains(nameOrAlias)
        })
    }


    public static SequencingKitLabel createNewSequencingKitLabel(String name, List<String> aliases = []) {
        assert name : "the input name must not be null"
        assert !findSequencingKitLabelByNameOrAlias(name) : "the SequencingKitLabel exists already"

        SequencingKitLabel sequencingKitLabel = new SequencingKitLabel(
                name: name,
                alias: aliases
        )
        assert sequencingKitLabel.save(flush: true)
        return sequencingKitLabel
    }


    public static SequencingKitLabel addNewAliasToSequencingKitLabel(String kitNameOrAlias, String alias) {
        assert kitNameOrAlias : "the input kitNameOrAlias must not be null"
        assert alias : "the input alias must not be null"

        SequencingKitLabel sequencingKitLabel = findSequencingKitLabelByNameOrAlias(kitNameOrAlias)
        assert sequencingKitLabel : "No SequencingKitLabel with name or alias ${kitNameOrAlias} exists"
        assert !sequencingKitLabel.alias.contains(alias) : "the alias was already created"
        sequencingKitLabel.alias.add(alias)
        assert sequencingKitLabel.save(flush: true)
        return sequencingKitLabel
    }
}
