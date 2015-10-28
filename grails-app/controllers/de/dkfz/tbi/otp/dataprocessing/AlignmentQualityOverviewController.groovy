package de.dkfz.tbi.otp.dataprocessing

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

import grails.converters.JSON
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.ProjectService
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeService
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.SeqTypeNames
import de.dkfz.tbi.otp.ngsdata.SeqTypeService
import de.dkfz.tbi.otp.utils.DataTableCommand
import de.dkfz.tbi.otp.utils.FormatHelper



class AlignmentQualityOverviewController {

    public static final String CHR_X_HG19 = 'chrX'
    public static final String CHR_Y_HG19 = 'chrY'


    enum WarningLevel {
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
        'alignment.quality.mappedReads',
        'alignment.quality.duplicates',
        'alignment.quality.totalReadCount',
        'alignment.quality.properlyPaired',
        'alignment.quality.singletons',
        'alignment.quality.standardDeviationPE_Insertsize',
        'alignment.quality.medianPE_insertsize',
        'alignment.quality.meanPE_Insertsize',
        'alignment.quality.workflow',
        'alignment.quality.date',
    ].asImmutable()

    private static final List<String> HEADER_EXOME = [
        'alignment.quality.individual',
        'alignment.quality.sampleType',
        'alignment.quality.onTargetRatio',
        'alignment.quality.targetCoverage',
        'alignment.quality.mappedReads',
        'alignment.quality.duplicates',
        'alignment.quality.totalReadCount',
        'alignment.quality.properlyPaired',
        'alignment.quality.singletons',
        'alignment.quality.standardDeviationPE_Insertsize',
        'alignment.quality.medianPE_insertsize',
        'alignment.quality.meanPE_Insertsize',
        'alignment.quality.workflow',
        'alignment.quality.date',
    ].asImmutable()



    OverallQualityAssessmentMergedService overallQualityAssessmentMergedService

    ChromosomeQualityAssessmentMergedService chromosomeQualityAssessmentMergedService

    ProcessedMergedBamFileQaFileService processedMergedBamFileQaFileService

    ReferenceGenomeService referenceGenomeService

    SeqTypeService seqTypeService

    ProjectService projectService



    Map index() {
        List<String> projects = projectService.getAllProjects()*.name
        String projectName = params.project ?: projects[0]
        Project project = projectService.getProjectByName(projectName)

        List<String> seqTypes = seqTypeService.alignableSeqTypesByProject(project)*.aliasOrName
        String seqTypeName = (params.seqType && seqTypes.contains(params.seqType)) ? params.seqType : seqTypes[0]
        SeqType seqType = SeqType.findWhere(
                'aliasOrName': seqTypeName
                )

        List<String> header = null
        switch (seqType?.name) {
            case null:
                header = ['alignment.quality.noSeqType']
                break
            case SeqTypeNames.WHOLE_GENOME.seqTypeName:
                header = HEADER_WHOLE_GENOME
                break
            case SeqTypeNames.EXOME.seqTypeName:
                header = HEADER_EXOME
                break
            default:
                throw new RuntimeException("How should ${seqTypeName} be handled")
        }

        return [
            projects: projects,
            project: projectName,
            seqTypes: seqTypes,
            seqType: seqTypeName,
            header: header,
        ]
    }



