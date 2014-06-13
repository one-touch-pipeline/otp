package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.TestCase
import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor
import org.junit.Test

/**
 */
@TestFor(SeqPlatformModelLabelService)
@Build([SeqPlatformModelLabel])
class SeqPlatformModelLabelServiceUnitTests {

    private final String MODEL_NAME = "model name"
    private final String ALIAS_MODEL_NAME = "model alias"

    @Test
    void testFindSeqPlatformModelLabelByNameOrAlias_FindByName(){
        SeqPlatformModelLabel seqPlatformModelLabel = SeqPlatformModelLabel.build(name: MODEL_NAME)
        assert seqPlatformModelLabel == service.findSeqPlatformModelLabelByNameOrAlias(MODEL_NAME)
    }

    @Test
    void testFindSeqPlatformModelLabelByNameOrAlias_FindByAlias(){
        SeqPlatformModelLabel seqPlatformModelLabel = SeqPlatformModelLabel.build(name: MODEL_NAME, alias: [ALIAS_MODEL_NAME])
        assert seqPlatformModelLabel == service.findSeqPlatformModelLabelByNameOrAlias(ALIAS_MODEL_NAME)
    }

    @Test
    void testFindSeqPlatformModelLabelByNameOrAlias_FindNothing_ReturnNull(){
        SeqPlatformModelLabel seqPlatformModelLabel = SeqPlatformModelLabel.build(name: MODEL_NAME)
        assert null == service.findSeqPlatformModelLabelByNameOrAlias(ALIAS_MODEL_NAME)
    }

    @Test
    void testCreateNewSeqPlatformModelLabel_InputNameIsNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            SeqPlatformModelLabelService.createNewSeqPlatformModelLabel(null)
        }
    }

    @Test
    void testCreateNewSeqPlatformModelLabel_ExistsAlready_ShouldFail() {
        SeqPlatformModelLabel seqPlatformModelLabel = SeqPlatformModelLabel.build(name: MODEL_NAME)
        TestCase.shouldFail(AssertionError) {
            SeqPlatformModelLabelService.createNewSeqPlatformModelLabel(MODEL_NAME)
        }
    }

    @Test
    void testCreateNewSeqPlatformModelLabel_WithoutAlias_AllFine() {
        assert SeqPlatformModelLabelService.createNewSeqPlatformModelLabel(MODEL_NAME) ==
                SeqPlatformModelLabel.findByName(MODEL_NAME)
    }

    @Test
    void testCreateNewSeqPlatformModelLabel_WithAlias_AllFine() {
        assert SeqPlatformModelLabelService.createNewSeqPlatformModelLabel(MODEL_NAME, [ALIAS_MODEL_NAME]) ==
                SeqPlatformModelLabel.findByName(MODEL_NAME)
    }

    @Test
    void testAddNewAliasToSeqPlatformModelLabel_InputNameIsNull_ShouldFail() {
        TestCase.shouldFail(AssertionError){
            SeqPlatformModelLabelService.addNewAliasToSeqPlatformModelLabel(null, ALIAS_MODEL_NAME)
        }
    }

    @Test
    void testAddNewAliasToSeqPlatformModelLabel_InputAliasIsNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            SeqPlatformModelLabelService.addNewAliasToSeqPlatformModelLabel(MODEL_NAME, null)
        }
    }

    @Test
    void testAddNewAliasToSeqPlatformModelLabel_LabelWithInputNameDoesNotExist_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            SeqPlatformModelLabelService.addNewAliasToSeqPlatformModelLabel(MODEL_NAME, ALIAS_MODEL_NAME)
        }
    }

    @Test
    void testAddNewAliasToSeqPlatformModelLabel_AliasExistsAlready_ShouldFail() {
        SeqPlatformModelLabel seqPlatformModelLabel = SeqPlatformModelLabel.build(name: MODEL_NAME, alias: [ALIAS_MODEL_NAME])
        TestCase.shouldFail(AssertionError) {
            SeqPlatformModelLabelService.addNewAliasToSeqPlatformModelLabel(MODEL_NAME, ALIAS_MODEL_NAME)
        }
    }

    @Test
    void testAddNewAliasToSeqPlatformModelLabel_AllFine() {
        SeqPlatformModelLabel seqPlatformModelLabel = SeqPlatformModelLabel.build(name: MODEL_NAME, alias: [])
        SeqPlatformModelLabelService.addNewAliasToSeqPlatformModelLabel(MODEL_NAME, ALIAS_MODEL_NAME)
        assert seqPlatformModelLabel.alias.contains(ALIAS_MODEL_NAME)
    }
}
