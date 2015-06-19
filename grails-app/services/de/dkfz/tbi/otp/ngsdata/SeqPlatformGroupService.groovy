package de.dkfz.tbi.otp.ngsdata

/**
 */
class SeqPlatformGroupService {

    public static createNewSeqPlatformGroup(String name) {
        assert name: "the input name must not be null"
        assert !SeqPlatformGroup.findByName(name) : "The seqPlatformGroup exists already"

        SeqPlatformGroup seqPlatformGroup = new SeqPlatformGroup(
                name: name
        )
        assert seqPlatformGroup.save(flush: true)
        return seqPlatformGroup
    }
}
