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
package de.dkfz.tbi.otp.ngsdata

import grails.converters.JSON
import grails.validation.Validateable
import groovy.transform.Immutable
import groovy.transform.TupleConstructor
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.multipart.MultipartFile

import de.dkfz.tbi.otp.CheckAndCall
import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.ContentWithPathAndProblems
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.FastqMetadataValidationService
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.directorystructures.DirectoryStructureBeanName
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.metadatasource.MetaDataFileSourceEnum
import de.dkfz.tbi.otp.security.Role
import de.dkfz.tbi.otp.tracking.Ticket
import de.dkfz.tbi.otp.tracking.TicketService
import de.dkfz.tbi.otp.user.UserException
import de.dkfz.tbi.otp.utils.CommentCommand
import de.dkfz.tbi.otp.utils.StringUtils
import de.dkfz.tbi.otp.utils.error.*
import de.dkfz.tbi.otp.utils.exceptions.MetadataFileImportException
import de.dkfz.tbi.util.TimeFormats
import de.dkfz.tbi.util.TimeUtils
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.*

import java.nio.file.FileSystem
import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern

@PreAuthorize("hasRole('ROLE_OPERATOR')")
class MetadataImportController implements CheckAndCall, PlainResponseExceptionHandler {

    static allowedMethods = [
            index                            : "GET",
            importByPathOrContent            : "POST",
            details                          : "GET",
            multiDetails                     : "GET",
            autoImport                       : "GET",
            blacklistedIlseNumbers           : "GET",
            addBlacklistedIlseNumbers        : "POST",
            unBlacklistIlseSubmissions       : "POST",
            saveComment                      : "POST",
            assignTicketToFastqImportInstance: "POST",
            updateSeqCenterComment           : "POST",
            updateAutomaticNotificationFlag  : "POST",
            updateFinalNotificationFlag      : "POST",
            validatePathsOrFiles             : "POST",
    ]

    CommentService commentService
    ConfigService configService
    RawSequenceFileService rawSequenceFileService
    FileSystemService fileSystemService
    IlseSubmissionService ilseSubmissionService
    MetadataImportService metadataImportService
    TicketService ticketService
    ProcessingOptionService processingOptionService
    FastqMetadataValidationService fastqMetadataValidationService
    RunService runService

    def index() {
        params.paths = params.paths instanceof String ? [params.paths] : params.paths
        return [
                cmd                   : params,
                metadataFileSources   : MetaDataFileSourceEnum.values(),
                directoryStructures   : DirectoryStructureBeanName.values(),
                implementedValidations: metadataImportService.implementedValidations,
        ]
    }

    def validatePathsOrFiles(MetadataImportControllerSubmitCommand cmd) {
        checkDefaultErrorsAndCallMethod(cmd) {
            List<ContentWithPathAndProblems> contentsWithPathAndProblems = null
            if (cmd.metadataFileSource == MetaDataFileSourceEnum.PATH && cmd.paths) {
                FileSystem fs = fileSystemService.remoteFileSystem
                contentsWithPathAndProblems = cmd.paths.collect { metadataFilePath ->
                    return fastqMetadataValidationService.readPath(fs.getPath(metadataFilePath))
                }
            } else if (cmd.metadataFileSource == MetaDataFileSourceEnum.FILE && cmd.contentList) {
                contentsWithPathAndProblems = cmd.contentList.collect { content ->
                    return new ContentWithPathAndProblems(content.bytes, Paths.get(content.originalFilename))
                }
            }

            List<MetadataValidationContext> metadataValidationContexts = contentsWithPathAndProblems.collect { contentWithPathAndProblems ->
                metadataImportService.validateWithAuth(contentWithPathAndProblems, cmd.directoryStructure, cmd.ignoreMd5sumError)
            }

            render([
                    contexts           : metadataValidationContexts.collect { context ->
                        [spreadsheet        : new SpreadsheetDTO(context),
                         problems           : context.problems?.collect { problem -> new ProblemDTO(problem, context) },
                         summary            : context.summary,
                         maximumProblemLevel: LogLevel.normalize(context.maximumProblemLevel).name,]
                    },
                    problemLevel       : metadataValidationContexts*.maximumProblemLevel*.intValue().max(),
                    warningLevel       : LogLevel.WARNING.intValue(),
                    metadataFileMd5sums: metadataValidationContexts*.metadataFileMd5sum,
            ] as JSON)
        }
    }

