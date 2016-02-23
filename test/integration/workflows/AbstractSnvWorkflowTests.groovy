package workflows

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFileService
import de.dkfz.tbi.otp.dataprocessing.ProcessingThresholds
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.jobs.snvcalling.SnvCallingJob
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.ExternalScript
import de.dkfz.tbi.otp.utils.HelperUtils
import org.joda.time.Duration
import org.junit.Test

import java.util.zip.GZIPInputStream
import static org.junit.Assert.assertNotNull


/**
 * More detailed documentation: https://wiki.local/Database/OTP+-+SNV+pipeline
 *
 * Steps which have to be executed for a CO SNV-scipts update:
 *
 * ! you have to be in group localGroup
 *
 * 1) find out the new revision number the CO wants you to import ("1.0.131")
 * 2) obtain ssh access to otp@headnode (or ask someone who has access to copy the data)
 * 3) ssh otp@headnode
 * 4) copy the new COWorkflows_version to "/path/to/programs/otp/"
 * 5) make all bash scripts in this directory executable: find . -name '*.sh' -exec chmod +x {} +
 * 6) change the version number in the "ImportSnvExternalScripts.groovy" script
 * 7) execute this script on your local OTP to verify it is working
 * 8) copy file "snv_application_file/applicationProperties_Stable.ini" to your local folder
 * 9) modify your "applicationProperties_Stable.ini":
 * 9.1) usePluginVersion=COWorkflows\:1.0.131 -> to newest version
 * 9.2) after the change to the new cluster the "***REMOVED***pbs" hostname has to be replaced by "headnode"
 * 9.3) enter your own user
 * 10) go to "/$WORKFLOW_ROOT/ngsPipelines/RoddyStable"
 * 11) execute "bash roddy.sh testrun coWorkflowsTestProject.test@snvCalling stds --useconfig=/AbolsutePathTo/applicationProperties_Stable.ini"
 * 12) execute "bash roddy.sh rerun coWorkflowsTestProject.test@snvCalling stds --useconfig=/AbolsutePathTo/applicationProperties_Stable.ini"
 * 13) results are here "[REDACTED]rpp/stds/mpileup"
 * 14) execution infos are here "[REDACTED]rpp/stds/roddyExecutionStore"
 * 15) create new result folder for this version in WORKFLOW_ROOT/SnvWorkflow (i.e. resultFiles_1.0.131)
 * 16) copy results of the roddy run in the current result folder
 * 17) since we decided not to overwrite the annotation results, but inserted "annotation" we have now differences in the naming with the CO group in some result files
 * therefore "annotation" has to be inserted to some file names of the Roddy results -> compare with already existing file names of previous Roddy results.
 * 18) change VERSION in SnvWorkflowTest
 * 19) create new config folder for this version in WORKFLOW_ROOT/SnvWorkflow (i.e. configFile_1.0.131)
 * 20) copy configs from previous version in this version & adapt paths within the configs (i.e in vi with :%s/1.0.114/1.0.131/gc)
 * 21) compare runtimeConfig.sh of this Roddy run and the previous Roddy run to find out if there are new variables -> add new ones in new config files
 * 22) run SnvWorkflowTests
 */


abstract class AbstractSnvWorkflowTests extends WorkflowTestCase {

    final String VERSION = "1.0.166"
    final String CO_SCRIPTS_BASE_DIR = "/path/to/programs/otp/COWorkflows_${VERSION}/resources/analysisTools"
    final String SNV_PIPELINE_SCRIPTS_PATH = "${CO_SCRIPTS_BASE_DIR}/snvPipeline"
    final String ANALYSIS_SCRIPTS_PATH = "${CO_SCRIPTS_BASE_DIR}/tools"
    final Double COVERAGE = 30.0
    final Map PROCESSED_BAM_FILE_PROPERTIES = DomainFactory.PROCESSED_BAM_FILE_PROPERTIES + [
            coverage: COVERAGE,
    ]

    ProcessedMergedBamFileService processedMergedBamFileService

