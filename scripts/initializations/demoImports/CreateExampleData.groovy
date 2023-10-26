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

import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.administration.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.bamfiles.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.*
import de.dkfz.tbi.otp.dataprocessing.qaalignmentoverview.PanCancerNoBedFileQaOverviewService
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaConfig
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellMappingFileService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaQc
import de.dkfz.tbi.otp.infrastructure.CreateLinkOption
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.job.processing.RoddyConfigService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.taxonomy.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.tracking.Ticket
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.workflow.alignment.AlignmentWorkflow
import de.dkfz.tbi.otp.workflow.alignment.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflow.alignment.rna.RnaAlignmentWorkflow
import de.dkfz.tbi.otp.workflow.alignment.wgbs.WgbsWorkflow
import de.dkfz.tbi.otp.workflow.datainstallation.DataInstallationWorkflow
import de.dkfz.tbi.otp.workflow.fastqc.WesFastQcWorkflow
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.util.TimeFormats

import java.nio.file.FileSystem
import java.nio.file.Path
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.LocalDateTime

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

/**
 * script to create example data.
 *
 * Its generates:
 * - fastq for all align-able seqTypes + two others
 * - fastqc for all fastq
 * - bam files
 *   - PanCancer alignment: WGS PAIRED, WES PAIRED, Chip PAIRED
 *   - Wgbs alignment: WGBS PAIRED, WGBS_TAG PAIRED
 *   - Rna alignment: RNA PAIRED, RNA SINGLE
 *   - Cell ranger: 10xSingleCellRnaSeqType PAIRED
 * - snv: WGS PAIRED, WES PAIRED
 * - indel: WGS PAIRED, WES PAIRED
 * - sophia: WGS PAIRED, WES PAIRED
 * - aceseq: WGS PAIRED
 * - runyapsa: WGS PAIRED, WES PAIRED
 *
 * It doesn't expect any existing data except the default SeqType's and workflows.
 *
 * The script can also generates dummy files for the objects. That is limited to the files OTP is aware of.
 * The real pipelines generates much more files.
 *
 * The script create the workflow artefacts of the new system for:
 * - Fastq
 * - Fastqc
 * - PanCancer
 * - WGBS Alignment
 * - Rna Alignment
 *
 * If more workflows are migrated, the corresponding script to create data needs to be executed.
 */
class ExampleData {
// ------------------------------
// input

    /**
     * Name of the project for the example data. If it not exist, it is created.
     */
    String projectName = "ExampleProject"

    /**
     * The count of patients to create
     */
    int individualCount = 2

    /**
     * The count of lanes to create for each sample and SeqType combination
     */
    int lanesPerSampleAndSeqType = 2

    /**
     * Should dummy files and other required directories be created?
     * Please ensure that otp has the necessary write permissions remotely for the project directory
     * (or any parent directory in which the directories needs to be created)
     */
    boolean createFilesOnFilesystem = true

    /**
     * A flag indicating to use CRAM instead of FASTQ files for WGS/WES
     */
    boolean usingCram = true

    /**
     * Should data files be marked as existing when no files were created?
     * Only taking effect if createFilesOnFilesystem is false.
     *
     * Usually there is no need to set this to false, only if you want to create data with missing files.
     */
    boolean markRawSequenceFilesAsExisting = true

    /**
     * The SeqTypes using panCancer
     */
    List<SeqType> panCanSeqTypes = [
            SeqTypeService.exomePairedSeqType,
            SeqTypeService.wholeGenomePairedSeqType,
            SeqTypeService.chipSeqPairedSeqType,
    ]

    /**
     * The SeqTypes using wgbs alignment
     */
    List<SeqType> wgbsAlignmentSeqTypes = [
            SeqTypeService.wholeGenomeBisulfitePairedSeqType,
            SeqTypeService.wholeGenomeBisulfiteTagmentationPairedSeqType,
    ]
    /**
     * The SeqTypes using rna alignment
     */
    List<SeqType> rnaSeqTypes = [
            SeqTypeService.rnaPairedSeqType,
            SeqTypeService.rnaSingleSeqType,

    ]

    /**
     * The SeqTypes for which single cell data is to be created.
     */
    List<SeqType> singleCellSeqTypes = [
            SeqTypeService.'10xSingleCellRnaSeqType'
    ]

    /**
     * no aligned seq types
     */
    List<SeqType> otherSeqTypes = [
            "AMPLICON",
            "ATAC",
    ].collect {
        findOrCreateSeqType(it)
    }

    /**
     * The disease SampleType names with info about xenograft for which data is to be created.
     *
     * If analysis are created, each diseaseSampleTypeNames is combined with each controlSampleTypeNames.
     */
    Map<String, MixedInSpecies> diseaseSampleTypeNames = [
            tumor01    : MixedInSpecies.NONE,
            tumor02    : MixedInSpecies.NONE,
            xenograft01: MixedInSpecies.MOUSE,
    ]

    /**
     * The control SampleType names for which data is to be created.
     *
     * If analysis are created, each diseaseSampleTypeNames is combined with each controlSampleTypeNames.
     */
    List<String> controlSampleTypeNames = [
            "control01",
    ]

    /**
     * The SampleType names (lanes are marked with well label) for which data is to be created.
     *
     * If analysis are created, each singleCellWellLabelSampleTypeNames is combined with each controlSampleTypeNames.
     */
    List<String> singleCellWellLabelSampleTypeNames = [
            "blood01",
            "blood02",
    ]

// ------------------------------
// work

    /**
     * The raw sequence file types can be either FASTQ or CRAM.
     * Currently only WGS and WES seq types can have CRAM file.
     */
    @TupleConstructor
    enum RawSequenceFileType {
        FASTQ('fastq.gz'),
        CRAM('unaligned.cram')

        final String type
    }

    AbstractBamFileService abstractBamFileService

    RoddyBamFileService roddyBamFileService

    RnaRoddyBamFileService rnaRoddyBamFileService

    FastqcDataFilesService fastqcDataFilesService

    FileService fileService

    FileSystemService fileSystemService

    LsdfFilesService lsdfFilesService

    SnvCallingService snvCallingService

    IndelCallingService indelCallingService

    SophiaService sophiaService

    AceseqService aceseqService

    RunYapsaService runYapsaService

    CellRangerConfigurationService cellRangerConfigurationService

    SingleCellBamFileService singleCellBamFileService

    CellRangerWorkflowService cellRangerWorkflowService

    SingleCellMappingFileService singleCellMappingFileService

    DocumentService documentService

    SeqTypeService seqTypeService
    RoddyConfigService roddyConfigService

    Project project
    FastqImportInstance fastqImportInstance
    FileType fileType
    LibraryPreparationKit libraryPreparationKit
    ProcessingPriority processingPriority
    Realm realm
    SpeciesWithStrain speciesWithStrainHuman
    SpeciesWithStrain speciesWithStrainMouse
    ReferenceGenome referenceGenomeHuman
    ReferenceGenome referenceGenomeMouse
    ReferenceGenome referenceGenomeHumanMouse
    ReferenceGenome singleCellReferenceGenomeHuman
    ReferenceGenomeIndex cellRangerReferenceGenomeIndex
    SeqCenter seqCenter
    SeqPlatform seqPlatform
    SeqPlatformGroup seqPlatformGroup
    SoftwareTool softwareTool
    AntibodyTarget antibodyTarget
    IlseSubmission ilseSubmission
    User otpUser

    final static Date FAST_QC_CREATE_DATE = LocalDateTime.of(2000,01,01,0,0,0).toDate()

    Workflow workflowDataInstallation
    Workflow workflowWesFastqc
    Workflow workflowPanCancer
    Workflow workflowWgbsAlignment
    Workflow workflowRnaAlignment

    int commentCounter = Comment.count()
    int individualCounter = Individual.count()
    int rawSequenceFileCounter = RawSequenceFile.count()
    int runCounter = Run.count()
    int seqTrackCounter = SeqTrack.count()

    Map<SampleType, MixedInSpecies> diseaseSampleTypes = [:]
    List<SampleType> singleCellWellLabelSampleTypes = []
    List<SampleType> controlSampleTypes = []
    List<RoddyBamFile> roddyBamFiles = []
    List<RoddyBamFile> rnaRoddyBamFiles = []
    List<SingleCellBamFile> singleCellBamFiles = []
    List<RoddySnvCallingInstance> roddySnvCallingInstances = []
    List<IndelCallingInstance> indelCallingInstances = []
    List<SophiaInstance> sophiaInstances = []
    List<AceseqInstance> aceseqInstances = []
    List<RunYapsaInstance> runYapsaInstances = []
    List<RawSequenceFile> rawSequenceFiles = []
    List<FastqcProcessedFile> fastqcProcessedFiles = []

    List<SeqType> analyseAbleSeqType = []
    List<SeqType> snvSeqTypes =[]
    List<SeqType> indelSeqTypes =[]
    List<SeqType> sophiaSeqTypes =[]
    List<SeqType> aceseqSeqTypes =[]
    List<SeqType> runYapsaSeqTypes =[]

