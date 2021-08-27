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
package de.dkfz.tbi.otp.ngsdata

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.validation.Validateable
import groovy.transform.Immutable
import groovy.transform.TupleConstructor
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.errors.ForbiddenErrorPlainResponseException
import de.dkfz.tbi.otp.errors.InternalServerErrorPlainResponseException
import de.dkfz.tbi.otp.errors.PlainResponseExceptionHandler
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.directorystructures.DirectoryStructureBeanName
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.security.Role
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.tracking.OtrsTicketService
import de.dkfz.tbi.otp.user.UserException
import de.dkfz.tbi.otp.utils.CommentCommand
import de.dkfz.tbi.otp.utils.StringUtils
import de.dkfz.tbi.util.TimeFormats
import de.dkfz.tbi.util.TimeUtils

import java.nio.file.FileSystem
import java.util.regex.Matcher
import java.util.regex.Pattern

@Secured("hasRole('ROLE_OPERATOR')")
class MetadataImportController implements CheckAndCall, PlainResponseExceptionHandler {

    static allowedMethods = [
            index                                : "GET",
            validateOrImport                     : "POST",
            details                              : "GET",
            multiDetails                         : "GET",
            autoImport                           : "GET",
            blacklistedIlseNumbers               : "GET",
            addBlacklistedIlseNumbers            : "POST",
            unBlacklistIlseSubmissions           : "POST",
            saveComment                          : "POST",
            assignOtrsTicketToFastqImportInstance: "POST",
            updateSeqCenterComment               : "POST",
            updateAutomaticNotificationFlag      : "POST",
            updateFinalNotificationFlag          : "POST",
    ]

    MetadataImportService metadataImportService
    ProcessingOptionService processingOptionService
    RunService runService
    OtrsTicketService otrsTicketService
    IlseSubmissionService ilseSubmissionService

    FileSystemService fileSystemService

    ConfigService configService

    CommentService commentService

    @SuppressWarnings("UnnecessaryGetter")
    def index(MetadataImportControllerSubmitCommand cmd) {
        boolean isValidated = false
        int problems = 0
        List<MetadataValidationContext> metadataValidationContexts = []

        if (flash.mvc) {
            metadataValidationContexts = flash.mvc
        } else if (cmd.paths) {
            cmd.paths.each { path ->
                MetadataValidationContext mvc = metadataImportService.validateWithAuth(new File(path), cmd.directoryStructure)
                metadataValidationContexts.add(mvc)
                if (mvc.maximumProblemLevel.intValue() > problems) {
                    problems = mvc.maximumProblemLevel.intValue()
                }
            }
            isValidated = true
        }

        return [
                directoryStructures   : DirectoryStructureBeanName.values(),
                cmd                   : cmd,
                paths                 : cmd.paths ?: [""],
                contexts              : metadataValidationContexts,
                implementedValidations: metadataImportService.getImplementedValidations(),
                isValidated           : isValidated,
                problems              : problems,
        ]
    }

    def validateOrImport(MetadataImportControllerSubmitCommand cmd) {
        List<MetadataValidationContext> metadataValidationContexts = []
        boolean hasRedirected = false
        withForm {
            if (cmd.hasErrors()) {
                flash.message = new FlashMessage("Error", cmd.errors)
            } else if (cmd.submit == "Import") {
                FileSystem fs = fileSystemService.filesystemForFastqImport

                List<MetadataImportService.PathWithMd5sum> pathWithMd5sums = cmd.paths.withIndex().collect {
                    return new MetadataImportService.PathWithMd5sum(fs.getPath(it.first), cmd.md5.get(it.second))
                }
                List<ValidateAndImportResult> validateAndImportResults = metadataImportService.validateAndImportWithAuth(
                        pathWithMd5sums, cmd.directoryStructure, cmd.align, cmd.ignoreWarnings, cmd.ticketNumber,
                        cmd.seqCenterComment, cmd.automaticNotification
                )
                metadataValidationContexts = validateAndImportResults*.context
                boolean allValid = validateAndImportResults.every {
                    it.metadataFile
                }
                if (allValid) {
                    log.debug("No problem")
                    if (validateAndImportResults.size() == 1) {
                        log.debug("This should be the id to the details page: ${validateAndImportResults.first().metadataFile.fastqImportInstance.id}")
                        redirect(action: "details", id: validateAndImportResults.first().metadataFile.fastqImportInstance.id)
                    } else {
                        redirect(action: "multiDetails", params: [metaDataFiles: validateAndImportResults*.metadataFile*.id])
                    }
                    hasRedirected = true
                } else {
                    log.debug("There was a problem")
                }
            }
        }.invalidToken {
            flash.message = new FlashMessage(g.message(code: "default.message.error") as String, g.message(code: "default.invalid.session") as String)
        }
        if (!hasRedirected) {
            flash.mvc = metadataValidationContexts
            redirect(action: "index", params: [
                    paths                : cmd.paths,
                    directoryStructure   : cmd.directoryStructure,
                    ticketNumber         : cmd.ticketNumber,
                    seqCenterComment     : cmd.seqCenterComment,
                    align                : cmd.align,
                    automaticNotification: cmd.automaticNotification,
            ])
        }
    }

    def details() {
        FastqImportInstance fastqImportInstance = FastqImportInstance.get(params.id)
        return [
                metaDataDetails    : getMetadataDetails(fastqImportInstance),
                fastqImportInstance: fastqImportInstance,
        ]
    }