    Individual individual
    SamplePair samplePair
    SeqType seqType
    Project project
    SnvConfig snvConfig
    AbstractMergedBamFile bamFileTumor
    AbstractMergedBamFile bamFileControl
    SampleType sampleTypeTumor
    SampleType sampleTypeControl

    ExternalScript callingScript
    ExternalScript joiningScript
    ExternalScript annotationScript
    ExternalScript deepAnnotationScript
    ExternalScript filterScript


    protected void setupForLoadingWorkflow() {
        DomainFactory.createAlignableSeqTypes()
    }

    @Test
    void testWholeSnvWorkflow() {
        snvConfig = SnvConfig.createFromFile(project, seqType, new File(getWorkflowDirectory(), "configFile_${VERSION}/runtimeConfig.sh-cluster_13.1"), VERSION)
        assertNotNull(snvConfig.save(flush: true))

        execute()
        check(SnvCallingStep.CALLING)
    }


    @Test
    void testSnvAnnotationDeepAnnotationAndFilter() {
        snvConfig = SnvConfig.createFromFile(project, seqType, new File(getWorkflowDirectory(), "configFile_${VERSION}/runtimeConfig_anno.sh-cluster_13.1"), VERSION)
        assertNotNull(snvConfig.save(flush: true))
        createJobResults(SnvCallingStep.SNV_ANNOTATION)

        execute()
        check(SnvCallingStep.SNV_ANNOTATION)
    }


    @Test
    void testSnvFilter() {
        snvConfig = SnvConfig.createFromFile(project, seqType, new File("${getWorkflowDirectory()}/configFile_${VERSION}/runtimeConfig_filter.sh-cluster_13.1"), VERSION)
        assertNotNull(snvConfig.save(flush: true))
        createJobResults(SnvCallingStep.FILTER_VCF)

        execute()
        check(SnvCallingStep.FILTER_VCF)
    }

    void createExternalScripts() {
        joiningScript = ExternalScript.build(
                scriptIdentifier: SnvCallingJob.CHROMOSOME_VCF_JOIN_SCRIPT_IDENTIFIER,
                filePath: "${SNV_PIPELINE_SCRIPTS_PATH}/joinSNVVCFFiles.sh",
                scriptVersion: VERSION
        )
        callingScript = ExternalScript.build(
                scriptIdentifier: "SnvCallingStep.CALLING",
                filePath: "${SNV_PIPELINE_SCRIPTS_PATH}/snvCalling.sh",
                scriptVersion: VERSION
        )
        annotationScript = ExternalScript.build(
                scriptIdentifier: "SnvCallingStep.SNV_ANNOTATION",
                filePath: "${SNV_PIPELINE_SCRIPTS_PATH}/snvAnnotation.sh",
                scriptVersion: VERSION
        )
        deepAnnotationScript = ExternalScript.build(
                scriptIdentifier: "SnvCallingStep.SNV_DEEPANNOTATION",
                filePath: "${ANALYSIS_SCRIPTS_PATH}/vcf_pipeAnnotator.sh",
                scriptVersion: VERSION
        )
        filterScript = ExternalScript.build(
                scriptIdentifier: "SnvCallingStep.FILTER_VCF",
                filePath: "${SNV_PIPELINE_SCRIPTS_PATH}/filter_vcf.sh",
                scriptVersion: VERSION
        )
    }


    void createThresholds() {
        ProcessingThresholds.build(
                project: project,
                seqType: seqType,
                sampleType: sampleTypeTumor,
                coverage: COVERAGE,
                numberOfLanes: null,
        )

        ProcessingThresholds.build(
                project: project,
                seqType: seqType,
                sampleType: sampleTypeControl,
                coverage: COVERAGE,
                numberOfLanes: null,
        )
    }


