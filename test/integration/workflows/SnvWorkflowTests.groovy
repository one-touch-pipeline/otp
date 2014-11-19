package workflows

import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFileService
import de.dkfz.tbi.otp.dataprocessing.ProcessingThresholds
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SampleTypeCombinationPerIndividual
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingInstanceTestData
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingStep
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvJobResult
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvProcessingStates
import de.dkfz.tbi.otp.job.jobs.snvcalling.SnvCallingJob
import de.dkfz.tbi.otp.job.jobs.snvcalling.SnvCallingStartJob
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.CreateClusterScriptService
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.job.processing.ProcessingError
import de.dkfz.tbi.otp.job.scheduler.ErrorLogService
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.FileType
import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.ngsdata.SampleTypePerProject
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.testing.GroovyScriptAwareIntegrationTest
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.ExternalScript
import de.dkfz.tbi.otp.utils.HelperUtils
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.junit.Ignore
import org.junit.Test

import de.dkfz.tbi.otp.ngsdata.DomainFactory

import java.util.zip.GZIPInputStream

import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

class SnvWorkflowTests extends GroovyScriptAwareIntegrationTest {

    // The scheduler needs to access the created objects while the test is being executed
    boolean transactional = false

    ProcessedMergedBamFileService processedMergedBamFileService
    ErrorLogService errorLogService
    ExecutionService executionService
    CreateClusterScriptService createClusterScriptService


    final int TIMEOUT_IN_MINUTES = 30

    File baseDirDKFZ
    File testDirDKFZ
    File baseDirBioQuant
    File testDirBioQuant

    Realm realmDataManagement
    Realm realmDataProcessing
    Individual individual
    ProcessedMergedBamFile bamFileTumor
    ProcessedMergedBamFile bamFileControl
    SampleTypeCombinationPerIndividual sampleTypeCombination
    SeqType seqType
    Project project
    SnvConfig snvConfig

    ExternalScript callingScript
    ExternalScript joiningScript
    ExternalScript annotationScript
    ExternalScript deepAnnotationScript
    ExternalScript filterScript

    SnvCallingStartJob snvStartJob

