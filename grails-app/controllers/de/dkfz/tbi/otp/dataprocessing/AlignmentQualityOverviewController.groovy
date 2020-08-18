/*
 * Copyright 2011-2019 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing

import grails.converters.JSON
import grails.validation.Validateable

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerService
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.qcTrafficLight.*
import de.dkfz.tbi.otp.utils.*

import java.nio.file.AccessDeniedException
import java.nio.file.NoSuchFileException

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class AlignmentQualityOverviewController implements CheckAndCall {

    static final String CHR_X_HG19 = 'chrX'
    static final String CHR_Y_HG19 = 'chrY'

    private static final List<String> CHROMOSOMES = [Chromosomes.CHR_X.alias, Chromosomes.CHR_Y.alias, CHR_X_HG19, CHR_Y_HG19].asImmutable()

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
            'alignment.quality.arribaPlots',
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

    private static final List<String> HEADER_CELL_RANGER = [
            'alignment.quality.individual',
            'alignment.quality.sampleType',
            'alignment.quality.qcStatus',
            'alignment.quality.cell.ranger.summary',
            'alignment.quality.cell.ranger.referenceGenome',
            'alignment.quality.cell.ranger.cellRangerVersion',
            'alignment.quality.cell.ranger.cells.expected',
            'alignment.quality.cell.ranger.cells.enforced',
            'alignment.quality.cell.ranger.estimatedNumberOfCells',
            'alignment.quality.cell.ranger.meanReadsPerCell',
            'alignment.quality.cell.ranger.medianGenesPerCell',
            'alignment.quality.cell.ranger.numberOfReads',
            'alignment.quality.cell.ranger.validBarcodes',
            'alignment.quality.cell.ranger.sequencingSaturation',
            'alignment.quality.cell.ranger.q30BasesInBarcode',
            'alignment.quality.cell.ranger.q30BasesInRnaRead',
            'alignment.quality.cell.ranger.q30BasesInUmi',
            'alignment.quality.cell.ranger.readsMappedConfidentlyToIntergenicRegions',
            'alignment.quality.cell.ranger.readsMappedConfidentlyToIntronicRegions',
            'alignment.quality.cell.ranger.readsMappedConfidentlyToExonicRegions',
            'alignment.quality.cell.ranger.readsMappedConfidentlyToTranscriptome',
            'alignment.quality.cell.ranger.fractionReadsInCells',
            'alignment.quality.cell.ranger.totalGenesDetected',
            'alignment.quality.cell.ranger.medianUmiCountsPerCell',
            'alignment.quality.kit',
            'alignment.quality.date',
    ]

    private static final List<String> SUPPORTED_SEQ_TYPES = [
            SeqTypeNames.WHOLE_GENOME.seqTypeName,
            SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName,
            SeqTypeNames.WHOLE_GENOME_BISULFITE_TAGMENTATION.seqTypeName,
            SeqTypeNames.CHIP_SEQ.seqTypeName,
            SeqTypeNames.EXOME.seqTypeName,
            SeqTypeNames.RNA.seqTypeName,
            SeqTypeNames._10X_SCRNA.seqTypeName,
    ].asImmutable()

    OverallQualityAssessmentMergedService overallQualityAssessmentMergedService

    ChromosomeQualityAssessmentMergedService chromosomeQualityAssessmentMergedService

    ReferenceGenomeService referenceGenomeService

    SeqTypeService seqTypeService

    CellRangerService cellRangerService

    ProjectSelectionService projectSelectionService
    QcThresholdService qcThresholdService
    QcTrafficLightService qcTrafficLightService

    def index(AlignmentQcCommand cmd) {
        Project project = projectSelectionService.selectedProject

        if (cmd.sample && cmd.sample.project != project) {
            response.sendError(404)
            return []
        }

        List<SeqType> seqTypes = seqTypeService.alignableSeqTypesByProject(project).findAll {
            it.name in SUPPORTED_SEQ_TYPES
        }

        SeqType seqType = (cmd.seqType && seqTypes.contains(cmd.seqType)) ? cmd.seqType : seqTypes[0]

        List<String> header
        String columns = ""
        switch (seqType?.name) {
            case null:
                header = ['alignment.quality.noSeqType']
                break
            case SeqTypeNames.WHOLE_GENOME.seqTypeName:
            case SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName:
            case SeqTypeNames.WHOLE_GENOME_BISULFITE_TAGMENTATION.seqTypeName:
            case SeqTypeNames.CHIP_SEQ.seqTypeName:
                header = HEADER_WHOLE_GENOME
                columns = "WHOLE_GENOME"
                break
            case SeqTypeNames.EXOME.seqTypeName:
                header = HEADER_EXOME
                columns = "EXOME"
                break
            case SeqTypeNames.RNA.seqTypeName:
                header = HEADER_RNA
                columns = "RNA"
                break
            case SeqTypeNames._10X_SCRNA.seqTypeName:
                header = HEADER_CELL_RANGER
                columns = "CELL_RANGER"
                break
            default:
                throw new RuntimeException("How should ${seqType.naturalId} be handled")
        }

        return [
                seqTypes: seqTypes,
                seqType : seqType,
                header  : header,
                columns : columns,
                sample  : cmd.sample,
        ]
    }

    JSON changeQcStatus(QcStatusCommand cmd) {
        checkErrorAndCallMethod(cmd) {
            qcTrafficLightService.setQcTrafficLightStatusWithComment(
                    cmd.abstractBamFile,
                    cmd.newValue as AbstractMergedBamFile.QcTrafficLightStatus,
                    cmd.comment
            )
        }
    }

    @SuppressWarnings(['AbcMetric', 'CyclomaticComplexity', 'MethodSize'])
    JSON dataTableSource(AlignmentQcDataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()

        Project project = projectSelectionService.requestedProject
        if (!cmd.seqType) {
            dataToRender.iTotalRecords = 0
            dataToRender.iTotalDisplayRecords = 0
            dataToRender.aaData = []
            render dataToRender as JSON
            return
        }

        List<AbstractQualityAssessment> dataOverall = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(project, cmd.seqType, cmd.sample)
        List<AbstractQualityAssessment> dataChromosomeXY = chromosomeQualityAssessmentMergedService.qualityAssessmentMergedForSpecificChromosomes(CHROMOSOMES,
                dataOverall*.qualityAssessmentMergedPass)
        Map<Long, Map<String, List<AbstractQualityAssessment>>> chromosomeMapXY
        chromosomeMapXY = dataChromosomeXY.groupBy([
                { it.qualityAssessmentMergedPass.id },
                { it.chromosomeName },
        ])

        QcThresholdService.ThresholdColorizer thresholdColorizer
        if (dataOverall) {
            thresholdColorizer = qcThresholdService.createThresholdColorizer(project, cmd.seqType, dataOverall.first().class)
        }

        Map<Long, Map<String, List<ReferenceGenomeEntry>>> chromosomeLengthForChromosome =
                overallQualityAssessmentMergedService.findChromosomeLengthForQualityAssessmentMerged(CHROMOSOMES, dataOverall).
                        groupBy([{ it.referenceGenome.id }, { it.alias }])

        dataToRender.iTotalRecords = dataOverall.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = dataOverall.collect { AbstractQualityAssessment it ->
            QualityAssessmentMergedPass qualityAssessmentMergedPass = it.qualityAssessmentMergedPass
            AbstractMergedBamFile abstractMergedBamFile = qualityAssessmentMergedPass.abstractMergedBamFile
            Set<SeqTrack> seqTracks = abstractMergedBamFile.mergingWorkPackage.seqTracks
            /*
                This method assumes, that it does not matter which seqTrack is used to get the sequencedLength.
                Within one merged bam file all are the same. This is incorrect, see OTP-1670.
            */
            Double readLength = null
            if (seqTracks) {
                DataFile dataFile = DataFile.findBySeqTrack(seqTracks.first())
                String readLengthString = dataFile.sequenceLength
                if (readLengthString) {
                    readLength = readLengthString.contains('-') ? (readLengthString.split('-').sum {
                        it as double
                    } / 2) : readLengthString as double
                }
            }

            Set<LibraryPreparationKit> kit = qualityAssessmentMergedPass.containedSeqTracks*.libraryPreparationKit.findAll().unique()
            TableCellValue.Icon icon = [
                    (AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED) : TableCellValue.Icon.WARNING,
                    (AbstractMergedBamFile.QcTrafficLightStatus.REJECTED): TableCellValue.Icon.ERROR,
            ].getOrDefault(abstractMergedBamFile.qcTrafficLightStatus, TableCellValue.Icon.OKAY)
            String comment = abstractMergedBamFile.comment ? "\n${abstractMergedBamFile.comment?.comment}\n${abstractMergedBamFile.comment?.author}" : ""
            Map<String, TableCellValue> map = [
                    pid               : new TableCellValue(
                            value: abstractMergedBamFile.individual.displayName,
                            link : g.createLink(
                                    controller: 'individual',
                                    action: 'show',
                                    id: abstractMergedBamFile.individual.id,
                            ).toString()
                    ),
                    sampleType        : abstractMergedBamFile.sampleType.name,
                    dateFromFileSystem: abstractMergedBamFile.dateFromFileSystem?.format("yyyy-MM-dd"),
                    withdrawn         : abstractMergedBamFile.withdrawn,
                    pipeline          : abstractMergedBamFile.workPackage.pipeline.displayName,
                    qcStatus          : new TableCellValue(
                            abstractMergedBamFile.comment ?
                                    "${abstractMergedBamFile.comment?.comment?.take(10)}" :
                                    "",
                            null, null,
                            "Status: ${(abstractMergedBamFile.qcTrafficLightStatus ?: "").toString()} ${comment}",
                            icon, (abstractMergedBamFile.qcTrafficLightStatus ?: "").toString(), abstractMergedBamFile.id
                    ),
                    kit               : new TableCellValue(
                            kit*.shortDisplayName.join(", ") ?: "-", null, null,
                            kit*.name.join(", ") ?: ""
                    ),
            ]

            Map<String, Double> qcKeysMap = [
                    insertSizeMedian: readLength,
            ]
            List<String> qcKeys = [
                    "percentMappedReads",
                    "percentDuplicates",
                    "percentDiffChr",
                    "percentProperlyPaired",
                    "percentSingletons",
            ]

            switch (cmd.seqType.name) {
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
                            targetCoverage: FormatHelper.formatNumber(abstractMergedBamFile.coverage),
                    ]
                    qcKeys += [
                            "onTargetRatio",
                    ]
                    break

                case SeqTypeNames.RNA.seqTypeName:
                    map << [
                            arribaPlots               : new TableCellValue(
                                    value: "PDF",
                                    linkTarget        : "_blank",
                                    link : g.createLink(
                                            action: "renderPDF",
                                            params: ["abstractMergedBamFile.id": abstractMergedBamFile.id])
                            ),
                    ]
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

                case SeqTypeNames._10X_SCRNA.seqTypeName:
                    map << [
                            summary: new TableCellValue(
                                value     : "summary",
                                linkTarget: "_blank",
                                link      : g.createLink(
                                        action: "viewCellRangerSummary",
                                        params: [
                                                "singleCellBamFile.id": ((SingleCellBamFile) abstractMergedBamFile).id,
                                        ],
                                ).toString()
                            ),
                            referenceGenome: abstractMergedBamFile.workPackage.referenceGenome.name,
                            cellRangerVersion: abstractMergedBamFile.workPackage.referenceGenomeIndex.indexToolVersion,
                    ]
                    qcKeys += [
                            'expectedCells',
                            'enforcedCells',
                            'estimatedNumberOfCells',
                            'meanReadsPerCell',
                            'medianGenesPerCell',
                            'numberOfReads',
                            'validBarcodes',
                            'sequencingSaturation',
                            'q30BasesInBarcode',
                            'q30BasesInRnaRead',
                            'q30BasesInUmi',
                            'readsMappedConfidentlyToIntergenicRegions',
                            'readsMappedConfidentlyToIntronicRegions',
                            'readsMappedConfidentlyToExonicRegions',
                            'readsMappedConfidentlyToTranscriptome',
                            'fractionReadsInCells',
                            'totalGenesDetected',
                            'medianUmiCountsPerCell',
                    ]
                    break

                default:
                    throw new RuntimeException("${cmd.seqType.name} cannot be handled")
            }

            map += thresholdColorizer.colorize(qcKeysMap, it)
            map += thresholdColorizer.colorize(qcKeys, it)
            return map
        }
        render dataToRender as JSON
    }

    def viewCellRangerSummary(ViewCellRangerSummaryCommand cmd) {
        String content
        try {
            content = cellRangerService.getWebSummaryResultFileContent(cmd.singleCellBamFile)
        } catch (NoSuchFileException e) {
            flash.message = new FlashMessage(g.message(code: "alignment.quality.exception.noSuchFile") as String, e.message)
            redirect(action: "index")
        } catch (AccessDeniedException e) {
            flash.message = new FlashMessage(g.message(code: "alignment.quality.exception.accessDenied") as String, e.message)
            redirect(action: "index")
        }
        render text: content, contentType: "text/html", encoding: "UTF-8"
    }

    private static AbstractQualityAssessment getQualityAssessmentForFirstMatchingChromosomeName(Map<String,
            List<AbstractQualityAssessment>> qualityAssessmentMergedPassGroupedByChromosome, List<String> chromosomeNames) {
        return exactlyOneElement(chromosomeNames.findResult { qualityAssessmentMergedPassGroupedByChromosome.get(it) })
    }

    def renderPDF(AbstractMergedBamFileCommand cmd) {
        if (cmd.hasErrors()) {
            response.sendError(404)
            return
        }

        // This page is semi-generic over AbstractMergedBamFile, with lots of SeqType-specific handling sprinkled all over.
        // This link is only generated for seqType RNA, so this cast is probably safe.
        if (cmd.abstractMergedBamFile instanceof RnaRoddyBamFile) {
            RnaRoddyBamFile rrbf = cmd.abstractMergedBamFile as RnaRoddyBamFile
            File file = rrbf.workArribaFusionPlotPdf
            if (file.exists()) {
                render file: file, contentType: "application/pdf"
            } else {
                render text: "no plot available", contentType: "text/plain"
            }
        } else {
            render status: 404
        }
    }
}

