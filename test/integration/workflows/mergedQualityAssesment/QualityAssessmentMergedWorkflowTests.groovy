package workflows.mergedQualityAssesment

import static org.junit.Assert.*
import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.FileType.Type
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry.Classification
import de.dkfz.tbi.otp.job.jobs.qualityAssessmentMerged.QualityAssessmentMergedStartJob
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.plan.StartJobDefinition
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.ngsqc.FastqcModule
import de.dkfz.tbi.otp.testing.AbstractIntegrationTest
import grails.util.Environment
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.junit.*
import grails.test.mixin.*
import grails.test.mixin.support.*
import grails.test.mixin.domain.*
import org.mozilla.classfile.SuperBlock


class QualityAssessmentMergedWorkflowTests extends AbstractIntegrationTest {
    // The scheduler needs to access the created objects while the test is being executed
    boolean transactional = false

    // TODO want to get rid of this hardcoded.. idea: maybe calculating from the walltime of the cluster jobs..
    int SLEEPING_TIME_IN_MINUTES = 200

    GrailsApplication grailsApplication

    ExecutionService executionService

    ProcessingOptionService processingOptionService

    ProcessedMergingFileService processedMergingFileService

    ProcessedMergedBamFileService processedMergedBamFileService

    QualityAssessmentMergedStartJob qualityAssessmentMergedStartJob

    ProcessedMergedBamFile processedMergedBamFile

    Realm realm

    // TODO This paths should be obtained from somewhere else..  maybe from .otpproperties, but I am hardcoding for now..
    String base = "STORAGE_ROOT/dmg/otp_test/workflows/"
    String testDataDir = "${base}/files/merged-quality-assessment/"
//    String myBase = "${base}/${grailsApplication.config.otp.pbs.ssh.unixUser}"
    String myBase = "${base}/QualityAssessmentMerged"
    String rootPath = "${myBase}/rootPath/"
    String processingRootPath = "${myBase}/processingRootPath/"

    // files to be processed by the tests
//    String orgFileName = "merged-bam-file.mdup.bam"
    String orgFileName = "seq.bam.bam"
//    String orgFileName = "bigBam.bam"

    String baiOrgFileName = "${orgFileName}.bai"

    void setUp() {
        printlnlocal("setup")
        createData()
        printlnlocal("data created")
        String path = processedMergingFileService.directory(processedMergedBamFile.mergingPass)
        String fileName = processedMergedBamFileService.fileName(processedMergedBamFile)
        String filePath = processedMergedBamFileService.filePath(processedMergedBamFile)
        String baiFilePath = processedMergedBamFileService.filePathForBai(processedMergedBamFile)
        String softLinkFilepath = "${testDataDir}/${orgFileName}"
        String softLinkBaiFilepath = "${testDataDir}/${baiOrgFileName}"

        // Just to be sure the rootPath and the processingRootPath are clean for new test
        String cmdCleanUp = cleanUpTestFoldersCommand()
        String cmdBuildFileStructure = "mkdir -p ${path}"
        String cmdBuildSoftLinkToFileToBeProcessed = "ln -s ${softLinkFilepath} ${filePath}"
        String cmdBuildSoftLinkToBaiFileToBeProcessed = "ln -s ${softLinkBaiFilepath} ${baiFilePath}"
        executionService.executeCommand(realm, "${cmdCleanUp}; ${cmdBuildFileStructure}; ${cmdBuildSoftLinkToFileToBeProcessed}; ${cmdBuildSoftLinkToBaiFileToBeProcessed}")
        // TODO check if file structure was created..
        File file = new File(filePath)
        assertTrue(file.exists())
        assertTrue(file.canRead())
    }