    void init() {
        diseaseSampleTypes = diseaseSampleTypeNames.collectEntries {
            [(findOrCreateSampleType(it.key)): it.value]
        }
        singleCellWellLabelSampleTypes = singleCellWellLabelSampleTypeNames.collect {
            findOrCreateSampleType(it)
        }
        controlSampleTypes = controlSampleTypeNames.collect {
            findOrCreateSampleType(it)
        }

        snvSeqTypes = SeqTypeService.snvPipelineSeqTypes
        indelSeqTypes = SeqTypeService.indelPipelineSeqTypes
        sophiaSeqTypes = SeqTypeService.sophiaPipelineSeqTypes
        aceseqSeqTypes = SeqTypeService.aceseqPipelineSeqTypes
        runYapsaSeqTypes = SeqTypeService.runYapsaPipelineSeqTypes
        analyseAbleSeqType = SeqTypeService.allAnalysableSeqTypes

        otpUser = User.findByUsername("otp")
        processingPriority = findOrCreateProcessingPriority()
        fileType = findOrCreateFileType()
        libraryPreparationKit = findOrCreateLibraryPreparationKit()
        realm = findOrCreateRealm()
        speciesWithStrainHuman = findOrCreateSpeciesWithStrainHuman()
        speciesWithStrainMouse = findOrCreateSpeciesWithStrainMouse()
        referenceGenomeHuman = findOrCreateReferenceGenome("1KGRef_PhiX", [speciesWithStrainHuman], [])
        referenceGenomeMouse = findOrCreateReferenceGenome("GRCm38mm_PhiX", [], [speciesWithStrainMouse.species])
        referenceGenomeHumanMouse = findOrCreateReferenceGenome("hs37d5_GRCm38mm_PhiX", [speciesWithStrainHuman], [speciesWithStrainMouse.species])
        singleCellReferenceGenomeHuman = findOrCreateReferenceGenome("hg_GRCh38", [speciesWithStrainHuman], [])
        cellRangerReferenceGenomeIndex = findOrCreateCellRangerReferenceGenomeIndex()
        seqCenter = findOrCreateSeqCenter()
        seqPlatform = findOrCreateSeqPlatform()
        seqPlatformGroup = findOrCreateSeqPlatformGroup()
        softwareTool = findOrCreateSoftwareTool()
        antibodyTarget = findOrCreateAntibodyTarget()

        workflowDataInstallation = findWorkflow(DataInstallationWorkflow.WORKFLOW)
        workflowWesFastqc = findWorkflow(WesFastQcWorkflow.WORKFLOW)
        workflowPanCancer = findWorkflow(PanCancerWorkflow.WORKFLOW)
        workflowWgbsAlignment = findWorkflow(WgbsWorkflow.WORKFLOW)
        workflowRnaAlignment = findWorkflow(RnaAlignmentWorkflow.WORKFLOW)

        ilseSubmission = createIlseSubmission()
        fastqImportInstance = createFastqImportInstance()
        createMetaDataFile()
        project = findOrCreateProject(projectName)
        [
                diseaseSampleTypes.keySet(),
                singleCellWellLabelSampleTypes,
        ].flatten().each { SampleType sampleType ->
            findOrCreateSampleTypePerProject(sampleType, SampleTypePerProject.Category.DISEASE)
        }
        controlSampleTypes.each { SampleType sampleType ->
            findOrCreateSampleTypePerProject(sampleType, SampleTypePerProject.Category.CONTROL)
        }
        seqTypeService.findAlignAbleSeqTypes().each {
            findOrCreateMergingCriteria(it)
        }
        configureWorkflowsNewSystem()
        findOrCreateProcessingThresholds()
        createDocumentTestData()
        SessionUtils.withTransaction {
            it.flush()
        }
    }

    void createObjects() {
        createExampleSeqType()
        (1..individualCount).each {
            Individual individual = createIndividual(project)
            println "- individual: ${individual}"
            Map<SeqType, List<AbstractBamFile>> diseaseBamFiles = diseaseSampleTypes.collectMany { SampleType sampleType, MixedInSpecies mixedInSpecies ->
                Sample sample = findOrCreateSample(individual, sampleType, mixedInSpecies)
                println "  - sample: ${sample}"
                createSampleWithSeqTracks(sample)
                [
                        createSampleWithSeqTracksAndPanCancerBamFile(sample),
                        createSampleWithSeqTracksAndRnaBamFile(sample),
                        mixedInSpecies == MixedInSpecies.NONE ? createSingleCellSampleWithSeqTracksAndBamFile(sample, singleCellSeqTypes) : [],
                ].flatten()
            }.groupBy {
                it.seqType
            }
            Map<SeqType, List<AbstractBamFile>> controlBamFiles = controlSampleTypes.collectMany { SampleType sampleType ->
                Sample sample = findOrCreateSample(individual, sampleType)
                println "  - sample: ${sample}"
                createSampleWithSeqTracks(sample)
                [
                        createSampleWithSeqTracksAndPanCancerBamFile(sample),
                        createSampleWithSeqTracksAndRnaBamFile(sample),
                        createSingleCellSampleWithSeqTracksAndBamFile(sample, singleCellSeqTypes),
                ].flatten()
            }.groupBy {
                it.seqType
            }
            singleCellWellLabelSampleTypes.collectMany { SampleType sampleType ->
                Sample sample = findOrCreateSample(individual, sampleType)
                println "  - sample: ${sample}"
                createSingleCellSampleWithSeqTracksAndBamFile(sample, singleCellSeqTypes, true)
            }
            analyseAbleSeqType.each { SeqType seqType ->
                diseaseBamFiles[seqType].each { AbstractBamFile diseaseBamFile ->
                    controlBamFiles[seqType].each { AbstractBamFile controlBamFile ->
                        SamplePair samplePair = createSamplePair(diseaseBamFile.mergingWorkPackage, controlBamFile.mergingWorkPackage)
                        createRoddySnvCallingInstance(samplePair)
                        createIndelCallingInstance(samplePair)
                        createSophiaInstance(samplePair)
                        createAceseqInstance(samplePair)
                        createRunYapsaInstance(samplePair)
                    }
                }
            }
        }
        SessionUtils.withTransaction {
            it.flush()
        }
    }

    void createExampleSeqType() {
        SeqType seqType = findOrCreateSeqType("EXAMPLE")
        if (SeqTrack.countBySeqType(seqType) == 0) {
            Individual individual = createIndividual(project)
            Sample sample = findOrCreateSample(individual, diseaseSampleTypes.find().key, diseaseSampleTypes.find().value)
            createSeqTrack(sample, seqType, false)
        }
    }

    void createFiles() {
        if (createFilesOnFilesystem) {
            createRawSequenceFilesFilesOnFilesystem()
            createFastqcFilesOnFilesystem()
            createPanCancerBamFilesOnFilesystem()
            createRnaBamFilesOnFilesystem()
            createSnvFilesOnFilesystem()
            createIndelFilesOnFilesystem()
            createSophiaFilesOnFilesystem()
            createAceseqFilesOnFilesystem()
            createRunYapsaFilesOnFilesystem()
            createCellRangerFilesOnFilesystem()
            createSingleCellWellLabelOnFilesystem()
        } else {
            println "Skip creating dummy files/directories on file system"
        }
    }

    void createRawSequenceFilesFilesOnFilesystem() {
        println "creating dummy datafiles on file system"
        rawSequenceFiles.each { RawSequenceFile rawSequenceFile ->
            Path directPath = lsdfFilesService.getFileFinalPathAsPath(rawSequenceFile)
            Path directPathMd5sum = directPath.resolveSibling("${rawSequenceFile.fileName}.md5sum")
            Path vbpPath = lsdfFilesService.getFileViewByPidPathAsPath(rawSequenceFile)
            [
                    directPath,
                    directPathMd5sum,
            ].each {
                fileService.createFileWithContent(it, it.toString(), realm, FileService.DEFAULT_FILE_PERMISSION, true)
            }
            fileService.createLink(vbpPath, directPath, realm, CreateLinkOption.DELETE_EXISTING_FILE)
        }
    }

    void createFastqcFilesOnFilesystem() {
        println "creating dummy fastqc reports on file system"
        fastqcProcessedFiles.each { FastqcProcessedFile fastqcProcessedFile ->
            Path fastqcPath = fastqcDataFilesService.fastqcOutputPath(fastqcProcessedFile)
            Path fastqcMd5Path = fastqcPath.resolveSibling("${fastqcPath.getFileName()}.md5sum")
            [
                    fastqcPath,
                    fastqcMd5Path,
            ].each {
                fileService.createFileWithContent(it, it.toString(), realm, FileService.DEFAULT_FILE_PERMISSION, true)
            }
        }
    }

    void createSingleCellWellLabelOnFilesystem() {
        println "creating additional files or links for well labeled lanes on file system"
        rawSequenceFiles.each { RawSequenceFile rawSequenceFile ->
            if (rawSequenceFile.seqTrack.singleCellWellLabel) {
                Path target = lsdfFilesService.getFileFinalPathAsPath(rawSequenceFile)
                Path link = lsdfFilesService.getFileViewByPidPathAsPath(rawSequenceFile, WellDirectory.ALL_WELL)

                fileService.createLink(link, target, realm, CreateLinkOption.DELETE_EXISTING_FILE)

                singleCellMappingFileService.addMappingFileEntryIfMissing(rawSequenceFile)
            }
        }
    }

    void createPanCancerBamFilesOnFilesystem() {
        println "creating dummy pancaner bam files on file system"

        roddyBamFiles.each { RoddyBamFile bam ->
            Path workDir = roddyBamFileService.getWorkDirectory(bam)
            Map<Path, Path> filesMap = [
                    (roddyBamFileService.getFinalBamFile(bam))   : roddyBamFileService.getWorkBamFile(bam),
                    (roddyBamFileService.getFinalBaiFile(bam))   : roddyBamFileService.getWorkBaiFile(bam),
                    (roddyBamFileService.getFinalMd5sumFile(bam)): roddyBamFileService.getWorkMd5sumFile(bam),
            ]

            Map<Path, Path> dirsMap = [
                    (roddyBamFileService.getFinalMergedQADirectory(bam))      : roddyBamFileService.getWorkMergedQADirectory(bam),
                    (roddyBamFileService.getFinalExecutionStoreDirectory(bam)): roddyBamFileService.getWorkExecutionStoreDirectory(bam),
            ]
            List<Path> dirs = [workDir]
            dirs.addAll(roddyBamFileService.getWorkSingleLaneQADirectories(bam).values())

            if (bam.seqType.isWgbs()) {
                filesMap[roddyBamFileService.getFinalMetadataTableFile(bam)] = roddyBamFileService.getWorkMetadataTableFile(bam)
                if (bam.containedSeqTracks*.libraryDirectoryName.unique().size() > 1) {
                    dirs.addAll(roddyBamFileService.getWorkLibraryQADirectories(bam).values())
                    dirs.addAll(roddyBamFileService.getWorkLibraryMethylationDirectories(bam).values())
                }
            }

            dirs.each {
                fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(it)
            }

            dirsMap.each { Path pathFinal, Path pathWork ->
                fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(pathWork, realm)
                fileService.createLink(pathFinal, pathWork, CreateLinkOption.DELETE_EXISTING_FILE)
            }

            filesMap.each { Path pathFinal, Path pathWork ->
                fileService.createFileWithContent(pathWork, pathWork.toString(), realm, FileService.DEFAULT_FILE_PERMISSION, true)
                fileService.createLink(pathFinal, pathWork, CreateLinkOption.DELETE_EXISTING_FILE)
            }
            Path config = roddyConfigService.getConfigFile(workDir)
            fileService.createFileWithContent(config, config.toString(), realm, FileService.DEFAULT_FILE_PERMISSION, true)
        }
    }

