package workflows

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.ExecuteRoddyCommandService
import de.dkfz.tbi.otp.utils.ProcessHelperService
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import grails.plugin.springsecurity.SpringSecurityUtils
import org.codehaus.groovy.grails.web.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import grails.converters.JSON

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement
import de.dkfz.tbi.otp.utils.LinkFileUtils
import org.joda.time.Duration

// TODO: change owner of input files to otptest

// all the tmp test directories are created with otptest user and will have localGroup as group
// (base dir is crated with SetGID);
// roddy.sh will be called with OtherUnixUser user, which has very restricted set of rights.
// it is assumed then that OtherUnixUser is also in localGroup group to be able to read
// tmp test data and write into roddyOutput dir created by workflow.

abstract class AbstractPanCanAlignmentWorkflowTests extends WorkflowTestCase {

    final static String REG_GEN_FILE_NAME_PREFIX = 'hs37d5'
    protected final static String CHROMOSOME_STAT_FILE_NAME = 'hs37d5.fa.chrLenOnlyACGT_realChromosomes.tab'

    // some text to be used to fill in files created on the fly
    protected final static String TEST_CONTENT = 'DummyContent'

    // files expected to be in every roddyExecutionDir
    final List<String> filesInRoddyExecutionDir = [
            'applicationProperties.ini',
            'executedJobs.txt',
            'jobStateLogfile.txt',
            'realJobCalls.txt',
            'repeatableJobCalls.sh',
            'roddyCall.sh',
            'runtimeConfig.sh',
            'runtimeConfig.xml',
            'versionsInfo.txt',
    ].asImmutable()

    // directory with source test data (e.g. configs, fastq)
    File baseTestDataDir

    // test fastq files grouped by lane
    Map testFastqFiles

    // test bam file used for further merging with new lanes
    File firstBamFile

    // directory with test reference genome files
    File refGenDir

    // file with name of chromosomes {@link ReferenceGenomeEntry#Classification#CHROMOSOME}
    // for reference genome in {@link #refGenDir}
    File chromosomeNamesFile

    // test project config (binary aligner)
    File projectConfigFile

    // test project config (convey aligner)
    File conveyProjectConfigFile

    // test project config with non-existing plugin
    File roddyFailsProjectConfig


    LsdfFilesService lsdfFilesService
    ReferenceGenomeService referenceGenomeService
    LinkFileUtils linkFileUtils
    ProcessingOptionService processingOptionService
    ExecuteRoddyCommandService executeRoddyCommandService
    AbstractBamFileService abstractBamFileService


    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()


    @Override
    List<String> getWorkflowScripts() {
        return ["scripts/workflows/PanCanWorkflow.groovy",
                "scripts/initializations/AddPathToConfigFilesToProcessingOptions.groovy",
                "scripts/initializations/AddRoddyPathAndVersionToProcessingOptions.groovy"
        ]
    }

    @Override
    Duration getTimeout() {
        return Duration.standardHours(3)
    }

    @Before
    void setUp() {
        String group = grailsApplication.config.otp.testing.group
        assert group: '"otp.testing.group" is not set in your "otp.properties". Please add it with an existing secondary group.'
        executionHelperService.setGroup(realm, realm.rootPath as File, group)

        setUpFilesVariables()

        MergingWorkPackage workPackage = createWorkPackage()

        setUpRefGenomeDir(workPackage)

        createProjectConfig(workPackage)


        BedFile bedFile = new BedFile(
                referenceGenome: workPackage.referenceGenome,
                libraryPreparationKit: workPackage.libraryPreparationKit,
                fileName: "TruSeqExomeTargetedRegions_plain.bed",
                targetSize: 62085295,
                mergedTargetSize: 62085286,
        )
        assert bedFile.save(flush: true, failOnError: true)

        //OtherUnixUser user has very limited permissions, it must be checked in the tests
        //that roddy has enough permissions to work and does not try to damage other files
        setPermissionsRecursive(baseDirectory, TEST_DATA_MODE_DIR, TEST_DATA_MODE_FILE)

        SpringSecurityUtils.doWithAuth("operator") {
            processingOptionService.createOrUpdate("basesPerBytesFastQ", null, null, "2.339", "Bases Per Byte for FastQ file used to calculate count of bases before FastQC-WG is finished")
        }
    }

