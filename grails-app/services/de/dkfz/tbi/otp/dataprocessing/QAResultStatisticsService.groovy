package de.dkfz.tbi.otp.dataprocessing

import static org.springframework.util.Assert.notEmpty
import static org.springframework.util.Assert.notNull
import de.dkfz.tbi.otp.filehandling.FileNames
import de.dkfz.tbi.otp.ngsdata.*

class QAResultStatisticsService {

    AbstractBamFileService abstractBamFileService

    ProcessedBamFileService processedBamFileService

    ProcessedMergedBamFileService processedMergedBamFileService

    QualityAssessmentPassService qualityAssessmentPassService

    QualityAssessmentMergedPassService qualityAssessmentMergedPassService

    ReferenceGenomeService referenceGenomeService

    ChromosomeQualityAssessmentMergedService chromosomeQualityAssessmentMergedService

    ChromosomeQualityAssessmentService chromosomeQualityAssessmentService

    ProcessedMergedBamFileQaFileService processedMergedBamFileQaFileService

    static final String SAMPLE = 'sample'
    static final String LANE = 'lane'
    static final String RUN = 'run'
    static final String SEQTYPE = 'seqType'
    static final String INDIVIDUAL = 'individual'
    static final String REFERENCE_GENOME = 'referenceGenome'
    static final String REFERENCE_GENOME_ENTRY_CHR_X = 'referenceGenomeEntryChrX'
    static final String REFERENCE_GENOME_ENTRY_CHR_Y = 'referenceGenomeEntryChrY'
    static final String REFERENCE_GENOME_LENGTH_WITH_N = 'referenceGenomeLengthWithN'
    static final String REFERENCE_GENOME_LENGTH_WITHOUT_N = 'referenceGenomeLengthWithoutN'
    static final String CHROMOSOME_QUALITY_ASSESSMENT_CHR_X = 'chromosomeQualityAssessmentChrX'
    static final String CHROMOSOME_QUALITY_ASSESSMENT_CHR_Y = 'chromosomeQualityAssessmentChrY'
    static final String OVERALL_QUALITY_ASSESSMENT = 'overallQualityAssessment'
    static final String PID = 'pid'
    static final String MOCK_FULL_NAME = 'mockFullName'
    static final String SAMPLE_TYPE = 'sampleType'
    static final String RUN_ID = 'runId'
    static final String COVERAGE_WITHOUT_N = 'coverageWithoutN'
    static final String COVERAGE_WITH_N = 'coverageWithN'
    static final String COVERAGE_WITHOUT_N_CHR_X = 'coverageWithoutNChrX'
    static final String COVERAGE_WITHOUT_N_CHR_Y = 'coverageWithoutNChrY'
    static final String TARGET_COVERAGE = 'targetCoverage'
    static final String TARGET_MAPPED_BASES = 'targetMappedBases'
    static final String ALL_MAPPED_BASES = 'allMappedBases'
    static final String ON_TARGET_RATE = 'onTargetRate'
    static final String QC_BASES_MAPPED = 'qcBasesMapped'
    static final String TOTAL_READ_COUNT = 'totalReadCount'
    static final String MAPPED_READ_COUNT = "mappedReadCount"
    static final String PERCENTAGE_MAPPED_READS = 'percentageMappedReads'
    static final String PROPERLY_PAIRED = 'properlyPaired'
    static final String SINGLETONS = 'singletons'
    static final String DUPLICATES = 'duplicates'
    static final String INSERT_SIZE_SD = 'insertSizeSD'
    static final String INSERT_SIZE_MEDIAN = 'insertSizeMedian'
    static final String INSERT_SIZE_MEAN = 'insertSizeMean'
    static final String DUPLICATES_READ_1 = 'duplicatesRead1'
    static final String DUPLICATES_READ_2 = 'duplicatesRead2'
    static final String PE_READS_MAPPED_ON_DIFF_CHR = 'peReadsMappedOnDiffChromosomes'
    static final String INCORRECT_PE_ORIENTATION = 'incorrectPEOrientation'
    static final String INCORRECT_PROPER_PAIR = 'incorrectProperPair'
    static final String PERCENTAGE_QC_BASES_MAPPED_WITHOUT_N = 'percentageQCBasesMappedWithoutN'
    static final String PERCENTAGE_QC_BASES_MAPPED_WITH_N = 'percentageQCBasesMappedWithN'
    static final String NOT_MAPPED_READ_1 = 'notMappedR1'
    static final String NOT_MAPPED_READ_2 = 'notMappedR2'
    static final String MAPPED_SHORT_READ_1 = 'mappedShortR1'
    static final String MAPPED_SHORT_READ_2 = 'mappedShortR2'
    static final String MAPPED_LOW_QUALITY_READ_1 = 'mappedLowQualityR1'
    static final String MAPPED_LOW_QUALITY_READ_2 = 'mappedLowQualityR2'
    static final String MAPPED_QUALITY_LONG_READ_1 = 'mappedQualityLongR1'
    static final String MAPPED_QUALITY_LONG_READ_2 = 'mappedQualityLongR2'
    static final String EXOME_ENRICHMENT_KIT = "exomeEnrichmentKit"