    def importByPathOrContent(MetadataImportControllerSubmitCommand cmd) {
        checkDefaultErrorsAndCallMethod(cmd) {
            try {
                withForm {
                    FileSystem fs = fileSystemService.remoteFileSystem
                    List<ContentWithProblemsAndPreviousMd5sum> contentsWithProblemsAndPreviousMd5sum = []
                    if (cmd.metadataFileSource == MetaDataFileSourceEnum.PATH && cmd.paths) {
                        contentsWithProblemsAndPreviousMd5sum = cmd.paths.withIndex().collect { String path, Integer index ->
                            [
                                    contentWithPathAndProblems: fastqMetadataValidationService.readPath(fs.getPath(path)),
                                    previousMd5sum            : cmd.md5.get(index),
                            ] as ContentWithProblemsAndPreviousMd5sum
                        }
                    } else if (cmd.metadataFileSource == MetaDataFileSourceEnum.FILE && cmd.contentList) {
                        contentsWithProblemsAndPreviousMd5sum = cmd.contentList.withIndex().collect { MultipartFile content, Integer index ->
                            [
                                    contentWithPathAndProblems: new ContentWithPathAndProblems(content.bytes, Paths.get(content.originalFilename)),
                                    previousMd5sum            : cmd.md5.get(index),
                            ] as ContentWithProblemsAndPreviousMd5sum
                        }
                    }
                    List<ValidateAndImportResult> validateAndImportResults = metadataImportService.validateAndImport(
                            contentsWithProblemsAndPreviousMd5sum, cmd.directoryStructure, cmd.ignoreWarnings,
                            cmd.ticketNumber, cmd.seqCenterComment, cmd.automaticNotification, cmd.ignoreMd5sumError
                    )
                    log.debug("No problem")
                    if (validateAndImportResults.size() == 1) {
                        log.debug("This should be the id to the details page: ${validateAndImportResults.first().metadataFile.fastqImportInstance.id}")
                        render([redirect: grailsLinkGenerator.link([
                                action  : 'details',
                                absolute: 'true',
                                id      : validateAndImportResults.first().metadataFile.fastqImportInstance.id,
                        ])] as JSON)
                    }
                    render([redirect: grailsLinkGenerator.link([
                            action  : 'multiDetails',
                            absolute: 'true',
                            params  : [metaDataFiles: validateAndImportResults*.metadataFile*.id],
                    ])] as JSON)
                }.invalidToken {
                    return response.sendError(HttpStatus.BAD_REQUEST.value(), g.message(code: "default.invalid.session") as String)
                }
            } catch (MetadataFileImportException e) {
                log.debug("There was a problem: ${e.message}")
                return response.sendError(HttpStatus.BAD_REQUEST.value(),
                        e.message.replace('\n', '<br/>'))
            }
        }
    }

    def details(FastqImportInstance fastqImportInstance) {
        return [
                metaDataDetails      : getMetadataDetails(fastqImportInstance),
                fastqImportInstanceId: fastqImportInstance.id,
                ticket           : fastqImportInstance.ticket,
                ticketUrl        : ticketService.buildTicketDirectLinkNullPointerSave(fastqImportInstance.ticket),
        ]
    }

    def multiDetails() {
        List<MetaDataFileWrapper> metaDataFiles = params.metaDataFiles.collect {
            MetaDataFile file = metadataImportService.findById(it as long)

            new MetaDataFileWrapper([
                    metaDataFile  : file,
                    fullPathTarget: file.filePathTarget,
            ])
        }

        return [
                metaDataFilesWrapper: metaDataFiles,
        ]
    }