    void createRnaBamFilesOnFilesystem() {
        println "creating dummy rna bam files on file system"
        FileSystem fileSystem = fileSystemService.getRemoteFileSystem(realm)

        rnaRoddyBamFiles.each { RoddyBamFile bam ->
            Path baseDir = abstractBamFileService.getBaseDirectory(bam)
            Path workDir = roddyBamFileService.getWorkDirectory(bam)

            Map<Path, Path> filesMap = [
                    (rnaRoddyBamFileService.getFinalBamFile(bam))   : rnaRoddyBamFileService.getWorkBamFile(bam),
                    (rnaRoddyBamFileService.getFinalBaiFile(bam))   : rnaRoddyBamFileService.getWorkBaiFile(bam),
                    (rnaRoddyBamFileService.getFinalMd5sumFile(bam)): rnaRoddyBamFileService.getWorkMd5sumFile(bam),
            ]

            [
                    "${bam.sampleType.name}_${bam.individual.pid}_chimeric_merged.junction",
                    "${bam.sampleType.name}_${bam.individual.pid}_chimeric_merged.mdup.bam",
                    "${bam.sampleType.name}_${bam.individual.pid}_chimeric_merged.mdup.bam.bai",
                    "${bam.sampleType.name}_${bam.individual.pid}_chimeric_merged.mdup.bam.md5",
                    "${bam.sampleType.name}_${bam.individual.pid}_merged.mdup.bam",
                    "${bam.sampleType.name}_${bam.individual.pid}_merged.mdup.bam.bai",
                    "${bam.sampleType.name}_${bam.individual.pid}_merged.mdup.bam.flagstat",
                    "${bam.sampleType.name}_${bam.individual.pid}_merged.mdup.bam.fp",
                    "${bam.sampleType.name}_${bam.individual.pid}_merged.mdup.bam.md5",
            ].each {
                filesMap[baseDir.resolve(it)] = workDir.resolve(it)
            }

            Map<File, File> dirsMap = [
                    (roddyBamFileService.getFinalExecutionStoreDirectory(bam)): roddyBamFileService.getWorkExecutionStoreDirectory(bam),
                    (roddyBamFileService.getFinalQADirectory(bam))            : roddyBamFileService.getWorkQADirectory(bam),
            ]
            [
                    "featureCounts",
                    "featureCounts_dexseq",
                    RnaRoddyBamFileService.ARRIBA_FOLDER,
                    "${bam.sampleType.name}_${bam.individual.pid}_star_logs_and_files",
            ].each {
                dirsMap[baseDir.resolve(it)] = workDir.resolve(it)
            }

            fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(workDir, realm)

            dirsMap.each {
                Path pathFinal = fileSystem.getPath(it.key.toString())
                Path pathWork = fileSystem.getPath(it.value.toString())
                fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(pathWork, realm)
                fileService.createLink(pathFinal, pathWork, realm, CreateLinkOption.DELETE_EXISTING_FILE)
            }

            filesMap.each {
                Path pathFinal = fileSystem.getPath(it.key.toString())
                Path pathWork = fileSystem.getPath(it.value.toString())
                fileService.createFileWithContent(pathWork, pathWork.toString(), realm, FileService.DEFAULT_FILE_PERMISSION, true)
                fileService.createLink(pathFinal, pathWork, realm, CreateLinkOption.DELETE_EXISTING_FILE)
            }
            Path plot = rnaRoddyBamFileService.getWorkArribaFusionPlotPdf(bam)
            fileService.createFileWithContent(plot, plot.toString(), realm, FileService.DEFAULT_FILE_PERMISSION, true)
            Path config = roddyConfigService.getConfigFile(workDir)
            fileService.createFileWithContent(config, config.toString(), realm, FileService.DEFAULT_FILE_PERMISSION, true)
        }
    }

    void createSnvFilesOnFilesystem() {
        println "creating dummy snv files on file system"

        roddySnvCallingInstances.each { RoddySnvCallingInstance snvCallingInstance ->
            [
                    snvCallingService.getSnvCallingResult(snvCallingInstance),
                    snvCallingService.getSnvDeepAnnotationResult(snvCallingInstance),
                    snvCallingService.getCombinedPlotPath(snvCallingInstance),
            ].each {
                fileService.createFileWithContent(it, it.toString(), realm, FileService.DEFAULT_FILE_PERMISSION, true)
            }
        }
    }

    void createIndelFilesOnFilesystem() {
        println "creating dummy indel files on file system"

        indelCallingInstances.each { IndelCallingInstance indelCallingInstance ->
            [
                    indelCallingService.getCombinedPlotPath(indelCallingInstance),
                    indelCallingService.getCombinedPlotPathTiNDA(indelCallingInstance),
                    indelCallingService.getIndelQcJsonFile(indelCallingInstance),
                    indelCallingService.getSampleSwapJsonFile(indelCallingInstance),
            ].each {
                fileService.createFileWithContent(it, it.toString(), realm, FileService.DEFAULT_FILE_PERMISSION, true)
            }
        }
    }

    void createSophiaFilesOnFilesystem() {
        println "creating dummy sophia files on file system"

        sophiaInstances.each { SophiaInstance sophiaInstance ->
            [
                    sophiaService.getCombinedPlotPath(sophiaInstance),
                    sophiaService.getFinalAceseqInputFile(sophiaInstance),
                    sophiaService.getQcJsonFile(sophiaInstance),
            ].each {
                fileService.createFileWithContent(it, it.toString(), realm, FileService.DEFAULT_FILE_PERMISSION, true)
            }
        }
    }

    void createAceseqFilesOnFilesystem() {
        println "creating dummy aceseq files on file system"

        aceseqInstances.each { AceseqInstance aceseqInstance ->
            Path base = aceseqService.getWorkDirectory(aceseqInstance)
            AceseqQc aceseqQc = CollectionUtils.exactlyOneElement(AceseqQc.findAllByNumberAndAceseqInstance(1, aceseqInstance))
            DecimalFormat decimalFormat = (DecimalFormat) NumberFormat.getInstance(Locale.ENGLISH)
            decimalFormat.applyPattern("0.##")
            String plotPrefixAceseqExtra = "${aceseqInstance.individual.pid}_plot_${aceseqQc.ploidyFactor}extra_${decimalFormat.format(aceseqQc.tcc)}_"
                    .replace('.', '\\.')

            [
                    aceseqService.getQcJsonFile(aceseqInstance),
                    aceseqService.getPlot(aceseqInstance, PlotType.ACESEQ_GC_CORRECTED),
                    aceseqService.getPlot(aceseqInstance, PlotType.ACESEQ_QC_GC_CORRECTED),
                    aceseqService.getPlot(aceseqInstance, PlotType.ACESEQ_TCN_DISTANCE_COMBINED_STAR),
                    aceseqService.getPlot(aceseqInstance, PlotType.ACESEQ_WG_COVERAGE),
                    // files for pattern for PlotType.ACESEQ_ALL
                    base.resolve("${aceseqInstance.individual.pid}_plot_1_ALL.png"),
                    base.resolve("${aceseqInstance.individual.pid}_plot_3_ALL.png"),
                    base.resolve("${aceseqInstance.individual.pid}_plot_4_ALL.png"),
                    // files for pattern for PlotType.ACESEQ_EXTRA
                    base.resolve("${plotPrefixAceseqExtra}_1.png"),
                    base.resolve("${plotPrefixAceseqExtra}_3.png"),
                    base.resolve("${plotPrefixAceseqExtra}_5.png"),
            ].each {
                fileService.createFileWithContent(it, it.toString(), realm, FileService.DEFAULT_FILE_PERMISSION, true)
            }
        }
    }

    void createRunYapsaFilesOnFilesystem() {
        println "creating dummy runYapsaInstances files on file system"

        runYapsaInstances.each { RunYapsaInstance runYapsaInstance ->
            Path base = runYapsaService.getWorkDirectory(runYapsaInstance)
            [
                    "snvs_${runYapsaInstance.individual.pid}_somatic_snvs_conf_8_to_10.vcf.combinedSignatureExposuresConfidence.pdf",
                    "snvs_${runYapsaInstance.individual.pid}_somatic_snvs_conf_8_to_10.vcf.combinedSignatureExposures.pdf",
                    "snvs_${runYapsaInstance.individual.pid}_somatic_snvs_conf_8_to_10.vcf.combinedSignatureExposures.tsv",
                    "snvs_${runYapsaInstance.individual.pid}_somatic_snvs_conf_8_to_10.vcf.combinedSignatureNormExposures.tsv",
                    "snvs_${runYapsaInstance.individual.pid}_somatic_snvs_conf_8_to_10.vcf.confIntSignatureExposures.tsv",
                    "snvs_${runYapsaInstance.individual.pid}_somatic_snvs_conf_8_to_10.vcfreportText.txt",
            ].each {
                Path file = base.resolve(it)
                fileService.createFileWithContent(file, file.toString(), realm, FileService.DEFAULT_FILE_PERMISSION, true)
            }
        }
    }

    void createCellRangerFilesOnFilesystem() {
        println "creating dummy cell ranger files on file system"

        singleCellBamFiles.each { SingleCellBamFile bam ->
            Path workdir = singleCellBamFileService.getWorkDirectory(bam)
            fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(workdir, realm)

            Path resultsPath = singleCellBamFileService.getResultDirectory(bam)

            [
                    singleCellBamFileService.getSampleDirectory(bam),
                    singleCellBamFileService.getOutputDirectory(bam),
                    resultsPath,
            ].each {
                fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(it, realm)
            }

            SingleCellBamFileService.CREATED_RESULT_DIRS.each {
                Path path = resultsPath.resolve(it)
                fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(path, realm)
            }

            SingleCellBamFileService.CREATED_RESULT_FILES.each {
                Path path = resultsPath.resolve(it)
                fileService.createFileWithContent(path, path.toString(), realm, FileService.DEFAULT_FILE_PERMISSION, true)
            }

            cellRangerWorkflowService.linkResultFiles(bam)
        }
    }

    SeqType findOrCreateSeqType(String name) {
        return SeqType.findByNameAndLibraryLayoutAndSingleCell(name, SequencingReadType.PAIRED, false) ?:
                new SeqType([
                        name             : name,
                        libraryLayout    : SequencingReadType.PAIRED,
                        singleCell       : false,
                        displayName      : name,
                        dirName          : name.toLowerCase(),
                        roddyName        : null,
                        hasAntibodyTarget: false,
                        needsBedFile     : false,
                ]).save(flush: false)
    }

    SampleType findOrCreateSampleType(String name) {
        return CollectionUtils.atMostOneElement(SampleType.findAllByName(name)) ?: new SampleType([
                name                   : name,
                specificReferenceGenome: SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT,
        ]).save(flush: false)
    }