    /**
     * return the basis for the qa statistic overview for one single lane bam file, from which all desired information can be received
     */
    Map prepareFetchingSingleLaneResults(ProcessedBamFile bamFile) {
        notNull(bamFile, "the input of the method prepareFetchingSingleLaneResults is null")
        Sample sample = processedBamFileService.sample(bamFile)
        Individual individual = sample.individual
        Project project = individual.project
        SeqTrack seqTrack = bamFile.alignmentPass.seqTrack
        ExomeEnrichmentKit kit = null
        if(seqTrack instanceof ExomeSeqTrack) {
            kit = seqTrack.exomeEnrichmentKit
        }
        SeqType seqType = seqTrack.seqType
        String run = seqTrack.run.name
        String lane = seqTrack.laneId
        ReferenceGenome referenceGenome = referenceGenomeService.referenceGenome(project, seqType)
        ReferenceGenomeEntry referenceGenomeEntryChrX = ReferenceGenomeEntry.findByReferenceGenomeAndAlias(referenceGenome, Chromosomes.CHR_X.alias)
        ReferenceGenomeEntry referenceGenomeEntryChrY = ReferenceGenomeEntry.findByReferenceGenomeAndAlias(referenceGenome, Chromosomes.CHR_Y.alias)
        long latestQualityAssessmentPassId = qualityAssessmentPassService.latestQualityAssessmentPass(bamFile).id
        OverallQualityAssessment overallQualityAssessment = OverallQualityAssessment.createCriteria().get {
            qualityAssessmentPass { eq("id", latestQualityAssessmentPassId) }
        }
        ChromosomeQualityAssessment chromosomeQualityAssessmentXChr = chromosomeQualityAssessmentService.
                        qualityAssessmentForSpecificChromosome(referenceGenomeEntryChrX, latestQualityAssessmentPassId)
        ChromosomeQualityAssessment chromosomeQualityAssessmentYChr = chromosomeQualityAssessmentService.
                        qualityAssessmentForSpecificChromosome(referenceGenomeEntryChrY, latestQualityAssessmentPassId)
        Map preparation = [
            (CHROMOSOME_QUALITY_ASSESSMENT_CHR_X): chromosomeQualityAssessmentXChr,
            (CHROMOSOME_QUALITY_ASSESSMENT_CHR_Y): chromosomeQualityAssessmentYChr,
            (OVERALL_QUALITY_ASSESSMENT): overallQualityAssessment,
            (REFERENCE_GENOME_ENTRY_CHR_X): referenceGenomeEntryChrX,
            (REFERENCE_GENOME_ENTRY_CHR_Y): referenceGenomeEntryChrY,
            (REFERENCE_GENOME): referenceGenome,
            (INDIVIDUAL): individual,
            (SAMPLE): sample,
            (RUN): run,
            (LANE): lane,
            (SEQTYPE): seqType,
            (EXOME_ENRICHMENT_KIT): kit,
        ]
        return preparation
    }