    @After
    void changeFilePermissionForRoddyFiles() {
        String cmd = "${executeRoddyCommandService.executeCommandAsRoddyUser()} bashScripts/OtherUnixUser/changePermissionForWorkflowTestCleanup.sh ${getBaseDirectory()}"
        LogThreadLocal.withThreadLog(System.out) {
            ProcessHelperService.ProcessOutput processOutput = ProcessHelperService.waitForCommand(cmd)
            assert processOutput.stderr.empty && processOutput.exitCode == 0
        }
    }

    void setUpFilesVariables() {
        baseTestDataDir = new File(rootDirectory, 'PanCanAlignmentSetupFiles')
        testFastqFiles = [
                readGroup1: [
                        new File(baseTestDataDir, 'fastq-files/run150319_D00133_0107_BC5YE7ACXX/sequence/D2826_GATCAGA_L002_R1_001.fastq.gz'),
                        new File(baseTestDataDir, 'fastq-files/run150319_D00133_0107_BC5YE7ACXX/sequence/D2826_GATCAGA_L002_R2_001.fastq.gz'),
                ].asImmutable(),
                readGroup2: [
                        new File(baseTestDataDir, 'fastq-files/run150326_D00695_0025_BC6B2MACXX/sequence/D2826_GATCAGA_L002_R1_001.fastq.gz'),
                        new File(baseTestDataDir, 'fastq-files/run150326_D00695_0025_BC6B2MACXX/sequence/D2826_GATCAGA_L002_R2_001.fastq.gz'),
                ].asImmutable(),
        ].asImmutable()
        firstBamFile = new File(baseTestDataDir, 'first-bam-file/first-bam-file_merged.mdup.bam')
        refGenDir = new File(baseTestDataDir, 'reference-genomes/bwa06_1KGRef')
        chromosomeNamesFile = new File(baseTestDataDir, 'reference-genomes/chromosome-names.txt')
        projectConfigFile = new File(baseTestDataDir, 'project-config/projectTestAlignment.xml')
        conveyProjectConfigFile = new File(baseTestDataDir, 'project-config/conveyProjectTestAlignment.xml')
        roddyFailsProjectConfig = new File(baseTestDataDir, 'project-config/roddy-fails-project-config.xml')
    }

    abstract SeqType findSeqType()

    MergingWorkPackage createWorkPackage() {
        DomainFactory.createAlignableSeqTypes()
        SeqType seqType = findSeqType()

        Workflow workflow = Workflow.build(
                name: Workflow.Name.PANCAN_ALIGNMENT,
                type: Workflow.Type.ALIGNMENT,
        )

        ReferenceGenome referenceGenome = ReferenceGenome.build(
                fileNamePrefix: REG_GEN_FILE_NAME_PREFIX,
        )
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(chromosomeNamesFile)
        chromosomeNamesFile.eachLine { String chromosomeName ->
            ReferenceGenomeEntry.build(
                    referenceGenome: referenceGenome,
                    classification: ReferenceGenomeEntry.Classification.CHROMOSOME,
                    name: chromosomeName,
                    alias: chromosomeName,
            )
        }

        LibraryPreparationKit kit = new LibraryPreparationKit(
                name: "~* xX liBrArYprEPaRaTioNkiT Xx *~",
                shortDisplayName: "~* xX lPk Xx *~",
        )
        assert kit.save(flush: true, failOnError: true)

        MergingWorkPackage workPackage = MergingWorkPackage.build(
                workflow: workflow,
                seqType: seqType,
                referenceGenome: referenceGenome,
                needsProcessing: false,
                statSizeFileName: CHROMOSOME_STAT_FILE_NAME,
                libraryPreparationKit: kit,
        )

        workPackage.sampleType.name = "CONTROL"
        workPackage.sampleType.save(flush: true)

        workPackage.project.realmName = realm.name
        workPackage.project.save(flush: true)

        return workPackage
    }

