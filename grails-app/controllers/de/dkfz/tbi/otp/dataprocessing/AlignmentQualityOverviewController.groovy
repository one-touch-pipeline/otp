package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import grails.converters.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*


class AlignmentQualityOverviewController {

    public static final String CHR_X_HG19 = 'chrX'
    public static final String CHR_Y_HG19 = 'chrY'


    static enum WarningLevel {
        NO('okay'),
        WarningLevel1('warning1'),
        WarningLevel2('warning2')

        final String styleClass

        WarningLevel(String styleClass) {
            this.styleClass = styleClass
        }
    }


    private static final List<String> chromosomes = [Chromosomes.CHR_X.alias, Chromosomes.CHR_Y.alias, CHR_X_HG19, CHR_Y_HG19].asImmutable()

    private static final List<String> HEADER_WHOLE_GENOME = [
            'alignment.quality.individual',
            'alignment.quality.sampleType',
            'alignment.quality.coverageWithoutN',
            'alignment.quality.coverageX',
            'alignment.quality.coverageY',
            'alignment.quality.kit',
            'alignment.quality.mappedReads',
            'alignment.quality.duplicates',
            'alignment.quality.properlyPaired',
            'alignment.quality.singletons',
            'alignment.quality.medianPE_insertsize',
            'alignment.quality.diffChr',
            'alignment.quality.workflow',
            'alignment.quality.date',
    ].asImmutable()

    private static final List<String> HEADER_RNA = [
            'alignment.quality.individual',
            'alignment.quality.sampleType',
            'alignment.quality.totalReadCounter',
            'alignment.quality.duplicates',
            'alignment.quality.3pnorm',
            'alignment.quality.5pnorm',
            'alignment.quality.chimericPairs',
            'alignment.quality.duplicationRateOfMapped',
            'alignment.quality.end1Sense',
            'alignment.quality.end2Sense',
            'alignment.quality.estimatedLibrarySize',
            'alignment.quality.exonicRate',
            'alignment.quality.expressionProfilingEfficiency',
            'alignment.quality.genesDetected',
            'alignment.quality.intergenicRate',
            'alignment.quality.intragenicRate',
            'alignment.quality.intronicRate',
            'alignment.quality.mapped',
            'alignment.quality.mappedUnique',
            'alignment.quality.mappedUniqueRateOfTotal',
            'alignment.quality.mappingRate',
            'alignment.quality.meanCV',
            'alignment.quality.uniqueRateOfMapped',
            'alignment.quality.rRNARate',
            'alignment.quality.kit',
            'alignment.quality.date',
    ].asImmutable()

    private static final List<String> HEADER_EXOME = [
            'alignment.quality.individual',
            'alignment.quality.sampleType',
            'alignment.quality.onTargetRatio',
            'alignment.quality.targetCoverage',
            'alignment.quality.kit',
            'alignment.quality.mappedReads',
            'alignment.quality.duplicates',
            'alignment.quality.properlyPaired',
            'alignment.quality.singletons',
            'alignment.quality.medianPE_insertsize',
            'alignment.quality.diffChr',
            'alignment.quality.workflow',
            'alignment.quality.date',
    ].asImmutable()


    OverallQualityAssessmentMergedService overallQualityAssessmentMergedService

    ChromosomeQualityAssessmentMergedService chromosomeQualityAssessmentMergedService

    ProcessedMergedBamFileQaFileService processedMergedBamFileQaFileService

    ReferenceGenomeService referenceGenomeService

    SeqTypeService seqTypeService

    ProjectService projectService
    ProjectSelectionService projectSelectionService