    SampleTypePerProject findOrCreateSampleTypePerProject(SampleType sampleType, SampleTypePerProject.Category category) {
        return CollectionUtils.atMostOneElement(SampleTypePerProject.findAllByProjectAndSampleType(project, sampleType)) ?: new SampleTypePerProject([
                project   : project,
                sampleType: sampleType,
                category  : category,
        ]).save(flush: false)
    }

    void createDocumentTestData() {
        Set<DocumentType> documentTypes = documentService.listDocumentTypes()
        documentService.updateDocument(documentTypes[0], "PROJECT_FORM".bytes, "", Document.FormatType.TXT)
        documentService.updateDocument(documentTypes[1], "METADATA_TEMPLATE".bytes, "", Document.FormatType.TXT)
        documentService.updateDocument(documentTypes[2], "PROCESSING_INFORMATION".bytes, "", Document.FormatType.TXT)
    }

    void configureWorkflowsNewSystem() {
        ensureWorkflowConfigured(WesFastQcWorkflow.WORKFLOW, null)
        panCanSeqTypes.each {
            ensureWorkflowConfigured(PanCancerWorkflow.WORKFLOW, it)
            ensureReferenceGenomeConfigured(PanCancerWorkflow.WORKFLOW, it)
        }
        wgbsAlignmentSeqTypes.each {
            ensureWorkflowConfigured(WgbsWorkflow.WORKFLOW, it)
            ensureReferenceGenomeConfigured(WgbsWorkflow.WORKFLOW, it)
        }
        rnaSeqTypes.each {
            ensureWorkflowConfigured(RnaAlignmentWorkflow.WORKFLOW, it)
            ensureReferenceGenomeConfigured(RnaAlignmentWorkflow.WORKFLOW, it)
        }
    }

    void ensureWorkflowConfigured(String workflowName, SeqType seqType) {
        Workflow workflow = CollectionUtils.exactlyOneElement(Workflow.findAllByName(workflowName))
        WorkflowVersion workflowVersionToUse = CollectionUtils.exactlyOneElement(
                WorkflowVersion.findAllByWorkflow(workflow, [sort: 'id', order: 'desc', max: 1])
        )

        List<WorkflowVersionSelector> workflowVersionSelectors = WorkflowVersionSelector.withCriteria {
            eq('project', project)
            if (seqType) {
                eq('seqType', seqType)
            } else {
                isNull('seqType')
            }
            workflowVersion {
                eq('workflow', workflow)
            }
        }

        if (!workflowVersionSelectors) {
            println "- configure workflow: ${workflowName} ${project.name} ${seqType?.displayNameWithLibraryLayout ?: ''}"
            new WorkflowVersionSelector([
                    project        : project,
                    seqType        : seqType,
                    workflowVersion: workflowVersionToUse,
            ]).save(flush: false)
        }
    }

    void ensureReferenceGenomeConfigured(String workflowName, SeqType seqType) {
        Workflow workflow = CollectionUtils.exactlyOneElement(Workflow.findAllByName(workflowName))
        List<ReferenceGenomeSelector> referenceGenomeSelectors = ReferenceGenomeSelector.findAllByProjectAndSeqTypeAndWorkflow(project, seqType, workflow)
        if (!referenceGenomeSelectors) {
            println "- configure reference genome for: ${workflowName} ${project.name} ${seqType?.displayNameWithLibraryLayout ?: ''}"
            new ReferenceGenomeSelector([
                    project        : project,
                    seqType        : seqType,
                    workflow       : workflow,
                    species        : [speciesWithStrainHuman] as Set,
                    referenceGenome: referenceGenomeHuman,
            ]).save(flush: false)
            new ReferenceGenomeSelector([
                    project        : project,
                    seqType        : seqType,
                    workflow       : workflow,
                    species        : [speciesWithStrainMouse] as Set,
                    referenceGenome: referenceGenomeMouse,
            ]).save(flush: false)
            new ReferenceGenomeSelector([
                    project        : project,
                    seqType        : seqType,
                    workflow       : workflow,
                    species        : [speciesWithStrainHuman, speciesWithStrainMouse] as Set,
                    referenceGenome: referenceGenomeHumanMouse,
            ]).save(flush: false)
        }
    }

    void findOrCreateProcessingThresholds() {
        [
                diseaseSampleTypes.keySet(),
                controlSampleTypes,
        ].flatten().each { SampleType sampleType ->
            analyseAbleSeqType.each { SeqType seqType ->
                return CollectionUtils.atMostOneElement(ProcessingThresholds.findAllByProjectAndSampleTypeAndSeqType(project, sampleType, seqType)) ?:
                        new ProcessingThresholds([
                                project      : project,
                                seqType      : seqType,
                                sampleType   : sampleType,
                                coverage     : 20,
                                numberOfLanes: 1,
                        ]).save(flush: false)
            }
        }
    }

    ProcessingPriority findOrCreateProcessingPriority() {
        String name = "queue"
        return CollectionUtils.atMostOneElement(ProcessingPriority.findAllByName(name)) ?: new ProcessingPriority([
                name                       : name,
                queue                      : name,
                priority                   : ProcessingPriority.count,
                errorMailPrefix            : "error",
                roddyConfigSuffix          : "error",
                allowedParallelWorkflowRuns: 10,
        ]).save(flush: false)
    }

    FileType findOrCreateFileType() {
        return FileType.findOrCreateWhere([
                type     : FileType.Type.SEQUENCE,
                subType  : 'fastq',
                vbpPath  : '/sequence/',
                signature: '.fastq',
        ])
    }

    LibraryPreparationKit findOrCreateLibraryPreparationKit() {
        return LibraryPreparationKit.last() ?: new LibraryPreparationKit([
                name: "ExampleLibPrep",
        ]).save(flush: false)
    }

    SpeciesWithStrain findOrCreateSpeciesWithStrainHuman() {
        return findOrCreateSpeciesWithStrain('Human', 'Homo sapiens', 'No strain available')
    }

    SpeciesWithStrain findOrCreateSpeciesWithStrainMouse() {
        return findOrCreateSpeciesWithStrain('Mouse', 'Mus musculus', 'No strain available')
    }

    SpeciesWithStrain findOrCreateSpeciesWithStrain(String name, String scientificName, String strainName) {
        SpeciesCommonName speciesCommonName = SpeciesCommonName.findByName(name) ?: new SpeciesCommonName([
                name: name,
        ]).save(flush: false)

        Species species = Species.findBySpeciesCommonNameAndScientificName(speciesCommonName, scientificName) ?: new Species([
                speciesCommonName: speciesCommonName,
                scientificName   : scientificName,
        ]).save(flush: false)

        Strain strain = Strain.findByName(strainName) ?: new SpeciesCommonName([
                name: strainName,
        ]).save(flush: false)

        return SpeciesWithStrain.findBySpeciesAndStrain(species, strain) ?: new SpeciesWithStrain([
                species: species,
                strain : strain,
        ]).save(flush: false)
    }

    Realm findOrCreateRealm() {
        return Realm.last() ?: new Realm([
                name: 'realm',
        ]).save(flush: false)
    }

    ReferenceGenome findOrCreateReferenceGenome(String name,
                                                Collection<SpeciesWithStrain> speciesWithStrains,
                                                Collection<Species> speciesCollection) {
        ReferenceGenome referenceGenome = CollectionUtils.atMostOneElement(ReferenceGenome.findAllByName(name))
        if (referenceGenome) {
            return referenceGenome
        }
        ReferenceGenome referenceGenomeCreated = new ReferenceGenome([
                name                        : name,
                path                        : name,
                fileNamePrefix              : name,
                chromosomePrefix            : "",
                chromosomeSuffix            : "",
                lengthWithoutN              : 100,
                lengthRefChromosomes        : 100,
                lengthRefChromosomesWithoutN: 100,
                length                      : 100,
                speciesWithStrain           : speciesWithStrains as Set,
                species                     : speciesCollection as Set,
        ]).save(flush: false)

        PanCancerNoBedFileQaOverviewService.CHROMOSOMES_XY.each {
            new ReferenceGenomeEntry([
                    name           : it,
                    alias          : it,
                    length         : 100,
                    lengthWithoutN : 100,
                    classification : ReferenceGenomeEntry.Classification.CHROMOSOME,
                    referenceGenome: referenceGenomeCreated,
            ]).save(flush: false)
        }

        return referenceGenomeCreated
    }

    SeqCenter findOrCreateSeqCenter() {
        String name = "ExampleCenter"
        return CollectionUtils.atMostOneElement(SeqCenter.findAllByName(name)) ?: new SeqCenter([
                name   : name,
                dirName: "center",
        ]).save(flush: false)
    }

    SeqPlatform findOrCreateSeqPlatform() {
        String name = "ExampleSeqPlatform"
        return CollectionUtils.atMostOneElement(SeqPlatform.findAllByName(name)) ?: new SeqPlatform([
                name                 : name,
                seqPlatformModelLabel: new SeqPlatformModelLabel([
                        name: "ExampleModel",
                ]).save(flush: false),
                sequencingKitLabel   : new SequencingKitLabel([
                        name: "ExampleKit",
                ]).save(flush: false),
        ]).save(flush: false)
    }

    MergingCriteria findOrCreateMergingCriteria(SeqType seqType) {
        return CollectionUtils.atMostOneElement(MergingCriteria.findAllByProjectAndSeqType(project, seqType)) ?: new MergingCriteria([
                project            : project,
                seqType            : seqType,
                useLibPrepKit      : !seqType.isWgbs(),
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT,
        ]).save(flush: true)
    }

    SeqPlatformGroup findOrCreateSeqPlatformGroup() {
        SeqPlatformGroup findSeqPlatformGroup = SeqPlatformGroup.createCriteria().get {
            seqPlatforms {
                eq('id', seqPlatform.id)
            }
            isNull('mergingCriteria')
        }

        if (findSeqPlatformGroup) {
            return findSeqPlatformGroup
        }
        SeqPlatformGroup newSeqPlatformGroup = new SeqPlatformGroup().save(flush: true)
        newSeqPlatformGroup.addToSeqPlatforms(seqPlatform)
        newSeqPlatformGroup.save(flush: true)

        return newSeqPlatformGroup
    }

    SoftwareTool findOrCreateSoftwareTool() {
        String name = "ExampleSoftwareTool"
        return CollectionUtils.atMostOneElement(SoftwareTool.findAllByProgramName(name)) ?: new SoftwareTool([
                programName   : "ExampleSoftwareTool",
                programVersion: "1.2.3",
                type          : SoftwareTool.Type.BASECALLING,
        ]).save(flush: true)
    }

