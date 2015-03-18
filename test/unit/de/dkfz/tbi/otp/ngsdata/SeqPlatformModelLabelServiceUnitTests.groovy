package de.dkfz.tbi.otp.ngsdata

import grails.buildtestdata.mixin.Build

/**
 */
@TestFor(SeqPlatformModelLabelService)
@Build([SeqPlatformModelLabel])
class SeqPlatformModelLabelServiceUnitTests {

    private final String MODEL_NAME = "model name"
    private final String ALIAS_MODEL_NAME = "model alias"

    void testFindSeqPlatformModelLabelByNameOrAlias_FindByName(){
        SeqPlatformModelLabel seqPlatformModelLabel = SeqPlatformModelLabel.build(name: MODEL_NAME)
        assert seqPlatformModelLabel == service.findSeqPlatformModelLabelByNameOrAlias(MODEL_NAME)
    }

    void testFindSeqPlatformModelLabelByNameOrAlias_FindByAlias(){
        SeqPlatformModelLabel seqPlatformModelLabel = SeqPlatformModelLabel.build(name: MODEL_NAME, alias: [ALIAS_MODEL_NAME])
        assert seqPlatformModelLabel == service.findSeqPlatformModelLabelByNameOrAlias(ALIAS_MODEL_NAME)
    }

    void testFindSeqPlatformModelLabelByNameOrAlias_FindNothing_ReturnNull(){
        SeqPlatformModelLabel seqPlatformModelLabel = SeqPlatformModelLabel.build(name: MODEL_NAME)
        assert null == service.findSeqPlatformModelLabelByNameOrAlias(ALIAS_MODEL_NAME)
    }
}