    void fileSystemSetup() {
        File inputDiseaseBamFile = new File(getWorkflowDirectory(), "inputFiles/tumor_SOMEPID_merged.mdup.bam")
        File inputDiseaseBaiFile = new File(getWorkflowDirectory(), "inputFiles/tumor_SOMEPID_merged.mdup.bam.bai")
        File inputControlBamFile = new File(getWorkflowDirectory(), "inputFiles/control_SOMEPID_merged.mdup.bam")
        File inputControlBaiFile = new File(getWorkflowDirectory(), "inputFiles/control_SOMEPID_merged.mdup.bam.bai")

        String mkDirs = createClusterScriptService.makeDirs([new File(realm.stagingRootPath, "clusterScriptExecutorScripts")], "0777")
        assert executionService.executeCommand(realm, mkDirs).toInteger() == 0

        List<File> targetLocations

        if (bamFileTumor instanceof RoddyBamFile && bamFileControl instanceof RoddyBamFile) {
            createDirectories([
                    bamFileTumor.workDirectory,
                    bamFileControl.workDirectory,
            ])

            targetLocations = [
                    bamFileTumor.workBamFile,
                    bamFileTumor.workBaiFile,
                    bamFileControl.workBamFile,
                    bamFileControl.workBaiFile
            ]

        } else if (bamFileTumor instanceof  ProcessedMergedBamFile && bamFileControl instanceof ProcessedMergedBamFile) {
            createDirectories([
                    new File(processedMergedBamFileService.filePath(bamFileTumor)).parentFile,
                    new File(processedMergedBamFileService.filePath(bamFileControl)).parentFile,
            ])

            targetLocations = [
                    new File(processedMergedBamFileService.filePath(bamFileTumor)),
                    new File(processedMergedBamFileService.filePathForBai(bamFileTumor)),
                    new File(processedMergedBamFileService.filePath(bamFileControl)),
                    new File(processedMergedBamFileService.filePathForBai(bamFileControl))
            ]
        } else {
            throw new RuntimeException("The following bamFiles can not be processed: ${bamFileTumor}, ${bamFileControl}")
        }
        [[inputDiseaseBamFile, inputDiseaseBaiFile, inputControlBamFile, inputControlBaiFile],
         targetLocations].transpose().each { List<String> transposed ->
            executionService.executeCommand(realm, "cp ${transposed[0]} ${transposed[1]}")
        }

        bamFileTumor.fileSize = inputDiseaseBamFile.size()
        assertNotNull(bamFileTumor.save(flush: true))

        bamFileControl.fileSize = inputControlBamFile.size()
        assertNotNull(bamFileControl.save(flush: true))
    }



    void createSnvSpecificSetup() {
        SampleTypePerProject.build(
                project: project,
                sampleType: sampleTypeTumor,
                category: SampleType.Category.DISEASE,
        )

        SampleTypePerProject.build(
                project: project,
                sampleType: sampleTypeControl,
                category: SampleType.Category.CONTROL,
        )

        samplePair = DomainFactory.createSamplePair(bamFileTumor.mergingWorkPackage, bamFileControl.mergingWorkPackage)
    }


