/*
 * Copyright 2011-2021 The OTP authors
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

import de.dkfz.tbi.otp.administration.*
import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.bamfiles.SingleCellBamFileService
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerConfig
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerConfigurationService
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerWorkflowService
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaQc
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.taxonomy.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority
import de.dkfz.tbi.util.TimeFormats

import java.nio.file.FileSystem
import java.nio.file.Path
import java.text.DecimalFormat
import java.text.NumberFormat

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

/**
 * script to create example data.
 *
 * Its generates:
 * - fastq
 * - fastqc
 * - bam files
 *   - only PanCan supported yet: WGS, WES, WGBS, and WGBS_TAG
 *   - would create the same also for other seqtypes
 * - snv
 * - indel
 * - sophia
 *
 * It doesn't expect any existing data except the default SeqType's.
 *
 * The script can also generates dummy files for the objects. That is limited to the files OTP is aware of.
 * The real pipelines generates much more files.
 *
 * For file generation (on per default), the selected {@link ExampleData#realmName} needs to be valid for remote access.
 */
class ExampleData {
//------------------------------
//input

    /**
     * Name of the project for the example data. If it not exist, it is created.
     */
    String projectName = "ExampleProject"

    /**
     * Name of the realm to use. If it not exist, it is created in {@link #findOrCreateRealm()}.
     */
    String realmName = "dev_realm"

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
     * Should data files be marked as existing when no files were created?
     * Only taking effect if createFilesOnFilesystem is false.
     *
     * Usually there is no need to set this to false, only if you want to create data with missing files.
     */
    boolean markDataFilesAsExisting = true

    /**
     * The SeqTypes for which data is to be created.
     */
    List<SeqType> seqTypes = [
            SeqTypeService.exomePairedSeqType,
            SeqTypeService.wholeGenomePairedSeqType,
            SeqTypeService.wholeGenomeBisulfitePairedSeqType,
            SeqTypeService.wholeGenomeBisulfiteTagmentationPairedSeqType,

    ]

    /**
     * The SeqTypes for which single cell data is to be created.
     */
    List<SeqType> singleCellSeqTypes = [
            SeqTypeService.'10xSingleCellRnaSeqType'
    ]

    /**
     * The disease SampleType names for which data is to be created.
     *
     * If analysis are created, each diseaseSampleTypeNames is combined with each controlSampleTypeNames.
     */
    List<String> diseaseSampleTypeNames = [
            "tumor01",
            "tumor02",
    ]

    /**
     * The control SampleType names for which data is to be created.
     *
     * If analysis are created, each diseaseSampleTypeNames is combined with each controlSampleTypeNames.
     */
    List<String> controlSampleTypeNames = [
            "control01",
    ]

//------------------------------
//work

    FastqcDataFilesService fastqcDataFilesService

    FileService fileService

    FileSystemService fileSystemService

    LsdfFilesService lsdfFilesService

    SnvCallingService snvCallingService

    IndelCallingService indelCallingService

    SophiaService sophiaService

    AceseqService aceseqService

    CellRangerConfigurationService cellRangerConfigurationService

    SingleCellBamFileService singleCellBamFileService

    CellRangerWorkflowService cellRangerWorkflowService

    DocumentService documentService

    static final List<String> chromosomeXY = [
            "X",
            "Y",
    ]

    Project project
    FastqImportInstance fastqImportInstance
    FileType fileType
    LibraryPreparationKit libraryPreparationKit
    ProcessingPriority processingPriority
    Realm realm
    SpeciesWithStrain speciesWithStrain
    ReferenceGenome referenceGenome
    ReferenceGenome singleCellReferenceGenome
    SeqCenter seqCenter
    SeqPlatform seqPlatform
    SeqPlatformGroup seqPlatformGroup
    SoftwareTool softwareTool

    List<SampleType> diseaseSampleTypes = []
    List<SampleType> controlSampleTypes = []
    List<RoddyBamFile> roddyBamFiles = []
    List<SingleCellBamFile> singleCellBamFiles = []
    List<RoddySnvCallingInstance> roddySnvCallingInstances = []
    List<IndelCallingInstance> indelCallingInstances = []
    List<SophiaInstance> sophiaInstances = []
    List<AceseqInstance> aceseqInstances = []
    List<DataFile> dataFiles = []
    List<FastqcProcessedFile> fastqcProcessedFiles = []