    private MetadataDetails getMetadataDetails(FastqImportInstance importInstance) {
        List<RawSequenceFile> dataFilesNotAssignedToSeqTrack = []

        List<MetaDataFileWrapper> metaDataFiles = metadataImportService.findAllByFastqImportInstance(importInstance).collect {
            new MetaDataFileWrapper([
                    metaDataFile  : it,
                    fullPathSource: metadataImportService.getMetaDataFileFullPath(it),
                    fullPathTarget: it.filePathTarget,
                    dateCreated   : TimeFormats.DATE_TIME.getFormattedZonedDateTime(TimeUtils.toZonedDateTime(it.dateCreated)),
            ])
        }

        List<RawSequenceFile> dataFiles = rawSequenceFileService.findAllByFastqImportInstance(importInstance)

        List<RunWithSeqTracks> runs = []
        dataFiles.each { RawSequenceFile dataFile ->
            if (dataFile.seqTrack) {
                RunWithSeqTracks run = runs.find { it.run.id == dataFile.run.id }
                if (run) {
                    SeqTrackWithDataFiles st = run.seqTracks.find { it.seqTrack.id == dataFile.seqTrack.id }
                    if (st) {
                        st.dataFiles.add(dataFile)
                    } else {
                        run.seqTracks.add(new SeqTrackWithDataFiles(dataFile.seqTrack, [dataFile]))
                    }
                } else {
                    runService.checkPermission(dataFile.run)
                    runs.add(new RunWithSeqTracks(dataFile.run, [new SeqTrackWithDataFiles(dataFile.seqTrack, [dataFile])]))
                }
            } else {
                dataFilesNotAssignedToSeqTrack.add(dataFile)
            }
        }

        return new MetadataDetails([
                metaDataFileWrapper           : metaDataFiles,
                dataFilesNotAssignedToSeqTrack: dataFilesNotAssignedToSeqTrack,
                runs                          : runs,
        ])
    }

    def assignTicketToFastqImportInstance() {
        def dataToRender = [:]

        try {
            ticketService.assignTicketToFastqImportInstance(params.value, params.id as Long)
            dataToRender.put("success", true)
        } catch (UserException e) {
            dataToRender.put("error", e.toString())
        }

        render(dataToRender as JSON)
    }

    @PreAuthorize("permitAll()")
    def autoImport(String secret) {
        String expectedSecret = configService.autoImportSecret
        if (!secret || !expectedSecret || secret != expectedSecret) {
            throw new ForbiddenErrorPlainResponseException("authentication with secret failed")
        }
        Authentication authentication = SecurityContextHolder.context.authentication

        try {
            List<GrantedAuthority> authorities = [
                    new SimpleGrantedAuthority(Role.ROLE_OPERATOR),
            ]
            UserDetails userDetails = new User('TicketSystem', "", authorities)
            SecurityContextHolder.context.authentication = new UsernamePasswordAuthenticationToken(userDetails, null, authorities)

            boolean ignoreMd5sumError = "TRUE".equalsIgnoreCase(params.ignoreMd5sumError)

            render(text: doAutoImport(params.ticketNumber as String, params.ilseNumbers as String, ignoreMd5sumError), contentType: "text/plain")
        } catch (Throwable t) {
            throw new InternalServerErrorPlainResponseException(t.message, t)
        } finally {
            SecurityContextHolder.context.authentication = authentication
        }
    }

    private StringBuilder doAutoImport(String ticketNumber, String ilseNumbers, boolean ignoreAlreadyKnownMd5sum) {
        boolean autoImport = processingOptionService.findOptionAsBoolean(OptionName.TICKET_SYSTEM_AUTO_IMPORT_ENABLED)
        if (!autoImport) {
            throw new IllegalStateException('Automatic import is currently disabled. Set processing option autoImportEnabled to "true" to enable it.')
        }
        StringBuilder text = new StringBuilder()
        try {
            Collection<ValidateAndImportResult> results = metadataImportService.validateAndImportMultiple(ticketNumber, ilseNumbers,
                    ignoreAlreadyKnownMd5sum)
            text.append('Automatic import succeeded :-)')
            results.each {
                text.append("\n\n${it.context.metadataFile} --> ${it.copiedFile}\n")
                text.append(g.createLink(action: 'details', id: it.metadataFile.fastqImportInstance.id, absolute: 'true'))
            }
        } catch (MultiImportFailedException e) {
            if (e.failedValidations.size() == 1) {
                text.append('This metadata file failed validation:')
            } else {
                text.append('These metadata files failed validation:')
            }
            StringBuilder problems = new StringBuilder()
            e.failedValidations.each { MetadataValidationContext context ->
                text.append("\n${context.metadataFile}")
                problems.append("The following validation summary messages were returned for ${context.metadataFile.fileName}:\n")
                problems.append("${context.problemsObject.sortedProblemListString}\n\n")
            }
            text.append("\n\nClick here for manual import:")
            text.append("\n" + g.createLink([
                    action  : 'index',
                    absolute: 'true',
                    params  : [
                            'ticketNumber'      : ticketNumber,
                            'paths'             : e.allPaths,
                            'directoryStructure': DirectoryStructureBeanName.GPCF_SPECIFIC,
                    ],
            ]))
            text.append("\n\n")
            text.append(problems)
        }
        return text
    }

