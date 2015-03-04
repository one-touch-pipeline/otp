package de.dkfz.tbi.otp.ngsdata

import grails.buildtestdata.mixin.Build

/**
 */
@TestFor(SequencingKitService)
@Build([SequencingKitSynonym])
class SequencingKitServiceUnitTests {

    private final String KIT_NAME = "sequencingKitName"
    private final String ALIAS_KIT_NAME = "aliasSequencingKitName"

    void testFindSequencingKitByNameOrAlias_FindByName(){
        SequencingKit sequencingKit = SequencingKit.build(name: KIT_NAME)
        assert sequencingKit == service.findSequencingKitByNameOrAlias(KIT_NAME)
    }

    void testFindSequencingKitByNameOrAlias_FindByAlias(){
        SequencingKit sequencingKit = SequencingKit.build(name: KIT_NAME)
        SequencingKitSynonym.build(name: ALIAS_KIT_NAME, sequencingKit: sequencingKit)
        assert sequencingKit == service.findSequencingKitByNameOrAlias(ALIAS_KIT_NAME)
    }

    void testFindSequencingKitByNameOrAlias_FindNothing_ReturnNull(){
        SequencingKit sequencingKit = SequencingKit.build(name: KIT_NAME)
        assert null == service.findSequencingKitByNameOrAlias(ALIAS_KIT_NAME)
    }
}
