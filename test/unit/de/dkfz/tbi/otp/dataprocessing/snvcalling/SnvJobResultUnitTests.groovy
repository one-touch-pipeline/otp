package de.dkfz.tbi.otp.dataprocessing.snvcalling

import grails.test.mixin.*
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.ExternalScript

@TestFor(SnvJobResult)
@Mock([SnvCallingInstance, ProcessedMergedBamFile, ExternalScript, Individual, Project])
class SnvJobResultUnitTests {

    @Test
    void testSavingOfSnvJobResultNoInputFileButCallingStep() {
        SnvJobResult snvJobResult = new SnvJobResult(
                step: SnvCallingStep.CALLING,
                snvCallingInstance: createSnvCallingInstance(),
                processingState: SnvProcessingStates.IN_PROGRESS,
                externalScript: new ExternalScript(scriptIdentifier: SnvCallingStep.CALLING.externalScriptIdentifier),
                )
        assert snvJobResult.validate()
        assert snvJobResult.save()
    }


    @Test
    void testSavingOfSnvJobResultNoExternalScript() {
        SnvJobResult snvJobResult = new SnvJobResult(
                step: SnvCallingStep.CALLING,
                snvCallingInstance: createSnvCallingInstance(),
                processingState: SnvProcessingStates.IN_PROGRESS
                )
        assert !snvJobResult.validate()
    }

    @Test
    void testSavingOfSnvJobResultWrongExternalScript() {
        SnvJobResult snvJobResult = new SnvJobResult(
                step: SnvCallingStep.CALLING,
                snvCallingInstance: createSnvCallingInstance(),
                processingState: SnvProcessingStates.IN_PROGRESS,
                externalScript: new ExternalScript(scriptIdentifier: SnvCallingStep.SNV_ANNOTATION.externalScriptIdentifier),
                )
        assert !snvJobResult.validate()
    }


    @Test
    void testSavingOfSnvJobResultNoInputFileNotCallingStep() {
        SnvJobResult snvJobResult = new SnvJobResult(
                step: SnvCallingStep.SNV_ANNOTATION,
                snvCallingInstance: createSnvCallingInstance(),
                processingState: SnvProcessingStates.IN_PROGRESS,
                externalScript: new ExternalScript(scriptIdentifier: SnvCallingStep.SNV_ANNOTATION.externalScriptIdentifier),
                )
        assert !snvJobResult.validate()
    }

    @Test
    void testSavingOfSnvJobResultInputFileNotCallingStep() {

        SnvCallingInstance snvCallingInstance = createSnvCallingInstance()

        SnvJobResult oldSnvJobResult = new SnvJobResult(
                processingState: SnvProcessingStates.FINISHED,
                snvCallingInstance: snvCallingInstance,
                externalScript: new ExternalScript()
                )

        SnvJobResult snvJobResult = new SnvJobResult(
                step: SnvCallingStep.SNV_ANNOTATION,
                snvCallingInstance: snvCallingInstance,
                processingState: SnvProcessingStates.IN_PROGRESS,
                inputResult: oldSnvJobResult,
                externalScript: new ExternalScript(scriptIdentifier: SnvCallingStep.SNV_ANNOTATION.externalScriptIdentifier),
                )
        assert snvJobResult.validate()
        assert snvJobResult.save()
    }

    @Test
    void testSavingOfSnvJobResultInputWithdrawnResultNotWithdrawn() {
        SnvCallingInstance snvCallingInstance = createSnvCallingInstance()

        SnvJobResult oldSnvJobResult = new SnvJobResult(
                processingState: SnvProcessingStates.FINISHED,
                snvCallingInstance: snvCallingInstance,
                withdrawn: true,
                externalScript: new ExternalScript(),
                )

        SnvJobResult snvJobResult = new SnvJobResult(
                step: SnvCallingStep.SNV_ANNOTATION,
                snvCallingInstance: snvCallingInstance,
                processingState: SnvProcessingStates.IN_PROGRESS,
                inputResult: oldSnvJobResult,
                externalScript: new ExternalScript(scriptIdentifier: SnvCallingStep.SNV_ANNOTATION.externalScriptIdentifier),
                )
        assertFalse snvJobResult.validate()
    }

