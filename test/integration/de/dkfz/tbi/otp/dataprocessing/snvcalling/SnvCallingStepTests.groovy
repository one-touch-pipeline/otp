package de.dkfz.tbi.otp.dataprocessing.snvcalling

import static org.junit.Assert.*
import org.junit.*
import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile
import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.TestData
import de.dkfz.tbi.otp.utils.ExternalScript

class SnvCallingStepTests {

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
        getExternalScript(SnvCallingStep.CALLING)
    }

    @Test
    void testGetExternalScript_FILTER() {
        getExternalScript(SnvCallingStep.FILTER_VCF)
    }

    @Test
    void testGetExternalScript_ANNOTATION() {
        getExternalScript(SnvCallingStep.SNV_ANNOTATION)
    }

    @Test
    void testGetExternalScript_DEEP_ANNOTATION() {
        getExternalScript(SnvCallingStep.SNV_DEEPANNOTATION)
    }

    private void getExternalScript(SnvCallingStep step) {
        final File testDir = TestCase.uniqueNonExistentPath

        ExternalScript externalScript = createExternalScript("SnvCallingStep.${step.name()}", new File(testDir, 'script_v1.sh').path)
        assert externalScript.save(flush: true)
        assertEquals(externalScript, step.getExternalScript("v1"))

        externalScript.deprecatedDate = new Date()
        assert externalScript.save(flush: true)
        ExternalScript externalScript2 = createExternalScript("SnvCallingStep.${step.name()}", new File(testDir, 'script_v2.sh').path)
        assert externalScript2.save(flush: true)
        assertEquals(externalScript2, step.getExternalScript("v1"))
    }

    @Test
    void testGetExternalScript_NoScriptVersion_shouldFail() {
        shouldFail(AssertionError, {SnvCallingStep.SNV_DEEPANNOTATION.getExternalScript(null)})
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

        assertEquals("", SnvCallingStep.FILTER_VCF.getResultFileName())
    }

    @Test
    void testGetIndexFileName() {
        TestData testData = new TestData()
        testData.createObjects()

        Individual individual = testData.individual
        final String pid = '654321'
        individual.pid = pid
        assert individual.save(failOnError: true)

        assertEquals('snvs_654321_raw.vcf.gz.tbi', SnvCallingStep.CALLING.getIndexFileName(individual))
        assertEquals('snvs_654321_annotation.vcf.gz.tbi', SnvCallingStep.SNV_ANNOTATION.getIndexFileName(individual))
        assertEquals('snvs_654321.vcf.gz.tbi', SnvCallingStep.SNV_DEEPANNOTATION.getIndexFileName(individual))
        assertEquals('snvs_654321.vcf.gz.tbi', SnvCallingStep.FILTER_VCF.getIndexFileName(individual))
    }

    @Test
    void testGetCheckpointFilePath() {
        final String SOME_INSTANCE_NAME = "2014-09-01_15h32"

        SnvCallingInstanceTestData testData = new SnvCallingInstanceTestData()
        testData.createSnvObjects()

        ProcessedMergedBamFile processedMergedBamFile1 = testData.bamFileTumor
        ProcessedMergedBamFile processedMergedBamFile2 = testData.bamFileControl

        SnvCallingInstance snvCallingInstance1 = testData.createSnvCallingInstance([
                sampleType1BamFile: processedMergedBamFile1,
                sampleType2BamFile: processedMergedBamFile2,
                instanceName      : SOME_INSTANCE_NAME,
        ])
        assert snvCallingInstance1.save()

        OtpPath expectedParentPath = snvCallingInstance1.snvInstancePath
        Project expectedProject = testData.samplePair.project

        OtpPath actualPathCalling = SnvCallingStep.CALLING.getCheckpointFilePath(snvCallingInstance1)
        assert actualPathCalling.project == expectedProject
        assert actualPathCalling.relativePath.path == "${expectedParentPath.relativePath}/CALLING_checkpoint"

        OtpPath actualPathAnnotation = SnvCallingStep.SNV_ANNOTATION.getCheckpointFilePath(snvCallingInstance1)
        assert actualPathAnnotation.project == expectedProject
        assert actualPathAnnotation.relativePath.path == "${expectedParentPath.relativePath}/SNV_ANNOTATION_checkpoint"

        OtpPath actualPathDeepAnnotation = SnvCallingStep.SNV_DEEPANNOTATION.getCheckpointFilePath(snvCallingInstance1)
        assert actualPathDeepAnnotation.project == expectedProject
        assert actualPathDeepAnnotation.relativePath.path == "${expectedParentPath.relativePath}/SNV_DEEPANNOTATION_checkpoint"

        OtpPath actualPathFilter = SnvCallingStep.FILTER_VCF.getCheckpointFilePath(snvCallingInstance1)
        assert actualPathFilter.project == expectedProject
        assert actualPathFilter.relativePath.path == "${expectedParentPath.relativePath}/FILTER_VCF_checkpoint"
    }

    private ExternalScript createExternalScript(String identifier, String path = "/tmp/testfolder/testScript.sh") {
        return new ExternalScript(
        scriptIdentifier: identifier,
        scriptVersion: 'v1',
        filePath: path,
        author: "testUser",
        comment: "lets see if it works ;)",
        )
    }
}
