package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.AlignmentPass
import de.dkfz.tbi.otp.dataprocessing.MergingPass
import de.dkfz.tbi.otp.dataprocessing.MergingSet
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.job.jobs.snvcalling.SnvCallingJob
import grails.test.mixin.*
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.ExternalScript
import grails.buildtestdata.mixin.Build

@TestFor(SnvJobResult)
@Mock([SnvCallingInstance, ExternalScript])
@Build([
        AlignmentPass,
        MergingPass,
        MergingSet,
        SampleTypePerProject,
        SeqPlatform,
        SnvCallingInstance,
])
class SnvJobResultUnitTests {

    final String MD5SUM = "a841c64c5825e986c4709ac7298e9366"
    final long FILE_SIZE = 1234l

    @Test
    void testSavingOfSnvJobResultNoInputFileButCallingStep() {
        SnvCallingInstance snvCallingInstance = createSnvCallingInstance()
        SnvJobResult snvJobResult = new SnvJobResult(
                step: SnvCallingStep.CALLING,
                snvCallingInstance: snvCallingInstance,
                processingState: SnvProcessingStates.IN_PROGRESS,
                externalScript: new ExternalScript(
                        scriptIdentifier: SnvCallingStep.CALLING.externalScriptIdentifier,
                        scriptVersion: snvCallingInstance.config.externalScriptVersion,
                ),
                chromosomeJoinExternalScript: new ExternalScript(
                        scriptIdentifier: SnvCallingJob.CHROMOSOME_VCF_JOIN_SCRIPT_IDENTIFIER,
                        scriptVersion: snvCallingInstance.config.externalScriptVersion,
                )
                )
        assert snvJobResult.validate()
        assert snvJobResult.save()
    }

    @Test
    void testSavingOfSnvJobResultInFailedState() {
        SnvCallingInstance snvCallingInstance = createSnvCallingInstance()
        SnvJobResult snvJobResult = new SnvJobResult(
                step: SnvCallingStep.CALLING,
                snvCallingInstance: snvCallingInstance,
                processingState: SnvProcessingStates.FAILED,
                externalScript: new ExternalScript(
                        scriptIdentifier: SnvCallingStep.CALLING.externalScriptIdentifier,
                        scriptVersion: snvCallingInstance.config.externalScriptVersion,
                ),
                chromosomeJoinExternalScript: new ExternalScript(
                        scriptIdentifier: SnvCallingJob.CHROMOSOME_VCF_JOIN_SCRIPT_IDENTIFIER,
                        scriptVersion: snvCallingInstance.config.externalScriptVersion,
                )
        )
        assert !snvJobResult.validate()
    }

    @Test
    void testSavingOfSnvJobResultNoExternalScript() {
        SnvCallingInstance snvCallingInstance = createSnvCallingInstance()
        SnvJobResult snvJobResult = new SnvJobResult(
                step: SnvCallingStep.CALLING,
                snvCallingInstance: snvCallingInstance,
                processingState: SnvProcessingStates.IN_PROGRESS,
                chromosomeJoinExternalScript: new ExternalScript(
                        scriptIdentifier: SnvCallingJob.CHROMOSOME_VCF_JOIN_SCRIPT_IDENTIFIER,
                        scriptVersion: snvCallingInstance.config.externalScriptVersion,
                )
                )
        assert !snvJobResult.validate()
    }

    @Test
    void testSavingOfSnvJobResultWrongExternalScript() {
        SnvCallingInstance snvCallingInstance = createSnvCallingInstance()
        SnvJobResult snvJobResult = new SnvJobResult(
                step: SnvCallingStep.CALLING,
                snvCallingInstance: snvCallingInstance,
                processingState: SnvProcessingStates.IN_PROGRESS,
                externalScript: new ExternalScript(
                        scriptIdentifier: SnvCallingStep.SNV_ANNOTATION.externalScriptIdentifier,
                        scriptVersion: snvCallingInstance.config.externalScriptVersion,
                ),
                chromosomeJoinExternalScript: new ExternalScript(
                        scriptIdentifier: SnvCallingJob.CHROMOSOME_VCF_JOIN_SCRIPT_IDENTIFIER),
                        scriptVersion: snvCallingInstance.config.externalScriptVersion,
                )
        assert !snvJobResult.validate()
    }