    void createProjectConfig(MergingWorkPackage workPackage) {
        assert projectConfigFile.exists()

        //The config file needs to be accessible by the OtherUnixUser user on the local host
        //The workflow directories are mounted via sshfs, they can only be accessed by one user
        File configFile = new File(temporaryFolder.newFolder(projectConfigFile.parentFile.name), projectConfigFile.name)
        configFile.text = projectConfigFile.text

        String pluginVersion = getPluginVersion(projectConfigFile)

        RoddyWorkflowConfig.build(
                configFilePath: configFile.absolutePath,
                workflow: workPackage.workflow,
                pluginVersion: pluginVersion,
                configVersion: DomainFactory.TEST_CONFIG_VERSION,
                project: workPackage.project,
                obsoleteDate: null
        )
        //ensure that expected identifier is available
        assert configFile.text.contains("${workPackage.workflow.name}_${pluginVersion}_${DomainFactory.TEST_CONFIG_VERSION}")
    }

    SeqTrack createSeqTrack(String readGroupNum) {
        MergingWorkPackage workPackage = exactlyOneElement(MergingWorkPackage.findAll())

        SeqTrack seqTrack = DomainFactory.buildSeqTrackWithDataFile(workPackage, [laneId: readGroupNum])

        DataFile.findAllBySeqTrack(seqTrack).eachWithIndex { DataFile dataFile, int index ->
            dataFile.vbpFileName = dataFile.fileName = "fastq_${seqTrack.individual.pid}_${seqTrack.sampleType.name}_${seqTrack.laneId}_${index + 1}.fastq.gz"
            dataFile.save(flush: true)
        }

        RunSegment.build(
                run: seqTrack.run,
                filesStatus: RunSegment.FilesStatus.FILES_CORRECT,
        )

        linkFastqFiles(seqTrack, readGroupNum)

        workPackage.needsProcessing = true
        workPackage.save(flush: true)
        return seqTrack
    }



    List<File> createFileListForFirstBam(RoddyBamFile firstBamFile, String finalOrWork) {
        Map<SeqTrack, File> singleLaneQa = firstBamFile."${finalOrWork}SingleLaneQAJsonFiles"
        List<File> baseAndQaJsonFiles = [
                firstBamFile."${finalOrWork}BaiFile",
                firstBamFile."${finalOrWork}Md5sumFile",
                firstBamFile."${finalOrWork}MergedQAJsonFile",
                singleLaneQa.values(),
        ].flatten()

        File roddyExecutionDirectory = new File(firstBamFile."${finalOrWork}ExecutionStoreDirectory", firstBamFile.roddyExecutionDirectoryNames.first())
        List<File> executionStorageFiles =  filesInRoddyExecutionDir.collect {
            new File(roddyExecutionDirectory, it)
        }

        return [baseAndQaJsonFiles, executionStorageFiles].flatten()
    }


    RoddyBamFile createFirstRoddyBamFile(boolean oldStructure = false) {
        assert firstBamFile.exists()

        MergingWorkPackage workPackage = exactlyOneElement(MergingWorkPackage.findAll())

        SeqTrack seqtrack = createSeqTrack("readGroup1")
        RoddyBamFile firstBamFile = new RoddyBamFile(
                workPackage: workPackage,
                identifier: RoddyBamFile.nextIdentifier(workPackage),
                seqTracks: [seqtrack] as Set,
                config: exactlyOneElement(RoddyWorkflowConfig.findAll()),
                roddyVersion: exactlyOneElement(ProcessingOption.findAllByName("roddyVersion")),
                numberOfMergedLanes: 1,
                fileOperationStatus: FileOperationStatus.PROCESSED,
                md5sum: calculateMd5Sum(firstBamFile),
                fileSize: firstBamFile.size(),
                fileExists: true,
                dateFromFileSystem: new Date(firstBamFile.lastModified()),
                roddyExecutionDirectoryNames: ['exec_123456_123456789_bla_bla']
        )
        if (!oldStructure) {
            firstBamFile.workDirectoryName = "${RoddyBamFile.WORK_DIR_PREFIX}_0"
        }
        assert firstBamFile.save(flush: true, failOnError: true)

        linkFileUtils.createAndValidateLinks([(this.firstBamFile): firstBamFile.finalBamFile], realm)

        Map<File, String> filesWithContent = createFileListForFirstBam(firstBamFile, 'final').collectEntries {
            [(it): TEST_CONTENT]
        }

        if (!oldStructure) {
            filesWithContent << createFileListForFirstBam(firstBamFile, 'work').collectEntries {
                [(it): TEST_CONTENT]
            }
            linkFileUtils.createAndValidateLinks([(this.firstBamFile): firstBamFile.workBamFile], realm)
        }

        createFilesWithContent(filesWithContent)

        workPackage.bamFileInProjectFolder = firstBamFile
        workPackage.needsProcessing = false
        assert workPackage.save(flush: true, failOnError: true)

        return firstBamFile
    }

