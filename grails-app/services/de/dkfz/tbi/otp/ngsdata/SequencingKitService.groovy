package de.dkfz.tbi.otp.ngsdata

/**
 */
class SequencingKitService {

    public SequencingKit findSequencingKitByNameOrAlias(String nameOrAlias) {
        assert nameOrAlias: "the input 'nameOrAlias' is null"
        return SequencingKit.findByName(nameOrAlias) ?: SequencingKitSynonym.findByName(nameOrAlias)?.sequencingKit
    }
}