    /**
     * return the basis for the qa statistic overview for the merged bam file, from which all desired information can be received
     */
    Map prepareFetchingMergedBamFileResults(ProcessedMergedBamFile bamFile) {
        notNull(bamFile, "the input for the method prepareFetchingMergedBamFileResults is null")
        Sample sample = processedMergedBamFileService.sample(bamFile)
        Individual individual = sample.individual
        Project project = individual.project
        SeqType seqType = processedMergedBamFileService.seqType(bamFile)
        ExomeEnrichmentKit kit = null
        if (seqType.name == SeqTypeNames.EXOME.seqTypeName) {
            kit = processedMergedBamFileService.exomeEnrichmentKit(bamFile)
        }
        String run = 'all_merged'
        String lane = 'all_merged'
        ReferenceGenome referenceGenome = referenceGenomeService.referenceGenome(project, seqType)
        ReferenceGenomeEntry referenceGenomeEntryChrX = ReferenceGenomeEntry.findByReferenceGenomeAndAlias(referenceGenome, Chromosomes.CHR_X.alias)
        ReferenceGenomeEntry referenceGenomeEntryChrY = ReferenceGenomeEntry.findByReferenceGenomeAndAlias(referenceGenome, Chromosomes.CHR_Y.alias)
        long latestQualityAssessmentMergedPassId = qualityAssessmentMergedPassService.latestQualityAssessmentMergedPass(bamFile).id
        OverallQualityAssessmentMerged overallQualityAssessment = OverallQualityAssessmentMerged.createCriteria().get {
            qualityAssessmentMergedPass { eq("id", latestQualityAssessmentMergedPassId) }
        }
        ChromosomeQualityAssessmentMerged chromosomeQualityAssessmentXChr = chromosomeQualityAssessmentMergedService.
                        qualityAssessmentMergedForSpecificChromosome(referenceGenomeEntryChrX, latestQualityAssessmentMergedPassId)
        ChromosomeQualityAssessmentMerged chromosomeQualityAssessmentYChr = chromosomeQualityAssessmentMergedService.
                        qualityAssessmentMergedForSpecificChromosome(referenceGenomeEntryChrY, latestQualityAssessmentMergedPassId)

        Map preparation = [
            (CHROMOSOME_QUALITY_ASSESSMENT_CHR_X): chromosomeQualityAssessmentXChr,
            (CHROMOSOME_QUALITY_ASSESSMENT_CHR_Y): chromosomeQualityAssessmentYChr,
            (OVERALL_QUALITY_ASSESSMENT): overallQualityAssessment,
            (REFERENCE_GENOME_ENTRY_CHR_X): referenceGenomeEntryChrX,
            (REFERENCE_GENOME_ENTRY_CHR_Y): referenceGenomeEntryChrY,
            (REFERENCE_GENOME): referenceGenome,
            (INDIVIDUAL): individual,
            (SAMPLE): sample,
            (RUN): run,
            (LANE): lane,
            (SEQTYPE): seqType,
            (EXOME_ENRICHMENT_KIT): kit,
        ]
        return preparation
    }