    void linkFastqFiles(SeqTrack seqTrack, String readGroupNum) {
        List<DataFile> dataFiles = DataFile.findAllBySeqTrack(seqTrack)
        assert 2 == dataFiles.size()

        Map sourceLinkMap = [:]
        dataFiles.eachWithIndex { dataFile, index ->
            File sourceFastqFile = testFastqFiles."${readGroupNum}"[index]
            assert sourceFastqFile.exists()
            dataFile.fileSize = sourceFastqFile.length()
            File linkFastqFile = new File(lsdfFilesService.getFileFinalPath(dataFile))
            sourceLinkMap.put(sourceFastqFile, linkFastqFile)
            File linkViewByPidFastqFile = new File(lsdfFilesService.getFileViewByPidPath(dataFile))
            sourceLinkMap.put(linkFastqFile, linkViewByPidFastqFile)
        }
        createDirectories(sourceLinkMap.values()*.parentFile.unique(), TEST_DATA_MODE_DIR)
        linkFileUtils.createAndValidateLinks(sourceLinkMap, realm)
    }

    void setUpRefGenomeDir(MergingWorkPackage workPackage) {
        File linkRefGenDir = new File(referenceGenomeService.filePathToDirectory(workPackage.project, workPackage.referenceGenome, false))
        File linkStatDir = referenceGenomeService.pathToChromosomeSizeFilesPerReference(workPackage.project, workPackage.referenceGenome, false)
        createDirectories([linkRefGenDir, linkStatDir], TEST_DATA_MODE_DIR)

        File sourceStatDir = new File(refGenDir, ReferenceGenomeService.CHROMOSOME_SIZE_FILES_PREFIX)
        assert refGenDir.exists()
        assert sourceStatDir.exists()

        // the stat dir is not linked, since one needs to create a broken stat file in this dir
        // in one of the tests
        executionService.executeCommand(realm, "cp ${sourceStatDir}/* ${linkStatDir}")

        refGenDir.listFiles().each { sourceFile ->
            File linkFile = new File(linkRefGenDir, sourceFile.name)
            if (sourceFile == sourceStatDir) {
                return
            }
            linkFileUtils.createAndValidateLinks([(sourceFile): linkFile], realm)
        }
    }

    void checkAllAfterSuccessfulExecution_alignBaseBamAndNewLanes() {
        checkDataBaseState_alignBaseBamAndNewLanes()
        RoddyBamFile latestBamFile = RoddyBamFile.findByIdentifier(1L)
        checkFileSystemState(latestBamFile)

        checkQC(latestBamFile)
    }

