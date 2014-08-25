package de.dkfz.tbi.otp.dataprocessing.snvcalling

import static org.junit.Assert.*
import org.junit.*
import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.Individual;
import de.dkfz.tbi.otp.ngsdata.TestData;
import de.dkfz.tbi.otp.utils.ExternalScript

class SnvCallingStepTests extends GroovyTestCase {

    @Test
    void testGetConfigExecuteFlagVariableName() {
        assertEquals("RUN_CALLING", SnvCallingStep.CALLING.getConfigExecuteFlagVariableName())
        assertEquals("RUN_FILTER_VCF", SnvCallingStep.FILTER_VCF.getConfigExecuteFlagVariableName())
        assertEquals("RUN_SNV_ANNOTATION", SnvCallingStep.SNV_ANNOTATION.getConfigExecuteFlagVariableName())
        assertEquals("RUN_SNV_DEEPANNOTATION", SnvCallingStep.SNV_DEEPANNOTATION.getConfigExecuteFlagVariableName())
    }

    @Test
    void testGetExternalScript_WrongIdentifier() {
        ExternalScript externalScript = createExternalScript("wrong identifier")
        assert externalScript.save()
        shouldFail {
            SnvCallingStep.CALLING.getExternalScript()
        }
    }

    @Test
    void testGetExternalScript_CALLING() {
        testGetExternalScript(SnvCallingStep.CALLING)
    }

    @Test
    void testGetExternalScript_FILTER() {
        testGetExternalScript(SnvCallingStep.FILTER_VCF)
    }

    @Test
    void testGetExternalScript_ANNOTATION() {
        testGetExternalScript(SnvCallingStep.SNV_ANNOTATION)
    }

    @Test
    void testGetExternalScript_DEEP_ANNOTATION() {
        testGetExternalScript(SnvCallingStep.SNV_DEEPANNOTATION)
    }

    void testGetExternalScript(SnvCallingStep step) {
        final File testDir = TestCase.uniqueNonExistentPath

        ExternalScript externalScript = createExternalScript("SnvCallingStep.${step.name()}", new File(testDir, 'script_v1.sh').path)
        assert externalScript.save()
        assertEquals(externalScript, step.getExternalScript())

        externalScript.deprecatedDate = new Date()
        assert externalScript.save()
        ExternalScript externalScript2 = createExternalScript("SnvCallingStep.${step.name()}", new File(testDir, 'script_v2.sh').path)
        assert externalScript2.save()
        assertEquals(externalScript2, step.getExternalScript())
    }

    @Test
    void testConfigFileNameSuffix() {
        assertEquals("calling", SnvCallingStep.CALLING.configFileNameSuffix)
        assertEquals("filter_vcf", SnvCallingStep.FILTER_VCF.configFileNameSuffix)
        assertEquals("snv_annotation", SnvCallingStep.SNV_ANNOTATION.configFileNameSuffix)
        assertEquals("snv_deepannotation", SnvCallingStep.SNV_DEEPANNOTATION.configFileNameSuffix)
    }

    @Test
    void testExternalScriptIdentifier() {
        assertEquals("SnvCallingStep.CALLING", SnvCallingStep.CALLING.externalScriptIdentifier)
        assertEquals("SnvCallingStep.FILTER_VCF", SnvCallingStep.FILTER_VCF.externalScriptIdentifier)
        assertEquals("SnvCallingStep.SNV_ANNOTATION", SnvCallingStep.SNV_ANNOTATION.externalScriptIdentifier)
        assertEquals("SnvCallingStep.SNV_DEEPANNOTATION", SnvCallingStep.SNV_DEEPANNOTATION.externalScriptIdentifier)
    }

    @Test
    void testGetResultFileName() {
        TestData testData = new TestData()
        testData.createObjects()

        Individual individual = testData.individual
        final String pid = '654321'
        individual.pid = pid
        assert individual.save(failOnError: true)

        assertEquals("snvs_${pid}.3.vcf", SnvCallingStep.CALLING.getResultFileName(individual, "3"))
        assertEquals("snvs_${pid}.X.vcf", SnvCallingStep.CALLING.getResultFileName(individual, "X"))
        shouldFail(AssertionError, {SnvCallingStep.CALLING.getResultFileName(individual, "")})
        assertEquals("snvs_${pid}_raw.vcf.gz", SnvCallingStep.CALLING.getResultFileName(individual, null))

        assertEquals("snvs_${pid}_annotation.vcf.gz", SnvCallingStep.SNV_ANNOTATION.getResultFileName(individual))

        assertEquals("snvs_${pid}.vcf.gz", SnvCallingStep.SNV_DEEPANNOTATION.getResultFileName(individual))

        //TODO: test for filter -> OTP-989"
    }

    private ExternalScript createExternalScript(String identifier, String path = "/tmp/testfolder/testScript.sh") {
        return new ExternalScript(
        scriptIdentifier: identifier,
        filePath: path,
        author: "testUser",
        comment: "lets see if it works ;)",
        )
    }
}
