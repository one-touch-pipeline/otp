package de.dkfz.tbi.otp.ngsdata

class SeqPlatformDSL {

    public static def seqPlatformDef = {String vendor, String model, c ->
        SeqPlatform platform = new SeqPlatform(name: vendor, model: model)
        assert(platform.save())
        c.name = {String name ->
            assert((new SeqPlatformModelIdentifier(seqPlatform: platform, name: name)).save())
        }
        c()
        assert(platform.save(flush: true))
    }
}