    /**
     * return a map, which contains all requested information for the merged bam file and the corresponding single lane bam files
     */
    List<Map> fetchResultsSmall(ProcessedMergedBamFile processedMergedBamFile) {
        notNull(processedMergedBamFile, "the input of the method fetchResultsSmall is null")
        List<Map> resultsSmall = []
        List<AbstractBamFile> singleLaneBamFiles = abstractBamFileService.findAllByProcessedMergedBamFile(processedMergedBamFile)
        singleLaneBamFiles.sort { -it.id }
        singleLaneBamFiles.add(0, processedMergedBamFile)
        singleLaneBamFiles.each { AbstractBamFile bamFile ->
            Map preparation = [:]
            if (bamFile instanceof ProcessedBamFile) {
                preparation = prepareFetchingSingleLaneResults(bamFile)
            } else if (bamFile instanceof ProcessedMergedBamFile) {
                preparation = prepareFetchingMergedBamFileResults(bamFile)
            } else {
                throw new IllegalArgumentException("Instance of AbstractBamFile is of unknown class")
            }
            AbstractQualityAssessment abstractQualityAssessment = preparation[OVERALL_QUALITY_ASSESSMENT]
            long qcBasesMapped = abstractQualityAssessment.qcBasesMapped
            long referenceGenomeLengthWithN = preparation[REFERENCE_GENOME].length
            long referenceGenomeLengthWithoutN = preparation[REFERENCE_GENOME].lengthWithoutN
            long chromosomeXLengthWithoutN = preparation[REFERENCE_GENOME_ENTRY_CHR_X].lengthWithoutN
            long chromosomeYLengthWithoutN = preparation[REFERENCE_GENOME_ENTRY_CHR_Y].lengthWithoutN
            long qcBasesMappedXChromosome = preparation[CHROMOSOME_QUALITY_ASSESSMENT_CHR_X].qcBasesMapped
            long qcBasesMappedYChromosome = preparation[CHROMOSOME_QUALITY_ASSESSMENT_CHR_Y].qcBasesMapped
            long totalReadCounter = abstractQualityAssessment.totalReadCounter
            long totalMappedReadCounter = abstractQualityAssessment.totalMappedReadCounter
            long properlyPaired = abstractQualityAssessment.properlyPaired
            long pairedInSequencing = abstractQualityAssessment.pairedInSequencing
            long singletons = abstractQualityAssessment.singletons
            long duplicates = abstractQualityAssessment.duplicates
            Map statisticResults = [
                (REFERENCE_GENOME_LENGTH_WITH_N): formatToTwoDecimals(referenceGenomeLengthWithN / 1e6),
                (REFERENCE_GENOME_LENGTH_WITHOUT_N): formatToTwoDecimals(referenceGenomeLengthWithoutN / 1e6),
                (PID): preparation[INDIVIDUAL].pid,
                (MOCK_FULL_NAME): preparation[INDIVIDUAL].mockFullName,
                (SAMPLE_TYPE): preparation[SAMPLE].sampleType.name,
                (RUN_ID): preparation[RUN],
                (LANE): preparation[LANE],
                (COVERAGE_WITHOUT_N): formatToTwoDecimals(qcBasesMapped / referenceGenomeLengthWithoutN),
                (COVERAGE_WITH_N): formatToTwoDecimals(qcBasesMapped / referenceGenomeLengthWithN),
                (COVERAGE_WITHOUT_N_CHR_X): formatToTwoDecimals(qcBasesMappedXChromosome / chromosomeXLengthWithoutN),
                (COVERAGE_WITHOUT_N_CHR_Y): formatToTwoDecimals(qcBasesMappedYChromosome / chromosomeYLengthWithoutN),
                (QC_BASES_MAPPED): qcBasesMapped,
                (TOTAL_READ_COUNT): totalReadCounter,
                (MAPPED_READ_COUNT): totalMappedReadCounter,
                (PERCENTAGE_MAPPED_READS): formatToTwoDecimals(totalMappedReadCounter / (totalReadCounter as Double) * 100.0),
                (PROPERLY_PAIRED): formatToTwoDecimals(properlyPaired / pairedInSequencing * 100.0),
                (SINGLETONS): formatToTwoDecimals(singletons / totalReadCounter * 100.0),
                (DUPLICATES): formatToTwoDecimals(duplicates / totalReadCounter * 100.0),
                (INSERT_SIZE_SD): formatToTwoDecimals(abstractQualityAssessment.insertSizeSD),
                (INSERT_SIZE_MEDIAN): formatToTwoDecimals(abstractQualityAssessment.insertSizeMedian),
                (INSERT_SIZE_MEAN): formatToTwoDecimals(abstractQualityAssessment.insertSizeMean),
            ]
            if (preparation[SEQTYPE].name.equals(SeqTypeNames.EXOME.seqTypeName)) {
                ExomeEnrichmentKit kit = preparation[EXOME_ENRICHMENT_KIT]
                BedFile bedFile = BedFile.findByReferenceGenomeAndExomeEnrichmentKit(preparation[REFERENCE_GENOME], kit)
                statisticResults.put(TARGET_COVERAGE,
                                formatToTwoDecimals(abstractQualityAssessment.onTargetMappedBases / bedFile.mergedTargetSize))
                statisticResults.put(ON_TARGET_RATE,
                                formatToTwoDecimals(abstractQualityAssessment.onTargetMappedBases / abstractQualityAssessment.allBasesMapped * 100.0))
            }
            resultsSmall << statisticResults
        }
        return resultsSmall
    }