    /**
     * Test execution of the workflow without any processing options defined
     */
    @Ignore
    void testExecutionWithProcessingOptions() {
        printlnlocal "start test"
        // there will be only one at the database
        JobExecutionPlan jobExecutionPlan = JobExecutionPlan.list()?.first()
        assertNotNull(jobExecutionPlan)
        // TODO hack to be able to start the workflow
        qualityAssessmentMergedStartJob.setJobExecutionPlan(jobExecutionPlan)

        printlnlocal "start waiting"
        boolean workflowFinishedSucessfully = waitUntilWorkflowIsOverOrTimeout(SLEEPING_TIME_IN_MINUTES)
        printlnlocal "finish waiting"
        assertTrue(workflowFinishedSucessfully)
    }

    // TODO maybe we can make this a sub class and put this method in parent..
    // TODO see if java or groovy already have a similar method..
    /**
     * Pauses the test until the workflow is finished or the timeout is reached
     * @return true if the process is finished, false otherwise
     */
    boolean waitUntilWorkflowIsOverOrTimeout(int timeout) {
        printlnlocal "Started to wait (until workflow is over or timeout)"
        int timeCount = 0
        boolean finished = false
        while (!finished && (timeCount < timeout)) {
            printlnlocal "waiting ... "
            timeCount++
            sleep(10000)
            finished = isProcessFinished()
        }
        return finished
    }

    // TODO maybe we can make this a sub class and put this method in parent..
    /**
     * Checks if the process created by the test is already finished and retrieves corresponding value
     * @return true if the process is finished, false otherwise
     */
    boolean isProcessFinished() {
        //TODO there should be not more than one .. can make assert to be sure
        List<Process> processes = Process.list()
        boolean finished = false
        if (processes.size() > 0) {
            Process process = processes.first()
            // Required otherwise will never detect the change..
            process.refresh()
            finished = process?.finished
        }
        return finished
    }

    void tearDown() {
        executionService.executeCommand(realm, cleanUpTestFoldersCommand())
    }

    /**
     * Helper to see logs at console ( besides seeing at the reports in the end)
     * @msg Message to be shown
     */
    void printlnlocal(String msg) {
        log.debug(msg)
        System.out.println(msg)
        new File("fileLocalLog.log") << msg << "\n"
    }

    /**
     * Returns a comand to clean up the rootPath and processingRootPath
     * @return Command to clean up used folders
     */
    String cleanUpTestFoldersCommand() {
        return "rm -rf ${rootPath}/* ${processingRootPath}/*"
    }

    private void createWorkflow() {
        plan("QualityAssessmentMergedWorkflow") {
            start("start", "qualityAssessmentMergedStartJob")
            job("createMergedQaOutputDirectory", "createMergedQaOutputDirectoryJob")
            job("executeMergedBamFileQaAnalysis", "executeMergedBamFileQaAnalysisJob") {
                outputParameter("__pbsIds")
                outputParameter("__pbsRealm")
            }
            job("executeMergedBamFileQaAnalysisWatchdog", "myPBSWatchdogJob") {
                inputParameter("__pbsIds", "executeMergedBamFileQaAnalysis", "__pbsIds")
                inputParameter("__pbsRealm", "executeMergedBamFileQaAnalysis", "__pbsRealm")
            }
            job("mergedQaOutputFileValidation", "mergedQaOutputFileValidationJob")
            job("parseMergedQaStatistics", "parseMergedQaStatisticsJob")
            job("createMergedChromosomeMappingFileJob", "createMergedChromosomeMappingFileJob")
            job("executeMergedMappingFilteringSortingToCoverageTable", "executeMergedMappingFilteringSortingToCoverageTableJob") {
                outputParameter("__pbsIds")
                outputParameter("__pbsRealm")
            }
            job("executeMergedMappingFilteringSortingToCoverageTableWatchdog", "myPBSWatchdogJob") {
                inputParameter("__pbsIds", "executeMergedMappingFilteringSortingToCoverageTable", "__pbsIds")
                inputParameter("__pbsRealm", "executeMergedMappingFilteringSortingToCoverageTable", "__pbsRealm")
            }
            job("mergedMappingFilteringSortingOutputFileValidation", "mergedMappingFilteringSortingOutputFileValidationJob")

            job("createMergedCoveragePlot", "createMergedCoveragePlotJob") {
                outputParameter("__pbsIds")
                outputParameter("__pbsRealm")
            }
            job("createMergedCoveragePlotWatchdog", "myPBSWatchdogJob") {
                inputParameter("__pbsIds", "createMergedCoveragePlot", "__pbsIds")
                inputParameter("__pbsRealm", "createMergedCoveragePlot", "__pbsRealm")
            }
            job("mergedCoveragePlotValidation", "mergedCoveragePlotValidationJob")
            job("createMergedInsertSizePlot", "createMergedInsertSizePlotJob") {
                outputParameter("__pbsIds")
                outputParameter("__pbsRealm")
            }
            job("createMergedInsertSizePlotWatchdog", "myPBSWatchdogJob") {
                inputParameter("__pbsIds", "createMergedInsertSizePlot", "__pbsIds")
                inputParameter("__pbsRealm", "createMergedInsertSizePlot", "__pbsRealm")
            }
            job("mergedInsertSizePlotValidation", "mergedInsertSizePlotValidationJob")
            job("assignMergedQaFlag", "assignMergedQaFlagJob")
        }
    }