    @Test
    void testSavingOfSnvJobResultInputWithdrawnResultWithdrawn() {
        SnvCallingInstance snvCallingInstance = createSnvCallingInstance()

        SnvJobResult oldSnvJobResult = new SnvJobResult(
                processingState: SnvProcessingStates.FINISHED,
                snvCallingInstance: snvCallingInstance,
                withdrawn: true,
                externalScript: new ExternalScript(),
                )

        SnvJobResult snvJobResult = new SnvJobResult(
                step: SnvCallingStep.SNV_ANNOTATION,
                snvCallingInstance: snvCallingInstance,
                processingState: SnvProcessingStates.IN_PROGRESS,
                inputResult: oldSnvJobResult,
                withdrawn: true,
                externalScript: new ExternalScript(scriptIdentifier: SnvCallingStep.SNV_ANNOTATION.externalScriptIdentifier),
                )
        assert snvJobResult.validate()
    }


    @Test
    void testSavingOfSnvJobResultSampleType1BamDifferentFromInputFile() {
        ProcessedMergedBamFile control = new ProcessedMergedBamFile()

        SnvCallingInstance snvCallingInstance1 = new SnvCallingInstance(
                sampleType1BamFile: new ProcessedMergedBamFile(),
                sampleType2BamFile: control
                )

        SnvCallingInstance snvCallingInstance2 = new SnvCallingInstance(
                sampleType1BamFile: new ProcessedMergedBamFile(),
                sampleType2BamFile: control
                )

        SnvJobResult oldSnvJobResult = new SnvJobResult(
                processingState: SnvProcessingStates.FINISHED,
                snvCallingInstance: snvCallingInstance1,
                externalScript: new ExternalScript(),
                )

        SnvJobResult snvJobResult = new SnvJobResult(
                step: SnvCallingStep.SNV_ANNOTATION,
                snvCallingInstance: snvCallingInstance2,
                processingState: SnvProcessingStates.IN_PROGRESS,
                inputResult: oldSnvJobResult,
                externalScript: new ExternalScript(scriptIdentifier: SnvCallingStep.SNV_ANNOTATION.externalScriptIdentifier),
                )
        assertFalse snvJobResult.validate()
    }

    @Test
    void testSavingOfSnvJobResultSampleType2BamDifferentFromInputFile() {
        ProcessedMergedBamFile tumor = new ProcessedMergedBamFile()

        SnvCallingInstance snvCallingInstance1 = new SnvCallingInstance(
                sampleType1BamFile: tumor,
                sampleType2BamFile: new ProcessedMergedBamFile()
                )

        SnvCallingInstance snvCallingInstance2 = new SnvCallingInstance(
                sampleType1BamFile: tumor,
                sampleType2BamFile: new ProcessedMergedBamFile()
                )

        SnvJobResult oldSnvJobResult = new SnvJobResult(
                processingState: SnvProcessingStates.FINISHED,
                snvCallingInstance: snvCallingInstance1,
                externalScript: new ExternalScript(),
                )

        SnvJobResult snvJobResult = new SnvJobResult(
                step: SnvCallingStep.SNV_ANNOTATION,
                snvCallingInstance: snvCallingInstance2,
                processingState: SnvProcessingStates.IN_PROGRESS,
                inputResult: oldSnvJobResult,
                externalScript: new ExternalScript(),
                )
        assertFalse snvJobResult.validate()
    }

    @Test
    void testGetSampleType1BamFile() {
        ProcessedMergedBamFile pmbf = new ProcessedMergedBamFile()

        SnvCallingInstance snvCallingInstance = new SnvCallingInstance(
                sampleType1BamFile: pmbf,
                sampleType2BamFile: new ProcessedMergedBamFile()
                )

        SnvJobResult snvJobResult = new SnvJobResult(
                step: SnvCallingStep.CALLING,
                snvCallingInstance: snvCallingInstance,
                processingState: SnvProcessingStates.IN_PROGRESS,
                externalScript: new ExternalScript(),
                )

        assertEquals(pmbf, snvJobResult.getSampleType1BamFile())
    }

