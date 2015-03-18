package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.CollectionUtils

/**
 */
class SeqPlatformModelLabelService {

    /**
     * Returns the SeqPlatformModelLabel when its name or alias contains the nameOrAlias input.
     * Returns null if no SeqPlatformModelLabel could be found.
     */
    public SeqPlatformModelLabel findSeqPlatformModelLabelByNameOrAlias(String nameOrAlias) {
        assert nameOrAlias: "the input 'nameOrAlias' is null"

        SeqPlatformModelLabel seqPlatformModelLabel = SeqPlatformModelLabel.findByName(nameOrAlias)

        if (seqPlatformModelLabel) {
            return seqPlatformModelLabel
        }

        return CollectionUtils.atMostOneElement(SeqPlatformModelLabel.list().findAll {
            it.alias?.contains(nameOrAlias)
        })
    }
}