    void prepare(Realm.Cluster where) {
        createUserAndRoles()

        baseDirDKFZ = new File("WORKFLOW_ROOT/SnvCallingWorkflow")
        baseDirBioQuant = new File("$BQ_ROOTPATH/dmg/otp/workflow-tests/SnvCallingWorkflow")
        testDirDKFZ = new File(baseDirDKFZ, "tmp-${HelperUtils.getUniqueString()}")
        testDirBioQuant = new File(baseDirBioQuant, "tmp-${HelperUtils.getUniqueString()}")

        File inputDiseaseBamFile = new File(baseDirDKFZ, "inputFiles/tumor_SOMEPID_merged.mdup.bam")
        File inputDiseaseBaiFile = new File(baseDirDKFZ, "inputFiles/tumor_SOMEPID_merged.mdup.bam.bai")
        File inputControlBamFile = new File(baseDirDKFZ, "inputFiles/control_SOMEPID_merged.mdup.bam")
        File inputControlBaiFile = new File(baseDirDKFZ, "inputFiles/control_SOMEPID_merged.mdup.bam.bai")

        project = Project.build(
                dirName: "test",
        )

        def realmOptions = [
                pbsOptions        : '{"-l": {nodes: "1:lsdf", walltime: "29:00"}, "-j": "oe"}',
                programsRootPath  : '/',
        ]
        if(where == Realm.Cluster.BIOQUANT) {
            realmOptions += [unixUser: "unixUser"]
        }

        def realm = DomainFactory.createRealmDataManagementDKFZ(realmOptions + [
                rootPath          : new File(testDirDKFZ, "root_path").absolutePath,
                processingRootPath: new File(testDirDKFZ, "processing_root_path").absolutePath,
                loggingRootPath   : new File(testDirDKFZ, "logging_root_path").absolutePath,
                stagingRootPath   : null,
        ])
        assertNotNull(realm.save(flush: true))

        realm = DomainFactory.createRealmDataProcessingDKFZ(realmOptions + [
                rootPath          : new File(testDirDKFZ, "root_path").absolutePath,
                processingRootPath: new File(testDirDKFZ, "processing_root_path").absolutePath,
                loggingRootPath   : new File(testDirDKFZ, "logging_root_path").absolutePath,
                stagingRootPath   : new File(testDirDKFZ, "staging_root_path").absolutePath,
        ])
        assertNotNull(realm.save(flush: true))

        realm = DomainFactory.createRealmDataManagementBioQuant(realmOptions + [
                rootPath          : new File(testDirBioQuant, "root_path").absolutePath,
                processingRootPath: new File(testDirBioQuant, "processing_root_path").absolutePath,
                loggingRootPath   : new File(testDirBioQuant, "logging_root_path").absolutePath,
                stagingRootPath   : null,
        ])
        realm.unixUser = "unixUser2"
        assertNotNull(realm.save(flush: true))

        realm = DomainFactory.createRealmDataProcessingBioQuant(realmOptions + [
                rootPath          : new File(testDirBioQuant, "root_path").absolutePath,
                processingRootPath: new File(testDirDKFZ, "processing_root_path").absolutePath,
                loggingRootPath   : new File(testDirDKFZ, "logging_root_path").absolutePath,
                stagingRootPath   : new File(testDirDKFZ, "staging_root_path").absolutePath,
        ])
        assertNotNull(realm.save(flush: true))


        if(where == Realm.Cluster.DKFZ) {
            realmDataManagement = Realm.findByNameAndOperationType("DKFZ", Realm.OperationType.DATA_MANAGEMENT)
            realmDataProcessing = Realm.findByNameAndOperationType("DKFZ", Realm.OperationType.DATA_PROCESSING)
        } else if (where == Realm.Cluster.BIOQUANT) {
            realmDataManagement = Realm.findByNameAndOperationType("BioQuant", Realm.OperationType.DATA_MANAGEMENT)
            realmDataProcessing = Realm.findByNameAndOperationType("BioQuant", Realm.OperationType.DATA_PROCESSING)
        } else {
            throw new UnsupportedOperationException()
        }
        project.realmName = realmDataManagement.name
        assertNotNull(project.save(flush: true))

        assert !testDirDKFZ.exists() && !testDirBioQuant.exists()
        String mkDirs = createClusterScriptService.makeDirs([new File(realmDataProcessing.rootPath), new File(realmDataProcessing.loggingRootPath, "log/status/"), new File(realmDataProcessing.stagingRootPath)])
        assert executionService.executeCommand(realmDataProcessing, mkDirs).toInteger() == 0
        mkDirs = createClusterScriptService.makeDirs([new File(realmDataProcessing.stagingRootPath, "clusterScriptExecutorScripts")], "0777")
        assert executionService.executeCommand(realmDataProcessing, mkDirs).toInteger() == 0
        mkDirs = createClusterScriptService.makeDirs([new File(realmDataManagement.rootPath)])
        assert executionService.executeCommand(realmDataManagement, mkDirs).toInteger() == 0

        individual = Individual.build(
                project: project,
                type: Individual.Type.REAL,
        )

        seqType = SeqType.build(
                name: "EXOME",
                libraryLayout: "PAIRED",
                dirName: "tmp",
        )

        SnvCallingInstanceTestData testData = new SnvCallingInstanceTestData()
        bamFileTumor = testData.createProcessedMergedBamFile(individual, seqType, "TUMOR")
        bamFileControl = testData.createProcessedMergedBamFile(individual, seqType, "CONTROL")

        SampleType sampleTypeTumor = bamFileTumor.sampleType
        bamFileTumor.fileSize = inputDiseaseBamFile.size()
        assertNotNull(bamFileTumor.save(flush: true))

        SampleTypePerProject.build(
                project: project,
                sampleType: sampleTypeTumor,
                category: SampleType.Category.DISEASE,
        )

        SampleType sampleTypeControl = bamFileControl.sampleType
        bamFileControl.fileSize = inputControlBamFile.size()
        assertNotNull(bamFileControl.save(flush: true))

        SampleTypePerProject.build(
                project: project,
                sampleType: sampleTypeControl,
                category: SampleType.Category.CONTROL,
        )

        sampleTypeCombination = SampleTypeCombinationPerIndividual.build(
                processingStatus: SampleTypeCombinationPerIndividual.ProcessingStatus.NEEDS_PROCESSING,
                sampleType1: sampleTypeTumor,
                sampleType2: sampleTypeControl,
                individual: individual,
                seqType: seqType,
        )

        ProcessingThresholds.build(
                project: project,
                seqType: seqType,
                sampleType: sampleTypeTumor,
                coverage: 1,
                numberOfLanes: null,
        )

        ProcessingThresholds.build(
                project: project,
                seqType: seqType,
                sampleType: sampleTypeControl,
                coverage: 1,
                numberOfLanes: null,
        )


        joiningScript = ExternalScript.build(
                scriptIdentifier: SnvCallingJob.CHROMOSOME_VCF_JOIN_SCRIPT_IDENTIFIER,
                filePath: "/path/to/programs/otp/snv-pipeline-r1610/joinSNVVCFFiles.sh",
        )
        callingScript = ExternalScript.build(
                scriptIdentifier: "SnvCallingStep.CALLING",
                filePath: "/path/to/programs/otp/snv-pipeline-r1610/snvCalling.sh",
        )
        annotationScript = ExternalScript.build(
                scriptIdentifier: "SnvCallingStep.SNV_ANNOTATION",
                filePath: "/path/to/programs/otp/snv-pipeline-r1610/snvAnnotation.sh",
        )
        deepAnnotationScript = ExternalScript.build(
                scriptIdentifier: "SnvCallingStep.SNV_DEEPANNOTATION",
                filePath: "/path/to/programs/otp/co-tools-r1610/vcf_pipeAnnotator.sh",
        )
        filterScript = ExternalScript.build(
                scriptIdentifier: "SnvCallingStep.FILTER_VCF",
                filePath: "/path/to/programs/otp/snv-pipeline-r1610/filter_vcf.sh",
        )


        mkDirs = createClusterScriptService.makeDirs([
                new File(processedMergedBamFileService.filePath(bamFileTumor)).parentFile,
                new File(processedMergedBamFileService.filePath(bamFileControl)).parentFile,
        ])
        assert executionService.executeCommand(realmDataManagement, mkDirs).toInteger() == 0

        def targetLocations = [processedMergedBamFileService.filePath(bamFileTumor), processedMergedBamFileService.filePathForBai(bamFileTumor),
                processedMergedBamFileService.filePath(bamFileControl), processedMergedBamFileService.filePathForBai(bamFileControl)]
        String copyFiles = createClusterScriptService.createTransferScript(
                [inputDiseaseBamFile, inputDiseaseBaiFile, inputControlBamFile, inputControlBaiFile],
                targetLocations.collect(new LinkedList<File>()) { new File(it) },
                [null, null, null, null])
        assert executionService.executeCommand(realmDataProcessing, copyFiles) == (targetLocations.collect { "${new File(it).absolutePath}: OK\n" }).join("")
    }