    List<Map> fetchResultsExtended(ProcessedMergedBamFile processedMergedBamFile) {
        notNull(processedMergedBamFile, "the input of the method fetchResultsSmall is null")
        List<Map> resultsExtended = []
        List<AbstractBamFile> singleLaneBamFiles = abstractBamFileService.findAllByProcessedMergedBamFile(processedMergedBamFile)
        singleLaneBamFiles.sort { -it.id }
        singleLaneBamFiles.add(0, processedMergedBamFile)
        singleLaneBamFiles.each { AbstractBamFile bamFile ->
            Map preparation = [:]
            if (bamFile instanceof ProcessedBamFile) {
                preparation = prepareFetchingSingleLaneResults(bamFile)
            } else if (bamFile instanceof ProcessedMergedBamFile) {
                preparation = prepareFetchingMergedBamFileResults(bamFile)
            } else {
                throw new IllegalArgumentException("Instance of AbstractBamFile is of unknown class")
            }
            AbstractQualityAssessment abstractQualityAssessment = preparation[OVERALL_QUALITY_ASSESSMENT]
            long qcBasesMapped = abstractQualityAssessment.qcBasesMapped
            long referenceGenomeLengthWithN = preparation[REFERENCE_GENOME].length
            long referenceGenomeLengthWithoutN = preparation[REFERENCE_GENOME].lengthWithoutN
            Map statisticResults = [
                (DUPLICATES_READ_1): abstractQualityAssessment.duplicateR1,
                (DUPLICATES_READ_2): abstractQualityAssessment.duplicateR2,
                (PE_READS_MAPPED_ON_DIFF_CHR): abstractQualityAssessment.percentReadPairsMapToDiffChrom,
                (INCORRECT_PE_ORIENTATION): abstractQualityAssessment.percentIncorrectPEorientation,
                (INCORRECT_PROPER_PAIR): abstractQualityAssessment.properPairStrandConflict,
                (PERCENTAGE_QC_BASES_MAPPED_WITHOUT_N): "${qcBasesMapped}/${referenceGenomeLengthWithoutN}",
                (PERCENTAGE_QC_BASES_MAPPED_WITH_N): "${qcBasesMapped}/${referenceGenomeLengthWithN}",
                (NOT_MAPPED_READ_1): abstractQualityAssessment.notMappedR1,
                (NOT_MAPPED_READ_2): abstractQualityAssessment.notMappedR2,
                (MAPPED_SHORT_READ_1): abstractQualityAssessment.mappedShortR1,
                (MAPPED_SHORT_READ_2): abstractQualityAssessment.mappedShortR2,
                (MAPPED_LOW_QUALITY_READ_1): abstractQualityAssessment.mappedLowQualityR1,
                (MAPPED_LOW_QUALITY_READ_2): abstractQualityAssessment.mappedLowQualityR2,
                (MAPPED_QUALITY_LONG_READ_1): abstractQualityAssessment.mappedQualityLongR1,
                (MAPPED_QUALITY_LONG_READ_2): abstractQualityAssessment.mappedQualityLongR2,
            ]
            if (preparation[SEQTYPE].name.equals(SeqTypeNames.EXOME.seqTypeName)) {
                statisticResults.put(TARGET_MAPPED_BASES, abstractQualityAssessment.onTargetMappedBases)
                statisticResults.put(ALL_MAPPED_BASES, abstractQualityAssessment.allBasesMapped)
            }
            resultsExtended << statisticResults
        }
        return resultsExtended
    }

    /**
     * return the directories to the small and the extended qa result statistic file
     */
    Map<String, String> statisticsFile(ProcessedMergedBamFile processedMergedBamFile) {
        notNull(processedMergedBamFile, "the input of the method statisticsFile is null")
        String path = processedMergedBamFileService.qaResultTempDestinationDirectory(processedMergedBamFile)
        Map statisticFiles = [
            'small': path + "/" + FileNames.QA_RESULT_OVERVIEW,
            'extended': path + "/" + FileNames.QA_RESULT_OVERVIEW_EXTENDED
        ]
        return statisticFiles
    }

