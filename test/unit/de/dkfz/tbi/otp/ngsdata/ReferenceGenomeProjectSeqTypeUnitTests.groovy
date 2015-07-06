package de.dkfz.tbi.otp.ngsdata

import grails.buildtestdata.mixin.Build
import de.dkfz.tbi.TestCase
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ErrorCollector

@TestFor(ReferenceGenomeProjectSeqType)
@Build([ReferenceGenomeProjectSeqType])
class ReferenceGenomeProjectSeqTypeUnitTests {

    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Test
    void test_constraint_onStatSizeFileName_withCorrectName_ShouldBeValid() {
        ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = ReferenceGenomeProjectSeqType.buildWithoutSave([statSizeFileName: DomainFactory.DEFAULT_TAB_FILE_NAME])
        assert referenceGenomeProjectSeqType.validate()
    }

    @Test
    void test_constraint_onStatSizeFileName_withNull_ShouldBeValid() {
        ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = ReferenceGenomeProjectSeqType.buildWithoutSave([statSizeFileName: null])
        assert referenceGenomeProjectSeqType.validate()
    }

    @Test
    void test_constraint_onStatSizeFileName_WhenBlank_ShouldBeInvalid() {
        ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = ReferenceGenomeProjectSeqType.buildWithoutSave([:])
        referenceGenomeProjectSeqType.statSizeFileName = '' //setting empty string does not work via map

        TestCase.assertValidateError(referenceGenomeProjectSeqType, 'statSizeFileName', 'blank', '')
    }

    @Test
    void test_constraint_onStatSizeFileName_WhenValidSpecialChar_ShouldBeValid() {
        "-_.".each {
            try {
                String name = "File${it}.tab"
                ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = ReferenceGenomeProjectSeqType.buildWithoutSave([statSizeFileName: name])
                referenceGenomeProjectSeqType.validate()
                assert 0 == referenceGenomeProjectSeqType.errors.errorCount
            } catch (Throwable e) {
                collector.addError(e)
            }
        }
    }

    @Test
    void test_constraint_onStatSizeFileName_WhenInvalidSpecialChar_ShouldBeInvalid() {
        "\"',:;%\$§&<>|^§!?=äöüÄÖÜß´`".each {
            try {
                String name = "File${it}.tab"
                ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = ReferenceGenomeProjectSeqType.buildWithoutSave([statSizeFileName: name])
                TestCase.assertValidateError(referenceGenomeProjectSeqType, 'statSizeFileName', 'matches.invalid', name)
            } catch (Throwable e) {
                collector.addError(e)
            }
        }
    }
}
