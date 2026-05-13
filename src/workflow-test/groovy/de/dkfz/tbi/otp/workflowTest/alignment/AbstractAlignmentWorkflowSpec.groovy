/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.workflowTest.alignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.FastqcDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.otp.workflow.datainstallation.DataInstallationWorkflow
import de.dkfz.tbi.otp.workflow.fastqc.BashFastQcWorkflow
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowTest.AbstractDecidedWorkflowSpec
import de.dkfz.tbi.otp.workflowTest.FileAssertHelper

import java.nio.file.Files
import java.nio.file.Path

/**
 * Provides some helper needed in all alignment workflows
 */
abstract class AbstractAlignmentWorkflowSpec extends AbstractDecidedWorkflowSpec implements FastqcDomainFactory {

    FileAssertHelper fileAssertHelper

    // The number of reads of the example fastqc files
    protected static final int NUMBER_OF_READS = 1000

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

    protected void linkFastqFiles(SeqTrack seqTrack, List<Path> testFastqFiles) {
        List<RawSequenceFile> rawSequenceFiles = RawSequenceFile.findAllBySeqTrack(seqTrack)
        assert seqTrack.seqType.libraryLayout.mateCount == rawSequenceFiles.size()

        rawSequenceFiles.sort {
            it.mateNumber
        }.eachWithIndex { rawSequenceFile, index ->
            Path sourceFastqFile = testFastqFiles[index]
            assert Files.exists(sourceFastqFile)
            assert Files.isReadable(sourceFastqFile)
            rawSequenceFile.fileSize = Files.size(sourceFastqFile)
            rawSequenceFile.save(flush: true)
            Path linkFastqFile = rawSequenceDataWorkFileService.getFilePath(rawSequenceFile)
            Path linkViewByPid = rawSequenceDataViewFileService.getFilePath(rawSequenceFile)
            fileService.createLink(linkFastqFile, sourceFastqFile, configService.workflowProjectUnixGroup)
            fileService.createLink(linkViewByPid, linkFastqFile, configService.workflowProjectUnixGroup)
        }
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
                state   : WorkflowRun.State.LEGACY,
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

    /**
     * link the adapter directory into the test structure
     */
    void linkAdapterDirectoryToReference(LibraryPreparationKit libraryPreparationKit) {
        Path target = referenceDataDirectory.resolve("adapters")
        Path link = remoteFileSystem.getPath(libraryPreparationKit.adapterFile).parent
        fileService.createLink(link, target, configService.workflowProjectUnixGroup)
    }

    protected void setUpDomainVariables() {
        seqType = findSeqType()
        log.info("Configure seqType ${seqType}")

        workflowDataInstallation = CollectionUtils.exactlyOneElement(Workflow.findAllByName(DataInstallationWorkflow.WORKFLOW))
        log.info("Fetch workflow DataInstallation ${workflowDataInstallation}")

        workflowFastqc = CollectionUtils.exactlyOneElement(Workflow.findAllByName(BashFastQcWorkflow.WORKFLOW))
        log.info("Fetch workflow Fastqc ${workflowFastqc}")

        workflowAlignment = CollectionUtils.exactlyOneElement(Workflow.findAllByName(workflowName))
        workflowAlignment.defaultSeqTypesForWorkflowVersions.add(seqType)
        workflowAlignment.save(flush: true)
        log.info("Fetch workflow Alignment ${workflowAlignment}")

        WorkflowApiVersion wav = CollectionUtils.exactlyOneElement(
                WorkflowApiVersion.findAllByWorkflow(workflowAlignment, [sort: 'id', order: 'desc', max: 1]))
        workflowVersionAlignment = CollectionUtils.exactlyOneElement(WorkflowVersion.findAllByApiVersion(wav, [sort: 'id', order: 'desc', max: 1]))
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

        List<String> chromosomeNames = ["21", "22"]
        DomainFactory.createReferenceGenomeEntries(referenceGenome, chromosomeNames)
        log.info("Create ReferenceGenomeEntry for ${chromosomeNames}")

        sample = createSample([
                individual: createIndividual([
                        project: createProject(),
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

    protected void checkWorkPackageState() {
        MergingWorkPackage workPackage = CollectionUtils.exactlyOneElement(MergingWorkPackage.list())
        workPackage.refresh()
        assert !workPackage.needsProcessing
    }

    protected void checkFirstBamFileState(AbstractBamFile bamFile, boolean isMostResentBamFile, Map bamFileProperties = [:]) {
        List<SeqTrack> seqTracks = SeqTrack.findAllByLaneIdInList(["readGroup1", "readGroup2"])
        checkBamFileState(bamFile, [
                identifier         : 0,
                mostResentBamFile  : isMostResentBamFile,
                seqTracks          : seqTracks,
                containedSeqTracks : seqTracks,
                fileOperationStatus: AbstractBamFile.FileOperationStatus.PROCESSED,
                withdrawn          : false,
        ] + bamFileProperties)
    }

    protected void checkBamFileState(AbstractBamFile bamFile, Map bamFileProperties) {
        MergingWorkPackage workPackage = bamFileProperties.mergingWorkPackage ?: CollectionUtils.exactlyOneElement(MergingWorkPackage.list())

        assert bamFileProperties.seqTracks.size() == bamFile.seqTracks.size()
        assert bamFileProperties.seqTracks*.id.containsAll(bamFile.seqTracks*.id)
        assert bamFileProperties.containedSeqTracks.size() == bamFile.containedSeqTracks.size()
        assert bamFileProperties.containedSeqTracks*.id.containsAll(bamFile.containedSeqTracks*.id)
        assert bamFileProperties.containedSeqTracks.size() == bamFile.numberOfMergedLanes

        assert workPackage.id == bamFile.workPackage.id
        assert bamFileProperties.fileOperationStatus == bamFile.fileOperationStatus
        assert bamFileProperties.withdrawn == bamFile.withdrawn

        assert bamFileProperties.identifier == bamFile.identifier
        assert bamFileProperties.mostResentBamFile == bamFile.mostRecentBamFile

        checkBamFileConfig(bamFile)
    }

    protected void assertBamFileFileSystemPropertiesSet(AbstractBamFile bamFile) {
        assert bamFile.md5sum =~ /^[a-f0-9]{32}$/
        assert null != bamFile.dateFromFileSystem
        assert bamFile.fileSize > 0
    }

    protected void verifyInputIsNotDeleted() {
        RawSequenceFile.list().each { RawSequenceFile rawSequenceFile ->
            fileAssertHelper.assertFileIsReadableAndNotEmpty(rawSequenceDataWorkFileService.getFilePath(rawSequenceFile))
            fileAssertHelper.assertFileIsReadableAndNotEmpty(rawSequenceDataViewFileService.getFilePath(rawSequenceFile))
        }
    }

    protected void verify_AlignLanesOnly_AllFine() {
        SessionUtils.withTransaction {
            checkWorkPackageState()

            AbstractBamFile bamFile = CollectionUtils.exactlyOneElement(AbstractBamFile.list())
            checkFirstBamFileState(bamFile, true)
            assertBamFileFileSystemPropertiesSet(bamFile)
            assertBaseFileSystemState(bamFile)
            checkQC(bamFile)
        }
    }

    protected void verify_alignLanesOnly_TwoLanes(SeqTrack firstSeqTrack, SeqTrack secondSeqTrack) {
        SessionUtils.withTransaction {
            checkWorkPackageState()

            AbstractBamFile bamFile = CollectionUtils.exactlyOneElement(AbstractBamFile.list())
            checkFirstBamFileState(bamFile, true, [seqTracks: [firstSeqTrack, secondSeqTrack], containedSeqTracks: [firstSeqTrack, secondSeqTrack]])
            assertBamFileFileSystemPropertiesSet(bamFile)
            assertBaseFileSystemState(bamFile)
            checkQC(bamFile)
        }
    }

    abstract protected void assertBaseFileSystemState(AbstractBamFile bamFile)

    abstract protected void checkQC(AbstractBamFile bamFile)

    abstract protected void checkBamFileConfig(AbstractBamFile bamFile)

    abstract protected boolean isFastQcRequired()

    abstract protected SeqType findSeqType()

    abstract Pipeline findOrCreatePipeline()
}