    /**
     * return a map with the content of the qa result statistic files (small and extended).
     * the content is in the correct order, which is defined in the sortOrder lists.
     */
    Map<String, String> defineOutput(ProcessedMergedBamFile processedMergedBamFile) {
        notNull(processedMergedBamFile, "the input of the method defineOutput is null")
        //determine which sequencing type was used for the processed merged bam file
        SeqType seqType = processedMergedBamFileService.seqType(processedMergedBamFile)
        Map<String, String> statisticFileContent = [:]
        String outputSmall = ""
        String outputExtended = ""

        /*
         * the definition of the sort order is split:
         * - general information about the bam file
         * - sequencing type specific statistics
         * - sequencing type independent statistics
         */
        List<String> sortOrderGeneralInformation = [
            PID,
            MOCK_FULL_NAME,
            SAMPLE_TYPE,
            RUN_ID,
            LANE
        ]

        List<String> sortOrderSmallWholeGenomeCoverage = sortOrderGeneralInformation + [
            COVERAGE_WITHOUT_N,
            COVERAGE_WITH_N,
            COVERAGE_WITHOUT_N_CHR_X,
            COVERAGE_WITHOUT_N_CHR_Y,
            QC_BASES_MAPPED,
            MAPPED_READ_COUNT,
            PERCENTAGE_MAPPED_READS,
        ]

        List<String> sortOrderSmallExomeCoverage = sortOrderGeneralInformation + [
            ON_TARGET_RATE,
            TARGET_COVERAGE,
        ]

        List<String> sortOrderGeneralStatistics = [
            TOTAL_READ_COUNT,
            PROPERLY_PAIRED,
            SINGLETONS,
            DUPLICATES,
            INSERT_SIZE_SD,
            INSERT_SIZE_MEDIAN,
            INSERT_SIZE_MEAN
        ]

        List<String> sortOrderSmall = []
        if (seqType.name.equals(SeqTypeNames.EXOME.seqTypeName)) {
            sortOrderSmall = sortOrderSmallExomeCoverage + sortOrderGeneralStatistics
        }
        else if (seqType.name.equals(SeqTypeNames.WHOLE_GENOME.seqTypeName)) {
            sortOrderSmall = sortOrderSmallWholeGenomeCoverage + sortOrderGeneralStatistics
        }
        else {
            throw new RuntimeException("The statistic file will not be produced for the sequencing type " + seqType.name)
        }

        List<String> sortOrderExtendedExome = [
            TARGET_MAPPED_BASES,
            ALL_MAPPED_BASES
        ]

        List<String> sortOrderExtendedGeneral = [
            DUPLICATES_READ_1,
            DUPLICATES_READ_2,
            PE_READS_MAPPED_ON_DIFF_CHR,
            INCORRECT_PE_ORIENTATION,
            INCORRECT_PROPER_PAIR,
            PERCENTAGE_QC_BASES_MAPPED_WITHOUT_N,
            PERCENTAGE_QC_BASES_MAPPED_WITH_N,
            NOT_MAPPED_READ_1,
            NOT_MAPPED_READ_2,
            MAPPED_SHORT_READ_1,
            MAPPED_SHORT_READ_2,
            MAPPED_LOW_QUALITY_READ_1,
            MAPPED_LOW_QUALITY_READ_2,
            MAPPED_QUALITY_LONG_READ_1,
            MAPPED_QUALITY_LONG_READ_2
        ]
        List<String> sortOrderExtended = []
        if (seqType.name.equals(SeqTypeNames.EXOME.seqTypeName)) {
            sortOrderExtended = sortOrderSmall + sortOrderExtendedExome + sortOrderExtendedGeneral
        }
        else if (seqType.name.equals(SeqTypeNames.WHOLE_GENOME.seqTypeName)) {
            sortOrderExtended = sortOrderSmall + sortOrderExtendedGeneral
        }
        else {
            throw new RuntimeException("The statistic file will not be produced for the sequencing type " + seqType.name)
        }
        List<Map> fetchResultsSmall = fetchResultsSmall(processedMergedBamFile)
        List<Map> fetchResultsExtended = fetchResultsExtended(processedMergedBamFile)
        String lengthWithN = fetchResultsSmall.first()."${REFERENCE_GENOME_LENGTH_WITH_N}"
        String lengthWithOutN = fetchResultsSmall.first()."${REFERENCE_GENOME_LENGTH_WITHOUT_N}"

        Map<String, String> columnNames = [
            (PID): "pid",
            (MOCK_FULL_NAME): "mock full name",
            (SAMPLE_TYPE): "sample type",
            (RUN_ID): "run id",
            (LANE): "lane",
            (COVERAGE_WITHOUT_N): "Coverage w/o N (${lengthWithOutN}Mbp)",
            (COVERAGE_WITH_N): "Coverage wN (${lengthWithN}Mbp)",
            (COVERAGE_WITHOUT_N_CHR_X): "ChrX Coverage w/o N",
            (COVERAGE_WITHOUT_N_CHR_Y): "ChrY Coverage w/o N",
            (QC_BASES_MAPPED): "#QC bases mapped",
            (TOTAL_READ_COUNT): "#total read count (flagstat)",
            (MAPPED_READ_COUNT): "#mapped read count (flagstat)",
            (PERCENTAGE_MAPPED_READS): "%mapped reads (flagstat)",
            (PROPERLY_PAIRED): "%properly_paired (flagstat)",
            (SINGLETONS): "%singletons (flagstat)",
            (DUPLICATES): "%duplicates (picard)",
            (INSERT_SIZE_SD): "Standard Deviation PE_insertsize",
            (INSERT_SIZE_MEDIAN): "Median PE_insertsize",
            (INSERT_SIZE_MEAN): "Mean PE_insertsize",
            (DUPLICATES_READ_1): "#duplicates Read1",
            (DUPLICATES_READ_2): "#duplicates Read2",
            (PE_READS_MAPPED_ON_DIFF_CHR): "%PE reads mapped on diff chromosomes",
            (INCORRECT_PE_ORIENTATION): "%incorrect PE orientation",
            (INCORRECT_PROPER_PAIR): "incorrect proper pair",
            (PERCENTAGE_QC_BASES_MAPPED_WITHOUT_N): "QC bases/ total bases w/o N",
            (PERCENTAGE_QC_BASES_MAPPED_WITH_N): "QC bases/ total bases w N",
            (NOT_MAPPED_READ_1): "mapq=0 read1",
            (NOT_MAPPED_READ_2): "mapq=0 read2",
            (MAPPED_SHORT_READ_1): "mapq>0,readlength<minlength read1",
            (MAPPED_SHORT_READ_2): "mapq>0,readlength<minlength read2",
            (MAPPED_LOW_QUALITY_READ_1): "mapq>0,BaseQualityMedian<basequalCutoff read1",
            (MAPPED_LOW_QUALITY_READ_2): "mapq>0,BaseQualityMedian<basequalCutoff read2",
            (MAPPED_QUALITY_LONG_READ_1): "mapq>0,BaseQualityMedian>=basequalCutoff read1",
            (MAPPED_QUALITY_LONG_READ_2): "mapq>0,BaseQualityMedian>=basequalCutoff read2",
            (TARGET_COVERAGE): "target Coverage",
            (TARGET_MAPPED_BASES): "target mapped bases",
            (ALL_MAPPED_BASES): "all mapped bases",
            (ON_TARGET_RATE): "%onTarget",
        ]

        // Merge the two maps
        fetchResultsSmall.eachWithIndex { item, pos ->
            fetchResultsExtended[pos] << item
        }
        outputSmall = createOutputLine(columnNames, sortOrderSmall)
        fetchResultsSmall.each { Map results ->
            outputSmall += createOutputLine(results, sortOrderSmall)
        }
        statisticFileContent.put("small", outputSmall)
        outputExtended = createOutputLine(columnNames, sortOrderExtended)
        fetchResultsExtended.each { Map results ->
            outputExtended += createOutputLine(results, sortOrderExtended)
        }
        statisticFileContent.put("extended", outputExtended)
        return statisticFileContent
    }

    private String createOutputLine(Map<String, String> values, List<String> sortOrder) {
        notNull values, "the argument \"values\" of the method \"createOutputLine\" is null"
        notNull sortOrder, "the argument \"sortOrder\" of the method \"createOutputLine\" is null"
        notEmpty values, "the argument \"values\" of the method \"createOutputLine\" is empty"
        notEmpty sortOrder, "the argument \"sortOrder\" of the method \"createOutputLine\" is empty"
        List v = []
        sortOrder.each { v.add(values[it]) }
        return v.join("\t") + "\n"
    }

    private formatToTwoDecimals(Double number) {
        notNull number, 'the argument "number" of the method "createOutputLine" is null'
        String.format(Locale.ENGLISH, '%.2f', number)
    }
}