    void createJobResults(SnvCallingStep startWith) {
        SnvCallingInstance instance = new SnvCallingInstance(
                processingState: SnvProcessingStates.FINISHED,
                sampleType1BamFile: bamFileTumor,
                sampleType2BamFile: bamFileControl,
                config: snvConfig,
                instanceName: "2014-08-25_15h32",
                samplePair: samplePair,
                latestDataFileCreationDate: DataFile.createCriteria().get {
                    seqTrack {
                        'in'('id', [bamFileTumor, bamFileControl].sum { it.containedSeqTracks }*.id)
                    }
                    fileType {
                        eq('type', FileType.Type.SEQUENCE)
                    }
                    projections {
                        max('dateCreated')
                    }
                },
        )
        assertNotNull(instance.save(flush: true))

        SnvJobResult jobResultCalling
        SnvJobResult jobResultAnnotation
        SnvJobResult jobResultDeepAnnotation

        jobResultCalling = new SnvJobResult(
                snvCallingInstance: instance,
                step: SnvCallingStep.CALLING,
                inputResult: null,
                processingState: SnvProcessingStates.FINISHED,
                withdrawn: false,
                externalScript: callingScript,
                chromosomeJoinExternalScript: joiningScript,
                md5sum: HelperUtils.randomMd5sum,
                fileSize: 1,
        )
        assertNotNull(jobResultCalling.save(flush: true))
        List<File> sourceFiles = [new File("${getWorkflowDirectory()}/resultFiles_${VERSION}/snvs_stds_raw.vcf.gz"), new File("${getWorkflowDirectory()}/resultFiles_${VERSION}/snvs_stds_raw.vcf.gz.tbi")]
        List<File> targetFiles = [jobResultCalling.getResultFilePath().absoluteDataManagementPath, new File("${jobResultCalling.getResultFilePath().absoluteDataManagementPath}.tbi")]
        List<File> sourceFilesForFilter = []
        List<File> targetFilesForFilter = []

        if (startWith == SnvCallingStep.FILTER_VCF) {
            jobResultAnnotation = new SnvJobResult(
                    snvCallingInstance: instance,
                    step: SnvCallingStep.SNV_ANNOTATION,
                    inputResult: jobResultCalling,
                    processingState: SnvProcessingStates.FINISHED,
                    withdrawn: false,
                    externalScript: annotationScript,
                    md5sum: HelperUtils.randomMd5sum,
                    fileSize: 1,
            )
            assertNotNull(jobResultAnnotation.save(flush: true))

            jobResultDeepAnnotation = new SnvJobResult(
                    snvCallingInstance: instance,
                    step: SnvCallingStep.SNV_DEEPANNOTATION,
                    inputResult: jobResultAnnotation,
                    processingState: SnvProcessingStates.FINISHED,
                    withdrawn: false,
                    externalScript: deepAnnotationScript,
                    md5sum: HelperUtils.randomMd5sum,
                    fileSize: 1,
            )
            assertNotNull(jobResultDeepAnnotation.save(flush: true))

            File previousResultDir = new File("${getWorkflowDirectory()}/resultFiles_${VERSION}")
            File deepAnnotationResultFile = jobResultDeepAnnotation.getResultFilePath().absoluteDataManagementPath

            previousResultDir.eachFileRecurse (groovy.io.FileType.FILES) { File resultFile ->
                if (resultFile.name =~ /annotation/) {
                    sourceFilesForFilter << resultFile
                    targetFilesForFilter << new File(deepAnnotationResultFile.parentFile.parent, resultFile.name)
                }
            }

            sourceFiles << new File(previousResultDir, "snvs_stds.vcf.gz")
            sourceFiles << new File(previousResultDir, "snvs_stds.vcf.gz.tbi")
            targetFiles << deepAnnotationResultFile
            targetFiles << new File("${deepAnnotationResultFile}.tbi")
        }   else if (startWith != SnvCallingStep.SNV_ANNOTATION) {
            throw new UnsupportedOperationException()
        }


        String makeDirs = createClusterScriptService.makeDirs([instance.snvInstancePath.absoluteDataManagementPath,instance.snvInstancePath.absoluteStagingPath])
        assert executionService.executeCommand(realm, makeDirs).toInteger() == 0

        [sourceFiles, targetFiles].transpose().each { List<String> transposed ->
             executionService.executeCommand(realm, "cp ${transposed[0]} ${transposed[1]}")
        }

        if (!sourceFilesForFilter.empty) {
            [sourceFilesForFilter, targetFilesForFilter].transpose().each { List<String> transposed ->
                executionService.executeCommand(realm, "cp ${transposed[0]} ${transposed[1]}")
            }
        }
    }


