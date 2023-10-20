/*
 * Copyright 2011-2023 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.alignment.roddy

import grails.converters.JSON
import org.grails.web.json.JSONObject

import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.alignment.AbstractAlignmentWorkflowTest
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.PanCanAlignmentConfiguration
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import de.dkfz.tbi.otp.workflowTest.FileAssertHelper
import de.dkfz.tbi.otp.workflowTest.roddy.RoddyReferences

import java.time.Duration

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement
import static de.dkfz.tbi.otp.utils.LocalShellHelper.executeAndWait

abstract class AbstractRoddyAlignmentWorkflowTests extends AbstractAlignmentWorkflowTest implements RoddyReferences {

    // The number of reads of the example fastqc files
    protected static final int NUMBER_OF_READS = 1000

    protected String getRefGenFileNamePrefix() {
        return 'hs37d5'
    }

    protected String getReferenceGenomeSpecificPath() {
        return 'bwa06_1KGRef'
    }

    protected String getChromosomeStatFileName() {
        return 'hs37d5.fa.chrLenOnlyACGT_realChromosomes.tab'
    }

    protected String getCytosinePositionsIndex() {
        return null
    }

    // some text to be used to fill in files created on the fly
    protected final static String TEST_CONTENT = 'dummy file, created by OTP'

    // test fastq files grouped by lane
    Map<String, List<File>> testFastqFiles

    // test bam file used for further merging with new lanes
    File firstBamFile

    // directory with test reference genome files
    File refGenDir

    // file with name of chromosomes {@link ReferenceGenomeEntry#Classification#CHROMOSOME}
    // for reference genome in {@link #refGenDir}
    File chromosomeNamesFile

    File fingerPrintingFile

    AbstractBamFileService abstractBamFileService

    ProjectService projectService

    FileAssertHelper fileAssertHelper

    @Override
    List<String> getWorkflowScripts() {
        return [
                "scripts/workflows/PanCanWorkflow.groovy",
        ]
    }

    @Override
    Duration getTimeout() {
        return Duration.ofHours(5)
    }

    @Override
    void setup() {
        SessionUtils.withTransaction {
            String group = configService.testingGroup
            executionHelperService.setGroup(realm, configService.rootPath as File, group)

            setUpFilesVariables()

            MergingWorkPackage workPackage = createWorkPackage()

            setUpRefGenomeDir(workPackage, refGenDir)

            createProjectConfig(workPackage)

            if (SeqTypeService.exomePairedSeqType == findSeqType()) {
                BedFile bedFile = new BedFile(
                        referenceGenome: workPackage.referenceGenome,
                        libraryPreparationKit: workPackage.libraryPreparationKit,
                        fileName: "TruSeqExomeTargetedRegions_plain.bed",
                        targetSize: 62085295,
                        mergedTargetSize: 62085286,
                )
                assert bedFile.save(flush: true)
            }

            setPermissionsRecursive(baseDirectory, TEST_DATA_MODE_DIR, TEST_DATA_MODE_FILE)

            // logging files are also created by the tomcat user, so the group needs write access
            setPermissionsRecursive(configService.loggingRootPath, '2770', TEST_DATA_MODE_FILE)
        }
    }

    void setUpFilesVariables() {
        testFastqFiles = [
                readGroup1: [
                        new File(inputRootDirectory, 'fastqFiles/wgs/normal/paired/run1/sequence/gerald_D1VCPACXX_6_R1.fastq.bz2'),
                        new File(inputRootDirectory, 'fastqFiles/wgs/normal/paired/run1/sequence/gerald_D1VCPACXX_6_R2.fastq.bz2'),
                ].asImmutable(),
                readGroup2: [
                        new File(inputRootDirectory, 'fastqFiles/wgs/normal/paired/run2/sequence/gerald_D1VCPACXX_7_R1.fastq.bz2'),
                        new File(inputRootDirectory, 'fastqFiles/wgs/normal/paired/run2/sequence/gerald_D1VCPACXX_7_R2.fastq.bz2'),
                ].asImmutable(),
        ].asImmutable()
        firstBamFile = new File(inputRootDirectory, 'bamFiles/wgs/first-bam-file/control_merged.mdup.bam')
        refGenDir = new File(referenceGenomeDirectory, 'bwa06_1KGRef')
        fingerPrintingFile = new File(referenceGenomeDirectory, 'bwa06_1KGRef/fingerPrinting/snp138Common.n1000.vh20140318.bed')
    }

    abstract SeqType findSeqType()

    Pipeline findPipeline() {
        return DomainFactory.createPanCanPipeline()
    }

    MergingWorkPackage createWorkPackage() {
        SeqType seqType = findSeqType()

        Pipeline pipeline = findPipeline()

        ReferenceGenome referenceGenome = createReferenceGenomeWithFile(
                referenceGenomeSpecificPath,
                refGenFileNamePrefix,
                cytosinePositionsIndex,
        )

        LibraryPreparationKit kit = new LibraryPreparationKit(
                name: "~* xX liBrArYprEPaRaTioNkiT Xx *~",
                adapterFile: new File(inputRootDirectory, 'adapters/TruSeq3-PE.fa').absolutePath,
                reverseComplementAdapterSequence: "AATGATACGGCGACCACCGAGATCTACACTCTTTCCCTACACGACGCTCTTCCGATCT",
        ).save(flush: true)

        MergingWorkPackage workPackage = DomainFactory.createMergingWorkPackage(
                pipeline: pipeline,
                seqType: seqType,
                referenceGenome: referenceGenome,
                needsProcessing: false,
                statSizeFileName: chromosomeStatFileName,
                libraryPreparationKit: seqType.wgbs ? null : kit,
        )

        workPackage.individual.pid = 'pid_4'  // This name is encoded in @RG of the test BAM file
        workPackage.individual.save(flush: true)

        workPackage.sampleType.name = "control"
        workPackage.sampleType.save(flush: true)

        workPackage.project.realm = realm
        workPackage.project.save(flush: true)

        workPackage.seqPlatformGroup.mergingCriteria = DomainFactory.createMergingCriteria(
                project: workPackage.individual.project,
                seqType: workPackage.seqType,
                useLibPrepKit: !seqType.wgbs,
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC
        )
        workPackage.seqPlatformGroup.save(flush: true)

        return workPackage
    }

    void createProjectConfig(MergingWorkPackage workPackage, Map options = [:]) {
        createDirectories([new File(projectService.getSequencingDirectory(workPackage.project).toString())])
        doWithAuth(OPERATOR) {
            projectService.configurePanCanAlignmentDeciderProject(new PanCanAlignmentConfiguration([
                    project          : workPackage.project,
                    seqType          : workPackage.seqType,
                    pluginName       : processingOptionService.findOptionAsString(PIPELINE_RODDY_ALIGNMENT_DEFAULT_PLUGIN_NAME, workPackage.seqType.roddyName),
                    programVersion   : processingOptionService.findOptionAsString(PIPELINE_RODDY_ALIGNMENT_DEFAULT_PLUGIN_VERSION, workPackage.seqType.roddyName),
                    baseProjectConfig: processingOptionService.findOptionAsString(PIPELINE_RODDY_ALIGNMENT_DEFAULT_BASE_PROJECT_CONFIG, workPackage.seqType.roddyName),
                    configVersion    : "v1_0",
                    referenceGenome  : workPackage.referenceGenome,
                    statSizeFileName : workPackage.statSizeFileName,
                    mergeTool        : processingOptionService.findOptionAsString(PIPELINE_RODDY_ALIGNMENT_DEFAULT_MERGE_TOOL, workPackage.seqType.roddyName),
                    bwaMemVersion    : processingOptionService.findOptionAsString(PIPELINE_RODDY_ALIGNMENT_BWA_VERSION_DEFAULT),
                    sambambaVersion  : processingOptionService.findOptionAsString(PIPELINE_RODDY_ALIGNMENT_SAMBAMBA_VERSION_DEFAULT),
                    resources        : 't',
            ] + options))
        }
    }

    SeqTrack createSeqTrack(String readGroupNum, Map properties = [:]) {
        MergingWorkPackage workPackage = exactlyOneElement(MergingWorkPackage.findAll())

        Map seqTrackProperties = [
                laneId               : readGroupNum,
                fastqcState          : SeqTrack.DataProcessingState.FINISHED,
                dataInstallationState: SeqTrack.DataProcessingState.FINISHED,
        ] + properties
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithFastqFiles(workPackage, seqTrackProperties)
        if (findSeqType().wgbs || findSeqType().chipSeq) {
            seqTrack.libraryPreparationKit = exactlyOneElement(LibraryPreparationKit.findAll())
            seqTrack.kitInfoReliability = InformationReliability.KNOWN
            seqTrack.save(flush: true)
        }

        RawSequenceFile.findAllBySeqTrack(seqTrack).eachWithIndex { RawSequenceFile rawSequenceFile, int index ->
            rawSequenceFile.vbpFileName = rawSequenceFile.fileName = "fastq_${seqTrack.individual.pid}_${seqTrack.sampleType.name}_${seqTrack.laneId}_${index + 1}.fastq.gz"
            rawSequenceFile.nReads = NUMBER_OF_READS
            rawSequenceFile.save(flush: true)
        }

        linkFastqFiles(seqTrack, testFastqFiles.get(readGroupNum))

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
        List<File> executionStorageFiles = filesInRoddyExecutionDir.collect {
            new File(roddyExecutionDirectory, it)
        }

        return [baseAndQaJsonFiles, executionStorageFiles].flatten()
    }

    Map<File, File> createLinkMapForFirstBamFile(RoddyBamFile firstBamFile) {
        Map<File, File> linkMapSourceLink = [:]

        ['Bam', 'Bai', 'Md5sum'].each {
            linkMapSourceLink.put(firstBamFile."work${it}File", firstBamFile."final${it}File")
        }
        linkMapSourceLink.put(firstBamFile.workMergedQADirectory, firstBamFile.finalMergedQADirectory)

        [firstBamFile.workExecutionDirectories, firstBamFile.finalExecutionDirectories].transpose().each {
            linkMapSourceLink.put(it[0], it[1])
        }

        Map<SeqTrack, File> workSingleLaneQADirectories = firstBamFile.workSingleLaneQADirectories
        Map<SeqTrack, File> finalSingleLaneQADirectories = firstBamFile.finalSingleLaneQADirectories
        workSingleLaneQADirectories.each { seqTrack, singleLaneQaWorkDir ->
            File singleLaneQcDirFinal = finalSingleLaneQADirectories.get(seqTrack)
            linkMapSourceLink.put(singleLaneQaWorkDir, singleLaneQcDirFinal)
        }

        return linkMapSourceLink
    }

    RoddyBamFile createFirstRoddyBamFile(boolean oldStructure = false) {
        assert firstBamFile.exists()

        MergingWorkPackage workPackage = exactlyOneElement(MergingWorkPackage.findAll())

        SeqTrack seqTrack = createSeqTrack("readGroup1", [run: DomainFactory.createRun(
                name: 'runName_33',  // This name is encoded in @RG of the test BAM file
                seqPlatform: DomainFactory.createSeqPlatformWithSeqPlatformGroup(seqPlatformGroups: [workPackage.seqPlatformGroup]),
        )])

        RoddyBamFile firstBamFile = new RoddyBamFile(
                workPackage: workPackage,
                identifier: RoddyBamFile.nextIdentifier(workPackage),
                seqTracks: [seqTrack] as Set,
                config: exactlyOneElement(RoddyWorkflowConfig.findAllByObsoleteDateIsNull()),
                numberOfMergedLanes: 1,
                fileOperationStatus: FileOperationStatus.PROCESSED,
                md5sum: calculateMd5Sum(firstBamFile),
                fileSize: firstBamFile.size(),
                dateFromFileSystem: new Date(firstBamFile.lastModified()),
                roddyExecutionDirectoryNames: ['exec_123456_123456789_bla_bla']
        )
        if (!oldStructure) {
            firstBamFile.workDirectoryName = "${RoddyBamFile.WORK_DIR_PREFIX}_0"
        }
        assert firstBamFile.save(flush: true)

        Map<File, String> filesWithContent = [:]
        Map<File, String> links = [:]

        links[this.firstBamFile] = firstBamFile.finalBamFile

        if (oldStructure) {
            filesWithContent << createFileListForFirstBam(firstBamFile, 'final').collectEntries {
                [(it): TEST_CONTENT]
            }
            links[this.firstBamFile] = firstBamFile.finalBamFile
        } else {
            filesWithContent << createFileListForFirstBam(firstBamFile, 'work').collectEntries {
                [(it): TEST_CONTENT]
            }
            links[this.firstBamFile] = firstBamFile.workBamFile
            links << createLinkMapForFirstBamFile(firstBamFile)
        }

        createFilesWithContent(filesWithContent)
        linkFileUtils.createAndValidateLinks(links, realm)

        workPackage.bamFileInProjectFolder = firstBamFile
        workPackage.needsProcessing = false
        assert workPackage.save(flush: true)

        return firstBamFile
    }

    static void setUpFingerPrintingFile() {
        ReferenceGenome referenceGenome = CollectionUtils.exactlyOneElement(ReferenceGenome.list())
        referenceGenome.fingerPrintingFileName = "snp138Common.n1000.vh20140318.bed"
        assert referenceGenome.save(flush: true)
    }

    void checkAllAfterSuccessfulExecution_alignBaseBamAndNewLanes() {
        SessionUtils.withTransaction {
            checkDataBaseState_alignBaseBamAndNewLanes()
            RoddyBamFile latestBamFile = CollectionUtils.atMostOneElement(RoddyBamFile.findAllByIdentifier(1))
            checkFileSystemState(latestBamFile)

            checkQC(latestBamFile)
        }
    }

    void checkQC(RoddyBamFile bamFile) {
        bamFile.seqTracks.each {
            List<RoddySingleLaneQa> qa = RoddySingleLaneQa.findAllBySeqTrack(it)
            assert qa
            qa.each {
                assert it.abstractBamFile == bamFile
            }
        }

        bamFile.finalSingleLaneQAJsonFiles.each { seqTrack, qaFile ->
            JSONObject json = JSON.parse(qaFile.text)
            Iterator chromosomes = json.keys()
            chromosomes.each { String chromosome ->
                assert CollectionUtils.atMostOneElement(RoddySingleLaneQa.findAllByChromosomeAndSeqTrack(chromosome, seqTrack))
            }
        }
        RoddyMergedBamQa mergedQa = CollectionUtils.atMostOneElement(RoddyMergedBamQa.findAllByAbstractBamFileAndChromosome(bamFile, RoddyQualityAssessment.ALL))
        assert mergedQa
        JSONObject json = JSON.parse(bamFile.finalMergedQAJsonFile.text)
        json.keys().each { String chromosome ->
            assert CollectionUtils.atMostOneElement(RoddyMergedBamQa.findAllByChromosomeAndAbstractBamFile(chromosome, bamFile))
        }
        assert bamFile.coverage == mergedQa.genomeWithoutNCoverageQcBases
        assert bamFile.coverageWithN == abstractBamFileService.calculateCoverageWithN(bamFile)

        assert bamFile.qualityAssessmentStatus == AbstractBamFile.QaProcessingStatus.FINISHED
        assert bamFile.qcTrafficLightStatus == AbstractBamFile.QcTrafficLightStatus.UNCHECKED

        if (bamFile.seqType.wgbs && bamFile.hasMultipleLibraries()) {
            List<RoddyLibraryQa> libraryQas = RoddyLibraryQa.findAllByAbstractBamFile(bamFile)
            assert libraryQas
            assert libraryQas*.libraryDirectoryName as Set == bamFile.seqTracks*.libraryDirectoryName as Set
        }
    }

    void checkAllAfterRoddyClusterJobsRestartAndSuccessfulExecution_alignBaseBamAndNewLanes() {
        SessionUtils.withTransaction {
            checkDataBaseState_alignBaseBamAndNewLanes()
            RoddyBamFile latestBamFile = CollectionUtils.atMostOneElement(RoddyBamFile.findAllByIdentifier(1))
            checkFileSystemState(latestBamFile)
        }
    }

    void checkDataBaseState_alignBaseBamAndNewLanes() {
        checkWorkPackageState()

        assert RoddyBamFile.findAll().size() == 2
        RoddyBamFile firstBamFile = CollectionUtils.atMostOneElement(RoddyBamFile.findAllByIdentifier(0))
        RoddyBamFile latestBamFile = CollectionUtils.atMostOneElement(RoddyBamFile.findAllByIdentifier(1))

        List<SeqTrack> seqTrackOfFirstBamFile = SeqTrack.findAllByLaneIdInList(["readGroup1"])

        checkFirstBamFileState(firstBamFile, false, [
                seqTracks         : seqTrackOfFirstBamFile,
                containedSeqTracks: seqTrackOfFirstBamFile,
        ])
        assertBamFileFileSystemPropertiesSet(firstBamFile)

        checkLatestBamFileState(latestBamFile, firstBamFile)
        assertBamFileFileSystemPropertiesSet(latestBamFile)
    }

    void checkFirstBamFileState(RoddyBamFile bamFile, boolean isMostResentBamFile, Map bamFileProperties = [:]) {
        List<SeqTrack> seqTracks = SeqTrack.findAllByLaneIdInList(["readGroup1", "readGroup2"])
        checkBamFileState(bamFile, [
                identifier         : 0,
                mostResentBamFile  : isMostResentBamFile,
                baseBamFile        : null,
                seqTracks          : seqTracks,
                containedSeqTracks : seqTracks,
                fileOperationStatus: FileOperationStatus.PROCESSED,
                withdrawn          : false,
        ] + bamFileProperties)
    }

    void checkLatestBamFileState(RoddyBamFile latestBamFile, RoddyBamFile firstbamFile, Map latestBamFileProperties = [:]) {
        SeqTrack firstSeqTrack = CollectionUtils.atMostOneElement(SeqTrack.findAllByLaneId("readGroup1"))
        SeqTrack secondSeqTrack = CollectionUtils.atMostOneElement(SeqTrack.findAllByLaneId("readGroup2"))
        checkBamFileState(latestBamFile, [
                identifier         : 1,
                mostResentBamFile  : true,
                baseBamFile        : firstbamFile,
                seqTracks          : [secondSeqTrack],
                containedSeqTracks : [firstSeqTrack, secondSeqTrack],
                fileOperationStatus: FileOperationStatus.PROCESSED,
                withdrawn          : false,
        ] + latestBamFileProperties)
    }

    void assertBamFileFileSystemPropertiesSet(RoddyBamFile bamFile) {
        assert bamFile.md5sum =~ /^[a-f0-9]{32}$/
        assert null != bamFile.dateFromFileSystem
        assert bamFile.fileSize > 0
    }

    void assertBamFileFileSystemPropertiesNotSet(RoddyBamFile bamFile) {
        assert bamFile.md5sum == null
        assert bamFile.dateFromFileSystem == null
        assert bamFile.fileSize == -1L
    }

    void checkBamFileState(RoddyBamFile bamFile, Map bamFileProperties) {
        MergingWorkPackage workPackage = bamFileProperties.mergingWorkPackage ?: exactlyOneElement(MergingWorkPackage.findAll())
        RoddyWorkflowConfig projectConfig = exactlyOneElement(RoddyWorkflowConfig.findAllByObsoleteDateIsNull())

        assert bamFileProperties.baseBamFile?.id == bamFile.baseBamFile?.id
        assert bamFileProperties.seqTracks.size() == bamFile.seqTracks.size()
        assert bamFileProperties.seqTracks*.id.containsAll(bamFile.seqTracks*.id)
        assert bamFileProperties.containedSeqTracks.size() == bamFile.containedSeqTracks.size()
        assert bamFileProperties.containedSeqTracks*.id.containsAll(bamFile.containedSeqTracks*.id)
        assert bamFileProperties.containedSeqTracks.size() == bamFile.numberOfMergedLanes

        assert workPackage.id == bamFile.workPackage.id
        assert projectConfig.id == bamFile.config.id
        assert bamFileProperties.fileOperationStatus == bamFile.fileOperationStatus
        assert bamFileProperties.withdrawn == bamFile.withdrawn

        assert bamFileProperties.identifier == bamFile.identifier
        assert bamFileProperties.mostResentBamFile == bamFile.mostRecentBamFile
    }

    void checkWorkPackageState() {
        SessionUtils.withTransaction {
            MergingWorkPackage workPackage = exactlyOneElement(MergingWorkPackage.findAll())
            workPackage.refresh()
            assert !workPackage.needsProcessing
        }
    }

    void checkFileSystemState(RoddyBamFile bamFile) {
        // content of the final dir: root
        if (!bamFile.seqType.rna) {
            List<File> rootDirs = [
                    bamFile.finalQADirectory,
                    bamFile.finalExecutionStoreDirectory,
                    bamFile.workDirectory,
            ]

            List<File> rootLinks = [
                    bamFile.finalBamFile,
                    bamFile.finalBaiFile,
                    bamFile.finalMd5sumFile,
                    bamFile.finalMergedQADirectory,
            ]
            if (bamFile.seqType.wgbs) {
                rootDirs += [
                        bamFile.finalMethylationDirectory,
                ]
                rootLinks += [
                        bamFile.finalMetadataTableFile,
                        bamFile.finalMergedMethylationDirectory,
                ]
                if (bamFile.hasMultipleLibraries()) {
                    rootLinks += bamFile.finalLibraryMethylationDirectories.values() +
                            bamFile.finalLibraryQADirectories.values()
                }
            }
            if (bamFile.baseBamFile && !bamFile.baseBamFile.oldStructureUsed) {
                rootDirs << bamFile.baseBamFile.workDirectory
            }
            fileAssertHelper.assertDirectoryContentReadable(rootDirs*.toPath(), [], rootLinks*.toPath())
        }

        // check work directories
        checkWorkDirFileSystemState(bamFile)
        if (bamFile.baseBamFile && !bamFile.baseBamFile.oldStructureUsed) {
            checkWorkDirFileSystemState(bamFile.baseBamFile, true)
        }

        // check md5sum content
        assert bamFile.md5sum == bamFile.finalMd5sumFile.text.replaceAll("\n", "")

        // content of the final dir: qa
        if (!bamFile.seqType.rna) {
            List<File> qaDirs = bamFile.finalSingleLaneQADirectories.values() + [bamFile.finalMergedQADirectory]
            List<File> qaSubDirs = []
            if (bamFile.baseBamFile) {
                if (bamFile.baseBamFile.oldStructureUsed) {
                    qaSubDirs.addAll(bamFile.baseBamFile.finalSingleLaneQADirectories.values())
                } else {
                    qaDirs.addAll(bamFile.baseBamFile.finalSingleLaneQADirectories.values())
                }
            }
            if (bamFile.seqType.wgbs && bamFile.hasMultipleLibraries()) {
                qaDirs.addAll(bamFile.finalLibraryQADirectories.values())
            }
            fileAssertHelper.assertDirectoryContentReadable(qaSubDirs*.toPath(), [], qaDirs*.toPath())
        }

        // qa only for merged and one for each read group
        if (!bamFile.seqType.rna) {
            int numberOfFilesInFinalQaDir = bamFile.numberOfMergedLanes + 1
            if (bamFile.seqType.wgbs && bamFile.hasMultipleLibraries()) {
                numberOfFilesInFinalQaDir += bamFile.seqTracks*.libraryDirectoryName.unique().size()
            }
            assert numberOfFilesInFinalQaDir == bamFile.finalQADirectory.list().length
        }

        // all roddyExecutionDirs have been linked
        List<File> expectedRoddyExecutionDirs = bamFile.finalExecutionDirectories
        if (bamFile.baseBamFile) {
            expectedRoddyExecutionDirs += bamFile.baseBamFile.finalExecutionDirectories
        }
        fileAssertHelper.assertDirectoryContentReadable(expectedRoddyExecutionDirs*.toPath())

        // content of the bam file
        LogThreadLocal.withThreadLog(System.out) {
            executeAndWait("zcat ${bamFile.finalBamFile} 1> /dev/null").assertExitCodeZeroAndStderrEmpty()
            assert bamFile.finalBamFile.length() == bamFile.fileSize
        }
        // samtools may under some circumstances produce small bam files of size larger than zero that however do not contain any reads.
        bamFile.finalBamFile.length() > 1024L

        checkInputIsNotDeleted()
    }

    void checkWorkDirFileSystemState(RoddyBamFile bamFile, boolean isBaseBamFile = false) {
        // content of the work dir: root
        if (!bamFile.seqType.rna) {
            List<File> rootDirs = [
                    bamFile.workQADirectory,
                    bamFile.workExecutionStoreDirectory,
                    bamFile.workMergedQADirectory,
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
            if (bamFile.seqType.wgbs) {
                rootDirs += [
                        bamFile.workMethylationDirectory,
                        bamFile.workMergedMethylationDirectory,
                ]
                rootFiles.add(bamFile.workMetadataTableFile)
                if (bamFile.hasMultipleLibraries()) {
                    rootDirs += bamFile.workLibraryMethylationDirectories.values() +
                            bamFile.workLibraryQADirectories.values()
                }
            }
            fileAssertHelper.assertDirectoryContentReadable(rootDirs*.toPath(), rootFiles*.toPath())
        }

        // content of the work dir: qa
        List<File> qaJson = [bamFile.workMergedQAJsonFile]
        if (!bamFile.seqType.rna) {
            List<File> qaDirs = [bamFile.workMergedQADirectory]
            qaDirs.addAll(bamFile.workSingleLaneQADirectories.values())
            qaJson.addAll(bamFile.workSingleLaneQAJsonFiles.values())
            if (bamFile.seqType.wgbs && bamFile.hasMultipleLibraries()) {
                qaDirs.addAll(bamFile.workLibraryQADirectories.values())
                qaJson.addAll(bamFile.workLibraryQAJsonFiles.values())
            }
            fileAssertHelper.assertDirectoryContentReadable(qaDirs*.toPath())
        }
        qaJson.each {
            assert it.exists() && it.file && it.canRead() && it.size() > 0
            JSON.parse(it.text) // throws ConverterException when the JSON content is not valid
        }

        //  content of the work dir: executionStoreDirectory
        fileAssertHelper.assertDirectoryContentReadable(bamFile.workExecutionDirectories*.toPath())

        // check that given files exist in the execution store:
        bamFile.workExecutionDirectories.each { executionStore ->
            filesInRoddyExecutionDir.each { String fileName ->
                fileService.ensureFileIsReadableAndNotEmpty(new File(executionStore.absolutePath, fileName).toPath(), realm)
            }
        }
    }

    void checkInputIsNotDeleted() {
        List<RawSequenceFile> fastqFiles = RawSequenceFile.findAll()
        fastqFiles.each { RawSequenceFile rawSequenceFile ->
            fileService.ensureFileIsReadableAndNotEmpty((lsdfFilesService.getFileFinalPath(rawSequenceFile) as File).toPath(), realm)
            fileService.ensureFileIsReadableAndNotEmpty((lsdfFilesService.getFileViewByPidPath(rawSequenceFile) as File).toPath(), realm)
        }
    }

    void verify_AlignLanesOnly_AllFine() {
        SessionUtils.withTransaction {
            checkWorkPackageState()

            RoddyBamFile bamFile = exactlyOneElement(RoddyBamFile.findAll())
            checkFirstBamFileState(bamFile, true)
            assertBamFileFileSystemPropertiesSet(bamFile)

            checkFileSystemState(bamFile)

            checkQC(bamFile)
        }
    }

    void fastTrackSetup() {
        SeqTrack seqTrack = createSeqTrack("readGroup1")
        assert seqTrack.project.save(flush: true)
        updateProcessingPriorityToFastrack()
    }

    protected void check_alignLanesOnly_NoBaseBamExist_TwoLanes(SeqTrack firstSeqTrack, SeqTrack secondSeqTrack) {
        SessionUtils.withTransaction {
            checkWorkPackageState()

            RoddyBamFile bamFile = exactlyOneElement(RoddyBamFile.findAll())
            checkLatestBamFileState(bamFile, null, [seqTracks: [firstSeqTrack, secondSeqTrack], identifier: 0L,])
            assertBamFileFileSystemPropertiesSet(bamFile)

            checkFileSystemState(bamFile)

            checkQC(bamFile)
        }
    }
}
