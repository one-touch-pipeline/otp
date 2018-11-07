package de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.*
import de.dkfz.tbi.otp.dataprocessing.singleCell.*
import de.dkfz.tbi.otp.domainFactory.pipelines.*
import de.dkfz.tbi.otp.ngsdata.*

trait CellRangerFactory implements IsAlignment {

    @Override
    Pipeline findOrCreatePipeline() {
        findOrCreatePipeline(Pipeline.Name.CELL_RANGER, Pipeline.Type.ALIGNMENT)
    }

    @Override
    CellRangerMergingWorkPackage createMergingWorkPackage(Map properties = [:], boolean saveAndValidate = true) {
        Pipeline pipeline = properties.pipeline ?: findOrCreatePipeline()
        SeqType seqType = properties.seqType ?: createSeqType()
        Sample sample = properties.sample ?: createSample()
        return createDomainObject(CellRangerMergingWorkPackage, baseMergingWorkPackageProperties(properties) + [
                seqType : seqType,
                pipeline: pipeline,
                sample  : sample,
                config  : {
                    findOrCreateConfig([
                            pipeline: pipeline,
                            seqType : seqType,
                            project : sample.project,
                    ])
                },
        ], properties, saveAndValidate)
    }

    @Override
    SingleCellBamFile createBamFile(Map properties = [:]) {
        CellRangerMergingWorkPackage workPackage = properties.workPackage
        if (!workPackage) {
            workPackage = createMergingWorkPackage()
            DomainFactory.createReferenceGenomeProjectSeqType(
                    referenceGenome : workPackage.referenceGenome,
                    project         : workPackage.project,
                    seqType         : workPackage.seqType,
                    statSizeFileName: workPackage.statSizeFileName,
            )
        }
        Collection<SeqTrack> seqTracks = properties.seqTracks ?: [DomainFactory.createSeqTrackWithDataFiles(workPackage)]
        workPackage.seqTracks = seqTracks
        workPackage.save(flush: true, failOnError: true)
        SingleCellBamFile bamFile = createDomainObject(SingleCellBamFile, bamFileDefaultProperties(properties, seqTracks, workPackage) +
                [
                        workDirectoryName  : "singleCell_${nextId}",
                        identifier         : SingleCellBamFile.nextIdentifier(workPackage),
                        type               : AbstractBamFile.BamType.MDUP,
                        fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
                        fileSize           : 10000,
                ], properties)
        return bamFile
    }

    @Override
    Map getSeqTypeProperties() {
        return [
                name         : SeqTypeNames._10X_SCRNA.seqTypeName,
                displayName  : '10x_scRNA',
                dirName      : '10x_scRNA_sequencing',
                roddyName    : null,
                libraryLayout: LibraryLayout.PAIRED,
                singleCell   : true,
        ]
    }

    @Override
    Map getConfigProperties(Map properties) {
        return [
                pipeline            : findOrCreatePipeline(),
                seqType             : { properties.seqType ?: createSeqType() },
                project             : { properties.project ?: createProject() },
                programVersion      : "programmVersion${nextId}",
                dateCreated         : { new Date() },
                lastUpdated         : { new Date() },
                referenceGenomeIndex: createReferenceGenomeIndex(),
        ]
    }

    @Override
    Class getConfigPerProjectAndSeqTypeClass() {
        return CellRangerConfig
    }

    AbstractQualityAssessment createQa(AbstractMergedBamFile abstractMergedBamFile, Map properties = [:]) {
        createDomainObject(CellRangerQualityAssessment, getDefaultValuesForAbstractQualityAssessment() + [
                qualityAssessmentMergedPass: DomainFactory.createQualityAssessmentMergedPass(
                        abstractMergedBamFile: abstractMergedBamFile
                )
            ] + getQaValuesProperties(), properties
        )
    }

    Map getQaValuesProperties() {
        return [
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
        ]
    }

    void createQaFileOnFileSystem(File qaFile, Map properties = [:]) {
        qaFile.parentFile.mkdirs()
        qaFile.text = getQaFileContent(properties)
    }

    String getQaFileContent(Map properties = [:]) {
        Map<String, Object> csvData = [
                "Estimated Number of Cells"                     : "1234",
                "Mean Reads per Cell"                           : "123456",
                "Median Genes per Cell"                         : "1234",
                "Number of Reads"                               : "123456789",
                "Valid Barcodes"                                : "12.3%",
                "Sequencing Saturation"                         : "45.6%",
                "Q30 Bases in Barcode"                          : "78.9%",
                "Q30 Bases in RNA Read"                         : "98.7%",
                "Q30 Bases in UMI"                              : "65.4%",
                "Reads Mapped to Genome"                        : "32.1%",
                "Reads Mapped Confidently to Genome"            : "11.1%",
                "Reads Mapped Confidently to Intergenic Regions": "2.2%",
                "Reads Mapped Confidently to Intronic Regions"  : "33.3%",
                "Reads Mapped Confidently to Exonic Regions"    : "44.4%",
                "Reads Mapped Confidently to Transcriptome"     : "55.5%",
                "Reads Mapped Antisense to Gene"                : "6.6%",
                "Fraction Reads in Cells"                       : "77.7%",
                "Total Genes Detected"                          : "54321",
                "Median UMI Counts per Cell"                    : "12345",
        ] + properties
        return [csvData.keySet().join(","), csvData.values().join(",")].join("\n")
    }
}
