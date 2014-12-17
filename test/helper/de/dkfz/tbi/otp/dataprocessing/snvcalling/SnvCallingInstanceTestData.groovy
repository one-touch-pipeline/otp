package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.jobs.snvcalling.SnvCallingJob
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.ExternalScript

class SnvCallingInstanceTestData extends TestData {

    ProcessedMergedBamFileService processedMergedBamFileService

    ProcessedMergedBamFile bamFileTumor
    ProcessedMergedBamFile bamFileTumor2
    ProcessedMergedBamFile bamFileControl
    SampleTypeCombinationPerIndividual sampleTypeCombination
    SampleTypeCombinationPerIndividual sampleTypeCombination2
    SnvConfig snvConfig
    ExternalScript externalScript_Joining

    void createSnvObjects() {
        processedMergedBamFileService = new ProcessedMergedBamFileService()
        processedMergedBamFileService.configService = new ConfigService()
        processedMergedBamFileService.mergedAlignmentDataFileService = new MergedAlignmentDataFileService()

        project = createProject()
        assert project.save(flush: true, failOnError: true)

        Individual individual = createIndividual([project: project])
        assert individual.save(flush: true, failOnError: true)

        SeqType seqType = createSeqType()
        assert seqType.save(flush: true, failOnError: true)

        bamFileTumor = createProcessedMergedBamFile(individual, seqType)
        bamFileTumor2 = createProcessedMergedBamFile(individual, seqType)
        bamFileControl = createProcessedMergedBamFile(individual, seqType)

        SampleTypePerProject.build(project: project, sampleType: bamFileTumor.sampleType, category: SampleType.Category.DISEASE)
        SampleTypePerProject.build(project: project, sampleType: bamFileTumor2.sampleType, category: SampleType.Category.DISEASE)

        sampleTypeCombination = new SampleTypeCombinationPerIndividual(
                individual: individual,
                sampleType1: bamFileTumor.sampleType,
                sampleType2: bamFileControl.sampleType,
                seqType: seqType
                )
        assert sampleTypeCombination.save()

        sampleTypeCombination2 = new SampleTypeCombinationPerIndividual(
                individual: individual,
                sampleType1: bamFileTumor2.sampleType,
                sampleType2: bamFileControl.sampleType,
                seqType: seqType
                )
        assert sampleTypeCombination2.save()

        snvConfig = new SnvConfig(
                project: project,
                seqType: seqType,
                configuration: "testConfig"
        )
        assert snvConfig.save()

        externalScript_Joining = new ExternalScript(
                scriptIdentifier: SnvCallingJob.CHROMOSOME_VCF_JOIN_SCRIPT_IDENTIFIER,
                filePath: "/tmp/scriptLocation/joining.sh",
                author: "otptest",
        )
        assert externalScript_Joining.save()
    }

    SnvJobResult createAndSaveSnvJobResult(SnvCallingInstance instance, SnvCallingStep step, SnvJobResult inputResult = null, SnvProcessingStates processingState = SnvProcessingStates.FINISHED, boolean withdrawn = false) {
        final ExternalScript externalScript = ExternalScript.findOrSaveWhere(
            scriptIdentifier: step.externalScriptIdentifier,
            deprecatedDate: null,
            filePath: "/dev/null/otp-test/${step.externalScriptIdentifier}",
            author: "otptest",
        )
        assert externalScript

        final SnvJobResult result = new SnvJobResult([
            snvCallingInstance: instance,
            step: step,
            inputResult: inputResult,
            processingState: processingState,
            withdrawn: withdrawn,
            externalScript: externalScript,
            md5sum: "a841c64c5825e986c4709ac7298e9366",
            fileSize: 1234l,
        ])
        if (step == SnvCallingStep.CALLING) {
            result.chromosomeJoinExternalScript = externalScript_Joining
        }
        assert result.save(failOnError: true)
        return result
    }