class AlignmentQcCommand {
    SeqType seqType
    Sample sample

    static constraints = {
        sample nullable: true
    }
}

class AlignmentQcDataTableCommand extends DataTableCommand {
    SeqType seqType
    Sample sample

    static constraints = {
        sample nullable: true
    }
}

class ViewCellRangerSummaryCommand {
    SingleCellBamFile singleCellBamFile
}

@SuppressWarnings('SerializableClassMustDefineSerialVersionUID')
class QcStatusCommand implements Validateable {
    String comment
    AbstractBamFile abstractBamFile
    String newValue

    @SuppressWarnings('Instanceof')
    static constraints = {
        comment(blank: false, nullable: false, validator: { val, obj ->
            if (val == obj.abstractBamFile?.comment?.comment) {
                return "not.changed"
            }
        })
        abstractBamFile(nullable: false, validator: { val, obj ->
            if (!((val instanceof RoddyBamFile) || (val instanceof SingleCellBamFile))) {
                return "invalid"
            }
        })
        newValue(blank: false, nullable: false, validator: { val, obj, errors ->
            if (!(val in AbstractMergedBamFile.QcTrafficLightStatus.values()*.toString())) {
                return ["status", AbstractMergedBamFile.QcTrafficLightStatus.values().join(", ")]
            }
        })
    }

    void setComment(String comment) {
        this.comment = StringUtils.trimAndShortenWhitespace(comment)
    }
}

class AbstractMergedBamFileCommand {
    AbstractMergedBamFile abstractMergedBamFile
}
