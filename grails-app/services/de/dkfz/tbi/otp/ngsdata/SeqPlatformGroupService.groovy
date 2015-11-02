package de.dkfz.tbi.otp.ngsdata

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

/**
 */
class SeqPlatformGroupService {

    public static SeqPlatformGroup findSeqPlatformGroup(String name) {
        SeqPlatformGroup seqPlatformGroup = atMostOneElement(SeqPlatformGroup.findAllByName(name))
        return seqPlatformGroup
    }

    public static createNewSeqPlatformGroup(String name) {
        assert name: "the input name must not be null"
        assert !SeqPlatformGroup.findByName(name) : "The seqPlatformGroup exists already, the name must be unique."

        SeqPlatformGroup seqPlatformGroup = new SeqPlatformGroup(
                name: name
        )
        assert seqPlatformGroup.save(flush: true)
        return seqPlatformGroup
    }
}