    @Test
    void testGetSampleType2BamFile() {
        ProcessedMergedBamFile pmbf = new ProcessedMergedBamFile()

        SnvCallingInstance snvCallingInstance = new SnvCallingInstance(
                sampleType1BamFile: new ProcessedMergedBamFile(),
                sampleType2BamFile: pmbf
                )

        SnvJobResult snvJobResult = new SnvJobResult(
                step: SnvCallingStep.CALLING,
                snvCallingInstance: snvCallingInstance,
                processingState: SnvProcessingStates.IN_PROGRESS,
                externalScript: new ExternalScript(),
                )

        assertEquals(pmbf, snvJobResult.getSampleType2BamFile())
    }

    @Test
    void testGetResultFilePath_Calling_RawVcfFile() {

       Map preparedObjects = preparationForGetResultFilePath(SnvCallingStep.CALLING)

        assert preparedObjects.project == preparedObjects.snvJobResult.getResultFilePath().project
        assert "testPath/snvs_${preparedObjects.individual.pid}_raw.vcf.gz" == preparedObjects.snvJobResult.getResultFilePath().relativePath.path
    }

    @Test
    void testGetResultFilePath_Calling_ChromosomeVcfFile() {
        Map preparedObjects = preparationForGetResultFilePath(SnvCallingStep.CALLING)

        assert preparedObjects.project == preparedObjects.snvJobResult.getResultFilePath("2").project
        assert "testPath/snvs_${preparedObjects.individual.pid}.2.vcf" == preparedObjects.snvJobResult.getResultFilePath("2").relativePath.path
    }

    @Test
    void testGetResultFilePath_Annotation() {
        Map preparedObjects = preparationForGetResultFilePath(SnvCallingStep.SNV_ANNOTATION)

        assert preparedObjects.project == preparedObjects.snvJobResult.getResultFilePath().project
        assert "testPath/snvs_${preparedObjects.individual.pid}_annotation.vcf.gz" == preparedObjects.snvJobResult.getResultFilePath().relativePath.path
    }

    @Test
    void testGetResultFilePath_DeepAnnotation() {
        Map preparedObjects = preparationForGetResultFilePath(SnvCallingStep.SNV_DEEPANNOTATION)

        assert preparedObjects.project == preparedObjects.snvJobResult.getResultFilePath().project
        assert "testPath/snvs_${preparedObjects.individual.pid}.vcf.gz" == preparedObjects.snvJobResult.getResultFilePath().relativePath.path
    }

    @Test
    void testGetResultFilePath_Filter() {
        Map preparedObjects = preparationForGetResultFilePath(SnvCallingStep.FILTER_VCF)

        shouldFail(UnsupportedOperationException, { preparedObjects.snvJobResult.getResultFilePath()})
    }

    Map preparationForGetResultFilePath(SnvCallingStep step) {
        Project project = new Project(
            dirName: "/tmp/project/"
            )

        OtpPath path = new OtpPath(project, "testPath/")
        SnvCallingInstance.metaClass.getSnvInstancePath = { return path }

        Individual individual = new Individual(
                pid: "pid"
                )

        SampleTypeCombinationPerIndividual sampleCombinationPerIndividual = new SampleTypeCombinationPerIndividual(
                individual: individual,
                )

        SnvCallingInstance snvCallingInstance = new SnvCallingInstance(
                sampleType1BamFile: new ProcessedMergedBamFile(),
                sampleType2BamFile: new ProcessedMergedBamFile(),
                sampleTypeCombination: sampleCombinationPerIndividual
                )

        SnvJobResult snvJobResult = new SnvJobResult(
                step: step,
                snvCallingInstance: snvCallingInstance,
                )
        return ["project": project, "individual": individual, "snvJobResult": snvJobResult]
    }

    private SnvCallingInstance createSnvCallingInstance(final Map properties = [:]) {
        return new SnvCallingInstance([
                sampleType1BamFile: new ProcessedMergedBamFile(),
                sampleType2BamFile: new ProcessedMergedBamFile()
        ] + properties)
    }
}