    def blacklistedIlseNumbers() {
        List<IlseSubmission> ilseSubmissions = ilseSubmissionService.sortedBlacklistedIlseSubmissions()
        return [
                ilseSubmissions: ilseSubmissions,
                command        : flash.cmd as BlackListedIlseCommand,
        ]
    }

    def addBlacklistedIlseNumbers(BlackListedIlseCommand command) {
        checkErrorAndCallMethodWithFlashMessage(command, "metadataImport.blackListedIlseNumbers.store") {
            List<Integer> ilseNumbers = command.splitToIlseNumbers()
            ilseSubmissionService.createNewIlseSubmissions(ilseNumbers, command.comment)
        }
        redirect action: 'blacklistedIlseNumbers'
    }

    JSON saveComment(CommentCommand cmd) {
        Map retMap = [:]
        return checkErrorAndCallMethod(cmd, {
            IlseSubmission ilseSubmission = ilseSubmissionService.findById(cmd.id)
            commentService.saveComment(ilseSubmission, cmd.comment)
            retMap << [
                    updateMap: [
                            author          : ilseSubmission.comment.author,
                            modificationDate: TimeFormats.DATE.getFormattedDate(ilseSubmission.comment.modificationDate),
                    ],
            ]
        }) {
            retMap
        }
    }

    def unBlacklistIlseSubmissions(RemoveBlackListedIlseCommand cmd) {
        checkErrorAndCallMethodWithFlashMessage(cmd, 'metadataImport.blackListedIlseNumbers.unBlacklist') {
            ilseSubmissionService.unBlacklistIlseSubmissions(cmd.ilseSubmission)
        }
        redirect action: 'blacklistedIlseNumbers'
    }

    JSON updateSeqCenterComment(Ticket ticket, String value) {
        ticketService.saveSeqCenterComment(ticket, value)
        Map map = [success: true]
        return render(map as JSON)
    }

    JSON updateAutomaticNotificationFlag(Ticket ticket, String value) {
        metadataImportService.updateAutomaticNotificationFlag(ticket, value.toBoolean())
        Map map = [success: true]
        return render(map as JSON)
    }

    JSON updateFinalNotificationFlag(Ticket ticket, String value) {
        metadataImportService.updateFinalNotificationFlag(ticket, value.toBoolean())
        Map map = [success: true]
        return render(map as JSON)
    }

    class SpreadsheetDTO {
        HeaderDTO header
        DataRowDTO[] dataRows

        SpreadsheetDTO(MetadataValidationContext context) {
            this.header = new HeaderDTO(context.spreadsheet?.header?.cells, context)
            this.dataRows = context.spreadsheet?.dataRows?.collect { dataRow ->
                new DataRowDTO(dataRow.cells, context)
            }
        }
    }

    class DataRowDTO {
        CellDTO[] cells
        String rowAddress

        DataRowDTO(List<Cell> cells, MetadataValidationContext context) {
            this.cells = cells.collect { cell -> new CellDTO(cell, context) }
            this.rowAddress = cells?.first()?.rowAddress
        }
    }

    class HeaderDTO {
        CellDTO[] cells
        String rowAddress

        HeaderDTO(List<Cell> cells, MetadataValidationContext context) {
            this.cells = cells.collect { cell -> new CellDTO(cell, context) }
            this.rowAddress = cells?.first()?.rowAddress
        }
    }

    class CellDTO {
        String columnAddress
        String text
        String cellAddress
        String cellProblemsLevelAndMessage
        String cellProblemsName

        CellDTO(Cell cell, MetadataValidationContext context) {
            this.columnAddress = cell.columnAddress
            this.text = cell.text
            this.cellAddress = cell.cellAddress
            this.cellProblemsLevelAndMessage = context.getProblems(cell)*.levelAndMessage.join('\n\n')
            this.cellProblemsName = LogLevel.normalize(Problems.getMaximumProblemLevel(context.getProblems(cell))).name
        }
    }

    class ProblemDTO {
        String levelAndMessage
        List<CellDTO> affectedCells
        String level