    AntibodyTarget findOrCreateAntibodyTarget() {
        String name = "AntibodyTarget"
        return CollectionUtils.atMostOneElement(AntibodyTarget.findAllByName(name)) ?: new AntibodyTarget([
                name: "AntibodyTarget",
        ]).save(flush: true)
    }

    Workflow findWorkflow(String name) {
        return CollectionUtils.exactlyOneElement(Workflow.findAllByName(name))
    }

    IlseSubmission createIlseSubmission() {
        return new IlseSubmission([
                ilseNumber: 10000 + IlseSubmission.count()
        ]).save(flush: true)
    }

    Ticket createTicket() {
        return new Ticket([
                ticketNumber: "${Ticket.count()}"
        ]).save(flush: true)
    }

    FastqImportInstance createFastqImportInstance() {
        return new FastqImportInstance([
                importMode: FastqImportInstance.ImportMode.MANUAL,
                ticket    : createTicket(),
        ]).save(flush: true)
    }

    MetaDataFile createMetaDataFile() {
        return new MetaDataFile([
                fileNameSource     : "fileName_${MetaDataFile.count()}",
                filePathSource     : "/tmp/filePath_${MetaDataFile.count()}",
                md5sum             : HelperUtils.randomMd5sum,
                fastqImportInstance: fastqImportInstance,
        ]).save(flush: true)
    }

    Project findOrCreateProject(String projectName) {
        return CollectionUtils.atMostOneElement(Project.findAllByName(projectName)) ?: new Project([
                name              : projectName,
                dirName           : projectName,
                individualPrefix  : "${projectName}_",
                realm             : realm,
                processingPriority: processingPriority,
                projectType       : Project.ProjectType.SEQUENCING,
                unixGroup         : "developer",
                speciesWithStrains: [speciesWithStrainHuman] as Set,
        ]).save(flush: true)
    }

    Individual createIndividual(Project project) {
        return new Individual([
                project: project,
                pid    : "${projectName}_pid_${individualCounter++}",
                type   : Individual.Type.REAL,
                species: speciesWithStrainHuman,
        ]).save(flush: false)
    }

    void createSampleWithSeqTracks(Sample sample) {
        otherSeqTypes.collect { SeqType seqType ->
            println "    - seqTracks for: ${seqType}"
            (1..lanesPerSampleAndSeqType).each {
                createSeqTrack(sample, seqType)
            }
        }
    }

    List<AbstractBamFile> createSampleWithSeqTracksAndPanCancerBamFile(Sample sample) {
        return [
                panCanSeqTypes,
                wgbsAlignmentSeqTypes,
        ].flatten().collect { SeqType seqType ->
            println "    - seqTracks for: ${seqType}"
            List<SeqTrack> seqTracks = (1..lanesPerSampleAndSeqType).collect {
                SeqTrack seqTrack = createSeqTrack(sample, seqType)
                println "      - seqtrack: ${seqTrack}"
                return seqTrack
            }
            MergingWorkPackage mergingWorkPackage = createMergingWorkPackage(seqTracks)
            println "      - mwp: ${mergingWorkPackage}"
            RoddyBamFile roddyBamFile = createRoddyBamFile(mergingWorkPackage)
            println "      - roddy: ${roddyBamFile}"
            return roddyBamFile
        }
    }

    List<AbstractBamFile> createSampleWithSeqTracksAndRnaBamFile(Sample sample) {
        return rnaSeqTypes.collect { SeqType seqType ->
            println "    - seqTracks for: ${seqType}"
            List<SeqTrack> seqTracks = (1..lanesPerSampleAndSeqType).collect {
                SeqTrack seqTrack = createSeqTrack(sample, seqType)
                println "      - seqtrack: ${seqTrack}"
                return seqTrack
            }
            MergingWorkPackage mergingWorkPackage = createMergingWorkPackage(seqTracks)
            println "      - mwp: ${mergingWorkPackage}"
            RnaRoddyBamFile roddyBamFile = createRnaRoddyBamFile(mergingWorkPackage)
            println "      - roddy: ${roddyBamFile}"
            return roddyBamFile
        }
    }

    List<AbstractBamFile> createSingleCellSampleWithSeqTracksAndBamFile(Sample sample, List<SeqType> seqTypes,
                                                                        boolean createWellLabel = false) {
        return seqTypes.collect { SeqType seqType ->
            println "    - seqTracks for: ${seqType}"
            List<SeqTrack> seqTracks = (1..lanesPerSampleAndSeqType).collect {
                SeqTrack seqTrack = createSeqTrack(sample, seqType, createWellLabel)
                println "      - seqtrack: ${seqTrack}"
                return seqTrack
            }

            Pipeline pipeline = Pipeline.Name.CELL_RANGER.pipeline

            CellRangerConfig config = findOrCreateCellRangerConfig(seqType, pipeline)

            List<CellRangerMergingWorkPackage> mwpGroup = []

            (0..3).each {
                CellRangerMergingWorkPackage crmwp = createCellRangerMergingWorkPackage(seqTracks, config, pipeline, it)
                println "      - crmwp: ${crmwp}"
                mwpGroup.add(crmwp)
            }

            mwpGroup.each {
                SingleCellBamFile singleCellBamFile = createSingleCellBamFile(it)
                println "      - cell ranger bam file: ${singleCellBamFile}"
            }
            return mwpGroup
        }.flatten()
    }

    Sample findOrCreateSample(Individual individual, SampleType sampleType, MixedInSpecies mixedInSpecies = MixedInSpecies.NONE) {
        Sample foundSample = atMostOneElement(Sample.findAllByIndividualAndSampleType(individual, sampleType))
        if (foundSample) {
            return foundSample
        }
        return new Sample([
                sampleType    : sampleType,
                individual    : individual,
                mixedInSpecies: (mixedInSpecies == MixedInSpecies.MOUSE ? [speciesWithStrainMouse] : []) as Set
        ]).save(flush: false)
    }

    SeqTrack createSeqTrack(Sample sample, SeqType seqType, createWellLabel = false) {
        int count = seqTrackCounter++
        String name = "${sample} ${seqType.displayNameWithLibraryLayout} ${count}"

        WorkflowRun workflowRun = createWorkflowRun(
                "/tmp/seqtrack",
                workflowDataInstallation,
                name,
        ).save(flush: false)

        WorkflowArtefact workflowArtefact = createWorkflowArtefact(
                workflowRun,
                DataInstallationWorkflow.OUTPUT_FASTQ,
                name
        ).save(flush: false)

        SeqTrack seqTrack = new SeqTrack([
                sample               : sample,
                seqType              : seqType,
                run                  : createRun(),
                laneId               : (count % 8) + 1,
                singleCellWellLabel  : createWellLabel ? "well_${count}" : "",
                sampleIdentifier     : "sample_${count}",
                pipelineVersion      : softwareTool,
                dataInstallationState: SeqTrack.DataProcessingState.FINISHED,
                fastqcState          : SeqTrack.DataProcessingState.FINISHED,
                libraryPreparationKit: libraryPreparationKit,
                kitInfoReliability   : InformationReliability.KNOWN,
                ilseSubmission       : ilseSubmission,
                antibodyTarget       : seqType.hasAntibodyTarget ? antibodyTarget : null,
                nBasePairs           : 1000000000,
                workflowArtefact     : workflowArtefact,
        ]).save(flush: false)

        WorkflowRun workflowRunFastqc = createWorkflowRun(
                "/tmp/fastqc",
                workflowWesFastqc,
                name,
        ).save(flush: false)

        createWorkflowRunInputArtefact(workflowRunFastqc, workflowArtefact, WesFastQcWorkflow.INPUT_FASTQ)

        // Currently CRAM files are available only for WGS and WES
        if (usingCram && (seqType.wgs || seqType.exome)) {
            (1..seqType.libraryLayout.mateCount).each {
                createCramFile(seqTrack, it, workflowRunFastqc)
            }
        } else {
            (1..seqType.libraryLayout.mateCount).each {
                createFastqFile(seqTrack, it, workflowRunFastqc)
            }
        }

        return seqTrack
    }

    Run createRun() {
        return new Run([
                name        : "run_${runCounter++}",
                dateExecuted: new Date(),
                blacklisted : false,
                seqCenter   : seqCenter,
                seqPlatform : seqPlatform,
        ]).save(flush: false)
    }

    // Create either a fastq or cram file depending on its RawSequenceFileType
    RawSequenceFile createRawSeqFile(SeqTrack seqTrack, int mateNumber, RawSequenceFileType rawSequenceFileType, WorkflowRun workflowRunFastqc) {
        String fileName = "file_${rawSequenceFileCounter++}_L${seqTrack.laneId}_R${mateNumber}.${rawSequenceFileType.type}"
        Map parameters = [
                seqTrack           : seqTrack,
                mateNumber         : mateNumber,
                fastqImportInstance: fastqImportInstance,
                vbpFileName        : fileName,
                fileType           : fileType,
                fileName           : fileName,
                pathName           : '',
                initialDirectory   : '/tmp',
                fastqMd5sum        : "0" * 32,
                run                : seqTrack.run,
                project            : seqTrack.project,
                used               : true,
                fileExists         : !createFilesOnFilesystem ? markRawSequenceFilesAsExisting : true,
                fileLinked         : true,
                fileSize           : 1000000000,
                nReads             : 185000000,
                dateLastChecked    : new Date()
        ]

        RawSequenceFile rawSequenceFile = rawSequenceFileType == RawSequenceFileType.CRAM
                ? new SequenceCramFile(parameters + [cramMd5sum: "1" * 32])
                : new FastqFile(parameters)
        rawSequenceFile.save(flush: false)

        rawSequenceFiles << rawSequenceFile
        createFastqcProcessedFiles(rawSequenceFile, workflowRunFastqc)

        return rawSequenceFile
    }

    RawSequenceFile createFastqFile(SeqTrack seqTrack, int mateNumber, WorkflowRun workflowRunFastqc) {
        return createRawSeqFile(seqTrack, mateNumber, RawSequenceFileType.FASTQ, workflowRunFastqc)
    }

    RawSequenceFile createCramFile(SeqTrack seqTrack, int mateNumber, WorkflowRun workflowRunFastqc) {
        return createRawSeqFile(seqTrack, mateNumber, RawSequenceFileType.CRAM, workflowRunFastqc)
    }