    private void createOptions() {
        // create Quality Assessment jar options for executeBamFileQaAnalysis job
        boolean overrideOutput = false
        String allChromosomeName = Chromosomes.overallChromosomesLabel()
        int minAlignedRecordLength = 36
        int minMeanBaseQuality = 25
        int mappingQuality = 0
        int coverageMappingQualityThreshold = 1
        int windowsSize = 1000
        int insertSizeCountHistogramBin = 10
        boolean testMode = false

        String cmd = "qualityAssessment.sh \${processedBamFilePath} \${processedBaiFilePath} \${qualityAssessmentFilePath} \${coverageDataFilePath} \${insertSizeDataFilePath} ${overrideOutput} ${allChromosomeName} ${minAlignedRecordLength} ${minMeanBaseQuality} ${mappingQuality} ${coverageMappingQualityThreshold} ${windowsSize} ${insertSizeCountHistogramBin} ${testMode}"
        SeqType seqType = SeqType.findByNameAndLibraryLayout("WHOLE_GENOME", "PAIRED")
        assertNotNull(seqType)
        SpringSecurityUtils.doWithAuth("admin") {
            processingOptionService.createOrUpdate(
                            "qualityAssessment",
                            seqType.naturalId,
                            null,
                            cmd,
                            "Quality assessment command and parameters template")
        }
    }

    /*
     * Function to map the classification strings to enums to match
     * the ReferenceGenomeEntry object.
     * Default classification is UNDEFINED
     */
    Classification mapClassification(String classificationInput) {
        Classification classification = Classification.UNDEFINED
        if ( classificationInput.equals("CHROMOSOME")) {
            classification = Classification.CHROMOSOME
        }
        if ( classificationInput.equals("MITOCHONDRIAL")) {
            classification = Classification.MITOCHONDRIAL
        }
        if ( classificationInput.equals("CONTIG")) {
            classification = Classification.CONTIG
        }
        return classification
    }