    JSON dataTableSource(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()
        String projectName = params.project
        String seqTypeName = params.seqType

        if (!projectName || !seqTypeName) {
            dataToRender.iTotalRecords = 0
            dataToRender.iTotalDisplayRecords = 0
            dataToRender.aaData = [:]
            render dataToRender as JSON
            return
        }

        Project project = projectService.getProjectByName(projectName)
        SeqType seqType = SeqType.findWhere(
                'aliasOrName': seqTypeName,
                'libraryLayout': SeqType.LIBRARYLAYOUT_PAIRED
                )


        List<AbstractQualityAssessment> dataOverall = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(project, seqType)
        List<AbstractQualityAssessment> dataChromosomeXY = chromosomeQualityAssessmentMergedService.qualityAssessmentMergedForSpecificChromosomes(chromosomes, dataOverall*.qualityAssessmentMergedPass)
        Map chromosomeMapXY = dataChromosomeXY.groupBy ([{it.qualityAssessmentMergedPass.id}, {it.chromosomeName}])

        List sequenceLengthsAndReferenceGenomeLengthWithoutN = overallQualityAssessmentMergedService.findSequenceLengthAndReferenceGenomeLengthWithoutNForQualityAssessmentMerged(dataOverall)
        // TODO: has to be adapted when issue OTP-1670 is solved
        Map sequenceLengthsAndReferenceGenomeLengthWithoutNMap = sequenceLengthsAndReferenceGenomeLengthWithoutN.groupBy{it[0]}

        dataToRender.iTotalRecords = dataOverall.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = dataOverall.collect { AbstractQualityAssessment it->

            AbstractMergedBamFile abstractMergedBamFile = it.qualityAssessmentMergedPass.abstractMergedBamFile
            double duplicates = it.duplicates / it.totalReadCounter * 100.0 //%duplicates (picard)
            double properlyPaired = it.properlyPaired / it.pairedInSequencing * 100.0
            double readLength = sequenceLengthsAndReferenceGenomeLengthWithoutNMap[it.id][0][1] as double

            Map map = [
                mockPid: abstractMergedBamFile.individual.mockPid,
                sampleType: abstractMergedBamFile.sampleType.name,
                mappedReads: FormatHelper.formatToTwoDecimalsNullSave(it.totalMappedReadCounter / (it.totalReadCounter as Double) * 100.0), //%mapped reads (flagstat)
                duplicates: FormatHelper.formatToTwoDecimalsNullSave(duplicates), //%duplicates (picard)
                totalReadCount: FormatHelper.formatGroupsNullSave(it.totalReadCounter), //#total read count
                properlyPaired: FormatHelper.formatToTwoDecimalsNullSave(properlyPaired), //%properly_paired (flagstat)
                singletons: FormatHelper.formatToTwoDecimalsNullSave(it.singletons / it.totalReadCounter * 100.0), //%singletons (flagstat)
                standardDeviationPE_Insertsize: FormatHelper.formatToTwoDecimalsNullSave(it.insertSizeSD), //Standard Deviation PE_insertsize
                medianPE_insertsize: FormatHelper.formatToTwoDecimalsNullSave(it.insertSizeMedian), //Median PE_insertsize
                dateFromFileSystem:abstractMergedBamFile.dateFromFileSystem?.format("yyyy-MM-dd"),
                //warning for duplicates
                duplicateWarning: warningLevelForDuplicates(duplicates).styleClass,

                //warning for Median PE_insertsize
                medianWarning: warningLevelForMedian(it.insertSizeMedian, readLength).styleClass,

                //warning for properlyPpaired
                properlyPpairedWarning: warningLevelForProperlyPaired(properlyPaired).styleClass,

                plot: it.id,
                withdrawn: abstractMergedBamFile.withdrawn,
                workflow: abstractMergedBamFile.workPackage.workflow.html
            ]
            if (it instanceof OverallQualityAssessmentMerged) {
                map << [
                        meanPE_Insertsize: FormatHelper.formatToTwoDecimalsNullSave(it.insertSizeMean), //Mean PE_insertsize
                ]
            } else {
                // there is no insert size mean for RoddyBamFile QA
                map << [
                        meanPE_Insertsize: "-"
                ]
            }

            switch (seqType.name) {
                case SeqTypeNames.WHOLE_GENOME.seqTypeName:
                    long referenceGenomeLengthWithoutN = sequenceLengthsAndReferenceGenomeLengthWithoutNMap[it.id][0][2] as long
                    Double coverageX
                    Double coverageY
                    if (referenceGenomeLengthWithoutN) {
                        Map chromosomeMap = chromosomeMapXY[it.qualityAssessmentMergedPass.id]
                        AbstractQualityAssessment x = getQualityAssessmentForFirstMatchingChromosomeName(chromosomeMap, [Chromosomes.CHR_X.alias, CHR_X_HG19])
                        AbstractQualityAssessment y = getQualityAssessmentForFirstMatchingChromosomeName(chromosomeMap, [Chromosomes.CHR_Y.alias, CHR_Y_HG19])
                        long qcBasesMappedXChromosome = x.qcBasesMapped
                        long qcBasesMappedYChromosome = y.qcBasesMapped
                        coverageX = qcBasesMappedXChromosome / referenceGenomeLengthWithoutN
                        coverageY = qcBasesMappedYChromosome / referenceGenomeLengthWithoutN
                    }

                    map << [
                        coverageWithoutN: FormatHelper.formatToTwoDecimalsNullSave(abstractMergedBamFile.coverage), //Coverage w/o N
                        coverageWitN: FormatHelper.formatToTwoDecimalsNullSave(abstractMergedBamFile.coverageWithN), //Coverage wN
                        coverageX: FormatHelper.formatToTwoDecimalsNullSave(coverageX), //ChrX Coverage w/o N
                        coverageY: FormatHelper.formatToTwoDecimalsNullSave(coverageY), //ChrY Coverage w/o N
                    ]
                    break
                case SeqTypeNames.EXOME.seqTypeName:
                    double onTargetRate = it.onTargetMappedBases / it.allBasesMapped * 100.0
                    map << [
                        onTargetRate: FormatHelper.formatToTwoDecimalsNullSave(onTargetRate),//on target ratio
                        targetCoverage: FormatHelper.formatToTwoDecimalsNullSave(abstractMergedBamFile.coverage), //coverage

                        //warning for onTargetRate
                        onTargetRateWarning: warningLevelForOnTargetRate(onTargetRate).styleClass,
                    ]
                    break
                default:
                    throw new RuntimeException("How should ${seqTypeName} be handled")
            }

            return map
        }

        render dataToRender as JSON
    }

    private AbstractQualityAssessment getQualityAssessmentForFirstMatchingChromosomeName(Map qualityAssessmentMergedPassGroupedByChromosome, List<String> chromosomeNames) {
        return exactlyOneElement(chromosomeNames.findResult { qualityAssessmentMergedPassGroupedByChromosome.get(it) })
    }

    private static WarningLevel warningLevelForDuplicates(Double duplicates) {
        if (duplicates > 25) {
            return WarningLevel.WarningLevel2
        } else if (duplicates > 15) {
            return WarningLevel.WarningLevel1
        } else {
            return WarningLevel.NO
        }
    }

    private static WarningLevel warningLevelForProperlyPaired(Double properlyPaired) {
        if (properlyPaired < 90) {
            return WarningLevel.WarningLevel2
        } else if (properlyPaired < 95) {
            return WarningLevel.WarningLevel1
        } else {
            return WarningLevel.NO
        }
    }

    private static WarningLevel warningLevelForMedian(Double median, double readLength) {
        if(median < 2.2 * readLength){
            return WarningLevel.WarningLevel2
        } else if(median < 2.5 * readLength) {
            return WarningLevel.WarningLevel1
        } else {
            return WarningLevel.NO
        }
    }
    private static WarningLevel warningLevelForOnTargetRate(Double onTargetRate) {
        if (onTargetRate < 60) {
            return WarningLevel.WarningLevel2
        } else if (onTargetRate < 70) {
            return WarningLevel.WarningLevel1
        } else {
            return WarningLevel.NO
        }
    }

}