    Map index(AlignmentQcCommand cmd) {
        String projectName = params.project
        if (projectName) {
            Project project
            if ((project = projectService.getProjectByName(projectName))) {
                projectSelectionService.setSelectedProject([project], project.name)
                redirect(controller: controllerName, action: actionName)
                return
            }
        }

        List<Project> projects = projectService.getAllProjects()
        ProjectSelection selection = projectSelectionService.getSelectedProject()

        Project project
        if (selection.projects.size() == 1) {
            project = selection.projects.first()
        } else {
            project = projects.first()
        }

        List<SeqType> seqTypes = seqTypeService.alignableSeqTypesByProject(project)

        SeqType seqType = (cmd.seqType && seqTypes.contains(cmd.seqType)) ? cmd.seqType : seqTypes[0]

        List<String> header = null
        switch (seqType?.name) {
            case null:
                header = ['alignment.quality.noSeqType']
                break
            case SeqTypeNames.WHOLE_GENOME.seqTypeName:
            case SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName:
            case SeqTypeNames.WHOLE_GENOME_BISULFITE_TAGMENTATION.seqTypeName:
            case SeqTypeNames.CHIP_SEQ.seqTypeName:
                header = HEADER_WHOLE_GENOME
                break
            case SeqTypeNames.EXOME.seqTypeName:
                header = HEADER_EXOME
                break
            case SeqTypeNames.RNA.seqTypeName:
                header = HEADER_RNA
                break
            default:
                throw new RuntimeException("How should ${seqType.naturalId} be handled")
        }

        return [
                projects: projects,
                project : project,
                seqTypes: seqTypes,
                seqType : seqType,
                header  : header,
        ]
    }


