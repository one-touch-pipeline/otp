package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.qcTrafficLight.*
import de.dkfz.tbi.otp.utils.*
import grails.converters.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*


class AlignmentQualityOverviewController {

    public static final String CHR_X_HG19 = 'chrX'
    public static final String CHR_Y_HG19 = 'chrY'

    private static final List<String> chromosomes = [Chromosomes.CHR_X.alias, Chromosomes.CHR_Y.alias, CHR_X_HG19, CHR_Y_HG19].asImmutable()

    private static final List<String> HEADER_WHOLE_GENOME = [
            'alignment.quality.individual',
            'alignment.quality.sampleType',
            'alignment.quality.qcStatus',
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
            'alignment.quality.qcStatus',
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
            'alignment.quality.qcStatus',
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
    QcThresholdService qcThresholdService

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

        Project project = projectSelectionService.getProjectFromProjectSelectionOrAllProjects(selection)

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

        QcThresholdService.ThresholdColorizer thresholdColorizer
        if (dataOverall) {
            thresholdColorizer = qcThresholdService.createThresholdColorizer(project, seqType, dataOverall.first().class)
        }

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

            TableCellValue.Icon icon = [
                    (AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED)  : TableCellValue.Icon.WARNING,
                    (AbstractMergedBamFile.QcTrafficLightStatus.REJECTED) : TableCellValue.Icon.ERROR,
            ].getOrDefault(abstractMergedBamFile.qcTrafficLightStatus, TableCellValue.Icon.OKAY)

            Map<String, TableCellValue> map = [
                    pid                   : new TableCellValue(
                                                abstractMergedBamFile.individual.displayName, null,
                                                g.createLink(
                                                        controller: 'individual',
                                                        action: 'show',
                                                        id: abstractMergedBamFile.individual.id,
                                                ).toString()
                                            ),
                    sampleType            : abstractMergedBamFile.sampleType.name,
                    dateFromFileSystem    : abstractMergedBamFile.dateFromFileSystem?.format("yyyy-MM-dd"),
                    withdrawn             : abstractMergedBamFile.withdrawn,
                    pipeline              : abstractMergedBamFile.workPackage.pipeline.displayName,
                    qcStatus              : new TableCellValue(
                                                abstractMergedBamFile.comment ?
                                                        "${abstractMergedBamFile.comment?.comment} — ${abstractMergedBamFile.comment?.author}" :
                                                        "",
                                                null, null,
                                                (abstractMergedBamFile.qcTrafficLightStatus ?: "").toString(),
                                                icon,
                                            ),
                    kit                   : new TableCellValue(
                                                kit*.shortDisplayName.join(", ") ?: "-", null, null,
                                                kit*.name.join(", ") ?: ""
                                            ),
            ]

            Map<String, Double> qcKeysMap = [
                    "insertSizeMedian": readLength,
            ]
            List<String> qcKeys = [
                    "percentMappedReads",
                    "percentDuplicates",
                    "percentDiffChr",
                    "percentProperlyPaired",
                    "percentSingletons",
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
                            coverageWithoutN: FormatHelper.formatNumber(abstractMergedBamFile.coverage), //Coverage w/o N
                            coverageX       : FormatHelper.formatNumber(coverageX), //ChrX Coverage w/o N
                            coverageY       : FormatHelper.formatNumber(coverageY), //ChrY Coverage w/o N
                    ]
                    break

                case SeqTypeNames.EXOME.seqTypeName:
                    map << [
                            targetCoverage     : FormatHelper.formatNumber(abstractMergedBamFile.coverage),
                    ]
                    qcKeys += [
                            "onTargetRatio",
                    ]
                    break

                case SeqTypeNames.RNA.seqTypeName:
                    qcKeys += [
                            "totalReadCounter",
                            "threePNorm",
                            "fivePNorm",
                            "chimericPairs",
                            "duplicatesRate",
                            "end1Sense",
                            "end2Sense",
                            "estimatedLibrarySize",
                            "exonicRate",
                            "expressionProfilingEfficiency",
                            "genesDetected",
                            "intergenicRate",
                            "intragenicRate",
                            "intronicRate",
                            "mapped",
                            "mappedUnique",
                            "mappedUniqueRateOfTotal",
                            "mappingRate",
                            "meanCV",
                            "uniqueRateofMapped",
                            "rRNARate",
                    ]
                    break

                default:
                    throw new RuntimeException("How should ${seqTypeName} be handled")
            }

            map += thresholdColorizer.colorize(qcKeysMap, it)
            map += thresholdColorizer.colorize(qcKeys, it)
            return map
        }

        render dataToRender as JSON
    }

    private static AbstractQualityAssessment getQualityAssessmentForFirstMatchingChromosomeName(Map<String,
            List<AbstractQualityAssessment>> qualityAssessmentMergedPassGroupedByChromosome, List<String> chromosomeNames) {
        return exactlyOneElement(chromosomeNames.findResult { qualityAssessmentMergedPassGroupedByChromosome.get(it) })
    }
}

class AlignmentQcCommand {
    SeqType seqType
}