    List<SeqType> analyseAbleSeqType = []

    void init() {
        diseaseSampleTypes = diseaseSampleTypeNames.collect {
            findOrCreateSampleType(it)
        }
        controlSampleTypes = controlSampleTypeNames.collect {
            findOrCreateSampleType(it)
        }
        analyseAbleSeqType = SeqTypeService.allAnalysableSeqTypes.findAll {
            seqTypes.contains(it)
        }

        processingPriority = findOrCreateProcessingPriority()
        fileType = findOrCreateFileType()
        libraryPreparationKit = findOrCreateLibraryPreparationKit()
        realm = findOrCreateRealm()
        speciesWithStrain = findOrCreateSpeciesWithStrain()
        referenceGenome = findOrCreateReferenceGenome("1KGRef_PhiX")
        singleCellReferenceGenome = findOrCreateReferenceGenome("hg_GRCh38")
        seqCenter = findOrCreateSeqCenter()
        seqPlatform = findOrCreateSeqPlatform()
        seqPlatformGroup = findOrCreateSeqPlatformGroup()
        softwareTool = findOrCreateSoftwareTool()

        fastqImportInstance = createFastqImportInstance()
        createMetaDataFile()
        project = findOrCreateProject(projectName)
        diseaseSampleTypes.each { SampleType sampleType ->
            findOrCreateSampleTypePerProject(sampleType, SampleTypePerProject.Category.DISEASE)
        }
        controlSampleTypes.each { SampleType sampleType ->
            findOrCreateSampleTypePerProject(sampleType, SampleTypePerProject.Category.CONTROL)
        }
        seqTypes.each {
            findOrCreateMergingCriteria(it)
        }
        findOrCreateProcessingThresholds()
        createDocumentTestData()
    }

    void createObjects() {
        (1..individualCount).each {
            String pid = "example_${Individual.count() + 1}"
            println "- pid: ${pid}"
            Individual individual = createIndividual(project, pid)
            println "- individual: ${individual}"
            Map<SeqType, List<AbstractMergedBamFile>> diseaseBamFiles = diseaseSampleTypes.collectMany { SampleType sampleType ->
                createSampleWithSeqTracksAndBamFile(individual, sampleType)
            }.groupBy {
                it.seqType
            }
            Map<SeqType, List<AbstractMergedBamFile>> controlBamFiles = controlSampleTypes.collectMany { SampleType sampleType ->
                createSampleWithSeqTracksAndBamFile(individual, sampleType)
            }.groupBy {
                it.seqType
            }
            diseaseSampleTypes.collectMany { SampleType sampleType ->
                createSingleCellSampleWithSeqTracksAndBamFile(individual, sampleType)
            }
            controlSampleTypes.collectMany { SampleType sampleType ->
                createSingleCellSampleWithSeqTracksAndBamFile(individual, sampleType)
            }
            analyseAbleSeqType.each { SeqType seqType ->
                diseaseBamFiles[seqType].each { AbstractMergedBamFile diseaseBamFile ->
                    controlBamFiles[seqType].each { AbstractMergedBamFile controlBamFile ->
                        SamplePair samplePair = createSamplePair(diseaseBamFile.mergingWorkPackage, controlBamFile.mergingWorkPackage)
                        createRoddySnvCallingInstance(samplePair)
                        createIndelCallingInstance(samplePair)
                        createSophiaInstance(samplePair)
                        createAceseqInstance(samplePair)
                    }
                }
            }
        }
    }

    void createFiles() {
        if (createFilesOnFilesystem) {
            createDataFilesFilesOnFilesystem()
            createFastqcFilesOnFilesystem()
            createBamFilesOnFilesystem()
            createSnvFilesOnFilesystem()
            createIndelFilesOnFilesystem()
            createSophiaFilesOnFilesystem()
            createAceseqFilesOnFilesystem()
            createCellRangerFilesOnFilesystem()
        } else {
            println "Skip creating dummy files/directories on file system"
        }
    }