    FastqcProcessedFile createFastqcProcessedFiles(RawSequenceFile rawSequenceFile, WorkflowRun workflowRunFastqc) {
        WorkflowArtefact workflowArtefact = createWorkflowArtefact(
                workflowRunFastqc,
                WesFastQcWorkflow.OUTPUT_FASTQC,
                "${workflowRunFastqc.displayName} ${rawSequenceFile.fileName}"
        ).save(flush: false)

        FastqcProcessedFile fastqcProcessedFile = new FastqcProcessedFile([
                sequenceFile     : rawSequenceFile,
                workDirectoryName: "wes-1.1.0-2000-01-01-00-00-00",
                workflowArtefact : workflowArtefact,
                contentUploaded:true,
                fileExists:true,
                fileSize: 100,
                dateFromFileSystem: FAST_QC_CREATE_DATE,
        ]).save(flush: false)

        fastqcProcessedFiles << fastqcProcessedFile
        return fastqcProcessedFile
    }

    MergingWorkPackage createMergingWorkPackage(List<SeqTrack> seqTracks) {
        SeqTrack seqTrack = seqTracks.first()
        Pipeline pipeline = Pipeline.Name.forSeqType(seqTrack.seqType).pipeline
        return new MergingWorkPackage([
                sample               : seqTrack.sample,
                seqType              : seqTrack.seqType,
                antibodyTarget       : seqTrack.antibodyTarget,
                seqTracks            : seqTracks as Set,
                referenceGenome      : (seqTracks.first().sample.mixedInSpecies ? referenceGenomeHumanMouse : referenceGenomeHuman),
                pipeline             : pipeline,
                statSizeFileName     : null,
                seqPlatformGroup     : seqPlatformGroup,
                libraryPreparationKit: seqTrack.seqType.isWgbs() ? null : libraryPreparationKit,
        ]).save(flush: false)
    }

    CellRangerMergingWorkPackage createCellRangerMergingWorkPackage(List<SeqTrack> seqTracks, CellRangerConfig config, Pipeline pipeline, Integer cells) {
        SeqTrack seqTrack = seqTracks.first()

        CellRangerMergingWorkPackage cellRangerMergingWorkPackage = new CellRangerMergingWorkPackage([
                config               : config,
                sample               : seqTrack.sample,
                seqType              : seqTrack.seqType,
                seqTracks            : seqTracks as Set,
                referenceGenome      : singleCellReferenceGenomeHuman,
                pipeline             : pipeline,
                seqPlatformGroup     : seqPlatformGroup,
                libraryPreparationKit: libraryPreparationKit,
                referenceGenomeIndex : cellRangerReferenceGenomeIndex,
                requester            : otpUser,
                expectedCells        : cells,
        ]).save(flush: false)

        return cellRangerMergingWorkPackage
    }

    SingleCellBamFile createSingleCellBamFile(CellRangerMergingWorkPackage cellRangerMergingWorkPackage) {
        SingleCellBamFile singleCellBamFile = new SingleCellBamFile([
                workPackage            : cellRangerMergingWorkPackage,
                seqTracks              : cellRangerMergingWorkPackage.seqTracks.collect() as Set,
                numberOfMergedLanes    : cellRangerMergingWorkPackage.seqTracks.size(),
                coverage               : 35,
                coverageWithN          : 35,
                dateFromFileSystem     : new Date(),
                workDirectoryName      : singleCellBamFileService.buildWorkDirectoryName(cellRangerMergingWorkPackage, 0),
                md5sum                 : "0" * 32,
                fileExists             : true,
                fileSize               : 100,
                fileOperationStatus    : AbstractBamFile.FileOperationStatus.PROCESSED,
                qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
                qcTrafficLightStatus   : AbstractBamFile.QcTrafficLightStatus.QC_PASSED,
                comment                : createComment(),
        ]).save(flush: false)

        cellRangerMergingWorkPackage.bamFileInProjectFolder = singleCellBamFile
        cellRangerMergingWorkPackage.save(flush: false)

        QualityAssessmentMergedPass qualityAssessmentMergedPass = new QualityAssessmentMergedPass([
                abstractBamFile: singleCellBamFile,
        ]).save(flush: false)

        new CellRangerQualityAssessment([
                qualityAssessmentMergedPass              : qualityAssessmentMergedPass,
                referenceLength                          : null,
                estimatedNumberOfCells                   : 1234,
                meanReadsPerCell                         : 123456,
                medianGenesPerCell                       : 1234,
                numberOfReads                            : 123456789,
                validBarcodes                            : 12.3,
                sequencingSaturation                     : 45.6,
                q30BasesInBarcode                        : 78.9,
                q30BasesInRnaRead                        : 98.7,
                q30BasesInUmi                            : 65.4,
                readsMappedToGenome                      : 32.1,
                readsMappedConfidentlyToGenome           : 11.1,
                readsMappedConfidentlyToIntergenicRegions: 2.2,
                readsMappedConfidentlyToIntronicRegions  : 33.3,
                readsMappedConfidentlyToExonicRegions    : 44.4,
                readsMappedConfidentlyToTranscriptome    : 55.5,
                readsMappedAntisenseToGene               : 6.6,
                fractionReadsInCells                     : 77.7,
                totalGenesDetected                       : 54321,
                medianUmiCountsPerCell                   : 12345,
        ]).save(flush: false)

        singleCellBamFiles << singleCellBamFile
        return singleCellBamFile
    }

    WorkflowArtefact createRoddyWorkflowArtefactWithDependencies(MergingWorkPackage mergingWorkPackage) {
        String name = "${mergingWorkPackage.sample} ${mergingWorkPackage.seqType} ${mergingWorkPackage.antibodyTarget ?: ''} ${mergingWorkPackage.libraryPreparationKit ?: ''} ${mergingWorkPackage.referenceGenome}"

        WorkflowRun workflowRun = createWorkflowRun(
                "/tmp/roddy",
                workflowPanCancer,
                name,
        ).save(flush: false)

        mergingWorkPackage.seqTracks.each {
            createWorkflowRunInputArtefact(workflowRun, it.workflowArtefact, "${AlignmentWorkflow.INPUT_FASTQ} ${it.id}")
        }

        return createWorkflowArtefact(
                workflowRun,
                AlignmentWorkflow.OUTPUT_BAM,
                name
        ).save(flush: false)
    }

    RoddyBamFile createRoddyBamFile(MergingWorkPackage mergingWorkPackage) {
        RoddyWorkflowConfig config = findOrCreateRoddyWorkflowConfig(mergingWorkPackage)

        WorkflowArtefact workflowArtefact = createRoddyWorkflowArtefactWithDependencies(mergingWorkPackage)

        RoddyBamFile roddyBamFile = new RoddyBamFile([
                workPackage            : mergingWorkPackage,
                seqTracks              : mergingWorkPackage.seqTracks.collect() as Set,
                numberOfMergedLanes    : mergingWorkPackage.seqTracks.size(),
                coverage               : 35,
                coverageWithN          : 35,
                config                 : config,
                dateFromFileSystem     : new Date(),
                workDirectoryName      : ".merging_0",
                md5sum                 : "0" * 32,
                fileExists             : true,
                fileSize               : 100,
                fileOperationStatus    : AbstractBamFile.FileOperationStatus.PROCESSED,
                qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
                qcTrafficLightStatus   : AbstractBamFile.QcTrafficLightStatus.QC_PASSED,
                comment                : createComment(),
                workflowArtefact       : workflowArtefact,
        ]).save(flush: false)

        mergingWorkPackage.bamFileInProjectFolder = roddyBamFile
        mergingWorkPackage.save(flush: false)

        QualityAssessmentMergedPass qualityAssessmentMergedPass = new QualityAssessmentMergedPass([
                abstractBamFile: roddyBamFile,
                identifier     : 0,
        ]).save(flush: false)

        createRoddyMergedBamQaAll(qualityAssessmentMergedPass)
        PanCancerNoBedFileQaOverviewService.CHROMOSOMES_XY.each {
            createRoddyMergedBamQaChromosome(qualityAssessmentMergedPass, it)
        }
        roddyBamFiles << roddyBamFile
        return roddyBamFile
    }

    RnaRoddyBamFile createRnaRoddyBamFile(MergingWorkPackage mergingWorkPackage) {
        RoddyWorkflowConfig config = findOrCreateRoddyWorkflowConfig(mergingWorkPackage)

        WorkflowArtefact workflowArtefact = createRoddyWorkflowArtefactWithDependencies(mergingWorkPackage)

        RoddyBamFile roddyBamFile = new RnaRoddyBamFile([
                workPackage            : mergingWorkPackage,
                seqTracks              : mergingWorkPackage.seqTracks.collect() as Set,
                numberOfMergedLanes    : mergingWorkPackage.seqTracks.size(),
                coverage               : 35,
                coverageWithN          : 35,
                config                 : config,
                dateFromFileSystem     : new Date(),
                workDirectoryName      : ".merging_0",
                md5sum                 : "0" * 32,
                fileExists             : true,
                fileSize               : 100,
                fileOperationStatus    : AbstractBamFile.FileOperationStatus.PROCESSED,
                qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
                qcTrafficLightStatus   : AbstractBamFile.QcTrafficLightStatus.QC_PASSED,
                comment                : createComment(),
                workflowArtefact       : workflowArtefact,
        ]).save(flush: false)

        mergingWorkPackage.bamFileInProjectFolder = roddyBamFile
        mergingWorkPackage.save(flush: false)

        QualityAssessmentMergedPass qualityAssessmentMergedPass = new QualityAssessmentMergedPass([
                abstractBamFile: roddyBamFile,
                identifier     : 0,
        ]).save(flush: false)

        createRnaRoddyMergedBamQaAll(qualityAssessmentMergedPass)
        rnaRoddyBamFiles << roddyBamFile
        return roddyBamFile
    }

    ReferenceGenomeIndex findOrCreateCellRangerReferenceGenomeIndex() {
        ReferenceGenomeIndex foundIndex = atMostOneElement(ReferenceGenomeIndex.findAllByReferenceGenome(singleCellReferenceGenomeHuman))
        if (foundIndex) {
            return foundIndex
        }
        ToolName tool = atMostOneElement(ToolName.findAllByName("CELL_RANGER"))
        return new ReferenceGenomeIndex(
                toolName: tool,
                referenceGenome: singleCellReferenceGenomeHuman,
                path: '1.2.0',
                indexToolVersion: '1.2.0',
        ).save(flush: false)
    }

