package de.dkfz.tbi.otp.ngsdata

/**
 */
class SeqPlatformGroupService {

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
