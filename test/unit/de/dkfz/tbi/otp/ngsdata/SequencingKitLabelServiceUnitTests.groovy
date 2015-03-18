package de.dkfz.tbi.otp.ngsdata

import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor

/**
 */
@TestFor(SequencingKitLabelService)
@Build([SequencingKitLabel])
class SequencingKitLabelServiceUnitTests {

    private final String KIT_NAME = "sequencingKitName"
    private final String ALIAS_KIT_NAME = "aliasSequencingKitName"

    void testFindSequencingKitLabelByNameOrAlias_FindByName(){
        SequencingKitLabel sequencingKitLabel = SequencingKitLabel.build(name: KIT_NAME)
        assert sequencingKitLabel == service.findSequencingKitLabelByNameOrAlias(KIT_NAME)
    }

    void testFindSequencingKitLabelByNameOrAlias_FindByAlias(){
        SequencingKitLabel sequencingKitLabel = SequencingKitLabel.build(name: KIT_NAME, alias: [ALIAS_KIT_NAME])
        assert sequencingKitLabel == service.findSequencingKitLabelByNameOrAlias(ALIAS_KIT_NAME)
    }

    void testFindSequencingKitLabelByNameOrAlias_FindNothing_ReturnNull(){
        SequencingKitLabel sequencingKitLabel = SequencingKitLabel.build(name: KIT_NAME)
        assert null == service.findSequencingKitLabelByNameOrAlias(ALIAS_KIT_NAME)
    }
}