    @Test
    void testSavingOfSnvJobResultCallingButNoJoiningExternalScript() {
        SnvCallingInstance snvCallingInstance = createSnvCallingInstance()
        SnvJobResult snvJobResult = new SnvJobResult(
                step: SnvCallingStep.CALLING,
                snvCallingInstance: snvCallingInstance,
                processingState: SnvProcessingStates.IN_PROGRESS,
                chromosomeJoinExternalScript: new ExternalScript(
                        scriptIdentifier: SnvCallingJob.CHROMOSOME_VCF_JOIN_SCRIPT_IDENTIFIER,
                        scriptVersion: snvCallingInstance.config.externalScriptVersion,
                )
        )
        assert !snvJobResult.validate()
    }

    @Test
    void testSavingOfSnvJobResultNotCallingNoJoiningScript() {

        SnvCallingInstance snvCallingInstance = createSnvCallingInstance()

        SnvJobResult callingSnvJobResult = new SnvJobResult(
                step: SnvCallingStep.CALLING,
                processingState: SnvProcessingStates.FINISHED,
                snvCallingInstance: snvCallingInstance,
                externalScript: new ExternalScript(
                        scriptIdentifier: SnvCallingStep.CALLING.externalScriptIdentifier,
                        scriptVersion: snvCallingInstance.config.externalScriptVersion,
                ),
                chromosomeJoinExternalScript: new ExternalScript(
                        scriptIdentifier: SnvCallingJob.CHROMOSOME_VCF_JOIN_SCRIPT_IDENTIFIER,
                        scriptVersion: snvCallingInstance.config.externalScriptVersion,
                )
        )

        SnvJobResult snvJobResult = new SnvJobResult(
                step: SnvCallingStep.SNV_ANNOTATION,
                snvCallingInstance: snvCallingInstance,
                processingState: SnvProcessingStates.IN_PROGRESS,
                inputResult: callingSnvJobResult,
                externalScript: new ExternalScript(
                        scriptIdentifier: SnvCallingStep.SNV_ANNOTATION.externalScriptIdentifier,
                        scriptVersion: snvCallingInstance.config.externalScriptVersion,
                ),
        )
        assert snvJobResult.validate()
        assert snvJobResult.save()
    }

    @Test
    void testSavingOfSnvJobResultNoInputFileNotCallingStep() {
        SnvCallingInstance snvCallingInstance = createSnvCallingInstance()
        SnvJobResult snvJobResult = new SnvJobResult(
                step: SnvCallingStep.SNV_ANNOTATION,
                snvCallingInstance: snvCallingInstance,
                processingState: SnvProcessingStates.IN_PROGRESS,
                externalScript: new ExternalScript(
                        scriptIdentifier: SnvCallingStep.SNV_ANNOTATION.externalScriptIdentifier,
                        scriptVersion: snvCallingInstance.config.externalScriptVersion,
                ),
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
                externalScript: new ExternalScript(
                        scriptIdentifier: SnvCallingStep.SNV_ANNOTATION.externalScriptIdentifier,
                        scriptVersion: snvCallingInstance.config.externalScriptVersion,
                ),
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
                externalScript: new ExternalScript(
                        scriptIdentifier: SnvCallingStep.SNV_ANNOTATION.externalScriptIdentifier,
                        scriptVersion: snvCallingInstance.config.externalScriptVersion,
                ),
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
                externalScript: new ExternalScript(
                        scriptIdentifier: SnvCallingStep.SNV_ANNOTATION.externalScriptIdentifier,
                        scriptVersion: snvCallingInstance.config.externalScriptVersion,
                ),
                )
        assert snvJobResult.validate()
    }


    @Test
    void testSavingOfSnvJobResultSampleType1BamDifferentFromInputFile() {
        SnvCallingInstanceTestData testData = new SnvCallingInstanceTestData()
        testData.createSnvObjects()
        def (ProcessedMergedBamFile disease2BamFile, SamplePair disease2SamplePair) =
                testData.createDisease(testData.bamFileControl.mergingWorkPackage)
        SnvCallingInstance snvCallingInstance1 = testData.createSnvCallingInstance()
        SnvCallingInstance snvCallingInstance2 = testData.createSnvCallingInstance(
                samplePair: disease2SamplePair,
                sampleType1BamFile: disease2BamFile,
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
                externalScript: new ExternalScript(
                        scriptIdentifier: SnvCallingStep.SNV_ANNOTATION.externalScriptIdentifier,
                        scriptVersion: snvCallingInstance2.config.externalScriptVersion,
                ),
                )
        assertFalse snvJobResult.validate()
    }