        ProblemDTO(Problem problem, MetadataValidationContext context) {
            this.levelAndMessage = problem.levelAndMessage
            this.affectedCells = problem.affectedCells.collect { cell -> new CellDTO(cell, context) }
            this.level = LogLevel.normalize(problem.level).name
        }
    }
}

class BlackListedIlseCommand implements Validateable {

    static final String SEPARATOR = /[,; \t]+/

    String ilse
    String comment

    static constraints = {
        ilse nullable: false, blank: false, validator: { val, obj, errors ->
            if (!val) {
                return
            }
            List<Integer> ilseToAdd = []
            val.split(SEPARATOR).each {
                if (it.isInteger()) {
                    Integer integer = it.toInteger()
                    if (integer < IlseSubmission.MIN_ILSE_VALUE) {
                        errors.rejectValue('ilse', 'metadataImport.blackListedIlseNumbers.cmd.tooSmall',
                                [it, IlseSubmission.MIN_ILSE_VALUE].toArray(), 'Ilse is to small')
                    } else if (integer > IlseSubmission.MAX_ILSE_NUMBER) {
                        errors.rejectValue('ilse', 'metadataImport.blackListedIlseNumbers.cmd.tooBig',
                                [it, IlseSubmission.MAX_ILSE_NUMBER].toArray(), 'Ilse is to big')
                    } else if (IlseSubmission.findAllByIlseNumber(integer)) {
                        errors.rejectValue('ilse', 'metadataImport.blackListedIlseNumbers.cmd.alreadyExists',
                                [it].toArray(), 'Ilse already exist')
                    } else if (ilseToAdd.contains(integer)) {
                        errors.rejectValue('ilse', 'metadataImport.blackListedIlseNumbers.cmd.duplicate',
                                [it].toArray(), 'Ilse is twice in list')
                    } else {
                        ilseToAdd << integer
                    }
                } else {
                    errors.rejectValue('ilse', 'metadataImport.blackListedIlseNumbers.cmd.notANumber',
                            [it].toArray(), 'Ilse is not a number')
                }
            }
            return
        }
        comment nullable: false, blank: false
    }

    List<Integer> splitToIlseNumbers() {
        return ilse.split(SEPARATOR)*.toInteger()
    }
}

class RemoveBlackListedIlseCommand {
    IlseSubmission ilseSubmission
}

@TupleConstructor
class MetaDataFileWrapper {
    MetaDataFile metaDataFile
    String fullPathSource
    String fullPathTarget
    String dateCreated
}

@Immutable
class MetadataDetails {
    List<MetaDataFileWrapper> metaDataFileWrapper
    List<RawSequenceFile> dataFilesNotAssignedToSeqTrack
    List<RunWithSeqTracks> runs
}

@TupleConstructor
class RunWithSeqTracks {
    Run run
    List<SeqTrackWithDataFiles> seqTracks
}

@TupleConstructor
class SeqTrackWithDataFiles {
    SeqTrack seqTrack
    List<RawSequenceFile> dataFiles
}

class MetadataImportControllerSubmitCommand implements Serializable, Validateable {
    ProcessingOptionService processingOptionService
    List<String> paths
    List<MultipartFile> contentList
    DirectoryStructureBeanName directoryStructure
    MetaDataFileSourceEnum metadataFileSource
    List<String> md5
    String ticketNumber
    String seqCenterComment
    boolean automaticNotification = true
    boolean ignoreWarnings = false

    boolean ignoreMd5sumError = false

    static constraints = {
        contentList(nullable: true)
        paths(nullable: true)
        directoryStructure(nullable: true)
        md5(nullable: true)
        seqCenterComment(nullable: true, validator: { val, obj ->
            return !val || obj.ticketNumber
        })
        ticketNumber(nullable: true, validator: { val, obj ->
            if (val == null) {
                return true
            }
            return Ticket.ticketNumberConstraint(val) ?: true
        })
    }

    void setTicketNumber(String name) {
        this.ticketNumber = StringUtils.trimAndShortenWhitespace(name)
    }

    String getTicketNumber() {
        String prefix = processingOptionService.findOptionAsString(OptionName.TICKET_SYSTEM_NUMBER_PREFIX)
        Matcher matcher = ticketNumber =~ /^\s*(((${Pattern.quote(prefix)})?#)?(?<number>(\d{16})))?\s*$/
        if (matcher.matches()) {
            return matcher.group('number') ?: null
        }
        return ticketNumber
    }
}