    void createDataFilesFilesOnFilesystem() {
        println "creating dummy datafiles on file system"
        dataFiles.each { DataFile dataFile ->
            Path directPath = lsdfFilesService.getFileFinalPathAsPath(dataFile)
            Path directPathMd5sum = directPath.resolveSibling("${dataFile.fileName}.md5sum")
            Path vbpPath = lsdfFilesService.getFileViewByPidPathAsPath(dataFile)
            [
                    directPath,
                    directPathMd5sum,
            ].each {
                fileService.createFileWithContent(it, it.toString(), realm)
            }
            fileService.createLink(vbpPath, directPath, realm)
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
                fileService.createFileWithContent(it, it.toString(), realm)
            }
        }
    }

    void createBamFilesOnFilesystem() {
        println "creating dummy bam files on file system"
        FileSystem fileSystem = fileSystemService.getRemoteFileSystem(realm)

        roddyBamFiles.each { RoddyBamFile bam ->
            Map<File, File> filesMap = [
                    (bam.finalBamFile)   : bam.workBamFile,
                    (bam.finalBaiFile)   : bam.workBaiFile,
                    (bam.finalMd5sumFile): bam.workMd5sumFile,
            ]

            Map<File, File> dirsMap = [
                    (bam.finalMergedQADirectory)      : bam.workMergedQADirectory,
                    (bam.finalExecutionStoreDirectory): bam.workExecutionStoreDirectory,
            ]
            List<File> dirs = [bam.workDirectory]
            dirs.addAll(bam.workSingleLaneQADirectories.values())

            if (bam.seqType.isWgbs()) {
                filesMap[bam.finalMetadataTableFile] = bam.workMetadataTableFile
                if (bam.containedSeqTracks*.libraryDirectoryName.unique().size() > 1) {
                    dirs.addAll(bam.workLibraryQADirectories.values())
                    dirs.addAll(bam.workLibraryMethylationDirectories.values())
                }
            }

            dirs.each {
                Path path = fileSystem.getPath(it.toString())
                fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(path, realm)
            }

            dirsMap.each {
                Path pathFinal = fileSystem.getPath(it.key.toString())
                Path pathWork = fileSystem.getPath(it.value.toString())
                fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(pathWork, realm)
                fileService.createLink(pathFinal, pathWork, realm)
            }

            filesMap.each {
                Path pathFinal = fileSystem.getPath(it.key.toString())
                Path pathWork = fileSystem.getPath(it.value.toString())
                fileService.createFileWithContent(pathWork, pathWork.toString(), realm)
                fileService.createLink(pathFinal, pathWork, realm)
            }
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
                fileService.createFileWithContent(it, it.toString(), realm)
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
                fileService.createFileWithContent(it, it.toString(), realm)
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
                fileService.createFileWithContent(it, it.toString(), realm)
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
                    //files for pattern for PlotType.ACESEQ_ALL
                    base.resolve("${aceseqInstance.individual.pid}_plot_1_ALL.png"),
                    base.resolve("${aceseqInstance.individual.pid}_plot_3_ALL.png"),
                    base.resolve("${aceseqInstance.individual.pid}_plot_4_ALL.png"),
                    //files for pattern for PlotType.ACESEQ_EXTRA
                    base.resolve("${plotPrefixAceseqExtra}_1.png"),
                    base.resolve("${plotPrefixAceseqExtra}_3.png"),
                    base.resolve("${plotPrefixAceseqExtra}_5.png"),
            ].each {
                fileService.createFileWithContent(it, it.toString(), realm)
            }
        }
    }

    void createCellRangerFilesOnFilesystem() {
        println "creating dummy cell ranger files on file system"

        singleCellBamFiles.each { SingleCellBamFile bam ->
            Path workdir = singleCellBamFileService.getWorkDirectory(bam)
            fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(workdir, realm)

            Path resultsPath = singleCellBamFileService.getResultDirectory(bam)

            [singleCellBamFileService.getSampleDirectory(bam),
             singleCellBamFileService.getOutputDirectory(bam),
             resultsPath,].each {
                fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(it, realm)
            }

            SingleCellBamFileService.CREATED_RESULT_DIRS.each {
                Path path = resultsPath.resolve(it)
                fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(path, realm)
            }

            SingleCellBamFileService.CREATED_RESULT_FILES.each {
                Path path = resultsPath.resolve(it)
                fileService.createFileWithContent(path, path.toString(), realm)
            }

            cellRangerWorkflowService.linkResultFiles(bam)
        }
    }

    SampleType findOrCreateSampleType(String name) {
        return CollectionUtils.atMostOneElement(SampleType.findAllByName(name)) ?: new SampleType([
                name                   : name,
                specificReferenceGenome: SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT,
        ]).save(flush: true)
    }

    SampleTypePerProject findOrCreateSampleTypePerProject(SampleType sampleType, SampleTypePerProject.Category category) {
        return CollectionUtils.atMostOneElement(SampleTypePerProject.findAllByProjectAndSampleType(project, sampleType)) ?: new SampleTypePerProject([
                project   : project,
                sampleType: sampleType,
                category  : category,
        ]).save(flush: true)
    }

    void createDocumentTestData() {
        Set<DocumentType> documentTypes = documentService.listDocumentTypes()
        documentService.updateDocument(documentTypes[0], "PROJECT_FORM".bytes, "some link", Document.FormatType.TXT)
        documentService.updateDocument(documentTypes[1], "METADATA_TEMPLATE".bytes, "some link", Document.FormatType.TXT)
        documentService.updateDocument(documentTypes[2], "PROCESSING_INFORMATION".bytes, "some link", Document.FormatType.TXT)
    }

    void findOrCreateProcessingThresholds() {
        [
                diseaseSampleTypes,
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
                        ]).save(flush: true)
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
        ]).save(flush: true)
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
                name            : "ExampleLibPrep",
                shortDisplayName: "ExampleLibPrep",
        ]).save(flush: true)
    }

    SpeciesWithStrain findOrCreateSpeciesWithStrain() {
        SpeciesCommonName speciesCommonName = SpeciesCommonName.findByName('Human') ?: new SpeciesCommonName([
                name: 'Human',
        ]).save(flush: true)

        Species species = Species.findBySpeciesCommonNameAndScientificName(speciesCommonName, 'Homo sapiens') ?: new Species([
                speciesCommonName: speciesCommonName,
                scientificName   : 'Homo sapiens',
        ]).save(flush: true)

        Strain strain = Strain.findByName('No strain available') ?: new SpeciesCommonName([
                name: 'No strain available',
        ]).save(flush: true)

        return SpeciesWithStrain.findBySpeciesAndStrain(species, strain) ?: new SpeciesWithStrain([
                species: species,
                strain : strain,
        ]).save(flush: true)
    }

    Realm findOrCreateRealm() {
        return CollectionUtils.atMostOneElement(Realm.findAllByName(realmName)) ?: new Realm([
                name                       : realmName,
                jobScheduler               : Realm.JobScheduler.LSF,
                host                       : "localhost",
                port                       : 22,
                timeout                    : 0,
                defaultJobSubmissionOptions: "",
        ]).save(flush: true)
    }

    ReferenceGenome findOrCreateReferenceGenome(String name) {
        ReferenceGenome referenceGenome = CollectionUtils.atMostOneElement(ReferenceGenome.findAllByName(name))
        if (referenceGenome) {
            return referenceGenome
        }
        referenceGenome = new ReferenceGenome([
                name                        : name,
                path                        : name,
                fileNamePrefix              : name,
                chromosomePrefix            : "",
                chromosomeSuffix            : "",
                lengthWithoutN              : 100,
                lengthRefChromosomes        : 100,
                lengthRefChromosomesWithoutN: 100,
                length                      : 100,
                species                     : [speciesWithStrain] as Set,
        ]).save(flush: true)

        new StatSizeFileName([
                name           : "ExampleReferenceGenome_realChromosomes.tab",
                referenceGenome: referenceGenome,
        ]).save(flush: true)
        return referenceGenome
    }

    SeqCenter findOrCreateSeqCenter() {
        String name = "ExampleCenter"
        return CollectionUtils.atMostOneElement(SeqCenter.findAllByName(name)) ?: new SeqCenter([
                name   : name,
                dirName: "center",
        ]).save(flush: true)
    }

    SeqPlatform findOrCreateSeqPlatform() {
        String name = "ExampleSeqPlatform"
        return CollectionUtils.atMostOneElement(SeqPlatform.findAllByName(name)) ?: new SeqPlatform([
                name                 : name,
                seqPlatformModelLabel: new SeqPlatformModelLabel([
                        name: "ExampleModel",
                ]).save(flush: true),
                sequencingKitLabel   : new SequencingKitLabel([
                        name: "ExampleKit",
                ]).save(flush: true),
        ]).save(flush: true)
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

    OtrsTicket createOtrsTicket() {
        return new OtrsTicket([
                ticketNumber: "${OtrsTicket.count()}"
        ]).save(flush: true)
    }

    FastqImportInstance createFastqImportInstance() {
        return new FastqImportInstance([
                importMode: FastqImportInstance.ImportMode.MANUAL,
                otrsTicket: createOtrsTicket(),
        ]).save(flush: true)
    }

    MetaDataFile createMetaDataFile() {
        return new MetaDataFile([
                fileName           : "fileName_${MetaDataFile.count()}",
                filePath           : "/tmp/filePath_${MetaDataFile.count()}",
                md5sum             : HelperUtils.randomMd5sum,
                fastqImportInstance: fastqImportInstance,
        ]).save(flush: true)
    }

    Project findOrCreateProject(String projectName) {
        return CollectionUtils.atMostOneElement(Project.findAllByName(projectName)) ?: new Project([
                name               : projectName,
                dirName            : projectName,
                individualPrefix   : "prefix_${Project.count()}",
                realm              : realm,
                processingPriority : processingPriority,
                projectType        : Project.ProjectType.SEQUENCING,
                qcThresholdHandling: QcThresholdHandling.CHECK_AND_NOTIFY,
                unixGroup          : "developer",
                speciesWithStrains : [speciesWithStrain] as Set,
        ]).save(flush: true)
    }

    Individual createIndividual(Project project, String pid) {
        return new Individual([
                project     : project,
                pid         : pid,
                mockPid     : pid,
                mockFullName: pid,
                type        : Individual.Type.REAL,
                species     : speciesWithStrain,
        ]).save(flush: true)
    }

    List<AbstractMergedBamFile> createSampleWithSeqTracksAndBamFile(Individual individual, SampleType sampleType) {
        Sample sample = findOrCreateSample(individual, sampleType)
        println "  - sample: ${sample}"
        return seqTypes.collect { SeqType seqType ->
            println "    - for: ${seqType}"
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

    List<AbstractMergedBamFile> createSingleCellSampleWithSeqTracksAndBamFile(Individual individual, SampleType sampleType) {
        Sample sample = findOrCreateSample(individual, sampleType)
        println "  - sample: ${sample}"
        return singleCellSeqTypes.collect { SeqType seqType ->
            println "    - for: ${seqType}"
            List<SeqTrack> seqTracks = (1..lanesPerSampleAndSeqType).collect {
                SeqTrack seqTrack = createSeqTrack(sample, seqType)
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

    Sample findOrCreateSample(Individual individual, SampleType sampleType) {
        Sample foundSample = atMostOneElement(Sample.findAllByIndividualAndSampleType(individual, sampleType))
        if (foundSample) {
            return foundSample
        }
        return new Sample([
                sampleType: sampleType,
                individual: individual,
        ]).save(flush: true)
    }

    SeqTrack createSeqTrack(Sample sample, SeqType seqType) {
        SeqTrack seqTrack = new SeqTrack([
                sample               : sample,
                seqType              : seqType,
                run                  : createRun(),
                laneId               : (SeqTrack.count() % 8) + 1,
                sampleIdentifier     : "sample_${SeqTrack.count() + 1}",
                pipelineVersion      : softwareTool,
                dataInstallationState: SeqTrack.DataProcessingState.FINISHED,
                fastqcState          : SeqTrack.DataProcessingState.FINISHED,
                libraryPreparationKit: libraryPreparationKit,
                kitInfoReliability   : InformationReliability.KNOWN,
        ]).save(flush: true)

        (1..2).each {
            createDataFile(seqTrack, it)
        }

        return seqTrack
    }

    Run createRun() {
        return new Run([
                name        : "run_${Run.count()}",
                dateExecuted: new Date(),
                blacklisted : false,
                seqCenter   : seqCenter,
                seqPlatform : seqPlatform,
        ]).save(flush: true)
    }

    DataFile createDataFile(SeqTrack seqTrack, int mateNumber) {
        String fileName = "file_${DataFile.count()}_L${seqTrack.laneId}_R${mateNumber}.fastq.gz"
        DataFile dataFile = new DataFile([
                seqTrack           : seqTrack,
                mateNumber         : mateNumber,
                fastqImportInstance: fastqImportInstance,
                vbpFileName        : fileName,
                fileType           : fileType,
                fileName           : fileName,
                pathName           : '',
                initialDirectory   : '/tmp',
                md5sum             : "0" * 32,
                run                : seqTrack.run,
                project            : seqTrack.project,
                used               : true,
                fileExists         : !createFilesOnFilesystem ? markDataFilesAsExisting : true,
                fileLinked         : true,
                fileSize           : 1000000000,
                nReads             : 185000000,
                dateLastChecked    : new Date()
        ]).save(flush: true)

        dataFiles << dataFile
        createFastqcProcessedFiles(dataFile)

        return dataFile
    }

    FastqcProcessedFile createFastqcProcessedFiles(DataFile dataFile) {
        FastqcProcessedFile fastqcProcessedFile = new FastqcProcessedFile([
                dataFile         : dataFile,
                workDirectoryName: "bash-unknown-version-2000-01-01-00-00-00"
        ]).save(flush: true)

        fastqcProcessedFiles << fastqcProcessedFile
        return fastqcProcessedFile
    }

    MergingWorkPackage createMergingWorkPackage(List<SeqTrack> seqTracks) {
        SeqTrack seqTrack = seqTracks.first()
        Pipeline pipeline = Pipeline.Name.forSeqType(seqTrack.seqType).pipeline
        return new MergingWorkPackage([
                sample               : seqTrack.sample,
                seqType              : seqTrack.seqType,
                seqTracks            : seqTracks as Set,
                referenceGenome      : referenceGenome,
                pipeline             : pipeline,
                statSizeFileName     : pipeline.name == Pipeline.Name.PANCAN_ALIGNMENT ?
                        CollectionUtils.exactlyOneElement(StatSizeFileName.findAllByReferenceGenome(referenceGenome, [max: 1, order: 'id'])).name : null,
                seqPlatformGroup     : seqPlatformGroup,
                libraryPreparationKit: seqTrack.seqType.isWgbs() ? null : libraryPreparationKit,
        ]).save(flush: true)
    }

    CellRangerMergingWorkPackage createCellRangerMergingWorkPackage(List<SeqTrack> seqTracks, CellRangerConfig config, Pipeline pipeline, Integer cells) {
        SeqTrack seqTrack = seqTracks.first()

        CellRangerMergingWorkPackage cellRangerMergingWorkPackage = new CellRangerMergingWorkPackage([
                config               : config,
                sample               : seqTrack.sample,
                seqType              : seqTrack.seqType,
                seqTracks            : seqTracks as Set,
                referenceGenome      : singleCellReferenceGenome,
                pipeline             : pipeline,
                seqPlatformGroup     : seqPlatformGroup,
                libraryPreparationKit: libraryPreparationKit,
                referenceGenomeIndex : findOrCreateCellRangerReferenceGenomeIndex(),
                requester            : User.findByUsername("otp"),
                expectedCells: cells,
        ]).save(flush: true)

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
                fileOperationStatus    : AbstractMergedBamFile.FileOperationStatus.PROCESSED,
                qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
                qcTrafficLightStatus   : AbstractMergedBamFile.QcTrafficLightStatus.QC_PASSED,
                comment                : createComment(),
        ]).save(flush: true)

        cellRangerMergingWorkPackage.bamFileInProjectFolder = singleCellBamFile
        cellRangerMergingWorkPackage.save(flush: true)

        singleCellBamFiles << singleCellBamFile
        return singleCellBamFile
    }

    RoddyBamFile createRoddyBamFile(MergingWorkPackage mergingWorkPackage) {
        RoddyWorkflowConfig config = findOrCreateRoddyWorkflowConfig(mergingWorkPackage)
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
                fileOperationStatus    : AbstractMergedBamFile.FileOperationStatus.PROCESSED,
                qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
                qcTrafficLightStatus   : AbstractMergedBamFile.QcTrafficLightStatus.QC_PASSED,
                comment                : createComment(),
        ]).save(flush: true)

        mergingWorkPackage.bamFileInProjectFolder = roddyBamFile
        mergingWorkPackage.save(flush: true)

        QualityAssessmentMergedPass qualityAssessmentMergedPass = new QualityAssessmentMergedPass([
                abstractMergedBamFile: roddyBamFile,
                identifier           : 0,
        ]).save(flush: true)

        createRoddyMergedBamQaAll(qualityAssessmentMergedPass)
        chromosomeXY.each {
            createRoddyMergedBamQaChromosome(qualityAssessmentMergedPass, it)
        }
        roddyBamFiles << roddyBamFile
        return roddyBamFile
    }

    ReferenceGenomeIndex findOrCreateCellRangerReferenceGenomeIndex() {
        ReferenceGenomeIndex foundIndex = atMostOneElement(ReferenceGenomeIndex.findAllByReferenceGenome(singleCellReferenceGenome))
        if (foundIndex) {
            return foundIndex
        }
        ToolName tool = atMostOneElement(ToolName.findAllByName("CELL_RANGER"))
        return new ReferenceGenomeIndex(
                toolName: tool,
                referenceGenome: singleCellReferenceGenome,
                path: '1.2.0',
                indexToolVersion: '1.2.0',
        ).save(flush: true)
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
        String file = "/tmp/file_${RoddyWorkflowConfig.count()}.xml"
        if (createFilesOnFilesystem) {
            Path path = fileSystemService.remoteFileSystemOnDefaultRealm.getPath(file)
            path.text = "someDummyContent"
        }
        return new RoddyWorkflowConfig([
                project              : mergingWorkPackage.project,
                seqType              : mergingWorkPackage.seqType,
                pipeline             : mergingWorkPackage.pipeline,
                programVersion       : "PanCan:1.2.3-4",
                configFilePath       : file,
                configVersion        : "v1_0",
                nameUsedInConfig     : "name",
                md5sum               : "0" * 32,
                adapterTrimmingNeeded: mergingWorkPackage.seqType.isWgbs(),
        ]).save(flush: true)
    }

    Comment createComment() {
        return new Comment([
                comment         : "comment_${Comment.count()}",
                author          : "author",
                modificationDate: new Date(),
        ]).save(flush: true)
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
        ] + map).save(flush: true)
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
        ]).save(flush: true)
        println "  - samplePair: ${samplePair}"
        return samplePair
    }

    RoddySnvCallingInstance createRoddySnvCallingInstance(SamplePair samplePair) {
        RoddyWorkflowConfig config = getOrCreateConfig(samplePair, Pipeline.Name.RODDY_SNV)
        String instanceName = "results_${config.programVersion.replaceAll(":", "-")}_${config.configVersion}_${TimeFormats.DATE_TIME_SECONDS_DASHES.getFormattedDate(new Date())}"
        BamFilePairAnalysis analysis = new RoddySnvCallingInstance([
                samplePair        : samplePair,
                instanceName      : instanceName,
                config            : config,
                sampleType1BamFile: samplePair.mergingWorkPackage1.bamFileInProjectFolder,
                sampleType2BamFile: samplePair.mergingWorkPackage2.bamFileInProjectFolder,
                processingState   : AnalysisProcessingStates.FINISHED,
        ]).save(flush: true)
        println "    - snv: ${analysis}"
        roddySnvCallingInstances << analysis
        return analysis
    }

    IndelCallingInstance createIndelCallingInstance(SamplePair samplePair) {
        RoddyWorkflowConfig config = getOrCreateConfig(samplePair, Pipeline.Name.RODDY_INDEL)
        String instanceName = "results_${config.programVersion.replaceAll(":", "-")}_${config.configVersion}_${TimeFormats.DATE_TIME_SECONDS_DASHES.getFormattedDate(new Date())}"
        BamFilePairAnalysis analysis = new IndelCallingInstance([
                samplePair        : samplePair,
                instanceName      : instanceName,
                config            : config,
                sampleType1BamFile: samplePair.mergingWorkPackage1.bamFileInProjectFolder,
                sampleType2BamFile: samplePair.mergingWorkPackage2.bamFileInProjectFolder,
                processingState   : AnalysisProcessingStates.FINISHED,
        ]).save(flush: true)
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
        ]).save(flush: true)

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
        ]).save(flush: true)

        return analysis
    }

    SophiaInstance createSophiaInstance(SamplePair samplePair) {
        RoddyWorkflowConfig config = getOrCreateConfig(samplePair, Pipeline.Name.RODDY_SOPHIA)
        String instanceName = "results_${config.programVersion.replaceAll(":", "-")}_${config.configVersion}_${TimeFormats.DATE_TIME_SECONDS_DASHES.getFormattedDate(new Date())}"
        BamFilePairAnalysis analysis = new SophiaInstance([
                samplePair        : samplePair,
                instanceName      : instanceName,
                config            : config,
                sampleType1BamFile: samplePair.mergingWorkPackage1.bamFileInProjectFolder,
                sampleType2BamFile: samplePair.mergingWorkPackage2.bamFileInProjectFolder,
                processingState   : AnalysisProcessingStates.FINISHED,
        ]).save(flush: true)
        println "    - sophia: ${analysis}"
        sophiaInstances << analysis

        new SophiaQc([
                sophiaInstance                       : analysis,
                controlMassiveInvPrefilteringLevel   : 10,
                tumorMassiveInvFilteringLevel        : 20,
                rnaContaminatedGenesMoreThanTwoIntron: "arbitraryGeneName1,arbitraryGeneName2",
                rnaContaminatedGenesCount            : 30,
                rnaDecontaminationApplied            : true,
        ]).save(flush: true)

        return analysis
    }

    AceseqInstance createAceseqInstance(SamplePair samplePair) {
        RoddyWorkflowConfig config = getOrCreateConfig(samplePair, Pipeline.Name.RODDY_ACESEQ)
        String instanceName = "results_${config.programVersion.replaceAll(":", "-")}_${config.configVersion}_${TimeFormats.DATE_TIME_SECONDS_DASHES.getFormattedDate(new Date())}"
        BamFilePairAnalysis analysis = new AceseqInstance([
                samplePair        : samplePair,
                instanceName      : instanceName,
                config            : config,
                sampleType1BamFile: samplePair.mergingWorkPackage1.bamFileInProjectFolder,
                sampleType2BamFile: samplePair.mergingWorkPackage2.bamFileInProjectFolder,
                processingState   : AnalysisProcessingStates.FINISHED,
        ]).save(flush: true)
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
        ]).save(flush: true)

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
                                        nameUsedInConfig     : "",
                                        adapterTrimmingNeeded: false,
                                        nameUsedInConfig     : nameUsedInConfig,
                                        md5sum               : HelperUtils.getRandomMd5sum(),
                                        configFilePath       : "/dev/null/${nameUsedInConfig}_${samplePair.id}"
        ]).save(flush: true)
    }
}

Project.withTransaction {
    ExampleData exampleData = new ExampleData([
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
            documentService               : ctx.documentService,
    ])

    exampleData.init()
    exampleData.createObjects()
    exampleData.createFiles()
}
''
