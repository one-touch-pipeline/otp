package de.dkfz.tbi.otp.dataprocessing.snvcalling

import grails.test.mixin.*
import org.apache.commons.jexl.junit.Asserter;
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile

@TestFor(SnvJobResult)
@Mock([SnvCallingInstance, ProcessedMergedBamFile])
class SnvJobResultUnitTests {

    @Test
    void testSavingOfSnvJobResultNoInputFileButCalling() {
        SnvJobResult snvJobResult = new SnvJobResult(
                step: SnvCallingStep.CALLING,
                snvCallingInstance: new SnvCallingInstance(),
                processingState: SnvProcessingStates.IN_PROGRESS,
                )
        assert snvJobResult.validate()
        assert snvJobResult.save()
    }

    @Test
    void testSavingOfSnvJobResultNoInputFileNotCalling() {
        SnvJobResult snvJobResult = new SnvJobResult(
                step: SnvCallingStep.SNV_ANNOTATION,
                snvCallingInstance: new SnvCallingInstance(),
                processingState: SnvProcessingStates.IN_PROGRESS,
                )
        assert !snvJobResult.validate()
    }

    @Test
    void testSavingOfSnvJobResultInputFileNotCalling() {

        SnvCallingInstance snvCallingInstance = new SnvCallingInstance(
                tumorBamFile: new ProcessedMergedBamFile(),
                controlBamFile: new ProcessedMergedBamFile()
                )

        SnvJobResult oldSnvJobResult = new SnvJobResult(
                processingState: SnvProcessingStates.FINISHED,
                snvCallingInstance: snvCallingInstance
                )

        SnvJobResult snvJobResult = new SnvJobResult(
                step: SnvCallingStep.SNV_ANNOTATION,
                snvCallingInstance: snvCallingInstance,
                processingState: SnvProcessingStates.IN_PROGRESS,
                inputResult: oldSnvJobResult
                )
        assert snvJobResult.validate()
        assert snvJobResult.save()
    }

    @Test
    void testSavingOfSnvJobResultInputWithdrawnResultNotWithdrawn() {
        SnvCallingInstance snvCallingInstance = new SnvCallingInstance(
                tumorBamFile: new ProcessedMergedBamFile(),
                controlBamFile: new ProcessedMergedBamFile()
                )

        SnvJobResult oldSnvJobResult = new SnvJobResult(
                processingState: SnvProcessingStates.FINISHED,
                snvCallingInstance: snvCallingInstance,
                withdrawn: true
                )

        SnvJobResult snvJobResult = new SnvJobResult(
                step: SnvCallingStep.SNV_ANNOTATION,
                snvCallingInstance: snvCallingInstance,
                processingState: SnvProcessingStates.IN_PROGRESS,
                inputResult: oldSnvJobResult
                )
        assertFalse snvJobResult.validate()
    }

    @Test
    void testSavingOfSnvJobResultInputWithdrawnResultWithdrawn() {
        SnvCallingInstance snvCallingInstance = new SnvCallingInstance(
                tumorBamFile: new ProcessedMergedBamFile(),
                controlBamFile: new ProcessedMergedBamFile()
                )

        SnvJobResult oldSnvJobResult = new SnvJobResult(
                processingState: SnvProcessingStates.FINISHED,
                snvCallingInstance: snvCallingInstance,
                withdrawn: true
                )

        SnvJobResult snvJobResult = new SnvJobResult(
                step: SnvCallingStep.SNV_ANNOTATION,
                snvCallingInstance: snvCallingInstance,
                processingState: SnvProcessingStates.IN_PROGRESS,
                inputResult: oldSnvJobResult,
                withdrawn: true
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
                )

        SnvJobResult snvJobResult = new SnvJobResult(
                step: SnvCallingStep.SNV_ANNOTATION,
                snvCallingInstance: snvCallingInstance2,
                processingState: SnvProcessingStates.IN_PROGRESS,
                inputResult: oldSnvJobResult,
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
                )

        SnvJobResult snvJobResult = new SnvJobResult(
                step: SnvCallingStep.SNV_ANNOTATION,
                snvCallingInstance: snvCallingInstance2,
                processingState: SnvProcessingStates.IN_PROGRESS,
                inputResult: oldSnvJobResult,
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
                )

        assertEquals(pmbf, snvJobResult.getControlBamFile())
    }
}