    private void createReferenceGenome(Project project, SeqType seqType) {
        // get list of all standard chromosomes which is: 1..22, X, Y
        List<String> standardChromosomes = Chromosomes.allLabels()
        standardChromosomes.remove("M")
        assert standardChromosomes.size() == 24


        ReferenceGenome refGen = new ReferenceGenome()
        refGen.name = "hg19"
        refGen.path = "bwa06_hg19_chr"
        refGen.fileNamePrefix = "hg19_1-22_X_Y_M"
        if (refGen.validate()) {
            refGen.save(flush: true)
            println("Inserted reference genome: " + refGen)
        }
        else {
            refGen.errors.allErrors.each {
                println it
            }
        }

        // list which holds information about all entries in the ref. genome fasta file
        Map<String, Integer> fastaEntriesColumn = ["name":0, "alias":1, "length":2, "lengthWithoutN":3, "classification":4]
        List<List<String, Long, Long>> fastaEntries = [
            ["chr1", "1", 249250621, 225280621, "CHROMOSOME"],
            ["chr10", "10", 135534747, 131314742, "CHROMOSOME"],
            ["chr11", "11", 135006516, 131129516, "CHROMOSOME"],
            ["chr12", "12", 133851895, 130481394, "CHROMOSOME"],
            ["chr13", "13", 115169878, 95589878, "CHROMOSOME"],
            ["chr14", "14", 107349540, 88289540, "CHROMOSOME"],
            ["chr15", "15", 102531392, 81694769, "CHROMOSOME"],
            ["chr16", "16", 90354753, 78884753, "CHROMOSOME"],
            ["chr17", "17", 81195210, 77795210, "CHROMOSOME"],
            ["chr18", "18", 78077248, 74657233, "CHROMOSOME"],
            ["chr19", "19", 59128983, 55808983, "CHROMOSOME"],
            ["chr2", "2", 243199373, 238204522, "CHROMOSOME"],
            ["chr20", "20", 63025520, 59505520, "CHROMOSOME"],
            ["chr21", "21", 48129895, 35106692, "CHROMOSOME"],
            ["chr22", "22", 51304566, 34894562, "CHROMOSOME"],
            ["chr3", "3", 198022430, 194797136, "CHROMOSOME"],
            ["chr4", "4", 191154276, 187661676, "CHROMOSOME"],
            ["chr5", "5", 180915260, 177695260, "CHROMOSOME"],
            ["chr6", "6", 171115067, 167395067, "CHROMOSOME"],
            ["chr7", "7", 159138663, 155353663, "CHROMOSOME"],
            ["chr8", "8", 146364022, 142888922, "CHROMOSOME"],
            ["chr9", "9", 141213431, 120143431, "CHROMOSOME"],
            ["chrM", "M", 16571, 16571, "MITOCHONDRIAL"],
            ["chrX", "X", 155270560, 151100560, "CHROMOSOME"],
            ["chrY", "Y", 59373566, 25653566, "CHROMOSOME"]
        ]

        // init counter for overall length values
        long length = 0
        long lengthWithoutN = 0
        long lengthRefChromosomes = 0
        long lengthRefChromosomesWithoutN = 0

        // put fastaEntry as ReferenceGenomeEntry information into database
        fastaEntries.each { entry ->
            ReferenceGenomeEntry refGenEntry = new ReferenceGenomeEntry()
            refGenEntry.name = entry[fastaEntriesColumn.name]
            refGenEntry.alias = entry[fastaEntriesColumn.alias]
            refGenEntry.length = entry[fastaEntriesColumn.length]
            refGenEntry.lengthWithoutN = entry[fastaEntriesColumn.lengthWithoutN]
            refGenEntry.classification = mapClassification(entry[fastaEntriesColumn.classification])
            refGenEntry.referenceGenome = refGen
            if (refGenEntry.validate()) {
                refGenEntry.save(flush: true)
                println "Inserted ReferenceGenomeEntry: " + refGenEntry
            }
            else {
                refGenEntry.errors.allErrors.each {
                    println it
                }
            }
            // overall counting
            length += entry[fastaEntriesColumn.length]
            lengthWithoutN += entry[fastaEntriesColumn.lengthWithoutN]
            // counting if entry is a standardChromosome
            if ( standardChromosomes.contains(refGenEntry.alias) ) {
                lengthRefChromosomes += refGenEntry.length
                lengthRefChromosomesWithoutN += refGenEntry.lengthWithoutN
            }
        }

        // put length values into database
        refGen.length = length
        refGen.lengthWithoutN = lengthWithoutN
        refGen.lengthRefChromosomes = lengthRefChromosomes
        refGen.lengthRefChromosomesWithoutN = lengthRefChromosomesWithoutN
        refGen.save(flush: true)

        ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = new ReferenceGenomeProjectSeqType()

        referenceGenomeProjectSeqType.project = project
        referenceGenomeProjectSeqType.seqType = seqType
        referenceGenomeProjectSeqType.referenceGenome = refGen
        referenceGenomeProjectSeqType.save(flush: true)
    }