    CellRangerConfig findOrCreateCellRangerConfig(SeqType seqType, Pipeline pipeline) {

        CellRangerConfig foundCellRangerConfig = atMostOneElement(CellRangerConfig.findAllByProjectAndSeqTypeAndPipelineAndObsoleteDateIsNull(
                project, seqType, pipeline
        ))
        if (foundCellRangerConfig) {
            return foundCellRangerConfig
        }
        return new CellRangerConfig([
                project       : project,
                seqType       : seqType,
                pipeline      : pipeline,
                programVersion: "1.2.3-4",
        ]).save(flush: true)
    }

    RoddyWorkflowConfig findOrCreateRoddyWorkflowConfig(MergingWorkPackage mergingWorkPackage) {
        RoddyWorkflowConfig searchRoddyWorkflowConfig = atMostOneElement(RoddyWorkflowConfig.findAllByProjectAndSeqTypeAndPipelineAndObsoleteDateIsNull(
                mergingWorkPackage.project, mergingWorkPackage.seqType, mergingWorkPackage.pipeline))
        if (searchRoddyWorkflowConfig) {
            return searchRoddyWorkflowConfig
        }
        String programVersion = "1.2.3-4"
        String configVersion = "v1_0"

        String file = RoddyWorkflowConfig.getStandardConfigFile(
                mergingWorkPackage.project,
                mergingWorkPackage.pipeline.name,
                mergingWorkPackage.seqType,
                programVersion,
                configVersion,
        ).toString()

        if (createFilesOnFilesystem) {
            Path path = fileSystemService.remoteFileSystem.getPath(file)
            fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(path.parent)
            fileService.createFileWithContent(path, "someDummyContent", null, FileService.DEFAULT_FILE_PERMISSION, true)
        }
        return new RoddyWorkflowConfig([
                project              : mergingWorkPackage.project,
                seqType              : mergingWorkPackage.seqType,
                pipeline             : mergingWorkPackage.pipeline,
                programVersion       : "${mergingWorkPackage.pipeline.name}:${programVersion}",
                configFilePath       : file,
                configVersion        : configVersion,
                nameUsedInConfig     : "name",
                md5sum               : "0" * 32,
                adapterTrimmingNeeded: mergingWorkPackage.seqType.isWgbs() || mergingWorkPackage.seqType.isRna() || mergingWorkPackage.seqType.isChipSeq(),
        ]).save(flush: true)
    }

    Comment createComment() {
        return new Comment([
                comment         : "comment_${commentCounter++}",
                author          : "author",
                modificationDate: new Date(),
        ]).save(flush: false)
    }

    RoddyMergedBamQa createRoddyMergedBamQaAll(QualityAssessmentMergedPass qualityAssessmentMergedPass) {
        return createRoddyMergedBamQa(qualityAssessmentMergedPass, [
                chromosome                     : RoddyQualityAssessment.ALL,
                totalMappedReadCounter         : 100,
                pairedRead1                    : 100,
                withMateMappedToDifferentChrMaq: 100,
                withItselfAndMateMapped        : 100,
                insertSizeMedian               : 100,
                qcFailedReads                  : 100,
                withMateMappedToDifferentChr   : 100,
                singletons                     : 100,
                insertSizeCV                   : 100,
                pairedRead2                    : 100,
                totalReadCounter               : 100,
                pairedInSequencing             : 100,
                duplicates                     : 100,
                insertSizeSD                   : 100,
                properlyPaired                 : 100,
        ])
    }

    RnaQualityAssessment createRnaRoddyMergedBamQaAll(QualityAssessmentMergedPass qualityAssessmentMergedPass) {
        boolean isPaired = qualityAssessmentMergedPass.mergingWorkPackage.seqType.libraryLayout.mateCount == 2
        return new RnaQualityAssessment([
                qualityAssessmentMergedPass      : qualityAssessmentMergedPass,
                chromosome                       : RoddyQualityAssessment.ALL,
                qcBasesMapped                    : 0,
                totalReadCounter                 : 0,
                qcFailedReads                    : 0,
                duplicates                       : 0,
                totalMappedReadCounter           : 0,
                pairedInSequencing               : 0,
                pairedRead1                      : 0,
                pairedRead2                      : 0,
                properlyPaired                   : isPaired ? 0 : null,
                withItselfAndMateMapped          : 0,
                withMateMappedToDifferentChr     : 0,
                withMateMappedToDifferentChrMaq  : 0,
                singletons                       : isPaired ? 0 : null,
                insertSizeMedian                 : 0,
                insertSizeSD                     : 0,
                referenceLength                  : 1,
                genomeWithoutNCoverageQcBases    : null,
                insertSizeCV                     : null,
                insertSizeMedian                 : null,
                pairedRead1                      : null,
                pairedRead2                      : null,
                percentageMatesOnDifferentChr    : null,
                referenceLength                  : null,
                properlyPairedPercentage         : isPaired ? 0 : null,
                singletonsPercentage             : isPaired ? 0 : null,
                alternativeAlignments            : 0,
                baseMismatchRate                 : 0.0123456789,
                chimericPairs                    : 0,
                cumulGapLength                   : 123456,
                end1Antisense                    : 12345678,
                end1MappingRate                  : 0.1234567,
                end1MismatchRate                 : isPaired ? 0.0123456789 : null,
                end1PercentageSense              : isPaired ? 0.12345678 : null,
                end1Sense                        : 123456,
                end2Antisense                    : 123456,
                end2MappingRate                  : 0.1234567,
                end2MismatchRate                 : isPaired ? 0.123456789 : null,
                end2PercentageSense              : isPaired ? 12.34567 : null,
                end2Sense                        : 12345678,
                estimatedLibrarySize             : 12345678,
                exonicRate                       : 0.12345678,
                expressionProfilingEfficiency    : 0.1234567,
                failedVendorQCCheck              : 0,
                fivePNorm                        : 0.12345678,
                gapPercentage                    : 0.123456789,
                genesDetected                    : 12345,
                insertSizeMean                   : 123,
                intergenicRate                   : 0.123456789,
                intragenicRate                   : 0.1234567,
                intronicRate                     : 0.12345678,
                mapped                           : 12345678,
                mappedPairs                      : 12345678,
                mappedRead1                      : 12345678,
                mappedRead2                      : 12345678,
                mappedUnique                     : 12345678,
                mappedUniqueRateOfTotal          : 0.12345678,
                mappingRate                      : 0.1234567,
                meanCV                           : 0.12345678,
                meanPerBaseCov                   : 12.34567,
                noCovered5P                      : 123,
                numGaps                          : 123,
                rRNARate                         : 1.234567E-8,
                rRNAReads                        : 123456,
                readLength                       : 123,
                secondaryAlignments              : 0,
                splitReads                       : 12345678,
                supplementaryAlignments          : 0,
                threePNorm                       : 0.12345678,
                totalPurityFilteredReadsSequenced: 123456789,
                transcriptsDetected              : 123456,
                uniqueRateofMapped               : 0.1234567,
                unpairedReads                    : 0,
        ]).save(flush: false)
    }

    RoddyMergedBamQa createRoddyMergedBamQaChromosome(QualityAssessmentMergedPass qualityAssessmentMergedPass, String chromosome) {
        return createRoddyMergedBamQa(qualityAssessmentMergedPass, [
                chromosome: chromosome,
        ])
    }

    RoddyMergedBamQa createRoddyMergedBamQa(QualityAssessmentMergedPass qualityAssessmentMergedPass, Map map) {
        return new RoddyMergedBamQa([
                qualityAssessmentMergedPass  : qualityAssessmentMergedPass,
                referenceLength              : 100,
                genomeWithoutNCoverageQcBases: 100,
        ] + map).save(flush: false)
    }

    SamplePair createSamplePair(MergingWorkPackage disease, MergingWorkPackage control) {
        SamplePair samplePair = new SamplePair([
                mergingWorkPackage1     : disease,
                mergingWorkPackage2     : control,
                snvProcessingStatus     : SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED,
                indelProcessingStatus   : SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED,
                sophiaProcessingStatus  : SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED,
                aceseqProcessingStatus  : SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED,
                runYapsaProcessingStatus: SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED,
        ]).save(flush: false)
        println "  - samplePair: ${samplePair}"
        return samplePair
    }

    RoddySnvCallingInstance createRoddySnvCallingInstance(SamplePair samplePair) {
        if (!snvSeqTypes.contains(samplePair.seqType)) {
            return null
        }
        RoddyWorkflowConfig config = getOrCreateConfig(samplePair, Pipeline.Name.RODDY_SNV)
        String instanceName = "results_${config.programVersion.replaceAll(":", "-")}_${config.configVersion}_${TimeFormats.DATE_TIME_SECONDS_DASHES.getFormattedDate(new Date())}"
        BamFilePairAnalysis analysis = new RoddySnvCallingInstance([
                samplePair        : samplePair,
                instanceName      : instanceName,
                config            : config,
                sampleType1BamFile: samplePair.mergingWorkPackage1.bamFileInProjectFolder,
                sampleType2BamFile: samplePair.mergingWorkPackage2.bamFileInProjectFolder,
                processingState   : AnalysisProcessingStates.FINISHED,
        ]).save(flush: false)
        println "    - snv: ${analysis}"
        roddySnvCallingInstances << analysis
        return analysis
    }

