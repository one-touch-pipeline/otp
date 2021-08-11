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

import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

import java.nio.file.FileSystem
import java.nio.file.Path

/**
 * script to create example data on Bam level for WGS, WES, WGBS, and WGBS_TAG.
 *
 * It doesn't expect any existing data except the default SeqType's.
 *
 * It needs some time for execution.
 */
class ExampleData {
//------------------------------
//input

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
    int lanesPerSampleAndSeqType = 1

    /**
     * The SeqTypes for which data is to be created.
     */
    List<SeqType> seqTypes = [
            SeqTypeService.exomePairedSeqType,
    ]

    /**
     * The SampleType names for which data is to be created.
     */
    List<String> sampleTypeNames = [
            "control01",
            "tumor01",
    ]

    /**
     * Should dummy files and links are created for the {@link DataFile}s?
     * Please ensure that otp has the necessary write permissions remotely for the project directory
     * (or any parent directory in which the directories needs to be created)
     */
    boolean shouldDataFilesCreatedOnFilesystem = false

    /**
     * Should dummy files are created for the {@link FastqcProcessedFile}s?
     * Please ensure that otp has the necessary write permissions remotely for the project directory
     * (or any parent directory in which the directories needs to be created)
     */
    boolean shouldFastqcFilesCreatedOnFilesystem = false

    /**
     * Should dummy files and other required directories be created for the generated {@link RoddyBamFile}s?
     * Please ensure that otp has the necessary write permissions remotely for the project directory
     * (or any parent directory in which the directories needs to be created)
     */
    boolean shouldBamFilesCreatedOnFilesystem = false

//------------------------------
//work

    FastqcDataFilesService fastqcDataFilesService

    FileService fileService

    FileSystemService fileSystemService

    LsdfFilesService lsdfFilesService

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
    ReferenceGenome referenceGenome
    SeqCenter seqCenter
    SeqPlatform seqPlatform
    SeqPlatformGroup seqPlatformGroup
    SoftwareTool softwareTool

    List<SampleType> sampleTypes = []
    List<RoddyBamFile> roddyBamFiles = []
    List<DataFile> dataFiles = []
    List<FastqcProcessedFile> fastqcProcessedFiles = []

    void init() {
        sampleTypes = sampleTypeNames.collect {
            findOrCreateSampleType(it)
        }
        processingPriority = findOrProcessingPriority()
        fileType = findOrCreateFileType()
        libraryPreparationKit = findOrCreateLibraryPreparationKit()
        realm = findOrCreateRealm()
        referenceGenome = findOrCreateReferenceGenome()
        seqCenter = findOrCreateSeqCenter()
        seqPlatform = findOrCreateSeqPlatform()
        seqPlatformGroup = findOrCreateSeqPlatformGroup()
        softwareTool = findOrCreateSoftwareTool()

        fastqImportInstance = createFastqImportInstance()
        project = findOrCreateProject(projectName)
    }

    void createObjects() {
        (1..individualCount).each {
            String pid = "example_${Individual.count() + 1}"
            println "- pid: ${pid}"
            Individual individual = createIndividual(project, pid)
            println "- individual: ${individual}"
            sampleTypes.each { SampleType sampleType ->
                Sample sample = createSample(individual, sampleType)
                println "  - sample: ${sample}"
                seqTypes.each { SeqType seqType ->
                    println "    - for: ${seqType}"
                    List<SeqTrack> seqTracks = (1..lanesPerSampleAndSeqType).collect {
                        SeqTrack seqTrack = createSeqTrack(fastqImportInstance, sample, seqType)
                        println "      - seqtrack: ${seqTrack}"
                        return seqTrack
                    }
                    MergingWorkPackage mergingWorkPackage = createMergingWorkPackage(seqTracks)
                    println "      - mwp: ${mergingWorkPackage}"
                    RoddyBamFile roddyBamFile = createRoddyBamFile(mergingWorkPackage)
                    println "      - roddy: ${roddyBamFile}"
                }
            }
        }
    }

    void createFiles() {
        createDataFilesFilesOnFilesystem()
        createFastqcFilesOnFilesystem()
        createBamFilesOnFilesystem()
    }

