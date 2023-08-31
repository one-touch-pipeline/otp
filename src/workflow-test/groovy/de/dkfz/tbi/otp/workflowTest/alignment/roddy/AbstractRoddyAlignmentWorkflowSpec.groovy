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
package de.dkfz.tbi.otp.workflowTest.alignment.roddy

import grails.converters.JSON
import org.grails.web.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.bamfiles.RoddyBamFileService
import de.dkfz.tbi.otp.domainFactory.FastqcDomainFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPancanFactory
import de.dkfz.tbi.otp.job.processing.RoddyConfigService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import de.dkfz.tbi.otp.workflow.datainstallation.DataInstallationWorkflow
import de.dkfz.tbi.otp.workflow.fastqc.BashFastQcWorkflow
import de.dkfz.tbi.otp.workflow.alignment.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowTest.FileAssertHelper
import de.dkfz.tbi.otp.workflowTest.alignment.AbstractAlignmentWorkflowSpec
import de.dkfz.tbi.otp.workflowTest.referenceGenome.UsingReferenceGenome
import de.dkfz.tbi.otp.workflowTest.roddy.RoddyReferences

import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

/**
 * Base class for roddy alignment workflows
 */
abstract class AbstractRoddyAlignmentWorkflowSpec extends AbstractAlignmentWorkflowSpec implements UsingReferenceGenome, RoddyReferences, RoddyPancanFactory,
        FastqcDomainFactory {

    // @Slf4j does not work with Spock containing tests and produces problems in closures
    @SuppressWarnings('PropertyName')
    final static Logger log = LoggerFactory.getLogger(AbstractRoddyAlignmentWorkflowSpec)

    // The number of reads of the example fastqc files
    protected static final int NUMBER_OF_READS = 1000

    /**
     * The pid is encoded in the example bam file. Therefore, this pid needs to be used in all test using this bam file.
     */
    protected static final String PID = "pid_4"

    /**
     * The run name is encoded in the example bam file. Therefore, this run name needs to be used in all test using this bam file.
     */
    protected static final String RUN_NAME = "runName_33"

    /**
     * The sample type is encoded in the example bam file. Therefore, this sample type needs to be used in all test using this bam file.
     */
    protected static final String SAMPLE_TYPE = "control"

    AbstractBamFileService abstractBamFileService

    RoddyConfigService roddyConfigService

    RoddyBamFileService roddyBamFileService

    FileAssertHelper fileAssertHelper

    // holds references to the the fastq files on the file system
    protected Map<String, List<Path>> testFastqFiles

    protected AntibodyTarget antibodyTarget

    protected LibraryPreparationKit libraryPreparationKit

    protected MergingCriteria mergingCriteria

    protected Pipeline pipeline

    protected Sample sample

    protected SpeciesWithStrain human

    protected ReferenceGenome referenceGenome

    protected Run run

    protected SeqPlatform seqPlatform

    protected SeqPlatformGroup seqPlatformGroup

    protected SeqType seqType

    protected Workflow workflowDataInstallation

    protected Workflow workflowFastqc

    protected Workflow workflowAlignment

    protected WorkflowVersion workflowVersionAlignment

    Duration runningTimeout = Duration.ofHours(24)

    Class<? extends OtpWorkflow> workflowComponentClass = PanCancerWorkflow

    @Override
    void setup() {
        log.debug("Start setup ${this.class.simpleName}")
        SessionUtils.withTransaction {
            setUpFilesVariables()

            setUpDomainVariables()

            linkReferenceGenomeDirectoryToReference(referenceGenome)
            linkAdapterDirectoryToReference(libraryPreparationKit)
        }
        log.debug("Finish setup ${this.class.simpleName}")
    }

    protected void setUpFilesVariables() {
        testFastqFiles = [
                readGroup1: [
                        referenceDataDirectory.resolve('fastqFiles/wgs/normal/paired/run1/sequence/gerald_D1VCPACXX_6_R1.fastq.bz2'),
                        referenceDataDirectory.resolve('fastqFiles/wgs/normal/paired/run1/sequence/gerald_D1VCPACXX_6_R2.fastq.bz2'),
                ].asImmutable(),
                readGroup2: [
                        referenceDataDirectory.resolve('fastqFiles/wgs/normal/paired/run2/sequence/gerald_D1VCPACXX_7_R1.fastq.bz2'),
                        referenceDataDirectory.resolve('fastqFiles/wgs/normal/paired/run2/sequence/gerald_D1VCPACXX_7_R2.fastq.bz2'),
                ].asImmutable(),
        ].asImmutable()
    }

    private void setUpDomainVariables() {
        seqType = findSeqType()
        log.info("Configure seqType ${seqType}")

        workflowDataInstallation = CollectionUtils.exactlyOneElement(Workflow.findAllByName(DataInstallationWorkflow.WORKFLOW))
        log.info("Fetch workflow DataInstallation ${workflowDataInstallation}")

        workflowFastqc = CollectionUtils.exactlyOneElement(Workflow.findAllByName(BashFastQcWorkflow.WORKFLOW))
        log.info("Fetch workflow Fastqc ${workflowFastqc}")

        workflowAlignment = CollectionUtils.exactlyOneElement(Workflow.findAllByName(workflowName))
        workflowAlignment.supportedSeqTypes.add(seqType)
        workflowAlignment.save(flush: true)
        log.info("Fetch workflow Alignment ${workflowAlignment}")

        workflowVersionAlignment = CollectionUtils.exactlyOneElement(
                WorkflowVersion.findAllByWorkflow(workflowAlignment, [sort: 'id', order: 'desc', max: 1]))
        log.info("Fetch alignment workflow version ${workflowVersionAlignment}")

        human = findOrCreateHumanSpecies()
        log.info("Create human species ${human}")

        pipeline = findOrCreatePipeline()
        log.info("Create pipeline ${pipeline}")

        libraryPreparationKit = createLibraryPreparationKit([
                adapterFile                     : additionalDataDirectory.resolve('adapters/TruSeq3-PE.fa').toString(),
                reverseComplementAdapterSequence: "AATGATACGGCGACCACCGAGATCTACACTCTTTCCCTACACGACGCTCTTCCGATCT",
        ])
        log.info("Create libraryPreparationKit ${libraryPreparationKit}")

        seqPlatform = createSeqPlatform()
        log.info("Create seqPlatform ${seqPlatform}")

        seqPlatformGroup = createSeqPlatformGroup([
                seqPlatforms: [seqPlatform] as Set
        ])
        log.info("Create seqPlatformGroup ${seqPlatformGroup}")

        run = createRun([
                seqPlatform: seqPlatform,
                name       : RUN_NAME,
        ])
        log.info("Create run ${run}")

        referenceGenome = createReferenceGenome([
                path                    : referenceGenomeSpecificPath,
                fileNamePrefix          : referenceGenomeFileNamePrefix,
                cytosinePositionsIndex  : referenceGenomeCytosinePositionsIndex,
                chromosomeLengthFilePath: chromosomeLengthFilePath,
                chromosomeSuffix        : '',
                chromosomePrefix        : '',
                species                 : [] as Set,
                speciesWithStrain       : [human] as Set,
        ])
        log.info("Create ReferenceGenome ${referenceGenome}")

        List<String> chromosomeNames =  ["21", "22"]
        DomainFactory.createReferenceGenomeEntries(referenceGenome, chromosomeNames)
        log.info("Create ReferenceGenomeEntry for ${chromosomeNames}")

        sample = createSample([
                individual: createIndividual([
                        project: createProject([
                                realm: realm,
                        ]),
                        pid    : PID,
                        species: human,
                ]),
                sampleType: createSampleType([
                        name: SAMPLE_TYPE,
                ]),
        ])
        log.info("Create sample ${sample}")

        ReferenceGenomeSelector referenceGenomeSelector = createReferenceGenomeSelector([
                project        : sample.project,
                seqType        : seqType,
                workflow       : workflowAlignment,
                species        : [human] as Set,
                referenceGenome: referenceGenome,
        ])
        log.info("Create referenceGenomeSelector ${referenceGenomeSelector}")

        mergingCriteria = createMergingCriteria([
                project            : sample.project,
                seqType            : seqType,
                useLibPrepKit      : !seqType.wgbs,
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT,
        ])
        log.info("Create mergingCriteria ${mergingCriteria}")

        WorkflowVersionSelector workflowVersionSelector = createWorkflowVersionSelector([
                project        : sample.project,
                seqType        : seqType,
                workflowVersion: workflowVersionAlignment,
        ])
        log.info("Create selectedProjectSeqTypeWorkflowVersion ${workflowVersionSelector}")

        createFragments()

        if (seqType.needsBedFile) {
            BedFile bedFile = DomainFactory.createBedFile([
                    referenceGenome      : referenceGenome,
                    libraryPreparationKit: libraryPreparationKit,
                    fileName             : "TruSeqExomeTargetedRegions_plain.bed",
                    targetSize           : 62085295,
                    mergedTargetSize     : 62085286,
            ])
            log.info("Create bedfile ${bedFile}")
        }
    }

    private void createFragments() {
        createFragmentAndSelector("statSizeFileFragment", """
                    {
                        "RODDY": {
                            "cvalues": {
                                "CHROM_SIZES_FILE": {
                                    "value": "\${BASE_REFERENCE_GENOME}/${referenceGenome.path}/stats/${chromosomeStatFileName}",
                                    "type": "path"
                                }
                            }
                        }
                    }
                """, [
                workflows       : [workflowAlignment],
                referenceGenomes: [referenceGenome],
        ])
    }

    private void createFragmentAndSelector(String name, String json, Map selectors) {
        ExternalWorkflowConfigFragment fragment = createExternalWorkflowConfigFragment([
                name        : name,
                configValues: json.replaceAll('[ \n]+', ' '),
        ])
        log.info("Create fragment ${name} ${fragment}")

        ExternalWorkflowConfigSelector selector = createExternalWorkflowConfigSelector([
                name                          : name,
                workflowVersions              : [],
                workflows                     : [],
                referenceGenomes              : [],
                libraryPreparationKits        : [],
                seqTypes                      : [],
                projects                      : [],
                externalWorkflowConfigFragment: fragment,
                selectorType                  : SelectorType.GENERIC,
        ] + selectors)
        log.info("Create selector ${name} ${selector}")
    }

    protected void setUpFingerPrintingFile() {
        referenceGenome.refresh()
        referenceGenome.fingerPrintingFileName = fingerPrintingFileName
        referenceGenome.save(flush: true)
        assert referenceGenome.fingerPrintingFileName
        log.info("setup fingerPrintingFileName ${referenceGenome.fingerPrintingFileName}")
    }

    protected void setupUseAdapterTrimming() {
        workflowAlignment.refresh()
        createFragmentAndSelector("adapterTrimming", """
                    {
                        "RODDY": {
                            "cvalues": {
                                "useAdaptorTrimming": {
                                    "value": "true"
                                }
                            }
                        }
                    }
                """, [
                workflows: [workflowAlignment],
        ])
    }

    protected SeqTrack createSeqTrack(String readGroupNum, Map properties = [:]) {
        SeqTrack seqTrack = createSeqTrack([
                laneId               : readGroupNum,
                fastqcState          : SeqTrack.DataProcessingState.FINISHED,
                dataInstallationState: SeqTrack.DataProcessingState.FINISHED,
                sample               : sample,
                seqType              : seqType,
                libraryPreparationKit: libraryPreparationKit,
                run                  : run,
                antibodyTarget       : antibodyTarget,
        ] + properties)
        log.info("Create seqTrack ${seqTrack}")

        List<RawSequenceFile> rawSequenceFiles = (1..seqType.libraryLayout.mateCount).collect { int index ->
            String fileName = "fastq_${seqTrack.individual.pid}_${seqTrack.sampleType.name}_${seqTrack.laneId}_${index}.fastq.gz"
            RawSequenceFile rawSequenceFile = createFastqFile([
                    seqTrack           : seqTrack,
                    mateNumber         : index,
                    vbpFileName        : fileName,
                    fileName           : fileName,
                    nReads             : NUMBER_OF_READS,
                    fastqImportInstance: fastqImportInstance,
            ])
            log.info("Create sequenceFile ${rawSequenceFile}")
            return rawSequenceFile
        }
        createWorkflowArtefacts(workflowDataInstallation, seqTrack, ArtefactType.FASTQ)
        log.info("Create workflow artefact for seqTrack")

        if (isFastQcRequired()) {
            List<FastqcProcessedFile> fastqcProcessedFiles = rawSequenceFiles.collect { RawSequenceFile rawSequenceFile ->
                createFastqcProcessedFile([
                        sequenceFile     : rawSequenceFile,
                        workDirectoryName: "workDirectoryName",
                ])
            }
            createWorkflowArtefacts(workflowFastqc, fastqcProcessedFiles, ArtefactType.FASTQC)
            log.info("Create fastQc files with workflow artefact")
        }

        linkFastqFiles(seqTrack, testFastqFiles.get(readGroupNum))
        return seqTrack
    }

    protected WorkflowArtefact createWorkflowArtefacts(Workflow workflow, Artefact artefact, ArtefactType artefactType) {
        return createWorkflowArtefacts(workflow, [artefact], artefactType).first()
    }

    List<WorkflowArtefact> createWorkflowArtefacts(Workflow workflow, List<Artefact> artefacts, ArtefactType artefactType) {
        WorkflowRun workflowRun = createWorkflowRun([
                workflow: workflow,
                project : sample.project,
                priority: processingPriority,
                state: WorkflowRun.State.LEGACY,
        ])

        List<WorkflowArtefact> workflowArtefacts = artefacts.collect { Artefact artefact ->
            WorkflowArtefact workflowArtefact = createWorkflowArtefact(
                    state: WorkflowArtefact.State.SUCCESS,
                    artefactType: artefactType,
                    producedBy: workflowRun,
            )
            artefact.workflowArtefact = workflowArtefact
            artefact.save(flush: true)
            return workflowArtefact
        }

        return workflowArtefacts
    }

    protected void checkAllAfterSuccessfulExecution_alignBaseBamAndNewLanes() {
        SessionUtils.withTransaction {
            checkDataBaseState_alignBaseBamAndNewLanes()
            RoddyBamFile latestBamFile = CollectionUtils.exactlyOneElement(RoddyBamFile.findAllByIdentifier(1))
            assertBaseFileSystemState(latestBamFile)

            checkQC(latestBamFile)
        }
    }

    protected void checkQC(RoddyBamFile bamFile) {
        QualityAssessmentMergedPass qaPass = CollectionUtils.exactlyOneElement(QualityAssessmentMergedPass.findAllWhere(
                abstractBamFile: bamFile,
        ))

        bamFile.seqTracks.each {
            List<RoddySingleLaneQa> qa = RoddySingleLaneQa.findAllBySeqTrack(it)
            assert qa
            qa.each {
                assert it.qualityAssessmentMergedPass == qaPass
            }
        }

        roddyBamFileService.getFinalSingleLaneQAJsonFiles(bamFile).each { SeqTrack seqTrack, Path qaFile ->
            JSONObject json = (JSONObject) JSON.parse(qaFile.text)
            Iterator chromosomes = json.keys()
            chromosomes.each { String chromosome ->
                CollectionUtils.exactlyOneElement(RoddySingleLaneQa.findAllByChromosomeAndSeqTrack(chromosome, seqTrack))
            }
        }
        RoddyMergedBamQa mergedQa = CollectionUtils.exactlyOneElement(
                RoddyMergedBamQa.findAllByQualityAssessmentMergedPassAndChromosome(qaPass, RoddyQualityAssessment.ALL))
        JSONObject json = (JSONObject) JSON.parse(roddyBamFileService.getFinalMergedQAJsonFile(bamFile).text)
        json.keys().each { String chromosome ->
            assert RoddyMergedBamQa.findAllByChromosomeAndQualityAssessmentMergedPass(chromosome, qaPass)
        }
        assert bamFile.coverage == mergedQa.genomeWithoutNCoverageQcBases
        assert bamFile.coverageWithN == abstractBamFileService.calculateCoverageWithN(bamFile)

        assert bamFile.qualityAssessmentStatus == AbstractBamFile.QaProcessingStatus.FINISHED
        assert bamFile.qcTrafficLightStatus == AbstractBamFile.QcTrafficLightStatus.UNCHECKED

        if (bamFile.seqType.wgbs && bamFile.hasMultipleLibraries()) {
            List<RoddyLibraryQa> libraryQas = RoddyLibraryQa.findAllByQualityAssessmentMergedPass(qaPass)
            assert libraryQas
            assert libraryQas*.libraryDirectoryName as Set == bamFile.seqTracks*.libraryDirectoryName as Set
        }
    }

    protected void checkDataBaseState_alignBaseBamAndNewLanes() {
        checkWorkPackageState()

        assert RoddyBamFile.findAll().size() == 2
        RoddyBamFile firstBamFile = CollectionUtils.exactlyOneElement(RoddyBamFile.findAllByIdentifier(0))
        RoddyBamFile latestBamFile = CollectionUtils.exactlyOneElement(RoddyBamFile.findAllByIdentifier(1))

        List<SeqTrack> seqTrackOfFirstBamFile = SeqTrack.findAllByLaneIdInList(["readGroup1"])

        checkFirstBamFileState(firstBamFile, false, [
                seqTracks         : seqTrackOfFirstBamFile,
                containedSeqTracks: seqTrackOfFirstBamFile,
        ])
        assertBamFileFileSystemPropertiesSet(firstBamFile)

        checkLatestBamFileState(latestBamFile, firstBamFile)
        assertBamFileFileSystemPropertiesSet(latestBamFile)
    }

    protected void checkFirstBamFileState(RoddyBamFile bamFile, boolean isMostResentBamFile, Map bamFileProperties = [:]) {
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

    protected void checkLatestBamFileState(RoddyBamFile latestBamFile, RoddyBamFile firstBamFile, Map latestBamFileProperties = [:]) {
        SeqTrack firstSeqTrack = CollectionUtils.exactlyOneElement(SeqTrack.findAllByLaneId("readGroup1"))
        SeqTrack secondSeqTrack = CollectionUtils.exactlyOneElement(SeqTrack.findAllByLaneId("readGroup2"))
        checkBamFileState(latestBamFile, [
                identifier         : 1,
                mostResentBamFile  : true,
                baseBamFile        : firstBamFile,
                seqTracks          : [secondSeqTrack],
                containedSeqTracks : [firstSeqTrack, secondSeqTrack],
                fileOperationStatus: FileOperationStatus.PROCESSED,
                withdrawn          : false,
        ] + latestBamFileProperties)
    }

    protected void assertBamFileFileSystemPropertiesSet(RoddyBamFile bamFile) {
        assert bamFile.md5sum =~ /^[a-f0-9]{32}$/
        assert null != bamFile.dateFromFileSystem
        assert bamFile.fileSize > 0
    }

    protected void checkBamFileState(RoddyBamFile bamFile, Map bamFileProperties) {
        MergingWorkPackage workPackage = bamFileProperties.mergingWorkPackage ?: CollectionUtils.exactlyOneElement(MergingWorkPackage.list())

        assert bamFileProperties.baseBamFile?.id == bamFile.baseBamFile?.id
        assert bamFileProperties.seqTracks.size() == bamFile.seqTracks.size()
        assert bamFileProperties.seqTracks*.id.containsAll(bamFile.seqTracks*.id)
        assert bamFileProperties.containedSeqTracks.size() == bamFile.containedSeqTracks.size()
        assert bamFileProperties.containedSeqTracks*.id.containsAll(bamFile.containedSeqTracks*.id)
        assert bamFileProperties.containedSeqTracks.size() == bamFile.numberOfMergedLanes

        assert workPackage.id == bamFile.workPackage.id
        assert bamFile.config == null
        assert bamFileProperties.fileOperationStatus == bamFile.fileOperationStatus
        assert bamFileProperties.withdrawn == bamFile.withdrawn

        assert bamFileProperties.identifier == bamFile.identifier
        assert bamFileProperties.mostResentBamFile == bamFile.mostRecentBamFile
    }

    protected void checkWorkPackageState() {
        MergingWorkPackage workPackage = CollectionUtils.exactlyOneElement(MergingWorkPackage.list())
        workPackage.refresh()
        assert !workPackage.needsProcessing
    }

    protected void assertBaseFileSystemState(RoddyBamFile bamFile) {
        assertWorkDirectoryFileSystemState(bamFile, false)
        if (bamFile.baseBamFile) {
            assertWorkDirectoryFileSystemState(bamFile.baseBamFile, true)
        }
        assertRoddyExecutionDirectories(bamFile)
        assertBamFileFileOnFileSystem(bamFile)
        assertWorkflowFileSystemState(bamFile)

        verifyInputIsNotDeleted()
    }

    protected void assertWorkDirectoryFileSystemState(RoddyBamFile bamFile, boolean isBaseBamFile) {
        //  content of the work dir: executionStoreDirectory
        fileAssertHelper.assertDirectoryContent(roddyBamFileService.getWorkExecutionStoreDirectory(bamFile),
                roddyBamFileService.getWorkExecutionDirectories(bamFile))

        // check that given files exist in the execution store:
        roddyBamFileService.getWorkExecutionDirectories(bamFile).each { executionStore ->
            filesInRoddyExecutionDir.each { String fileName ->
                fileAssertHelper.assertFileIsReadableAndNotEmpty(executionStore.resolve(fileName))
            }
        }

        // check default json, additional needs to be checked in the subclass
        Path qaJson = roddyBamFileService.getWorkMergedQAJsonFile(bamFile)
        fileAssertHelper.assertFileIsReadableAndNotEmpty(qaJson)
        JSON.parse(qaJson.text) // throws ConverterException when the JSON content is not valid

        assertWorkflowWorkDirectoryFileSystemState(bamFile, isBaseBamFile)
    }

    private void assertRoddyExecutionDirectories(RoddyBamFile bamFile) {
        List<Path> expectedRoddyExecutionDirs = roddyBamFileService.getFinalExecutionDirectories(bamFile)
        if (bamFile.baseBamFile) {
            expectedRoddyExecutionDirs.addAll(roddyBamFileService.getFinalExecutionDirectories(bamFile.baseBamFile))
        }
        fileAssertHelper.assertDirectoryContent(roddyBamFileService.getFinalExecutionStoreDirectory(bamFile), expectedRoddyExecutionDirs)
    }

    private void assertBamFileFileOnFileSystem(RoddyBamFile bamFile) {
        // check md5sum content
        assert bamFile.md5sum == roddyBamFileService.getFinalMd5sumFile(bamFile).text.replaceAll("\n", "")

        // content of the bam file
        LogThreadLocal.withThreadLog(System.out) {
            LocalShellHelper.executeAndWait(" zcat  ${roddyBamFileService.getFinalBamFile(bamFile)} 1> /dev/null").assertExitCodeZeroAndStderrEmpty()
        }
        assert Files.size(roddyBamFileService.getFinalBamFile(bamFile)) == bamFile.fileSize

        // samtools may under some circumstances produce small bam files of size larger than zero that however do not contain any reads.
        assert Files.size(roddyBamFileService.getFinalBamFile(bamFile)) > 1024L
    }

    protected void verifyInputIsNotDeleted() {
        RawSequenceFile.list().each { RawSequenceFile rawSequenceFile ->
            fileAssertHelper.assertFileIsReadableAndNotEmpty(lsdfFilesService.getFileFinalPathAsPath(rawSequenceFile))
            fileAssertHelper.assertFileIsReadableAndNotEmpty(lsdfFilesService.getFileViewByPidPathAsPath(rawSequenceFile))
        }
    }

    protected void verify_AlignLanesOnly_AllFine() {
        SessionUtils.withTransaction {
            checkWorkPackageState()

            RoddyBamFile bamFile = CollectionUtils.exactlyOneElement(RoddyBamFile.list())
            checkFirstBamFileState(bamFile, true)
            assertBamFileFileSystemPropertiesSet(bamFile)

            assertBaseFileSystemState(bamFile)

            checkQC(bamFile)
        }
    }

    protected void verify_alignLanesOnly_NoBaseBamExist_TwoLanes(SeqTrack firstSeqTrack, SeqTrack secondSeqTrack) {
        SessionUtils.withTransaction {
            checkWorkPackageState()

            RoddyBamFile bamFile = CollectionUtils.exactlyOneElement(RoddyBamFile.list())
            checkLatestBamFileState(bamFile, null, [seqTracks: [firstSeqTrack, secondSeqTrack], identifier: 0L,])
            assertBamFileFileSystemPropertiesSet(bamFile)

            assertBaseFileSystemState(bamFile)

            checkQC(bamFile)
        }
    }

    abstract protected boolean isFastQcRequired()

    abstract protected SeqType findSeqType()

    abstract protected void assertWorkflowFileSystemState(RoddyBamFile bamFile)

    abstract protected void assertWorkflowWorkDirectoryFileSystemState(RoddyBamFile bamFile, boolean isBaseBamFile)
}