    def multiDetails() {
        List<MetaDataFileWrapper> metaDataFiles = params.metaDataFiles.collect {
            MetaDataFile file = MetaDataFile.get(it)

            new MetaDataFileWrapper(
                    metaDataFile: file,
                    fullPath    : metadataImportService.getMetaDataFileFullPath(file),
            )
        }

        return [
                metaDataFilesWrapper: metaDataFiles,
        ]
    }

    private MetadataDetails getMetadataDetails(FastqImportInstance importInstance) {
        List<DataFile> dataFilesNotAssignedToSeqTrack = []

        List<MetaDataFileWrapper> metaDataFiles = MetaDataFile.findAllByFastqImportInstance(importInstance, [sort: "dateCreated", order: "desc"]).collect {
            new MetaDataFileWrapper(
                    metaDataFile: it,
                    fullPath    : metadataImportService.getMetaDataFileFullPath(it),
                    dateCreated : TimeFormats.DATE_TIME.getFormatted(TimeUtils.toZonedDateTime(it.dateCreated)),
            )
        }

        List<DataFile> dataFiles = DataFile.createCriteria().list {
            createAlias('run', 'r')
            createAlias('seqTrack', 'st')
            eq('fastqImportInstance', importInstance)
            order('r.name', 'asc')
            order('st.laneId', 'asc')
            order('mateNumber', 'asc')
        }

        List<RunWithSeqTracks> runs = []
        dataFiles.each { DataFile dataFile ->
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

        return new MetadataDetails(
                metaDataFileWrapper: metaDataFiles,
                dataFilesNotAssignedToSeqTrack: dataFilesNotAssignedToSeqTrack,
                runs: runs,
        )
    }

    def assignOtrsTicketToFastqImportInstance() {
        def dataToRender = [:]

        try {
            otrsTicketService.assignOtrsTicketToFastqImportInstance(params.value, params.id as Long)
            dataToRender.put("success", true)
        } catch (UserException e) {
            dataToRender.put("error", e.toString())
        }

        render dataToRender as JSON
    }

    @Secured('permitAll')
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
            render text: doAutoImport(params.ticketNumber as String, params.ilseNumbers as String), contentType: "text/plain"
        } catch (Throwable t) {
            throw new InternalServerErrorPlainResponseException(t.message, t)
        } finally {
            SecurityContextHolder.context.authentication = authentication
        }
    }

    private StringBuilder doAutoImport(String otrsTicketNumber, String ilseNumbers) {
        boolean autoImport = processingOptionService.findOptionAsBoolean(OptionName.TICKET_SYSTEM_AUTO_IMPORT_ENABLED)
        if (!autoImport) {
            throw new IllegalStateException('Automatic import is currently disabled. Set processing option autoImportEnabled to "true" to enable it.')
        }
        StringBuilder text = new StringBuilder()
        try {
            Collection<ValidateAndImportResult> results = metadataImportService.validateAndImportMultiple(otrsTicketNumber, ilseNumbers)
            text.append('Automatic import succeeded :-)')
            results.each {
                text.append("\n\n${it.context.metadataFile}:\n")
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
            text.append("\n" + g.createLink(
                    action: 'index',
                    absolute: 'true',
                    params: [
                            'ticketNumber'      : otrsTicketNumber,
                            'paths'             : e.allPaths,
                            'directoryStructure': DirectoryStructureBeanName.GPCF_SPECIFIC,
                    ])
            )
            text.append("\n\n")
            text.append(problems)
        }
        return text
    }

    def blacklistedIlseNumbers() {
        List<IlseSubmission> ilseSubmissions = ilseSubmissionService.sortedBlacklistedIlseSubmissions
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
        checkErrorAndCallMethod(cmd, {
            IlseSubmission ilseSubmission = IlseSubmission.get(cmd.id)
            commentService.saveComment(ilseSubmission, cmd.comment)
            retMap << [
                    updateMap: [
                            author          : ilseSubmission.comment.author,
                            modificationDate: TimeFormats.DATE.getFormatted(ilseSubmission.comment.modificationDate),
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

    JSON updateSeqCenterComment(Long id, String value) {
        OtrsTicket otrsTicket = OtrsTicket.get(id)
        otrsTicket.seqCenterComment = value
        assert otrsTicket.save(flush: true)
        Map map = [success: true]
        render map as JSON
    }

    JSON updateAutomaticNotificationFlag(Long id, String value) {
        metadataImportService.updateAutomaticNotificationFlag(OtrsTicket.get(id), value.toBoolean())
        Map map = [success: true]
        render map as JSON
    }

    JSON updateFinalNotificationFlag(Long id, String value) {
        metadataImportService.updateFinalNotificationFlag(OtrsTicket.get(id), value.toBoolean())
        Map map = [success: true]
        render map as JSON
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
    String fullPath
    String dateCreated
}

@Immutable
class MetadataDetails {
    List<MetaDataFileWrapper> metaDataFileWrapper
    List<DataFile> dataFilesNotAssignedToSeqTrack
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
    List<DataFile> dataFiles
}

class MetadataImportControllerSubmitCommand implements Serializable {
    ProcessingOptionService processingOptionService

    List<String> paths
    DirectoryStructureBeanName directoryStructure
    List<String> md5
    String submit
    String ticketNumber
    String seqCenterComment
    boolean align = true
    boolean automaticNotification = true
    boolean ignoreWarnings

    static constraints = {
        paths(nullable: true)
        directoryStructure(nullable: true)
        md5(nullable: true)
        submit(nullable: true)
        seqCenterComment(nullable: true, validator: { val, obj ->
            return !val || obj.ticketNumber
        })
        ticketNumber(nullable: true, validator: { val, obj ->
            if (val == null) {
                return true
            }
            return OtrsTicket.ticketNumberConstraint(val) ?: true
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