    SnvCallingInstance createAndSaveSnvCallingInstance(Map properties = [:]) {
        final SnvCallingInstance instance = createSnvCallingInstance(properties)
        assert instance.save(failOnError: true)
        return instance
    }

    SnvCallingInstance createSnvCallingInstance(Map properties = [:]) {
        return DomainFactory.createSnvCallingInstance([
            processingState: SnvProcessingStates.IN_PROGRESS,
            sampleType1BamFile: bamFileTumor,
            sampleType2BamFile: bamFileControl,
            config: snvConfig,
            instanceName: "2014-08-25_15h32",
            sampleTypeCombination: sampleTypeCombination
        ] + properties)
    }

    ProcessedMergedBamFile createProcessedMergedBamFile(Individual individual, SeqType seqType, String sampleTypeIdentifier = TestCase.uniqueString) {
        SampleType sampleType = new SampleType(
                name: "SampleType${sampleTypeIdentifier}")
        assert sampleType.save(flush: true, failOnError: true)

        Sample sample = new Sample (
                individual: individual,
                sampleType: sampleType)
        assert sample.save(flush: true, failOnError: true)

        MergingWorkPackage workPackage = new MergingWorkPackage(
                sample: sample,
                seqType: seqType
                )
        assert workPackage.save(flush: true, failOnError: true)

        MergingSet mergingSet = new MergingSet(
                mergingWorkPackage: workPackage)
        assert mergingSet.save(flush: true, failOnError: true)

        DomainFactory.assignNewProcessedBamFile(mergingSet)

        MergingPass mergingPass = new MergingPass(
                identifier: 1,
                mergingSet: mergingSet)
        assert mergingPass.save(flush: true, failOnError: true)

        ProcessedMergedBamFile bamFile = new ProcessedMergedBamFile(
                type: AbstractBamFile.BamType.SORTED,
                mergingPass: mergingPass,
                fileExists: true,
                fileSize: 123456,
                md5sum: '0123456789ABCDEF0123456789ABCDEF',
                qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
                status: AbstractBamFile.State.PROCESSED,
                fileOperationStatus: AbstractBamFile.FileOperationStatus.PROCESSED,
        )
        assert bamFile.save(flush: true, failOnError: true)

        return bamFile
    }

    File createConfigFileWithContentInFileSystem(File configFile, String configuration) {
        File configDir = configFile.parentFile
        configDir.mkdirs()
        if (configFile.exists()) {
            configFile.delete()
        }
        configFile << configuration
        configFile.deleteOnExit()
        return configFile
    }

    private void createInputResultFile_Staging(SnvCallingInstance instance, SnvCallingStep step) {
        File inputResultFile = createInputResultFile(instance, step).absoluteStagingPath
        CreateFileHelper.createFile(inputResultFile)
    }

    private void createInputResultFile_Production(SnvCallingInstance instance, SnvCallingStep step) {
        File inputResultFile = createInputResultFile(instance, step).absoluteDataManagementPath
        CreateFileHelper.createFile(inputResultFile)
    }

    private OtpPath createInputResultFile(SnvCallingInstance instance, SnvCallingStep step) {
        OtpPath file
        if (step == SnvCallingStep.CALLING) {
            file = new OtpPath(instance.snvInstancePath, step.getResultFileName(instance.individual, null))
        } else if (step == SnvCallingStep.FILTER_VCF) {
            file = new OtpPath(instance.snvInstancePath, step.getResultFileName())
        } else {
            file = new OtpPath(instance.snvInstancePath, step.getResultFileName(instance.individual))
        }
        return file
    }

    File createBamFile(ProcessedMergedBamFile processedMergedBamFile) {
        File file = new File(processedMergedBamFileService.destinationDirectory(processedMergedBamFile), processedMergedBamFileService.fileName(processedMergedBamFile))
        CreateFileHelper.createFile(file)

        processedMergedBamFile.fileSize = file.size()
        assert processedMergedBamFile.save(flush: true, failOnError: true)
        return file
    }
}
