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
package de.dkfz.tbi.otp.dataprocessing

import grails.converters.JSON
import grails.validation.Validateable
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerConfigurationService
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerService
import de.dkfz.tbi.otp.dataprocessing.qaalignmentoverview.QaOverviewService
import de.dkfz.tbi.otp.dataprocessing.qaalignmentoverview.QcStatusCellService
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.job.processing.RoddyConfigService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightService
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.workflow.alignment.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflow.alignment.wgbs.WgbsWorkflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowService

import java.nio.file.*

@PreAuthorize('isFullyAuthenticated()')
class AlignmentQualityOverviewController implements CheckAndCall {

    static allowedMethods = [
            index                : "GET",
            changeQcStatus       : "POST",
            dataTableSource      : "POST",
            viewCellRangerSummary: "GET",
            renderPDF            : "GET",
            viewConfigFile       : "GET",
    ]

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
            'alignment.quality.createdWithVersion',
            'alignment.quality.referenceGenome',
            'alignment.quality.date',
            'alignment.quality.configFile',
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
            'alignment.quality.createdWithVersion',
            'alignment.quality.referenceGenome',
            'alignment.quality.date',
            'alignment.quality.configFile',
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
            'alignment.quality.createdWithVersion',
            'alignment.quality.referenceGenome',
            'alignment.quality.date',
            'alignment.quality.configFile',
    ].asImmutable()

    private static final List<String> HEADER_CELL_RANGER = HEADER_COMMON + [
            'alignment.quality.cell.ranger.summary',
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
            'alignment.quality.createdWithVersion',
            'alignment.quality.referenceGenome',
            'alignment.quality.date',
    ].asImmutable()

    CellRangerConfigurationService cellRangerConfigurationService
    CellRangerService cellRangerService
    ConfigService configService
    FileService fileService
    FileSystemService fileSystemService
    ProcessingOptionService processingOptionService
    ProjectSelectionService projectSelectionService
    QaOverviewService qaOverviewService
    QcStatusCellService qcStatusCellService
    QcTrafficLightService qcTrafficLightService
    ReferenceGenomeService referenceGenomeService
    RoddyConfigService roddyConfigService
    RoddyResultServiceFactoryService roddyResultServiceFactoryService
    SeqTypeService seqTypeService
    WorkflowService workflowService

    def index(AlignmentQcCommand cmd) {
        Project project = projectSelectionService.selectedProject

        if (cmd.sample && cmd.sample.project != project) {
            return response.sendError(HttpStatus.NOT_FOUND.value())
        }

        List<SeqType> suppSeqTypes = qaOverviewService.allSupportedSeqTypes()
        List<SeqType> seqTypes = seqTypeService.alignableSeqTypesByProject(project).findAll {
            it in suppSeqTypes
        }

        SeqType seqType = (cmd.seqType && seqTypes.contains(cmd.seqType)) ? cmd.seqType : seqTypes[0]

        List<String> header
        String columnsSelectionKey = ""
        switch (seqType) {
            case null:
                header = ['alignment.quality.noSeqType']
                break
            case workflowService.getSupportedSeqTypes(PanCancerWorkflow.WORKFLOW).findAll { !it.needsBedFile }:
            case workflowService.getSupportedSeqTypes(WgbsWorkflow.WGBS_WORKFLOW):
                header = HEADER_PANCANCER_AND_WGBS
                columnsSelectionKey = "PANCANCER_AND_WGBS"
                break
            case workflowService.getSupportedSeqTypes(PanCancerWorkflow.WORKFLOW).findAll { it.needsBedFile }:
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
            // if db version has changed after loading the page -> someone else has made changes
            // simplified way to resolve concurrent access, cheaper than explicit locking
            if (cmd.abstractBamFile.version > cmd.dbVersion) {
                String errorMessage = g.message(code: "alignment.quality.concurrentWrite.message")
                return response.sendError(HttpStatus.CONFLICT.value(), errorMessage)
            }

            qcTrafficLightService.setQcTrafficLightStatusWithComment(
                    cmd.abstractBamFile as AbstractBamFile,
                    cmd.newValue as AbstractBamFile.QcTrafficLightStatus,
                    cmd.comment
            )

            Map<String, ?> qcStatusMap = [
                    qcStatus : cmd.abstractBamFile.qualityAssessmentStatus,
                    qcComment: cmd.abstractBamFile.comment?.comment,
                    qcAuthor : cmd.abstractBamFile.comment?.author,
                    bamId    : cmd.abstractBamFile.id,
            ]

            render(qcStatusCellService.generateQcStatusCell(qcStatusMap) as JSON)
        }
    }

    def dataTableSource(AlignmentQcDataTableCommand cmd) {
        LogUsedTimeUtils.logUsedTimeStartEnd(log, "dataTableSource") {
            Map dataToRender = cmd.dataToRender()
            Project project = projectSelectionService.requestedProject

            if (!cmd.seqType) {
                dataToRender.iTotalRecords = 0
                dataToRender.iTotalDisplayRecords = 0
                dataToRender.aaData = []
                render(dataToRender as JSON)
                return
            }

            List<Map<String, ?>> dataOverall = qaOverviewService.overviewData(project, cmd.seqType, cmd.sample)

            dataToRender.iTotalRecords = dataOverall.size()
            dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
            dataToRender.aaData = dataOverall
            render(dataToRender as JSON)
        }
    }

    def viewCellRangerSummary(ViewCellRangerSummaryCommand cmd) {
        try {
            String content = cmd.singleCellBamFile.project.archived ? g.message(code: "alignment.quality.projectArchived.warning") :
                    cellRangerService.getWebSummaryResultFileContent(cmd.singleCellBamFile)
            render(text: content, contentType: "text/html", encoding: "UTF-8")
        } catch (NoSuchFileException e) {
            flash.message = new FlashMessage(g.message(code: "alignment.quality.exception.noSuchFile") as String, e.message)
            redirect(action: "index")
        } catch (AccessDeniedException e) {
            flash.message = new FlashMessage(g.message(code: "alignment.quality.exception.accessDenied") as String, e.message)
            redirect(action: "index")
        }
    }

    def renderPDF(AbstractBamFileCommand cmd) {
        if (cmd.hasErrors()) {
            return response.sendError(HttpStatus.NOT_FOUND.value())
        }

        if (!(cmd.abstractBamFile instanceof RnaRoddyBamFile)) {
            return response.sendError(HttpStatus.NOT_FOUND.value())
        }

        if (cmd.abstractBamFile.project.archived) {
            return render(g.message(code: "alignment.quality.projectArchived.warning"))
        }

        // This page is semi-generic over AbstractBamFile, with lots of SeqType-specific handling sprinkled all over.
        // This link is only generated for seqType RNA, so this cast is probably safe.
        RnaRoddyBamFile rrbf = cmd.abstractBamFile as RnaRoddyBamFile
        FileSystem fileSystem = fileSystemService.getRemoteFileSystem(cmd.abstractBamFile.realm)
        Path file = fileSystem.getPath(rrbf.workArribaFusionPlotPdf)

        if (fileService.fileIsReadable(file, configService.defaultRealm)) {
            render(file: file.bytes, contentType: "application/pdf")
        } else {
            render(text: "no plot available", contentType: "text/plain")
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#roddyResult.project, 'OTP_READ_ACCESS')")
    def viewConfigFile(RoddyBamFile roddyResult, String to) {
        Path workDir = roddyResultServiceFactoryService.getService(roddyResult).getWorkDirectory(roddyResult)
        Path configFile = roddyConfigService.getConfigFile(workDir)

        if (fileService.fileIsReadable(configFile, configService.defaultRealm)) {
            render(
                    file: configFile.bytes,
                    contentType: "text/plain",
                    fileName: (to == 'DOWNLOAD') ? "config.txt" : null,
            )
        } else {
            render(text: "no config file available", contentType: "text/plain")
        }
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
            if (!(val in AbstractBamFile.QcTrafficLightStatus.values()*.toString())) {
                return ["status", AbstractBamFile.QcTrafficLightStatus.values().join(", ")]
            }
        })
        dbVersion(blank: false)
    }

    void setComment(String comment) {
        this.comment = StringUtils.trimAndShortenWhitespace(comment)
    }
}

class AbstractBamFileCommand {
    AbstractBamFile abstractBamFile
}
