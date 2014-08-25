package de.dkfz.tbi.otp.dataprocessing.snvcalling

import grails.test.mixin.*
import org.apache.commons.jexl.junit.Asserter;
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile
import de.dkfz.tbi.otp.utils.ExternalScript

@TestFor(SnvJobResult)
@Mock([SnvCallingInstance, ProcessedMergedBamFile, ExternalScript])
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
    void testSavingOfSnvJobResultTumorBamDifferentFromInputFile() {
        ProcessedMergedBamFile control = new ProcessedMergedBamFile()

        SnvCallingInstance snvCallingInstance1 = new SnvCallingInstance(
                tumorBamFile: new ProcessedMergedBamFile(),
                controlBamFile: control
                )

        SnvCallingInstance snvCallingInstance2 = new SnvCallingInstance(
                tumorBamFile: new ProcessedMergedBamFile(),
                controlBamFile: control
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
    void testSavingOfSnvJobResultControlBamDifferentFromInputFile() {
        ProcessedMergedBamFile tumor = new ProcessedMergedBamFile()

        SnvCallingInstance snvCallingInstance1 = new SnvCallingInstance(
                tumorBamFile: tumor,
                controlBamFile: new ProcessedMergedBamFile()
                )

        SnvCallingInstance snvCallingInstance2 = new SnvCallingInstance(
                tumorBamFile: tumor,
                controlBamFile: new ProcessedMergedBamFile()
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
    void testGetTumorBamFile() {
        ProcessedMergedBamFile pmbf = new ProcessedMergedBamFile()

        SnvCallingInstance snvCallingInstance = new SnvCallingInstance(
                tumorBamFile: pmbf,
                controlBamFile: new ProcessedMergedBamFile()
                )

        SnvJobResult snvJobResult = new SnvJobResult(
                step: SnvCallingStep.CALLING,
                snvCallingInstance: snvCallingInstance,
                processingState: SnvProcessingStates.IN_PROGRESS,
                externalScript: new ExternalScript(),
                )

        assertEquals(pmbf, snvJobResult.getTumorBamFile())
    }

    @Test
    void testGetControlBamFile() {
        ProcessedMergedBamFile pmbf = new ProcessedMergedBamFile()

        SnvCallingInstance snvCallingInstance = new SnvCallingInstance(
                tumorBamFile: new ProcessedMergedBamFile(),
                controlBamFile: pmbf
                )

        SnvJobResult snvJobResult = new SnvJobResult(
                step: SnvCallingStep.CALLING,
                snvCallingInstance: snvCallingInstance,
                processingState: SnvProcessingStates.IN_PROGRESS,
                externalScript: new ExternalScript(),
                )

        assertEquals(pmbf, snvJobResult.getControlBamFile())
    }

    private SnvCallingInstance createSnvCallingInstance(final Map properties = [:]) {
        return new SnvCallingInstance([
                tumorBamFile: new ProcessedMergedBamFile(),
                controlBamFile: new ProcessedMergedBamFile()
        ] + properties)
    }
}
