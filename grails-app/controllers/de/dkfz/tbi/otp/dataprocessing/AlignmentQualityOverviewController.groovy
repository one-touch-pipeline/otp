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
import grails.plugin.springsecurity.annotation.Secured
import grails.validation.Validateable
import org.springframework.http.HttpStatus

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerConfigurationService
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerService
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.qcTrafficLight.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.workflow.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflowExecution.Workflow
import de.dkfz.tbi.util.TimeFormats

import java.nio.file.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

@Secured('isFullyAuthenticated()')
class AlignmentQualityOverviewController implements CheckAndCall {

    static allowedMethods = [
            index                : "GET",
            changeQcStatus       : "POST",
            dataTableSource      : "POST",
            viewCellRangerSummary: "GET",
            renderPDF            : "GET",
    ]

    static final String CHR_X_HG19 = 'chrX'
    static final String CHR_Y_HG19 = 'chrY'

    private static final List<String> CHROMOSOMES = [Chromosomes.CHR_X.alias, Chromosomes.CHR_Y.alias, CHR_X_HG19, CHR_Y_HG19].asImmutable()

    private static final List<String> HEADER_COMMON = [
            'alignment.quality.rowId',
            'alignment.quality.individual',
            'alignment.quality.sampleType',
            'alignment.quality.qcStatus',
            'alignment.quality.qcStatus',
            'alignment.quality.qcComment',
            'alignment.quality.qcAuthor',
            'alignment.quality.dbVersion',
    ].asImmutable()

