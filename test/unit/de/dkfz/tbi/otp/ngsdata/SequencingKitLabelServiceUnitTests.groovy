package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.TestCase
import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor
import org.junit.Test

/**
 */
@TestFor(SequencingKitLabelService)
@Build([SequencingKitLabel])
class SequencingKitLabelServiceUnitTests {

    private final String KIT_NAME = "sequencingKitName"
    private final String ALIAS_KIT_NAME = "aliasSequencingKitName"

    @Test
    void testFindSequencingKitLabelByNameOrAlias_FindByName(){
        SequencingKitLabel sequencingKitLabel = SequencingKitLabel.build(name: KIT_NAME)
        assert sequencingKitLabel == service.findSequencingKitLabelByNameOrAlias(KIT_NAME)
    }

    @Test
    void testFindSequencingKitLabelByNameOrAlias_FindByAlias(){
        SequencingKitLabel sequencingKitLabel = SequencingKitLabel.build(name: KIT_NAME, alias: [ALIAS_KIT_NAME])
        assert sequencingKitLabel == service.findSequencingKitLabelByNameOrAlias(ALIAS_KIT_NAME)
    }

    @Test
    void testFindSequencingKitLabelByNameOrAlias_FindNothing_ReturnNull(){
        SequencingKitLabel sequencingKitLabel = SequencingKitLabel.build(name: KIT_NAME)
        assert null == service.findSequencingKitLabelByNameOrAlias(ALIAS_KIT_NAME)
    }

    @Test
    void testCreateNewSequencingKitLabel_InputNameIsNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            SequencingKitLabelService.createNewSequencingKitLabel(null)
        }
    }

    @Test
    void testCreateNewSequencingKitLabel_ExistsAlready_ShouldFail() {
        SequencingKitLabel sequencingKitLabel = SequencingKitLabel.build(name: KIT_NAME)
        TestCase.shouldFail(AssertionError) {
            SequencingKitLabelService.createNewSequencingKitLabel(KIT_NAME)
        }
    }

    @Test
    void testCreateNewSequencingKitLabel_WithoutAlias_AllFine() {
        assert SequencingKitLabelService.createNewSequencingKitLabel(KIT_NAME).name == KIT_NAME
    }

    @Test
    void testCreateNewSequencingKitLabel_WithAlias_AllFine() {
        SequencingKitLabel sequencingKitLabel = SequencingKitLabelService.createNewSequencingKitLabel(KIT_NAME, [ALIAS_KIT_NAME])
        assert sequencingKitLabel.name == KIT_NAME
        assert sequencingKitLabel.alias.contains(ALIAS_KIT_NAME)
    }

    @Test
    void testAddNewAliasToSequencingKitLabel_InputNameIsNull_ShouldFail() {
        TestCase.shouldFail(AssertionError){
            SequencingKitLabelService.addNewAliasToSequencingKitLabel(null, ALIAS_KIT_NAME)
        }
    }

    @Test
    void testAddNewAliasToSequencingKitLabel_InputAliasIsNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            SequencingKitLabelService.addNewAliasToSequencingKitLabel(KIT_NAME, null)
        }
    }

    @Test
    void testAddNewAliasToSequencingKitLabel_LabelWithInputNameDoesNotExist_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            SequencingKitLabelService.addNewAliasToSequencingKitLabel(KIT_NAME, ALIAS_KIT_NAME)
        }
    }

    @Test
    void testAddNewAliasToSequencingKitLabel_AliasExistsAlready_ShouldFail() {
        SequencingKitLabel sequencingKitLabel = SequencingKitLabel.build(name: KIT_NAME, alias: [ALIAS_KIT_NAME])
        TestCase.shouldFail(AssertionError) {
            SequencingKitLabelService.addNewAliasToSequencingKitLabel(KIT_NAME, ALIAS_KIT_NAME)
        }
    }

    @Test
    void testAddNewAliasToSequencingKitLabel_AllFine() {
        SequencingKitLabel sequencingKitLabel = SequencingKitLabel.build(name: KIT_NAME, alias: [])
        SequencingKitLabelService.addNewAliasToSequencingKitLabel(KIT_NAME, ALIAS_KIT_NAME)
        assert sequencingKitLabel.alias.contains(ALIAS_KIT_NAME)
    }


}