    IndelCallingInstance createIndelCallingInstance(SamplePair samplePair) {
        if (!indelSeqTypes.contains(samplePair.seqType)) {
            return null
        }
        RoddyWorkflowConfig config = getOrCreateConfig(samplePair, Pipeline.Name.RODDY_INDEL)
        String instanceName = "results_${config.programVersion.replaceAll(":", "-")}_${config.configVersion}_${TimeFormats.DATE_TIME_SECONDS_DASHES.getFormattedDate(new Date())}"
        BamFilePairAnalysis analysis = new IndelCallingInstance([
                samplePair        : samplePair,
                instanceName      : instanceName,
                config            : config,
                sampleType1BamFile: samplePair.mergingWorkPackage1.bamFileInProjectFolder,
                sampleType2BamFile: samplePair.mergingWorkPackage2.bamFileInProjectFolder,
                processingState   : AnalysisProcessingStates.FINISHED,
        ]).save(flush: false)
        println "    - indel: ${analysis}"
        indelCallingInstances << analysis

        new IndelQualityControl([
                indelCallingInstance : analysis,
                file                 : "/tmp/file",
                numIndels            : 10,
                numIns               : 20,
                numDels              : 30,
                numSize1_3           : 40,
                numSize4_10          : 50,
                numSize11plus        : 60,
                numInsSize1_3        : 70,
                numInsSize4_10       : 80,
                numInsSize11plus     : 90,
                numDelsSize1_3       : 100,
                numDelsSize4_10      : 110,
                numDelsSize11plus    : 120,
                percentIns           : 130.123,
                percentDels          : 140.123,
                percentSize1_3       : 150.123,
                percentSize4_10      : 160.123,
                percentSize11plus    : 170.123,
                percentInsSize1_3    : 180.123,
                percentInsSize4_10   : 190.123,
                percentInsSize11plus : 200.123,
                percentDelsSize1_3   : 210.123,
                percentDelsSize4_10  : 220.123,
                percentDelsSize11plus: 230.123,
        ]).save(flush: false)

        new IndelSampleSwapDetection([
                indelCallingInstance                            : analysis,
                somaticSmallVarsInTumorCommonInGnomADPer        : 500,
                somaticSmallVarsInControlCommonInGnomad         : 510,
                tindaSomaticAfterRescue                         : 520,
                somaticSmallVarsInControlInBiasPer              : 530,
                somaticSmallVarsInTumorPass                     : 540,
                pid                                             : analysis.individual.pid,
                somaticSmallVarsInControlPass                   : 550,
                somaticSmallVarsInControlPassPer                : 560,
                tindaSomaticAfterRescueMedianAlleleFreqInControl: 560.123,
                somaticSmallVarsInTumorInBiasPer                : 570.123,
                somaticSmallVarsInControlCommonInGnomadPer      : 580,
                somaticSmallVarsInTumorInBias                   : 590,
                somaticSmallVarsInControlCommonInGnomasPer      : 600,
                germlineSNVsHeterozygousInBothRare              : 610,
                germlineSmallVarsHeterozygousInBothRare         : 620,
                tindaGermlineRareAfterRescue                    : 630,
                somaticSmallVarsInTumorCommonInGnomad           : 640,
                somaticSmallVarsInControlInBias                 : 650,
                somaticSmallVarsInControl                       : 660,
                somaticSmallVarsInTumor                         : 670,
                germlineSNVsHeterozygousInBoth                  : 680,
                somaticSmallVarsInTumorPassPer                  : 690.123,
                somaticSmallVarsInTumorCommonInGnomadPer        : 700,
        ]).save(flush: false)

        return analysis
    }

    SophiaInstance createSophiaInstance(SamplePair samplePair) {
        if (!sophiaSeqTypes.contains(samplePair.seqType)) {
            return null
        }
        RoddyWorkflowConfig config = getOrCreateConfig(samplePair, Pipeline.Name.RODDY_SOPHIA)
        String instanceName = "results_${config.programVersion.replaceAll(":", "-")}_${config.configVersion}_${TimeFormats.DATE_TIME_SECONDS_DASHES.getFormattedDate(new Date())}"
        BamFilePairAnalysis analysis = new SophiaInstance([
                samplePair        : samplePair,
                instanceName      : instanceName,
                config            : config,
                sampleType1BamFile: samplePair.mergingWorkPackage1.bamFileInProjectFolder,
                sampleType2BamFile: samplePair.mergingWorkPackage2.bamFileInProjectFolder,
                processingState   : AnalysisProcessingStates.FINISHED,
        ]).save(flush: false)
        println "    - sophia: ${analysis}"
        sophiaInstances << analysis

        new SophiaQc([
                sophiaInstance                       : analysis,
                controlMassiveInvPrefilteringLevel   : 10,
                tumorMassiveInvFilteringLevel        : 20,
                rnaContaminatedGenesMoreThanTwoIntron: "arbitraryGeneName1,arbitraryGeneName2",
                rnaContaminatedGenesCount            : 30,
                rnaDecontaminationApplied            : true,
        ]).save(flush: false)

        return analysis
    }

    AceseqInstance createAceseqInstance(SamplePair samplePair) {
        if (!aceseqSeqTypes.contains(samplePair.seqType)) {
            return null
        }
        RoddyWorkflowConfig config = getOrCreateConfig(samplePair, Pipeline.Name.RODDY_ACESEQ)
        String instanceName = "results_${config.programVersion.replaceAll(":", "-")}_${config.configVersion}_${TimeFormats.DATE_TIME_SECONDS_DASHES.getFormattedDate(new Date())}"
        BamFilePairAnalysis analysis = new AceseqInstance([
                samplePair        : samplePair,
                instanceName      : instanceName,
                config            : config,
                sampleType1BamFile: samplePair.mergingWorkPackage1.bamFileInProjectFolder,
                sampleType2BamFile: samplePair.mergingWorkPackage2.bamFileInProjectFolder,
                processingState   : AnalysisProcessingStates.FINISHED,
        ]).save(flush: false)
        println "    - aceseq: ${analysis}"
        aceseqInstances << analysis

        new AceseqQc([
                aceseqInstance  : analysis,
                number          : 1,
                tcc             : 20,
                ploidyFactor    : '1.0',
                ploidy          : 30,
                goodnessOfFit   : 40,
                gender          : 'M',
                solutionPossible: 50,
        ]).save(flush: false)

        return analysis
    }

    RunYapsaInstance createRunYapsaInstance(SamplePair samplePair) {
        if (!runYapsaSeqTypes.contains(samplePair.seqType)) {
            return null
        }
        RunYapsaConfig config = getOrCreateRunYapsaConfig(samplePair, Pipeline.Name.RUN_YAPSA)

        String instanceName = "runYapsa_${config.programVersion.replaceAll("/", "-")}_${TimeFormats.DATE_TIME_SECONDS_DASHES.getFormattedDate(new Date())}"
        BamFilePairAnalysis analysis = new RunYapsaInstance([
                samplePair        : samplePair,
                instanceName      : instanceName,
                config            : config,
                sampleType1BamFile: samplePair.mergingWorkPackage1.bamFileInProjectFolder,
                sampleType2BamFile: samplePair.mergingWorkPackage2.bamFileInProjectFolder,
                processingState   : AnalysisProcessingStates.FINISHED,
        ]).save(flush: false)
        println "    - runyapsa: ${analysis}"
        runYapsaInstances << analysis

        return analysis
    }

    ConfigPerProjectAndSeqType getOrCreateConfig(SamplePair samplePair, Pipeline.Name pipelineName) {
        Pipeline pipeline = pipelineName.pipeline
        RoddyWorkflowConfig config = atMostOneElement(RoddyWorkflowConfig.findAllByProjectAndSeqTypeAndPipelineAndObsoleteDateAndIndividual(
                samplePair.project, samplePair.seqType, pipeline, null, null))
        if (config) {
            return config
        }
        String nameUsedInConfig = "${pipelineName.name()}_${samplePair.seqType.roddyName}_${samplePair.seqType.libraryLayout}"
        return new RoddyWorkflowConfig([project              : samplePair.project,
                                        seqType              : samplePair.seqType,
                                        pipeline             : pipeline,
                                        programVersion       : "programVersion:1.1.1",
                                        configVersion        : "v1_0",
                                        adapterTrimmingNeeded: false,
                                        nameUsedInConfig     : nameUsedInConfig,
                                        md5sum               : HelperUtils.getRandomMd5sum(),
                                        configFilePath       : "/dev/null/${nameUsedInConfig}_${samplePair.id}"
        ]).save(flush: false)
    }

    RunYapsaConfig getOrCreateRunYapsaConfig(SamplePair samplePair, Pipeline.Name pipelineName) {
        Pipeline pipeline = pipelineName.pipeline
        RunYapsaConfig config = atMostOneElement(RunYapsaConfig.findAllByProjectAndSeqTypeAndPipelineAndObsoleteDate(
                samplePair.project, samplePair.seqType, pipeline, null))
        if (config) {
            return config
        }
        return new RunYapsaConfig([
                project       : samplePair.project,
                seqType       : samplePair.seqType,
                pipeline      : pipeline,
                programVersion: 'yapsa-devel/b765fa8',
                previousConfig: null,
        ]).save(flush: true)
    }

    WorkflowRun createWorkflowRun(String workDirectory, Workflow workflow, String name) {
        return new WorkflowRun([
                workDirectory   : workDirectory,
                state           : WorkflowRun.State.SUCCESS,
                project         : project,
                combinedConfig  : '{}',
                priority        : processingPriority,
                workflowSteps   : [],
                workflow        : workflow,
                displayName     : name,
                shortDisplayName: name,
        ]).save(flush: false)
    }

    WorkflowArtefact createWorkflowArtefact(WorkflowRun workflowRun, String outputRole, String name) {
        return new WorkflowArtefact([
                producedBy  : workflowRun,
                state       : WorkflowArtefact.State.SUCCESS,
                outputRole  : outputRole,
                artefactType: ArtefactType.FASTQ,
                displayName : name,
        ]).save(flush: true)
    }

    WorkflowRunInputArtefact createWorkflowRunInputArtefact(WorkflowRun workflowRun, WorkflowArtefact workflowArtefact, String role) {
        return new WorkflowRunInputArtefact([
                workflowRun     : workflowRun,
                workflowArtefact: workflowArtefact,
                role            : role,
        ]).save(flush: true)
    }
}

enum MixedInSpecies {
    NONE,
    MOUSE,
}

Project.withTransaction {
    ExampleData exampleData = new ExampleData([
            abstractBamFileService        : ctx.abstractBamFileService,
            fastqcDataFilesService        : ctx.fastqcDataFilesService,
            fileService                   : ctx.fileService,
            fileSystemService             : ctx.fileSystemService,
            lsdfFilesService              : ctx.lsdfFilesService,
            snvCallingService             : ctx.snvCallingService,
            indelCallingService           : ctx.indelCallingService,
            sophiaService                 : ctx.sophiaService,
            aceseqService                 : ctx.aceseqService,
            cellRangerConfigurationService: ctx.cellRangerConfigurationService,
            singleCellBamFileService      : ctx.singleCellBamFileService,
            cellRangerWorkflowService     : ctx.cellRangerWorkflowService,
            singleCellMappingFileService  : ctx.singleCellMappingFileService,
            documentService               : ctx.documentService,
            roddyBamFileService           : ctx.roddyBamFileService,
            rnaRoddyBamFileService        : ctx.rnaRoddyBamFileService,
            runYapsaService               : ctx.runYapsaService,
            seqTypeService                : ctx.seqTypeService,
            roddyConfigService            : ctx.roddyConfigService,
    ])

    exampleData.init()
    exampleData.createObjects()
    exampleData.createFiles()
    println "script finished"
}
''