    void tearDown(Realm.Cluster where) {
        String cmdCleanUp = createClusterScriptService.removeDirs([testDirDKFZ], CreateClusterScriptService.RemoveOption.RECURSIVE_FORCE)
        assert executionService.executeCommand(realmDataProcessing, cmdCleanUp).toInteger() == 0
        if(where == Realm.Cluster.BIOQUANT) {
            cmdCleanUp = createClusterScriptService.removeDirs([testDirBioQuant], CreateClusterScriptService.RemoveOption.RECURSIVE_FORCE)
            assert executionService.executeCommand(realmDataManagement, cmdCleanUp).toInteger() == 0
        }
    }


    @Ignore
    @Test
    void testWholeSnvWorkflow() {
        prepare(Realm.Cluster.DKFZ)
        snvConfig = SnvConfig.createFromFile(project, seqType, new File(baseDirDKFZ, "configFile/runtimeConfig.sh"))
        assertNotNull(snvConfig.save(flush: true))

        execute()
        check(SnvCallingStep.CALLING)

        tearDown(Realm.Cluster.DKFZ)
    }


    @Ignore
    @Test
    void testWholeSnvWorkflowAtBioQuant() {
        prepare(Realm.Cluster.BIOQUANT)
        snvConfig = SnvConfig.createFromFile(project, seqType, new File(baseDirDKFZ, "configFile/runtimeConfig.sh"))
        assertNotNull(snvConfig.save(flush: true))

        execute()
        check(SnvCallingStep.CALLING)

        tearDown(Realm.Cluster.BIOQUANT)
    }


