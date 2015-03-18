package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.CollectionUtils

/**
 */
class SequencingKitLabelService {

    public SequencingKitLabel findSequencingKitLabelByNameOrAlias(String nameOrAlias) {
        assert nameOrAlias: "the input 'nameOrAlias' is null"

        SequencingKitLabel sequencingKitLabel = SequencingKitLabel.findByName(nameOrAlias)

        if (sequencingKitLabel) {
            return sequencingKitLabel
        }

        return CollectionUtils.atMostOneElement(SequencingKitLabel.list().findAll {
            it.alias?.contains(nameOrAlias)
        })
    }
}