    void checkQC(RoddyBamFile bamFile) {

        QualityAssessmentMergedPass qaPass = QualityAssessmentMergedPass.findWhere(
                abstractMergedBamFile: bamFile,
                identifier: 0,
        )
        assert qaPass

        bamFile.seqTracks.each {
            List<RoddySingleLaneQa> qa = RoddySingleLaneQa.findAllBySeqTrack(it)
            assert qa
            qa.each {
                assert it.qualityAssessmentMergedPass == qaPass
            }
        }

        bamFile.getFinalSingleLaneQAJsonFiles().each { seqTrack, qaFile ->
            JSONObject json = JSON.parse(qaFile.text)
            Iterator chromosomes = json.keys()
            chromosomes.each { String chromosome ->
                assert RoddySingleLaneQa.findByChromosomeAndSeqTrack(chromosome, seqTrack)
            }
        }

        RoddyMergedBamQa mergedQa = RoddyMergedBamQa.findByQualityAssessmentMergedPass(qaPass)
        assert mergedQa

        JSONObject json = JSON.parse(bamFile.getFinalMergedQAJsonFile().text)
        json.keys().each { String chromosome ->
            assert RoddyMergedBamQa.findByChromosomeAndQualityAssessmentMergedPass(chromosome, qaPass)
        }

        assert bamFile.coverage == mergedQa.genomeWithoutNCoverageQcBases
        assert bamFile.coverageWithN == abstractBamFileService.calculateCoverageWithN(bamFile)

        assert bamFile.qualityAssessmentStatus == AbstractBamFile.QaProcessingStatus.FINISHED
    }

    void checkAllAfterRoddyPbsJobsRestartAndSuccessfulExecution_alignBaseBamAndNewLanes() {
        checkDataBaseState_alignBaseBamAndNewLanes()
        RoddyBamFile latestBamFile = RoddyBamFile.findByIdentifier(1L)
        checkFileSystemState(latestBamFile, 1)
    }

    void checkDataBaseState_alignBaseBamAndNewLanes() {
        checkWorkPackageState()

        assert 2 == RoddyBamFile.findAll().size()
        RoddyBamFile firstBamFile = RoddyBamFile.findByIdentifier(0L)
        RoddyBamFile latestBamFile = RoddyBamFile.findByIdentifier(1L)

        checkFirstBamFileState(firstBamFile, false)
        assertBamFileFileSystemPropertiesSet(firstBamFile)

        checkLatestBamFileState(latestBamFile, firstBamFile)
        assertBamFileFileSystemPropertiesSet(latestBamFile)
    }

    void checkFirstBamFileState(RoddyBamFile bamFile, boolean isMostResentBamFile, Map bamFileProperties = [:]) {
        SeqTrack seqTrack = SeqTrack.findByLaneId("readGroup1")
        checkBamFileState(bamFile, [
                identifier         : 0L,
                mostResentBamFile  : isMostResentBamFile,
                baseBamFile        : null,
                seqTracks          : [seqTrack],
                containedSeqTracks : [seqTrack],
                fileOperationStatus: FileOperationStatus.PROCESSED,
                withdrawn          : false
        ] + bamFileProperties)
    }

    void checkLatestBamFileState(RoddyBamFile latestBamFile, RoddyBamFile firstbamFile, Map latestBamFileProperties = [:]) {
        SeqTrack firstSeqTrack = SeqTrack.findByLaneId("readGroup1")
        SeqTrack secondSeqTrack = SeqTrack.findByLaneId("readGroup2")
        checkBamFileState(latestBamFile, [
                identifier         : 1L,
                mostResentBamFile  : true,
                baseBamFile        : firstbamFile,
                seqTracks          : [secondSeqTrack],
                containedSeqTracks : [firstSeqTrack, secondSeqTrack],
                fileOperationStatus: FileOperationStatus.PROCESSED,
                withdrawn          : false
        ] + latestBamFileProperties)
    }

    void assertBamFileFileSystemPropertiesSet(RoddyBamFile bamFile) {
        assert bamFile.md5sum =~ /^[a-f0-9]{32}$/
        assert bamFile.fileExists
        assert null != bamFile.dateFromFileSystem
        assert bamFile.fileSize > 0
    }

    void assertBamFileFileSystemPropertiesNotSet(RoddyBamFile bamFile) {
        assert null == bamFile.md5sum
        assert !bamFile.fileExists
        assert null == bamFile.dateFromFileSystem
        assert -1 == bamFile.fileSize
    }