    private static final List<String> HEADER_PANCANCER_AND_WGBS = HEADER_COMMON + [
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

    private static final List<String> HEADER_RNA = HEADER_COMMON + [
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

    private static final List<String> HEADER_PANCANCER_BED = HEADER_COMMON + [
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

    private static final List<String> HEADER_CELL_RANGER = HEADER_COMMON + [
            'alignment.quality.cell.ranger.summary',
            'alignment.quality.cell.ranger.cellRangerVersion',
            'alignment.quality.cell.ranger.referenceGenome',
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
    ].asImmutable()

    OverallQualityAssessmentMergedService overallQualityAssessmentMergedService
    ChromosomeQualityAssessmentMergedService chromosomeQualityAssessmentMergedService
    ReferenceGenomeService referenceGenomeService
    SeqTypeService seqTypeService
    CellRangerService cellRangerService
    FileSystemService fileSystemService
    ProjectSelectionService projectSelectionService
    QcThresholdService qcThresholdService
    QcTrafficLightService qcTrafficLightService
    ProcessingOptionService processingOptionService
    CellRangerConfigurationService cellRangerConfigurationService
    DataFileService dataFileService

    def index(AlignmentQcCommand cmd) {
        Project project = projectSelectionService.selectedProject

        if (cmd.sample && cmd.sample.project != project) {
            return response.sendError(HttpStatus.NOT_FOUND.value())
        }

        List<String> suppSeqTypes = supportedSeqTypes
        List<SeqType> seqTypes = seqTypeService.alignableSeqTypesByProject(project).findAll {
            it.name in suppSeqTypes
        }

        SeqType seqType = (cmd.seqType && seqTypes.contains(cmd.seqType)) ? cmd.seqType : seqTypes[0]

        List<String> header
        String columnsSelectionKey = ""
        switch (seqType) {
            case null:
                header = ['alignment.quality.noSeqType']
                break
            case exactlyOneElement(Workflow.findAllByName(PanCancerWorkflow.WORKFLOW)).supportedSeqTypes.findAll { !it.needsBedFile }:
            case { it.name in [SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName, SeqTypeNames.WHOLE_GENOME_BISULFITE_TAGMENTATION.seqTypeName] }:
                header = HEADER_PANCANCER_AND_WGBS
                columnsSelectionKey = "PANCANCER_AND_WGBS"
                break
            case exactlyOneElement(Workflow.findAllByName(PanCancerWorkflow.WORKFLOW)).supportedSeqTypes.findAll { it.needsBedFile }:
                header = HEADER_PANCANCER_BED
                columnsSelectionKey = "PANCANCER_BED"
                break
            case { it.name == SeqTypeNames.RNA.seqTypeName }:
                header = HEADER_RNA
                columnsSelectionKey = "RNA"
                break
            case { it.name == SeqTypeNames._10X_SCRNA.seqTypeName }:
                header = HEADER_CELL_RANGER
                columnsSelectionKey = "CELL_RANGER"
                break
            default:
                throw new NotSupportedException("How should ${seqType.naturalId} be handled")
        }

        return [
                seqTypes    : seqTypes,
                seqType     : seqType,
                header      : header,
                columns     : columnsSelectionKey,
                sample      : cmd.sample,
                supportEmail: processingOptionService.findOptionAsString(ProcessingOption.OptionName.GUI_CONTACT_DATA_SUPPORT_EMAIL),
        ]
    }

    /**
     * Change the QC status of a single row.
     *
     * @param cmd QcStatusCommand
     * @return render JSON response
     */
    def changeQcStatus(QcStatusCommand cmd) {
        checkDefaultErrorsAndCallMethod(cmd) {
            //if db version has changed after loading the page -> someone else has made changes
            //simplified way to resolve concurrent access, cheaper than explicit locking
            if (cmd.abstractBamFile.version > cmd.dbVersion) {
                String errorMessage = g.message(code: "alignment.quality.concurrentWrite.message")
                return response.sendError(HttpStatus.CONFLICT.value(), errorMessage)
            }

            qcTrafficLightService.setQcTrafficLightStatusWithComment(
                    cmd.abstractBamFile as AbstractMergedBamFile,
                    cmd.newValue as AbstractMergedBamFile.QcTrafficLightStatus,
                    cmd.comment
            )

            render generateQcStatusCell(cmd.abstractBamFile as AbstractMergedBamFile) as JSON
        }
    }

    def dataTableSource(AlignmentQcDataTableCommand cmd) {
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

        dataToRender.iTotalRecords = dataOverall.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = generateAlignmentQcTableRows(dataOverall, project, cmd.seqType)
        render dataToRender as JSON
    }

    def viewCellRangerSummary(ViewCellRangerSummaryCommand cmd) {
        try {
            String content = cmd.singleCellBamFile.project.archived ? g.message(code: "alignment.quality.projectArchived.warning") :
                    cellRangerService.getWebSummaryResultFileContent(cmd.singleCellBamFile)
            render text: content, contentType: "text/html", encoding: "UTF-8"
        } catch (NoSuchFileException e) {
            flash.message = new FlashMessage(g.message(code: "alignment.quality.exception.noSuchFile") as String, e.message)
            redirect(action: "index")
        } catch (AccessDeniedException e) {
            flash.message = new FlashMessage(g.message(code: "alignment.quality.exception.accessDenied") as String, e.message)
            redirect(action: "index")
        }
    }

    def renderPDF(AbstractMergedBamFileCommand cmd) {
        if (cmd.hasErrors()) {
            return response.sendError(HttpStatus.NOT_FOUND.value())
        }

        if (!(cmd.abstractMergedBamFile instanceof RnaRoddyBamFile)) {
            return response.sendError(HttpStatus.NOT_FOUND.value())
        }

        if (cmd.singleCellBamFile.project.archived) {
            return render(g.message(code: "alignment.quality.projectArchived.warning"))
        }

        // This page is semi-generic over AbstractMergedBamFile, with lots of SeqType-specific handling sprinkled all over.
        // This link is only generated for seqType RNA, so this cast is probably safe.
        RnaRoddyBamFile rrbf = cmd.abstractMergedBamFile as RnaRoddyBamFile
        FileSystem fileSystem = fileSystemService.getRemoteFileSystem(cmd.abstractMergedBamFile.realm)
        Path file = fileSystem.getPath(rrbf.workArribaFusionPlotPdf)

        if (Files.isReadable(file)) {
            render file: file.bytes, contentType: "application/pdf"
        } else {
            render text: "no plot available", contentType: "text/plain"
        }
    }

    @SuppressWarnings(['AbcMetric', 'CyclomaticComplexity', 'MethodSize'])
    private List<Map<String, TableCellValue>> generateAlignmentQcTableRows(List<AbstractQualityAssessment> dataOverall, Project project, SeqType seqType) {
        List<AbstractQualityAssessment> dataChromosomeXY = chromosomeQualityAssessmentMergedService.qualityAssessmentMergedForSpecificChromosomes(CHROMOSOMES,
                dataOverall*.qualityAssessmentMergedPass)
        Map<Long, Map<String, List<AbstractQualityAssessment>>> chromosomeMapXY = dataChromosomeXY.groupBy([
                { it.qualityAssessmentMergedPass.id },
                { it.chromosomeName },
        ])

        QcThresholdService.ThresholdColorizer thresholdColorizer
        if (dataOverall) {
            thresholdColorizer = qcThresholdService.createThresholdColorizer(project, seqType, dataOverall.first().class as Class<QcTrafficLightValue>)
        }

        Map<Long, Map<String, List<ReferenceGenomeEntry>>> chromosomeLengthForChromosome =
                overallQualityAssessmentMergedService.findChromosomeLengthForQualityAssessmentMerged(CHROMOSOMES, dataOverall)
                        .groupBy([{ it.referenceGenome.id }, { it.alias }])

        return dataOverall.collect { AbstractQualityAssessment it ->
            QualityAssessmentMergedPass qualityAssessmentMergedPass = it.qualityAssessmentMergedPass
            AbstractMergedBamFile abstractMergedBamFile = qualityAssessmentMergedPass.abstractMergedBamFile
            Set<SeqTrack> seqTracks = abstractMergedBamFile.mergingWorkPackage.seqTracks
            /*
                This method assumes, that it does not matter which seqTrack is used to get the sequencedLength.
                Within one merged bam file all are the same. This is incorrect, see OTP-1670.
            */
            Double readLength = null
            if (seqTracks) {
                DataFile dataFile = dataFileService.findAllBySeqTrack(seqTracks.first()).first()
                String readLengthString = dataFile.sequenceLength
                if (readLengthString) {
                    readLength = readLengthString.contains('-') ? (readLengthString.split('-').sum {
                        it as double
                    } / 2) : readLengthString as double
                }
            }

            Set<LibraryPreparationKit> kit = qualityAssessmentMergedPass.containedSeqTracks*.libraryPreparationKit.findAll().unique()

            Map<String, TableCellValue> qcTableRow = [
                    rowId             : abstractMergedBamFile.id,
                    pid               : new TableCellValue(
                            value: abstractMergedBamFile.individual.displayName,
                            link: g.createLink(
                                    controller: 'individual',
                                    action: 'show',
                                    id: abstractMergedBamFile.individual.id,
                            ).toString()
                    ),
                    sampleType        : abstractMergedBamFile.sampleType.name,
                    dateFromFileSystem: TimeFormats.DATE.getFormattedDate(abstractMergedBamFile.dateFromFileSystem),
                    withdrawn         : abstractMergedBamFile.withdrawn,
                    pipeline          : abstractMergedBamFile.workPackage.pipeline.displayName,
                    qcStatus          : generateQcStatusCell(abstractMergedBamFile),
                    qcStatusOnly      : abstractMergedBamFile.qcTrafficLightStatus,
                    qcComment         : abstractMergedBamFile.comment?.comment,
                    qcAuthor          : abstractMergedBamFile.comment?.author,
                    kit               : new TableCellValue(
                            value: kit*.shortDisplayName.join(", ") ?: "-",
                            warnColor: null,
                            link: null,
                            tooltip: kit*.name.join(", ") ?: ""
                    ),
                    dbVersion         : new TableCellValue(
                            value: abstractMergedBamFile.version
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

            switch (seqType) {
                case exactlyOneElement(Workflow.findAllByName(PanCancerWorkflow.WORKFLOW)).supportedSeqTypes.findAll { !it.needsBedFile }:
                case { it.name in [SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName, SeqTypeNames.WHOLE_GENOME_BISULFITE_TAGMENTATION.seqTypeName] }:
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

                    qcTableRow << [
                            coverageWithoutN: FormatHelper.formatNumber(abstractMergedBamFile.coverage), //Coverage w/o N
                            coverageX       : FormatHelper.formatNumber(coverageX), //ChrX Coverage w/o N
                            coverageY       : FormatHelper.formatNumber(coverageY), //ChrY Coverage w/o N
                    ]
                    break

                case exactlyOneElement(Workflow.findAllByName(PanCancerWorkflow.WORKFLOW)).supportedSeqTypes.findAll { it.needsBedFile }:
                    qcTableRow << [
                            targetCoverage: FormatHelper.formatNumber(abstractMergedBamFile.coverage),
                    ]
                    qcKeys += [
                            "onTargetRatio",
                    ]
                    break

                case { it.name == SeqTypeNames.RNA.seqTypeName }:
                    qcTableRow << [
                            arribaPlots: new TableCellValue(
                                    archived: project.archived,
                                    value: "PDF",
                                    linkTarget: "_blank",
                                    link: g.createLink(
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

                case { it.name == SeqTypeNames._10X_SCRNA.seqTypeName }:
                    qcTableRow << [
                            summary          : new TableCellValue(
                                    archived: project.archived,
                                    value: "Summary",
                                    linkTarget: "_blank",
                                    link: g.createLink(
                                            action: "viewCellRangerSummary",
                                            params: [
                                                    "singleCellBamFile.id": ((SingleCellBamFile) abstractMergedBamFile).id,
                                            ],
                                    ).toString()
                            ),
                            referenceGenome  : abstractMergedBamFile.workPackage.referenceGenomeIndex.toString(),
                            cellRangerVersion: ((SingleCellBamFile)abstractMergedBamFile).mergingWorkPackage.config.programVersion,
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
                    throw new NotSupportedException("${seqType.name} cannot be handled")
            }

            qcTableRow += thresholdColorizer.colorize(qcKeysMap, it)
            qcTableRow += thresholdColorizer.colorize(qcKeys, it)
            return qcTableRow
        }
    }

    /**
     * Generate the QC Status info in a DataTable cell format.
     *
     * @param abstractMergedBamFile containing the infos to display
     * @return TableCellValue for the DataTable
     */
    private static TableCellValue generateQcStatusCell(AbstractMergedBamFile abstractMergedBamFile) {
        TableCellValue.Icon icon = [
                (AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED) : TableCellValue.Icon.WARNING,
                (AbstractMergedBamFile.QcTrafficLightStatus.WARNING) : TableCellValue.Icon.WARNING,
                (AbstractMergedBamFile.QcTrafficLightStatus.REJECTED): TableCellValue.Icon.ERROR,
        ].getOrDefault(abstractMergedBamFile.qcTrafficLightStatus, TableCellValue.Icon.OKAY)
        String comment = abstractMergedBamFile.comment ? "\n${abstractMergedBamFile.comment?.comment}\n${abstractMergedBamFile.comment?.author}" : ""

        return new TableCellValue(
                value: abstractMergedBamFile.comment ? "${abstractMergedBamFile.comment?.comment}" : "",
                warnColor: null,
                link: null,
                tooltip: "Status: ${(abstractMergedBamFile.qcTrafficLightStatus)} ${comment}",
                icon: icon,
                status: (abstractMergedBamFile.qcTrafficLightStatus).toString(),
                id: abstractMergedBamFile.id
        )
    }

    private static AbstractQualityAssessment getQualityAssessmentForFirstMatchingChromosomeName(Map<String,
            List<AbstractQualityAssessment>> qualityAssessmentMergedPassGroupedByChromosome, List<String> chromosomeNames) {
        return exactlyOneElement(chromosomeNames.findResult { qualityAssessmentMergedPassGroupedByChromosome.get(it) })
    }

    private List<String> getSupportedSeqTypes() {
        return [
                SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName,
                SeqTypeNames.WHOLE_GENOME_BISULFITE_TAGMENTATION.seqTypeName,
                SeqTypeNames.RNA.seqTypeName,
                SeqTypeNames._10X_SCRNA.seqTypeName,
        ] + exactlyOneElement(Workflow.findAllByName(PanCancerWorkflow.WORKFLOW)).supportedSeqTypes*.name
    }
}

class AlignmentQcCommand {
    SeqType seqType
    Sample sample

    static Closure constraints = {
        sample nullable: true
    }
}

class AlignmentQcDataTableCommand extends DataTableCommand {
    SeqType seqType
    Sample sample

    static Closure constraints = {
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
    int dbVersion

    @SuppressWarnings('Instanceof')
    static Closure constraints = {
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
        dbVersion(blank: false, nullable: true)
    }

    void setComment(String comment) {
        this.comment = StringUtils.trimAndShortenWhitespace(comment)
    }
}

class AbstractMergedBamFileCommand {
    AbstractMergedBamFile abstractMergedBamFile
}