    @Test
    void testSavingOfSnvJobResultSampleType2BamDifferentFromInputFile() {
        ProcessedMergedBamFile tumor = DomainFactory.createProcessedMergedBamFile()

        SnvCallingInstance snvCallingInstance1 = new SnvCallingInstance(
                sampleType1BamFile: tumor,
                sampleType2BamFile: DomainFactory.createProcessedMergedBamFile()
                )

        SnvCallingInstance snvCallingInstance2 = new SnvCallingInstance(
                sampleType1BamFile: tumor,
                sampleType2BamFile: DomainFactory.createProcessedMergedBamFile()
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
        ProcessedMergedBamFile pmbf = DomainFactory.createProcessedMergedBamFile()

        SnvCallingInstance snvCallingInstance = new SnvCallingInstance(
                sampleType1BamFile: pmbf,
                sampleType2BamFile: DomainFactory.createProcessedMergedBamFile()
                )

        SnvJobResult snvJobResult = new SnvJobResult(
                step: SnvCallingStep.CALLING,
                snvCallingInstance: snvCallingInstance,
                processingState: SnvProcessingStates.IN_PROGRESS,
                externalScript: new ExternalScript(),
                chromosomeJoinExternalScript: new ExternalScript(),
                )

        assertEquals(pmbf, snvJobResult.getSampleType1BamFile())
    }

    @Test
    void testGetSampleType2BamFile() {
        ProcessedMergedBamFile pmbf = DomainFactory.createProcessedMergedBamFile()

        SnvCallingInstance snvCallingInstance = new SnvCallingInstance(
                sampleType1BamFile: DomainFactory.createProcessedMergedBamFile(),
                sampleType2BamFile: pmbf
                )

        SnvJobResult snvJobResult = new SnvJobResult(
                step: SnvCallingStep.CALLING,
                snvCallingInstance: snvCallingInstance,
                processingState: SnvProcessingStates.IN_PROGRESS,
                externalScript: new ExternalScript(),
                chromosomeJoinExternalScript: new ExternalScript(),
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

        assert preparedObjects.project == preparedObjects.snvJobResult.getResultFilePath().project
        assert "testPath" == preparedObjects.snvJobResult.getResultFilePath().relativePath.path
    }

    @Test
    void testSaveMd5sum() {
        SnvCallingInstance snvCallingInstance = createSnvCallingInstance()
        SnvJobResult snvJobResult = new SnvJobResult(
                step: SnvCallingStep.CALLING,
                snvCallingInstance: snvCallingInstance,
                processingState: SnvProcessingStates.FINISHED,
                externalScript: new ExternalScript(
                        scriptIdentifier: SnvCallingStep.CALLING.externalScriptIdentifier,
                        scriptVersion: snvCallingInstance.config.externalScriptVersion,
                ),
                chromosomeJoinExternalScript: new ExternalScript(
                        scriptIdentifier: SnvCallingJob.CHROMOSOME_VCF_JOIN_SCRIPT_IDENTIFIER,
                        scriptVersion: snvCallingInstance.config.externalScriptVersion,
                ),
                fileSize: FILE_SIZE,
        )
        assertFalse snvJobResult.validate()

        snvJobResult.md5sum = MD5SUM
        assert snvJobResult.validate()
        assert snvJobResult.save(flush: true)
    }

    @Test
    void testSaveMd5sumWrongMd5sum() {
        SnvCallingInstance snvCallingInstance = createSnvCallingInstance()
        SnvJobResult snvJobResult = new SnvJobResult(
                step: SnvCallingStep.CALLING,
                snvCallingInstance: snvCallingInstance,
                processingState: SnvProcessingStates.FINISHED,
                externalScript: new ExternalScript(
                        scriptIdentifier: SnvCallingStep.CALLING.externalScriptIdentifier,
                        scriptVersion: snvCallingInstance.config.externalScriptVersion,
                ),
                chromosomeJoinExternalScript: new ExternalScript(
                        scriptVersion: snvCallingInstance.config.externalScriptVersion,
                        scriptIdentifier: SnvCallingJob.CHROMOSOME_VCF_JOIN_SCRIPT_IDENTIFIER
                ),
                md5sum: "1234",
                fileSize: FILE_SIZE,
        )
        assertFalse snvJobResult.validate()

        snvJobResult.md5sum = MD5SUM
        assert snvJobResult.validate()
        assert snvJobResult.save(flush: true)
    }

    @Test
    void testSaveFileSize() {
        SnvCallingInstance snvCallingInstance = createSnvCallingInstance()
        SnvJobResult snvJobResult = new SnvJobResult(
                step: SnvCallingStep.CALLING,
                snvCallingInstance: snvCallingInstance,
                processingState: SnvProcessingStates.FINISHED,
                externalScript: new ExternalScript(
                        scriptIdentifier: SnvCallingStep.CALLING.externalScriptIdentifier,
                        scriptVersion: snvCallingInstance.config.externalScriptVersion,
                ),
                chromosomeJoinExternalScript: new ExternalScript(
                        scriptIdentifier: SnvCallingJob.CHROMOSOME_VCF_JOIN_SCRIPT_IDENTIFIER,
                        scriptVersion: snvCallingInstance.config.externalScriptVersion,
                ),
                md5sum: MD5SUM,
        )
        assertFalse snvJobResult.validate()

        snvJobResult.fileSize = FILE_SIZE
        assert snvJobResult.validate()
        assert snvJobResult.save(flush: true)
    }

    @Test
    void testSaveFileSizeWrongFileSize() {
        SnvCallingInstance snvCallingInstance = createSnvCallingInstance()
        SnvJobResult snvJobResult = new SnvJobResult(
                step: SnvCallingStep.CALLING,
                snvCallingInstance: snvCallingInstance,
                processingState: SnvProcessingStates.FINISHED,
                externalScript: new ExternalScript(
                        scriptIdentifier: SnvCallingStep.CALLING.externalScriptIdentifier,
                        scriptVersion: snvCallingInstance.config.externalScriptVersion,
                ),
                chromosomeJoinExternalScript: new ExternalScript(
                        scriptIdentifier: SnvCallingJob.CHROMOSOME_VCF_JOIN_SCRIPT_IDENTIFIER,
                        scriptVersion: snvCallingInstance.config.externalScriptVersion,
                ),
                md5sum: MD5SUM,
                fileSize:0,
        )
        assertFalse snvJobResult.validate()

        snvJobResult.fileSize = FILE_SIZE
        assert snvJobResult.validate()
        assert snvJobResult.save(flush: true)
    }


    Map preparationForGetResultFilePath(SnvCallingStep step) {
        Project project = TestData.createProject(
            dirName: TestCase.uniqueNonExistentPath,
            )

        OtpPath path = new OtpPath(project, "testPath")
        SnvCallingInstance.metaClass.getSnvInstancePath = { return path }

        Individual individual = new Individual(
                pid: "pid"
                )

        MergingWorkPackage mergingWorkPackage = new MergingWorkPackage(
                sample: new Sample(
                        individual: individual,
                        )
                )

        SamplePair samplePair = new SamplePair(
                mergingWorkPackage1: mergingWorkPackage,
                mergingWorkPackage2: mergingWorkPackage,
                )

        SnvCallingInstance snvCallingInstance = new SnvCallingInstance(
                sampleType1BamFile: DomainFactory.createProcessedMergedBamFile(),
                sampleType2BamFile: DomainFactory.createProcessedMergedBamFile(),
                samplePair: samplePair
                )

        SnvJobResult snvJobResult = new SnvJobResult(
                step: step,
                snvCallingInstance: snvCallingInstance,
                )
        return ["project": project, "individual": individual, "snvJobResult": snvJobResult]
    }

    private SnvCallingInstance createSnvCallingInstance(final Map properties = [:]) {
        SnvCallingInstanceTestData testData = new SnvCallingInstanceTestData()
        testData.createSnvObjects()
        return testData.createSnvCallingInstance(properties)
    }
}