    JSON dataTableSource(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()
        Long projectName = params.project as Long
        Long seqTypeName = params.seqType as Long

        if (!projectName || !seqTypeName) {
            dataToRender.iTotalRecords = 0
            dataToRender.iTotalDisplayRecords = 0
            dataToRender.aaData = []
            render dataToRender as JSON
            return
        }

        Project project = projectService.getProject(projectName)
        SeqType seqType = SeqType.get(seqTypeName)


        List<AbstractQualityAssessment> dataOverall = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(project, seqType)
        List<AbstractQualityAssessment> dataChromosomeXY = chromosomeQualityAssessmentMergedService.qualityAssessmentMergedForSpecificChromosomes(chromosomes, dataOverall*.qualityAssessmentMergedPass)
        Map<Long, Map<String, List<AbstractQualityAssessment>>> chromosomeMapXY = dataChromosomeXY.groupBy([{
                                                                                                                it.qualityAssessmentMergedPass.id
                                                                                                            }, {
                                                                                                                it.chromosomeName
                                                                                                            }])

        List sequenceLengths = overallQualityAssessmentMergedService.findSequenceLengthForQualityAssessmentMerged(dataOverall)
        // TODO: has to be adapted when issue OTP-1670 is solved
        Map sequenceLengthsMap = sequenceLengths.groupBy { it[0] }

        Map<Long, Map<String, List<ReferenceGenomeEntry>>> chromosomeLengthForChromosome =
                overallQualityAssessmentMergedService.findChromosomeLengthForQualityAssessmentMerged(chromosomes, dataOverall).
                        groupBy({ it.referenceGenome.id }, { it.alias })

        dataToRender.iTotalRecords = dataOverall.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = dataOverall.collect { AbstractQualityAssessment it ->

            QualityAssessmentMergedPass qualityAssessmentMergedPass = it.qualityAssessmentMergedPass
            AbstractMergedBamFile abstractMergedBamFile = qualityAssessmentMergedPass.abstractMergedBamFile
            Set<LibraryPreparationKit> kit = qualityAssessmentMergedPass.containedSeqTracks*.libraryPreparationKit.findAll().unique() //findAll removes null values
            String readLengthString = sequenceLengthsMap[it.id][0][1]
            double readLength = readLengthString.contains('-') ? (readLengthString.split('-').sum {
                it as double
            } / 2) : readLengthString as double

            Map map = [
                    mockPid               : abstractMergedBamFile.individual.mockPid,
                    sampleType            : abstractMergedBamFile.sampleType.name,
                    mappedReads           : FormatHelper.formatToTwoDecimalsNullSave(it.percentMappedReads),    // flagstat
                    duplicates            : FormatHelper.formatToTwoDecimalsNullSave(it.percentDuplicates),     // picard
                    diffChr               : FormatHelper.formatToTwoDecimalsNullSave(it.percentDiffChr),
                    properlyPaired        : FormatHelper.formatToTwoDecimalsNullSave(it.percentProperlyPaired), // flagstat
                    singletons            : FormatHelper.formatToTwoDecimalsNullSave(it.percentSingletons),     // flagstat
                    medianPE_insertsize   : FormatHelper.formatToTwoDecimalsNullSave(it.insertSizeMedian), //Median PE_insertsize
                    dateFromFileSystem    : abstractMergedBamFile.dateFromFileSystem?.format("yyyy-MM-dd"),
                    //warning for duplicates
                    duplicateWarning      : warningLevelForDuplicates(it.percentDuplicates).styleClass,

                    //warning for Median PE_insertsize
                    medianWarning         : warningLevelForMedian(it.insertSizeMedian, readLength).styleClass,

                    //warning for properlyPpaired
                    properlyPpairedWarning: warningLevelForProperlyPaired(it.properlyPaired).styleClass,

                    //warning for diff chrom
                    diffChrWarning        : warningLevelForDiffChrom(it.percentDiffChr).styleClass,

                    plot                  : it.id,
                    withdrawn             : abstractMergedBamFile.withdrawn,
                    pipeline              : abstractMergedBamFile.workPackage.pipeline.displayName,
                    kit                   : [name: kit*.name.join(", ") ?: "", shortName: kit*.shortDisplayName.join(", ") ?: "-"],
            ]


            switch (seqType.name) {
                case SeqTypeNames.WHOLE_GENOME.seqTypeName:
                case SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName:
                case SeqTypeNames.WHOLE_GENOME_BISULFITE_TAGMENTATION.seqTypeName:
                case SeqTypeNames.CHIP_SEQ.seqTypeName:
                    Double coverageX
                    Double coverageY
                    if (chromosomeLengthForChromosome[it.referenceGenome.id]) {
                        Map<String, List<AbstractQualityAssessment>> chromosomeMap = chromosomeMapXY[qualityAssessmentMergedPass.id]
                        AbstractQualityAssessment x = getQualityAssessmentForFirstMatchingChromosomeName(chromosomeMap, [Chromosomes.CHR_X.alias, CHR_X_HG19])
                        AbstractQualityAssessment y = getQualityAssessmentForFirstMatchingChromosomeName(chromosomeMap, [Chromosomes.CHR_Y.alias, CHR_Y_HG19])
                        long qcBasesMappedXChromosome = x.qcBasesMapped
                        long qcBasesMappedYChromosome = y.qcBasesMapped
                        long chromosomeLengthX = chromosomeLengthForChromosome[it.referenceGenome.id][Chromosomes.CHR_X.alias][0].lengthWithoutN
                        long chromosomeLengthY = chromosomeLengthForChromosome[it.referenceGenome.id][Chromosomes.CHR_Y.alias][0].lengthWithoutN
                        coverageX = qcBasesMappedXChromosome / chromosomeLengthX
                        coverageY = qcBasesMappedYChromosome / chromosomeLengthY
                    }

                    map << [
                            coverageWithoutN: FormatHelper.formatToTwoDecimalsNullSave(abstractMergedBamFile.coverage), //Coverage w/o N
                            coverageX       : FormatHelper.formatToTwoDecimalsNullSave(coverageX), //ChrX Coverage w/o N
                            coverageY       : FormatHelper.formatToTwoDecimalsNullSave(coverageY), //ChrY Coverage w/o N
                    ]
                    break

                case SeqTypeNames.EXOME.seqTypeName:
                    map << [
                            onTargetRate       : FormatHelper.formatToTwoDecimalsNullSave(it.onTargetRatio),
                            targetCoverage     : FormatHelper.formatToTwoDecimalsNullSave(abstractMergedBamFile.coverage), // coverage

                            //warning for onTargetRate
                            onTargetRateWarning: warningLevelForOnTargetRate(it.onTargetRatio).styleClass,
                    ]
                    break

                case SeqTypeNames.RNA.seqTypeName:
                    map<<[
                            threePNorm : FormatHelper.formatToTwoDecimalsNullSave(it.threePNorm),
                            fivePNorm : FormatHelper.formatToTwoDecimalsNullSave(it.fivePNorm),
                            chimericPairs : it.chimericPairs,
                            duplicatesRate : FormatHelper.formatToTwoDecimalsNullSave(it.duplicatesRate),
                            end1Sense : FormatHelper.formatToTwoDecimalsNullSave(it.end1Sense),
                            end2Sense : FormatHelper.formatToTwoDecimalsNullSave(it.end2Sense),
                            estimatedLibrarySize : it.estimatedLibrarySize,
                            exonicRate : FormatHelper.formatToTwoDecimalsNullSave(it.exonicRate),
                            expressionProfilingEfficiency : FormatHelper.formatToTwoDecimalsNullSave(it.expressionProfilingEfficiency),
                            genesDetected : it.genesDetected,
                            intergenicRate : FormatHelper.formatToTwoDecimalsNullSave(it.intergenicRate),
                            intragenicRate : FormatHelper.formatToTwoDecimalsNullSave(it.intragenicRate),
                            intronicRate : FormatHelper.formatToTwoDecimalsNullSave(it.intronicRate),
                            mapped : it.mapped,
                            mappedUnique : it.mappedUnique,
                            mappedUniqueRateOfTotal : FormatHelper.formatToTwoDecimalsNullSave(it.mappedUniqueRateOfTotal),
                            mappingRate : FormatHelper.formatToTwoDecimalsNullSave(it.mappingRate),
                            meanCV : FormatHelper.formatToTwoDecimalsNullSave(it.meanCV),
                            uniqueRateofMapped : FormatHelper.formatToTwoDecimalsNullSave(it.uniqueRateofMapped),
                            rRNARate : FormatHelper.formatToTwoDecimalsNullSave(it.rRNARate),
                            totalReadCounter : it.totalReadCounter,
                    ]
                    break

                default:
                    throw new RuntimeException("How should ${seqTypeName} be handled")
            }

            return map
        }

        render dataToRender as JSON
    }

