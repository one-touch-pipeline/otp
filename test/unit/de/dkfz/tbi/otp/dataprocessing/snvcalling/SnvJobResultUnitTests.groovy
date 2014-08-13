package de.dkfz.tbi.otp.dataprocessing.snvcalling

import grails.test.mixin.*
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile;
import de.dkfz.tbi.otp.ngsdata.*

@TestFor(SnvJobResult)
@Mock([SnvCallingInstance, ProcessedMergedBamFile])
class SnvJobResultUnitTests {

    @Test
    void testSavingOfSnvJobResultNoInputFileButCalling() {
        SnvJobResult snvJobResult = new SnvJobResult(
                step: SnvCallingStep.SNV_CALL,
                snvCallingInstance: new SnvCallingInstance(),
                processingState: SnvProcessingStates.IN_PROGRESS,
                )
        assert snvJobResult.validate()
        assert snvJobResult.save()
    }

    @Test
    void testSavingOfSnvJobResultNoInputFileNotCalling() {
        SnvJobResult snvJobResult = new SnvJobResult(
                step: SnvCallingStep.ANNOTATION,
                snvCallingInstance: new SnvCallingInstance(),
                processingState: SnvProcessingStates.IN_PROGRESS,
                )
        assert !snvJobResult.validate()
    }

    @Test
    void testSavingOfsnvJobResultInputFileNotCalling() {

        SnvCallingInstance snvCallingInstance = new SnvCallingInstance(
                tumorBamFile: new ProcessedMergedBamFile(),
                controlBamFile: new ProcessedMergedBamFile()
                )

        SnvJobResult oldSnvJobResult = new SnvJobResult(
                processingState: SnvProcessingStates.FINISHED,
                snvCallingInstance: snvCallingInstance
                )

        SnvJobResult snvJobResult = new SnvJobResult(
                step: SnvCallingStep.ANNOTATION,
                snvCallingInstance: snvCallingInstance,
                processingState: SnvProcessingStates.IN_PROGRESS,
                inputResult: oldSnvJobResult
                )
        assert snvJobResult.validate()
        assert snvJobResult.save()
    }
}