    @Ignore
    @Test
    void testSnvAnnotationDeepAnnotationAndFilter() {
        prepare(Realm.Cluster.DKFZ)
        snvConfig = SnvConfig.createFromFile(project, seqType, new File(baseDirDKFZ, "configFile/runtimeConfig_anno.sh"))
        assertNotNull(snvConfig.save(flush: true))
        createJobResults(SnvCallingStep.SNV_ANNOTATION)

        execute()
        check(SnvCallingStep.SNV_ANNOTATION)

        tearDown(Realm.Cluster.DKFZ)
    }


    @Ignore
    @Test
    void testSnvFilter() {
        prepare(Realm.Cluster.DKFZ)
        snvConfig = SnvConfig.createFromFile(project, seqType, new File("${baseDirDKFZ}/configFile/runtimeConfig_filter.sh"))
        assertNotNull(snvConfig.save(flush: true))
        createJobResults(SnvCallingStep.FILTER_VCF)

        execute()
        check(SnvCallingStep.FILTER_VCF)

        tearDown(Realm.Cluster.DKFZ)
    }



    void createJobResults(SnvCallingStep startWith) {
        SnvCallingInstance instance = new SnvCallingInstance(
                processingState: SnvProcessingStates.FINISHED,
                sampleType1BamFile: bamFileTumor,
                sampleType2BamFile: bamFileControl,
                config: snvConfig,
                instanceName: "2014-08-25_15h32",
                sampleTypeCombination: sampleTypeCombination,
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
                md5sum: "123456789012345678901234567890AB",
                fileSize: 1,
        )
        assertNotNull(jobResultCalling.save(flush: true))
        def sourceFiles = [new File("${baseDirDKFZ}/resultFiles/snvs_stds_raw.vcf.gz"), new File("${baseDirDKFZ}/resultFiles/snvs_stds_raw.vcf.gz.tbi")]
        def targetFiles = [jobResultCalling.getResultFilePath().absoluteDataManagementPath, new File("${jobResultCalling.getResultFilePath().absoluteDataManagementPath}.tbi")]


        if(startWith == SnvCallingStep.FILTER_VCF) {
            jobResultAnnotation = new SnvJobResult(
                    snvCallingInstance: instance,
                    step: SnvCallingStep.SNV_ANNOTATION,
                    inputResult: jobResultCalling,
                    processingState: SnvProcessingStates.FINISHED,
                    withdrawn: false,
                    externalScript: annotationScript,
                    md5sum: "123456789012345678901234567890AB",
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
                    md5sum: "123456789012345678901234567890AB",
                    fileSize: 1,
            )
            assertNotNull(jobResultDeepAnnotation.save(flush: true))

            sourceFiles += [new File("${baseDirDKFZ}/resultFiles/snvs_stds.vcf.gz"), new File("${baseDirDKFZ}/resultFiles/snvs_stds.vcf.gz.tbi")]
            targetFiles += [jobResultDeepAnnotation.getResultFilePath().absoluteDataManagementPath, new File("${jobResultDeepAnnotation.getResultFilePath().absoluteDataManagementPath}.tbi")]
        }   else if (startWith != SnvCallingStep.SNV_ANNOTATION) {
            throw new UnsupportedOperationException()
        }


        String makeDirs = createClusterScriptService.makeDirs([instance.snvInstancePath.absoluteDataManagementPath,instance.snvInstancePath.absoluteStagingPath])
        assert executionService.executeCommand(realmDataManagement, makeDirs).toInteger() == 0

        String copyFiles = createClusterScriptService.createTransferScript(
            sourceFiles, targetFiles, [null] * targetFiles.size()
        )
        assert executionService.executeCommand(realmDataManagement, copyFiles) == (targetFiles.collect { "${it.absolutePath}: OK\n" }).join("")
    }