    private static AbstractQualityAssessment getQualityAssessmentForFirstMatchingChromosomeName(Map<String,
            List<AbstractQualityAssessment>> qualityAssessmentMergedPassGroupedByChromosome, List<String> chromosomeNames) {
        return exactlyOneElement(chromosomeNames.findResult { qualityAssessmentMergedPassGroupedByChromosome.get(it) })
    }

    private static WarningLevel warningLevelForDuplicates(Double duplicates) {
        warningLevel(duplicates, duplicates > 25, duplicates > 15)
    }

    private static WarningLevel warningLevelForProperlyPaired(Double properlyPaired) {
        warningLevel(properlyPaired, properlyPaired < 90, properlyPaired < 95)
    }

    private static WarningLevel warningLevelForMedian(Double median, double readLength) {
        warningLevel(median, median < 2.2 * readLength, median < 2.5 * readLength)
    }

    private static WarningLevel warningLevelForOnTargetRate(Double onTargetRate) {
        warningLevel(onTargetRate, onTargetRate < 60, onTargetRate < 70)
    }

    private static WarningLevel warningLevelForDiffChrom(Double diffChrom) {
        warningLevel(diffChrom, diffChrom > 3, diffChrom > 2)
    }

    private static WarningLevel warningLevel(Double value, boolean conditionForLevel2, boolean conditionForLevel1) {
        if (value != null) {
            if (conditionForLevel2) {
                return WarningLevel.WarningLevel2
            } else if (conditionForLevel1) {
                return WarningLevel.WarningLevel1
            }
        }
        return WarningLevel.NO
    }
}

class AlignmentQcCommand {
    SeqType seqType
}