    void checkBamFileState(RoddyBamFile bamFile, Map bamFileProperties) {
        MergingWorkPackage workPackage = exactlyOneElement(MergingWorkPackage.findAll())
        RoddyWorkflowConfig projectConfig = exactlyOneElement(RoddyWorkflowConfig.findAll())
        ProcessingOption roddyVersionOption = exactlyOneElement(ProcessingOption.findAllByName("roddyVersion"))

        assert bamFileProperties.baseBamFile?.id == bamFile.baseBamFile?.id
        assert bamFileProperties.seqTracks.size() == bamFile.seqTracks.size()
        assert bamFileProperties.seqTracks*.id.containsAll(bamFile.seqTracks*.id)
        assert bamFileProperties.containedSeqTracks.size() == bamFile.containedSeqTracks.size()
        assert bamFileProperties.containedSeqTracks*.id.containsAll(bamFile.containedSeqTracks*.id)
        assert bamFileProperties.containedSeqTracks.size() == bamFile.numberOfMergedLanes

        assert workPackage.id == bamFile.workPackage.id
        assert projectConfig.id == bamFile.config.id
        assert roddyVersionOption.id == bamFile.roddyVersion.id
        assert bamFileProperties.fileOperationStatus == bamFile.fileOperationStatus
        assert bamFileProperties.withdrawn == bamFile.withdrawn

        assert bamFileProperties.identifier == bamFile.identifier
        assert bamFileProperties.mostResentBamFile == bamFile.isMostRecentBamFile()
    }

    void checkWorkPackageState() {
        MergingWorkPackage workPackage = exactlyOneElement(MergingWorkPackage.findAll())
        workPackage.refresh()
        assert false == workPackage.needsProcessing
    }

    void checkFileSystemState(RoddyBamFile bamFile, Integer numberOfWorkflowRestarts = 0) {

        //content of the final dir: root
        List<File> rootDirs = [
                bamFile.finalQADirectory,
                bamFile.finalExecutionStoreDirectory,
                bamFile.workDirectory,
        ]

        List<File> rootFileLinks = [
                bamFile.finalBamFile,
                bamFile.finalBaiFile,
                bamFile.finalMd5sumFile,
        ]
        if (bamFile.baseBamFile && !bamFile.baseBamFile.isOldStructureUsed()) {
            rootDirs << bamFile.baseBamFile.workDirectory
        }
        TestCase.checkDirectoryContentHelper(bamFile.baseDirectory, rootDirs, [], rootFileLinks)

        //check work directories
        checkWorkDirFileSystemState(bamFile)
        if (bamFile.baseBamFile && !bamFile.baseBamFile.isOldStructureUsed()) {
            checkWorkDirFileSystemState(bamFile.baseBamFile, true)
        }

        //check md5sum content
        assert bamFile.md5sum == bamFile.finalMd5sumFile.text.replaceAll("\n", "")

        // content of the final dir: qa
        List<File> qaDirs = [
                bamFile.finalSingleLaneQADirectories.values(),
                bamFile.finalMergedQADirectory,
        ]
        if (bamFile.baseBamFile) {
            qaDirs << bamFile.baseBamFile.finalSingleLaneQADirectories.values()
        }
        TestCase.checkDirectoryContentHelper(bamFile.finalQADirectory, [], [], qaDirs.flatten())

        // qa only for merged and one for each read group
        assert bamFile.numberOfMergedLanes + 1 == bamFile.finalQADirectory.list().length

        // all roddyExecutionDirs have been linked
        List<File> expectedRoddyExecutionDirs = bamFile.finalExecutionDirectories
        if (bamFile.baseBamFile) {
            expectedRoddyExecutionDirs += bamFile.baseBamFile.finalExecutionDirectories
        }
        TestCase.checkDirectoryContentHelper(bamFile.finalExecutionStoreDirectory, expectedRoddyExecutionDirs)

        // content of the bam file
        LogThreadLocal.withThreadLog(System.out) {
            ProcessHelperService.ProcessOutput processOutput = ProcessHelperService.executeCommandAndAssertExistCodeAndReturnProcessOutput("zcat ${bamFile.finalBamFile} 1> /dev/null")
            assert processOutput.stderr.length() == 0: "Stderr is not null, but ${processOutput.stderr.toString()}"
            assert bamFile.finalBamFile.length() == bamFile.fileSize
        }
        // samtools may under some circumstances produce small bam files of size larger than zero that however do not contain any reads.
        bamFile.finalBamFile.length() > 1024L

        checkInputIsNotDeleted()
    }

