package workflows.analysis.pair.snv

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.jobs.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.junit.*

import java.util.zip.*

import static org.junit.Assert.*

abstract class AbstractOtpSnvWorkflowTests extends AbstractSnvWorkflowTests {

    final String VERSION = "1.0.166"
    final String CO_SCRIPTS_BASE_DIR = "/path/to/programs/otp/COWorkflows_${VERSION}/resources/analysisTools"
    final String SNV_PIPELINE_SCRIPTS_PATH = "${CO_SCRIPTS_BASE_DIR}/snvPipeline"
    final String ANALYSIS_SCRIPTS_PATH = "${CO_SCRIPTS_BASE_DIR}/tools"

    ExternalScript callingScript
    ExternalScript joiningScript
    ExternalScript annotationScript
    ExternalScript deepAnnotationScript
    ExternalScript filterScript


    @Override
    protected void setupForLoadingWorkflow() {
        super.setupForLoadingWorkflow()
        DomainFactory.createOtpSnvPipelineLazy()
        createExternalScripts()

        String mkDirs = createClusterScriptService.makeDirs([new File(realm.stagingRootPath, "clusterScriptExecutorScripts")], "0777")
        assert executionService.executeCommand(realm, mkDirs).toInteger() == 0
    }


    @Override
    ConfigPerProject createConfig() {
        config = SnvConfig.createFromFile(project, seqType, new File(getWorkflowData(), "configFile_${VERSION}/runtimeConfig.sh-cluster_13.1"), VERSION)
    }

    @Override
    ReferenceGenome createReferenceGenome() {
        return DomainFactory.createReferenceGenome()
    }

    @Override
    List<String> getWorkflowScripts() {
        return ["scripts/workflows/SnvWorkflow.groovy"]
    }


    @Test
    void testSnvAnnotationDeepAnnotationAndFilter() {
        config = SnvConfig.createFromFile(project, seqType, new File(getWorkflowData(), "configFile_${VERSION}/runtimeConfig_anno.sh-cluster_13.1"), VERSION)
        createJobResults(SnvCallingStep.SNV_ANNOTATION)

        execute()
        check(SnvCallingStep.SNV_ANNOTATION)
    }


    @Test
    void testSnvFilter() {
        config = SnvConfig.createFromFile(project, seqType, new File(getWorkflowData(), "configFile_${VERSION}/runtimeConfig_filter.sh-cluster_13.1"), VERSION)
        createJobResults(SnvCallingStep.FILTER_VCF)

        execute()
        check(SnvCallingStep.FILTER_VCF)
    }


    void createExternalScripts() {
        joiningScript = DomainFactory.createExternalScript(
                scriptIdentifier: SnvCallingJob.CHROMOSOME_VCF_JOIN_SCRIPT_IDENTIFIER,
                filePath: "${SNV_PIPELINE_SCRIPTS_PATH}/joinSNVVCFFiles.sh",
                scriptVersion: VERSION
        )
        callingScript = DomainFactory.createExternalScript(
                scriptIdentifier: "SnvCallingStep.CALLING",
                filePath: "${SNV_PIPELINE_SCRIPTS_PATH}/snvCalling.sh",
                scriptVersion: VERSION
        )
        annotationScript = DomainFactory.createExternalScript(
                scriptIdentifier: "SnvCallingStep.SNV_ANNOTATION",
                filePath: "${SNV_PIPELINE_SCRIPTS_PATH}/snvAnnotation.sh",
                scriptVersion: VERSION
        )
        deepAnnotationScript = DomainFactory.createExternalScript(
                scriptIdentifier: "SnvCallingStep.SNV_DEEPANNOTATION",
                filePath: "${ANALYSIS_SCRIPTS_PATH}/vcf_pipeAnnotator.sh",
                scriptVersion: VERSION
        )
        filterScript = DomainFactory.createExternalScript(
                scriptIdentifier: "SnvCallingStep.FILTER_VCF",
                filePath: "${SNV_PIPELINE_SCRIPTS_PATH}/filter_vcf.sh",
                scriptVersion: VERSION
        )
    }