    private void createData() {
        super.createUserAndRoles()
        // Realm
        String realmName = "DKFZ"
        String realmBioquantUnixUser = "$USER"
        String realmDKFZUnixUser = "$USER"

        String realmProgramsRootPath = "/"
        String realmHost = "headnode"
        int realmPort = 22
        String realmWebHost = "https://otp.local/ngsdata/"
        String realmPbsOptions = '{"-l": {nodes: "1:lsdf", walltime: "48:00:00"}}'
        int realmTimeout = 0

        Realm.OperationType.values().each { Realm.OperationType operationType ->
            realm = new Realm(
                            cluster : Realm.Cluster.DKFZ,
                            rootPath : rootPath,
                            processingRootPath : processingRootPath,
                            programsRootPath : realmProgramsRootPath,
                            webHost : realmWebHost,
                            host : realmHost,
                            port : realmPort,
//                            unixUser : grailsApplication.config.otp.pbs.ssh.unixUser,
                            unixUser : realmDKFZUnixUser,
                            timeout : realmTimeout,
                            pbsOptions : realmPbsOptions,
                            name : "realmName",
                            operationType : operationType,
                            env : Environment.getCurrent().getName()
                            )
            realm.save([flush: true])
        }

        Project project = new Project(
                        name: "project",
                        dirName: "project-dir",
                        realmName: "realmName"
                        )
        assertNotNull(project.save([flush: true]))

        Individual individual = new Individual(
                        pid: "patient",
                        mockPid: "mockPid",
                        mockFullName: "mockFullName",
                        type: Individual.Type.UNDEFINED,
                        project: project
                        )
        assertNotNull(individual.save([flush: true]))

        SampleType sampleType = new SampleType(
                        name: "sample-type"
                        )
        assertNotNull(sampleType.save([flush: true]))

        Sample sample = new Sample(
                        individual: individual,
                        sampleType: sampleType
                        )
        assertNotNull(sample.save([flush: true]))

        SeqType seqType = new SeqType(
                        name: "WHOLE_GENOME",
                        libraryLayout: "PAIRED",
                        dirName: "seq-type-dir"
                        )
        assertNotNull(seqType.save([flush: true]))

        MergingWorkPackage mergingWorkPackage = new MergingWorkPackage(
                        sample: sample,
                        seqType: seqType
                        )
        assertNotNull(mergingWorkPackage.save([flush: true]))

        MergingSet mergingSet = new MergingSet(
                        identifier: 0,
                        mergingWorkPackage: mergingWorkPackage,
                        status: MergingSet.State.PROCESSED
                        )
        assertNotNull(mergingSet.save([flush: true]))

        MergingPass mergingPass = new MergingPass(
                        identifier: 0,
                        mergingSet: mergingSet
                        )
        assertNotNull(mergingPass.save([flush: true]))

        processedMergedBamFile = new ProcessedMergedBamFile(
                        mergingPass: mergingPass,
                        fileExists: true,
                        type: AbstractBamFile.BamType.MDUP,
                        status: AbstractBamFile.State.PROCESSED,
                        qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.NOT_STARTED
                        )
        assertNotNull(processedMergedBamFile.save([flush: true]))

        createWorkflow()
        createOptions()
        createReferenceGenome(project, seqType)
    }
}