    void checkWorkDirFileSystemState(RoddyBamFile bamFile, boolean isBaseBamFile = false) {
        // content of the work dir: root
        List<File> rootDir = [
                bamFile.workQADirectory,
                bamFile.workExecutionStoreDirectory,
        ]
        List<File> rootFiles
        if (isBaseBamFile) {
            rootFiles = [
                    bamFile.workMd5sumFile,
            ]
        } else {
            rootFiles = [
                    bamFile.workBamFile,
                    bamFile.workBaiFile,
                    bamFile.workMd5sumFile,
            ]
        }
        TestCase.checkDirectoryContentHelper(bamFile.workDirectory, rootDir, rootFiles)

        // content of the work dir: qa
        List<File> qaDirs = bamFile.workSingleLaneQADirectories.values() as List
        List<File> qaJson = bamFile.workSingleLaneQAJsonFiles.values() as List
        qaDirs << bamFile.workMergedQADirectory
        qaJson << bamFile.workMergedQAJsonFile
        TestCase.checkDirectoryContentHelper(bamFile.workQADirectory, qaDirs)
        qaJson.each {
            assert it.exists() && it.isFile() && it.canRead() && it.size() > 0
            JSON.parse(it.text) // throws ConverterException when the JSON content is not valid
        }

        //  content of the work dir: executionStoreDirectory
        TestCase.checkDirectoryContentHelper(bamFile.workExecutionStoreDirectory, bamFile.workExecutionDirectories)

        //check that given files exist in the execution store:
        bamFile.workExecutionDirectories.each { executionStore ->
            filesInRoddyExecutionDir.each { String fileName ->
                lsdfFilesService.ensureFileIsReadableAndNotEmpty(new File(executionStore.absolutePath, fileName))
            }
        }
    }



    void checkInputIsNotDeleted() {
        List<DataFile> fastqFiles = DataFile.findAll()
        fastqFiles.each { DataFile dataFile ->
            lsdfFilesService.ensureFileIsReadableAndNotEmpty(lsdfFilesService.getFileFinalPath(dataFile) as File)
            lsdfFilesService.ensureFileIsReadableAndNotEmpty(lsdfFilesService.getFileViewByPidPath(dataFile) as File)
        }
    }

    String getPluginVersion(File projectConfig) {
        def configuration = new XmlParser().parseText(projectConfig.text)
        String nameValue = configuration.@name
        return nameValue.replaceAll("${Workflow.Name.PANCAN_ALIGNMENT}_", "").replaceAll("_${DomainFactory.TEST_CONFIG_VERSION}", '')
    }

    void setPluginVersion(String pluginVersion) {
        RoddyWorkflowConfig config = CollectionUtils.exactlyOneElement(RoddyWorkflowConfig.findAll())
        config.pluginVersion = pluginVersion
        config.save(flush: true, failOnError: true)
    }

    void resetProjectConfig(File sourceProjectConfig) {
        assert sourceProjectConfig.exists()

        //The config file needs to be accessible by the OtherUnixUser user on the local host
        //The workflow directories are mounted via sshfs, they can only be accessed by one user
        File configFile = new File(temporaryFolder.newFolder(sourceProjectConfig.name), sourceProjectConfig.name)
        configFile.text = sourceProjectConfig.text

        RoddyWorkflowConfig config = exactlyOneElement(RoddyWorkflowConfig.findAll())
        config.configFilePath = configFile.absolutePath
        config.save(flush: true, failOnError: true)
        setPluginVersion(getPluginVersion(configFile))
    }
}