    void createDataFilesFilesOnFilesystem() {
        if (!shouldDataFilesCreatedOnFilesystem) {
            println "Skipp creating dummy datafiles on file system"
            return
        }
        println "creating dummy datafiles on file system"
        FileSystem fileSystem = fileSystemService.getRemoteFileSystem(realm)

        dataFiles.each { DataFile dataFile ->
            Path directPath = lsdfFilesService.getFileFinalPathAsPath(dataFile, fileSystem)
            Path directPathMd5sum = directPath.resolveSibling("${dataFile.fileName}.md5sum")
            Path vbpPath = lsdfFilesService.getFileViewByPidPathAsPath(dataFile, fileSystem)
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
        if (!shouldFastqcFilesCreatedOnFilesystem) {
            println "Skipp creating dummy fastqc reports on file system"
            return
        }
        println "creating dummy fastqc reports on file system"
        FileSystem fileSystem = fileSystemService.getRemoteFileSystem(realm)

        fastqcProcessedFiles.each { FastqcProcessedFile fastqcProcessedFile ->
            Path fastqcPath = fileSystem.getPath(fastqcDataFilesService.fastqcOutputFile(fastqcProcessedFile.dataFile))
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
        if (!shouldBamFilesCreatedOnFilesystem) {
            println "Skipp creating dummy bam files on file system"
            return
        }
        println "creating dummy bam files on file system"
        FileSystem fileSystem = fileSystemService.getRemoteFileSystem(realm)

        roddyBamFiles.each { RoddyBamFile bam ->
            List<File> files = [
                    bam.workBamFile,
                    bam.workBaiFile,
                    bam.workMd5sumFile,
            ]
            List<File> dirs = [
                    bam.workDirectory,
                    bam.workMergedQADirectory,
            ]
            dirs.addAll(bam.workExecutionDirectories)
            dirs.addAll(bam.workSingleLaneQADirectories.values())

            if (bam.seqType.isWgbs()) {
                files << bam.workMetadataTableFile
                if (bam.containedSeqTracks*.libraryDirectoryName.unique().size() > 1) {
                    dirs.addAll(bam.workLibraryQADirectories.values())
                    dirs.addAll(bam.workLibraryMethylationDirectories.values())
                }
            }

            dirs.each {
                Path path = fileSystem.getPath(it.toString())
                fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(path, realm)
            }

            files.each {
                Path path = fileSystem.getPath(it.toString())
                fileService.createFileWithContent(path, path.toString(), realm)
            }
        }
    }

    SampleType findOrCreateSampleType(String name) {
        return SampleType.findByName(name) ?: new SampleType([
                name: name,
        ]).save(flush: true)
    }

    ProcessingPriority findOrProcessingPriority() {
        String name = "queue"
        return ProcessingPriority.findByName(name) ?: new ProcessingPriority([
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

    Realm findOrCreateRealm() {
        String name = "dev_realm"
        return Realm.findByName(name) ?: new Realm([
                name                       : name,
                jobScheduler               : Realm.JobScheduler.LSF,
                host                       : "localhost",
                port                       : 22,
                timeout                    : 0,
                defaultJobSubmissionOptions: "",
        ]).save(flush: true)
    }

    ReferenceGenome findOrCreateReferenceGenome() {
        String name = "ExampleReferenceGenome"
        ReferenceGenome referenceGenome = ReferenceGenome.findByName(name)
        if (referenceGenome) {
            return referenceGenome
        }
        referenceGenome = new ReferenceGenome([
                name                        : name,
                path                        : "ExampleReferenceGenome",
                fileNamePrefix              : "ExampleReferenceGenome",
                chromosomePrefix            : "",
                chromosomeSuffix            : "",
                lengthWithoutN              : 100,
                lengthRefChromosomes        : 100,
                lengthRefChromosomesWithoutN: 100,
                length                      : 100,
        ]).save(flush: true)

        new StatSizeFileName([
                name           : "ExampleReferenceGenome_realChromosomes.tab",
                referenceGenome: referenceGenome,
        ]).save(flush: true)
        return referenceGenome
    }

    SeqCenter findOrCreateSeqCenter() {
        String name = "ExampleCenter"
        return SeqCenter.findByName(name) ?: new SeqCenter([
                name   : name,
                dirName: "center",
        ]).save(flush: true)
    }

    SeqPlatform findOrCreateSeqPlatform() {
        String name = "ExampleSeqPlatform"
        return SeqPlatform.findByName(name) ?: new SeqPlatform([
                name                 : name,
                seqPlatformModelLabel: new SeqPlatformModelLabel([
                        name: "ExampleModel",
                ]).save(flush: true),
                sequencingKitLabel   : new SequencingKitLabel([
                        name: "ExampleKit",
                ]).save(flush: true),
        ]).save(flush: true)
    }

    SeqPlatformGroup findOrCreateSeqPlatformGroup() {
        return SeqPlatformGroup.createCriteria().get {
            seqPlatforms {
                eq('id', seqPlatform.id)
            }
            isNull('mergingCriteria')
        } ?: new SeqPlatformGroup([
                seqPlatforms: [seqPlatform] as Set
        ]).save(flush: true)
    }

    SoftwareTool findOrCreateSoftwareTool() {
        String name = "ExampleSoftwareTool"
        return SoftwareTool.findByProgramName(name) ?: new SoftwareTool([
                programName   : "ExampleSoftwareTool",
                programVersion: "1.2.3",
                type          : SoftwareTool.Type.BASECALLING,
        ]).save(flush: true)
    }

    FastqImportInstance createFastqImportInstance() {
        return new FastqImportInstance([
                importMode: FastqImportInstance.ImportMode.MANUAL,
        ]).save(flush: true)
    }

    Project findOrCreateProject(String projectName) {
        return Project.findByName(projectName) ?: new Project([
                name               : projectName,
                dirName            : projectName,
                individualPrefix   : "prefix_${Project.count()}",
                realm              : realm,
                processingPriority : processingPriority,
                projectType        : Project.ProjectType.SEQUENCING,
                qcThresholdHandling: QcThresholdHandling.CHECK_NOTIFY_AND_BLOCK,
                unixGroup          : "developer",
        ]).save(flush: true)
    }

    Individual createIndividual(Project project, String pid) {
        return new Individual([
                project     : project,
                pid         : pid,
                mockPid     : pid,
                mockFullName: pid,
                type        : Individual.Type.REAL,
        ]).save(flush: true)
    }

    Sample createSample(Individual individual, SampleType sampleType) {
        return new Sample([
                sampleType: sampleType,
                individual: individual,
        ]).save(flush: true)
    }

    SeqTrack createSeqTrack(FastqImportInstance fastqImportInstance, Sample sample, SeqType seqType) {
        SeqTrack seqTrack = new SeqTrack([
                sample          : sample,
                seqType         : seqType,
                run             : createRun(),
                laneId          : (SeqTrack.count() % 8) + 1,
                sampleIdentifier: "sample_${SeqTrack.count() + 1}",
                pipelineVersion : softwareTool,
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
                fileExists         : true,
                fileLinked         : true,
        ]).save(flush: true)

        dataFiles << dataFile
        createFastqcProcessedFiles(dataFile)

        return dataFile
    }

    FastqcProcessedFile createFastqcProcessedFiles(DataFile dataFile) {
        FastqcProcessedFile fastqcProcessedFile = new FastqcProcessedFile([
                dataFile: dataFile,
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
                statSizeFileName     : pipeline.name == Pipeline.Name.PANCAN_ALIGNMENT ? StatSizeFileName.findByReferenceGenome(referenceGenome).name : null,
                seqPlatformGroup     : seqPlatformGroup,
                libraryPreparationKit: seqTrack.seqType.isWgbs() ? null : libraryPreparationKit,
        ]).save(flush: true)
    }

    RoddyBamFile createRoddyBamFile(MergingWorkPackage mergingWorkPackage) {
        RoddyBamFile roddyBamFile = new RoddyBamFile([
                workPackage            : mergingWorkPackage,
                seqTracks              : mergingWorkPackage.seqTracks.collect() as Set,
                numberOfMergedLanes    : mergingWorkPackage.seqTracks.size(),
                coverage               : 35,
                coverageWithN          : 35,
                config                 : findOrCreateRoddyWorkflowConfig(mergingWorkPackage),
                dateFromFileSystem     : new Date(),
                workDirectoryName      : ".merging_0",
                md5sum                 : "0" * 32,
                fileExists             : true,
                fileSize               : 100,
                fileOperationStatus    : AbstractMergedBamFile.FileOperationStatus.PROCESSED,
                qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
                qcTrafficLightStatus   : AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED,
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

    RoddyWorkflowConfig findOrCreateRoddyWorkflowConfig(MergingWorkPackage mergingWorkPackage) {
        return RoddyWorkflowConfig.findByProjectAndSeqType(mergingWorkPackage.project, mergingWorkPackage.seqType) ?: new RoddyWorkflowConfig([
                project              : mergingWorkPackage.project,
                seqType              : mergingWorkPackage.seqType,
                pipeline             : mergingWorkPackage.pipeline,
                programVersion       : "PanCan:1.2.3-4",
                configFilePath       : "/tmp/file_${RoddyWorkflowConfig.count()}.xml",
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
}

Project.withTransaction {
    ExampleData exampleData = new ExampleData([
            fastqcDataFilesService: ctx.fastqcDataFilesService,
            fileService           : ctx.fileService,
            fileSystemService     : ctx.fileSystemService,
            lsdfFilesService      : ctx.lsdfFilesService,
    ])
    exampleData.init()
    exampleData.createObjects()
    exampleData.createFiles()
}
''