    void createJobResults(SnvCallingStep startWith) {
        SnvCallingInstance instance = new SnvCallingInstance(
                processingState: AnalysisProcessingStates.FINISHED,
                sampleType1BamFile: bamFileTumor,
                sampleType2BamFile: bamFileControl,
                config: config,
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
                processingState: AnalysisProcessingStates.FINISHED,
                withdrawn: false,
                externalScript: callingScript,
                chromosomeJoinExternalScript: joiningScript,
                md5sum: HelperUtils.randomMd5sum,
                fileSize: 1,
        )
        assertNotNull(jobResultCalling.save(flush: true))
        List<File> sourceFiles = [new File("${getWorkflowData()}/resultFiles_${VERSION}/snvs_stds_raw.vcf.gz"), new File("${getWorkflowData()}/resultFiles_${VERSION}/snvs_stds_raw.vcf.gz.tbi")]
        List<File> targetFiles = [jobResultCalling.getResultFilePath().absoluteDataManagementPath, new File("${jobResultCalling.getResultFilePath().absoluteDataManagementPath}.tbi")]
        List<File> sourceFilesForFilter = []
        List<File> targetFilesForFilter = []

        if (startWith == SnvCallingStep.FILTER_VCF) {
            jobResultAnnotation = new SnvJobResult(
                    snvCallingInstance: instance,
                    step: SnvCallingStep.SNV_ANNOTATION,
                    inputResult: jobResultCalling,
                    processingState: AnalysisProcessingStates.FINISHED,
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
                    processingState: AnalysisProcessingStates.FINISHED,
                    withdrawn: false,
                    externalScript: deepAnnotationScript,
                    md5sum: HelperUtils.randomMd5sum,
                    fileSize: 1,
            )
            assertNotNull(jobResultDeepAnnotation.save(flush: true))

            File previousResultDir = new File("${getWorkflowData()}/resultFiles_${VERSION}")
            File annotationResultFile = jobResultAnnotation.getResultFilePath().absoluteDataManagementPath
            File deepAnnotationResultFile = jobResultDeepAnnotation.getResultFilePath().absoluteDataManagementPath

            sourceFiles << new File(previousResultDir, "snvs_stds.vcf.gz")
            sourceFiles << new File(previousResultDir, "snvs_stds.vcf.gz.tbi")
            sourceFiles << new File(previousResultDir, "snvs_stds.vcf.gz")
            sourceFiles << new File(previousResultDir, "snvs_stds.vcf.gz.tbi")
            targetFiles << deepAnnotationResultFile
            targetFiles << new File("${deepAnnotationResultFile}.tbi")
            targetFiles << annotationResultFile
            targetFiles << new File("${annotationResultFile}.tbi")
        } else if (startWith != SnvCallingStep.SNV_ANNOTATION) {
            throw new UnsupportedOperationException()
        }


        String makeDirs = createClusterScriptService.makeDirs([instance.instancePath.absoluteDataManagementPath, instance.instancePath.absoluteStagingPath])
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

    void checkSpecific() {
        check(SnvCallingStep.CALLING)
    }

    void check(SnvCallingStep startedWith) {
        SnvCallingInstance createdInstance = SnvCallingInstance.listOrderById().last()

        File resultFileCalling = new File(getWorkflowData(), "resultFiles_${VERSION}/snvs_stds_raw.vcf.gz")
        File resultFileAnnotation = new File(getWorkflowData(), "resultFiles_${VERSION}/snvs_stds.vcf.gz")

        List<String> expected, actual

        SnvJobResult callingResult = createdInstance.findLatestResultForSameBamFiles(SnvCallingStep.CALLING)
        if (startedWith == SnvCallingStep.CALLING) {
            assert callingResult.processingState == AnalysisProcessingStates.FINISHED
            assert callingResult.inputResult == null
            assert callingResult.externalScript == callingScript
            assert callingResult.chromosomeJoinExternalScript == joiningScript
            assert SnvCallingInstance.count() == 1

            expected = new GZIPInputStream(new FileInputStream(resultFileCalling)).readLines()
            actual = new GZIPInputStream(new FileInputStream(callingResult.getResultFilePath().absoluteDataManagementPath)).readLines()
            compareFiles(expected, actual)

            File roddyResultsDir = new File(getWorkflowData(), "resultFiles_${VERSION}")
            File otpResultsDir = createdInstance.instancePath.absoluteDataManagementPath
            roddyResultsDir.eachFileRecurse(groovy.io.FileType.FILES) { File resultFile ->
                File otpResultFile = new File(otpResultsDir, resultFile.name)
                if (!resultFile.name =~ /^snvCallingCheckPoint/) {
                    assert otpResultFile.exists()
                }
                if (resultFile.name =~ /\.vcf$/ || resultFile.name =~ /\.txt$/) {
                    compareFiles([resultFile.readLines()], [otpResultFile.readLines()])
                }
            }
        } else {
            assert SnvCallingInstance.count() == 2
        }

        SnvJobResult deepAnnotationResult = createdInstance.findLatestResultForSameBamFiles(SnvCallingStep.SNV_DEEPANNOTATION)
        if (startedWith == SnvCallingStep.CALLING || startedWith == SnvCallingStep.SNV_ANNOTATION) {
            SnvJobResult annotationResult = createdInstance.findLatestResultForSameBamFiles(SnvCallingStep.SNV_ANNOTATION)
            assert annotationResult.processingState == AnalysisProcessingStates.FINISHED
            assert annotationResult.inputResult == callingResult
            assert annotationResult.externalScript == annotationScript
            assert deepAnnotationResult.processingState == AnalysisProcessingStates.FINISHED
            assert deepAnnotationResult.inputResult.id == annotationResult.id
            assert deepAnnotationResult.externalScript == deepAnnotationScript

            expected = new GZIPInputStream(new FileInputStream(resultFileAnnotation)).readLines()
            actual = new GZIPInputStream(new FileInputStream(deepAnnotationResult.getResultFilePath().absoluteDataManagementPath)).readLines()
            compareFiles(expected, actual)
        }

        SnvJobResult filterResult = createdInstance.findLatestResultForSameBamFiles(SnvCallingStep.FILTER_VCF)
        assert filterResult.processingState == AnalysisProcessingStates.FINISHED
        assert filterResult.inputResult == deepAnnotationResult
        assert filterResult.externalScript == filterScript
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
}