    void check(SnvCallingStep startedWith) {
        SnvCallingInstance existingInstance = SnvCallingInstance.listOrderById().first()
        SnvCallingInstance createdInstance = SnvCallingInstance.listOrderById().last()

        File resultFileCalling = new File(getWorkflowDirectory(), "resultFiles_${VERSION}/snvs_stds_raw.vcf.gz")
        File resultFileAnnotation = new File(getWorkflowDirectory(), "resultFiles_${VERSION}/snvs_stds.vcf.gz")

        assert createdInstance.processingState == SnvProcessingStates.FINISHED
        assert createdInstance.config == snvConfig
        assert createdInstance.sampleType1BamFile == bamFileTumor
        assert createdInstance.sampleType2BamFile == bamFileControl

        List<String> expected, actual

        SnvJobResult callingResult = createdInstance.findLatestResultForSameBamFiles(SnvCallingStep.CALLING)
        if (startedWith == SnvCallingStep.CALLING) {
            assert callingResult.processingState == SnvProcessingStates.FINISHED
            assert callingResult.inputResult == null
            assert callingResult.externalScript == callingScript
            assert callingResult.chromosomeJoinExternalScript == joiningScript
            assert SnvCallingInstance.count() == 1

            expected = new GZIPInputStream(new FileInputStream(resultFileCalling)).readLines()
            actual = new GZIPInputStream(new FileInputStream(callingResult.getResultFilePath().absoluteDataManagementPath)).readLines()
            compareFiles(expected, actual)
        } else {
            assert SnvCallingInstance.count() == 2
        }

        SnvJobResult deepAnnotationResult = createdInstance.findLatestResultForSameBamFiles(SnvCallingStep.SNV_DEEPANNOTATION)
        if (startedWith == SnvCallingStep.CALLING || startedWith == SnvCallingStep.SNV_ANNOTATION) {
            SnvJobResult annotationResult = createdInstance.findLatestResultForSameBamFiles(SnvCallingStep.SNV_ANNOTATION)
            assert annotationResult.processingState == SnvProcessingStates.FINISHED
            assert annotationResult.inputResult == callingResult
            assert annotationResult.externalScript == annotationScript
            assert deepAnnotationResult.processingState == SnvProcessingStates.FINISHED
            assert deepAnnotationResult.inputResult.id == annotationResult.id
            assert deepAnnotationResult.externalScript == deepAnnotationScript

            expected = new GZIPInputStream(new FileInputStream(resultFileAnnotation)).readLines()
            actual = new GZIPInputStream(new FileInputStream(deepAnnotationResult.getResultFilePath().absoluteDataManagementPath)).readLines()
            compareFiles(expected, actual)
        }

        SnvJobResult filterResult = createdInstance.findLatestResultForSameBamFiles(SnvCallingStep.FILTER_VCF)
        assert filterResult.processingState == SnvProcessingStates.FINISHED
        assert filterResult.inputResult == deepAnnotationResult
        assert filterResult.externalScript == filterScript

        File roddyResultsDir = new File(getWorkflowDirectory(), "resultFiles_${VERSION}")
        File otpResultsDir = createdInstance.samplePair.samplePairPath.absoluteDataManagementPath
        roddyResultsDir.eachFileRecurse (groovy.io.FileType.FILES) { File resultFile ->
            File otpResultFile = new File(otpResultsDir, resultFile.name)
            if (!resultFile.name =~ /^snvCallingCheckPoint/) {
                assert otpResultFile.exists()
            }
            if (resultFile.name =~ /\.vcf$/ || resultFile.name =~ /\.txt$/) {
                compareFiles([resultFile.readLines()], [otpResultFile.readLines()])
            }
        }
    }

    static void compareFiles(List<String> expected, List<String> actual) {
        String differentLineContainingFileName = /#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	.*	SEQUENCE_CONTEXT	INFO_control\(VAF=variant_allele_fraction;TSR=total_variant_supporting_reads_incl_lowqual\)	ANNOTATION_control/
        String differentLineContainingTimeMeasurements = /(Requested time:|Complet time:) .*/
        [expected, actual].transpose().collect { List<String> transposed ->
            assert transposed[0] == transposed[1] ||
                    (transposed[0] =~ differentLineContainingFileName && transposed[1] =~ differentLineContainingFileName) ||
                    (transposed[0] =~ differentLineContainingTimeMeasurements && transposed[1] =~ differentLineContainingTimeMeasurements)
        }
    }

    @Override
    List<String> getWorkflowScripts() {
        return ["scripts/workflows/SnvWorkflow.groovy"]
    }

    @Override
    Duration getTimeout() {
        Duration.standardMinutes(30)
    }
}
