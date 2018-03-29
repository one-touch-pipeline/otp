package de.dkfz.tbi.otp.dataprocessing.snvcalling

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.jobs.snvcalling.SnvCallingJob
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.ExternalScript
import de.dkfz.tbi.otp.utils.HelperUtils
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

@Component
class SnvCallingInstanceTestData {

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    Realm realm
    ProcessedMergedBamFile bamFileTumor
    ProcessedMergedBamFile bamFileControl
    SamplePair samplePair
    SnvConfig snvConfig
    ExternalScript externalScript_Joining

    void createSnvObjects() {
        bamFileControl = DomainFactory.createProcessedMergedBamFile(DomainFactory.createMergingSet(), DomainFactory.randomProcessedBamFileProperties)

        this.realm = bamFileControl.project.realm

        (bamFileTumor, samplePair) = createDisease(bamFileControl.mergingWorkPackage)

        externalScript_Joining = new ExternalScript(
                scriptIdentifier: SnvCallingJob.CHROMOSOME_VCF_JOIN_SCRIPT_IDENTIFIER,
                scriptVersion: 'v1',
                filePath: "/tmp/scriptLocation/joining.sh",
                author: "otptest",
        )
        assert externalScript_Joining.save(flush: true)
    }

    SnvConfig createSnvConfig(String configuration = "testConfig") {
        snvConfig = new SnvConfig(
                project: samplePair.project,
                seqType: samplePair.seqType,
                configuration: configuration,
                externalScriptVersion: "v1",
                pipeline: DomainFactory.createOtpSnvPipelineLazy(),
        )
        assert snvConfig.save(flush: true, failOnError: true)
        return snvConfig
    }

    static List createDisease(MergingWorkPackage controlMwp) {
        SamplePair samplePair = DomainFactory.createDisease(controlMwp)
        ProcessedMergedBamFile diseaseBamFile = DomainFactory.createProcessedMergedBamFile(
                samplePair.mergingWorkPackage1,
                DomainFactory.randomProcessedBamFileProperties)
        assert diseaseBamFile.save(flush: true)
        return [diseaseBamFile, samplePair]
    }

    SnvJobResult createAndSaveSnvJobResult(SnvCallingInstance instance, SnvCallingStep step, SnvJobResult inputResult = null, AnalysisProcessingStates processingState = AnalysisProcessingStates.FINISHED, boolean withdrawn = false) {
        ExternalScript externalScript = atMostOneElement(ExternalScript.findAllByScriptIdentifierAndScriptVersionAndDeprecatedDateIsNull(step.externalScriptIdentifier, instance.config.externalScriptVersion))
        if (externalScript == null) {
            externalScript = createOrFindExternalScript(
                    scriptIdentifier: step.externalScriptIdentifier,
                    scriptVersion: instance.config.externalScriptVersion,
                    filePath: "/dev/null/otp-test/${step.externalScriptIdentifier}",
            )
        }
        final SnvJobResult result = new SnvJobResult([
                snvCallingInstance: instance,
                step              : step,
                inputResult       : inputResult,
                processingState   : processingState,
                withdrawn         : withdrawn,
                externalScript    : externalScript,
                md5sum            : HelperUtils.randomMd5sum,
                fileSize          : 1234l,
        ])
        if (step == SnvCallingStep.CALLING) {
            result.chromosomeJoinExternalScript = externalScript_Joining
        }
        assert result.save(flush: true, failOnError: true)
        return result
    }

    SnvCallingInstance createAndSaveSnvCallingInstance(Map properties = [:]) {
        final SnvCallingInstance instance = createSnvCallingInstance(properties)
        assert instance.save(flush: true, failOnError: true)
        return instance
    }

    SnvCallingInstance createSnvCallingInstance(Map properties = [:]) {
        return DomainFactory.createSnvCallingInstance([
                processingState   : AnalysisProcessingStates.IN_PROGRESS,
                sampleType1BamFile: bamFileTumor,
                sampleType2BamFile: bamFileControl,
                config            : (properties.snvConfig ?: snvConfig) ?: createSnvConfig(),
                instanceName      : "2014-08-25_15h32",
                samplePair        : samplePair
        ] + properties)
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
            file = new OtpPath(instance.instancePath, step.getResultFileName(instance.individual, null))
        } else if (step == SnvCallingStep.FILTER_VCF) {
            file = new OtpPath(instance.instancePath, step.getResultFileName())
        } else {
            file = new OtpPath(instance.instancePath, step.getResultFileName(instance.individual))
        }
        return file
    }

    File createBamFile(ProcessedMergedBamFile processedMergedBamFile) {
        File file = new File(AbstractMergedBamFileService.destinationDirectory(processedMergedBamFile), processedMergedBamFile.getBamFileName())
        CreateFileHelper.createFile(file)

        processedMergedBamFile.fileSize = file.size()
        assert processedMergedBamFile.save(flush: true, failOnError: true)

        processedMergedBamFile.mergingWorkPackage.bamFileInProjectFolder = processedMergedBamFile
        assert processedMergedBamFile.mergingWorkPackage.save(flush: true)

        return file
    }

    static ExternalScript createOrFindExternalScript(Map properties = [:]) {
        final ExternalScript externalScript = ExternalScript.findOrSaveWhere([
                scriptIdentifier: "externalScriptIdentifier",
                scriptVersion   : 'v1',
                deprecatedDate  : null,
                filePath        : "/dev/null/otp-test/externalScript.sh",
                author          : "otptest",
        ] + properties)
        assert externalScript.save(flush: true)
        return externalScript
    }
}