    void execute() {
        SpringSecurityUtils.doWithAuth("admin") {
            run("scripts/workflows/SnvWorkflow.groovy")
        }

        // there will be only one plan in the database
        JobExecutionPlan jobExecutionPlan = CollectionUtils.exactlyOneElement(JobExecutionPlan.list())

        // hack to be able to start the workflow
        snvStartJob.setJobExecutionPlan(jobExecutionPlan)

        boolean workflowFinishedInGivenTimeLimit = waitUntilWorkflowIsOverOrTimeout(TIMEOUT_IN_MINUTES)

        assertTrue(workflowFinishedInGivenTimeLimit)
    }

    void check(SnvCallingStep startedWith) {
        SnvCallingInstance existingInstance = SnvCallingInstance.listOrderById().first()
        SnvCallingInstance createdInstance = SnvCallingInstance.listOrderById().last()

        File resultFileCalling = new File(baseDirDKFZ, "resultFiles/snvs_stds_raw.vcf.gz")
        File resultFileAnnotation = new File(baseDirDKFZ, "resultFiles/snvs_stds.vcf.gz")

        assert createdInstance.processingState == SnvProcessingStates.FINISHED
        assert createdInstance.config == snvConfig
        assert createdInstance.sampleType1BamFile == bamFileTumor
        assert createdInstance.sampleType2BamFile == bamFileControl

        List<String> expected, actual

        SnvJobResult callingResult = createdInstance.findLatestResultForSameBamFiles(SnvCallingStep.CALLING)
        if(startedWith == SnvCallingStep.CALLING) {
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
        if(startedWith == SnvCallingStep.CALLING || startedWith == SnvCallingStep.SNV_ANNOTATION) {
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


        // check content only for text files
        ["germline_functional_snvs_conf_8_to_10.vcf",
         "purityEST.txt",
         "sequence_specific_error_Matrix_conf_8_to_10.txt",
         "sequencing_specific_error_Matrix_conf_8_to_10.txt",
         "somatic_functional_ncRNA_snvs_conf_8_to_10.vcf",
         "somatic_functional_snvs_conf_8_to_10.vcf",
         "somatic_in_dbSNP_conf_8_to_10.txt",
         "somatic_snvs_conf_8_to_10.vcf",
        ].each {
            expected = new File(baseDirDKFZ, "resultFiles/snvs_stds_${it}").readLines()
            actual = new File(filterResult.getResultFilePath().absoluteDataManagementPath, "snvs_${individual.pid}_${it}").readLines()
            compareFiles(expected, actual)
        }
        // check that all other files were created
        ["allSNVdiagnosticsPlots.pdf",
         "intermutation_distance_conf_8_to_10.pdf",
         "MAF_conf_8_to_10.pdf",
         "perChromFreq_conf_8_to_10.pdf",
         "sequence_specific_error_plot_conf_8_to_10.pdf",
         "sequencing_specific_error_plot_conf_8_to_10.pdf",
         "snvs_with_context_conf_8_to_10.pdf",
        ].each {
            assert new File(filterResult.getResultFilePath().absoluteDataManagementPath, "snvs_${individual.pid}_" + it).exists()
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


    /**
     * Pauses the test until the workflow is finished or the timeout is reached
     * @return true if the process is finished, false otherwise
     */
    static boolean waitUntilWorkflowIsOverOrTimeout(int timeout) {
        println "Started to wait (until workflow is over or timeout)"
        int timeCount = 0
        boolean finished = false
        while (!finished && (timeCount < timeout)) {
            println "waiting ... "
            timeCount++
            sleep(60000)
            finished = isProcessFinished()
        }
        return finished
    }

    // TODO  ( jira: OTP-566) maybe we can make this a sub class and put this method in parent..
    /**
     * Checks if the process created by the test is already finished and retrieves corresponding value
     * @return true if the process is finished, false otherwise
     */
    static boolean isProcessFinished() {
        //TODO ( jira: OTP-566) there should be not more than one .. can make assert to be sure
        List<de.dkfz.tbi.otp.job.processing.Process> processes = de.dkfz.tbi.otp.job.processing.Process.list()
        boolean finished = false
        if (processes.size() > 0) {
            de.dkfz.tbi.otp.job.processing.Process process = CollectionUtils.exactlyOneElement(processes)
            // Required otherwise will never detect the change..
            process.refresh()
            finished = process?.finished
        }
        return finished
    }
}
