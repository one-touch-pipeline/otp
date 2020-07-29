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

import de.dkfz.tbi.otp.FlashMessage
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.directorystructures.DirectoryStructureBeanName
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.security.Role
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.tracking.OtrsTicketService
import de.dkfz.tbi.otp.user.UserException
import de.dkfz.tbi.otp.utils.StringUtils

import java.nio.file.FileSystem
import java.util.regex.Matcher
import java.util.regex.Pattern

class MetadataImportController {

    static allowedMethods = [
            index           : "GET",
            validateOrImport: "POST",
            details         : "GET",
            multiDetails    : "GET",
            autoImport      : "GET",
    ]

    MetadataImportService metadataImportService
    ProcessingOptionService processingOptionService
    RunService runService
    OtrsTicketService otrsTicketService
    IlseSubmissionService ilseSubmissionService

    FileSystemService fileSystemService

    ConfigService configService

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
                return
            } else {
                log.debug("There was a problem")
            }
        }
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

    def details() {
        FastqImportInstance fastqImportInstance = FastqImportInstance.get(params.id)
        return [
                data               : getMetadataDetails(fastqImportInstance),
                fastqImportInstance: fastqImportInstance,
        ]
    }

    def multiDetails() {
        List<MetaDataFile> metaDataFiles = params.metaDataFiles.collect {
            MetaDataFile.get(it)
        }
        return [
                metaDataFiles: metaDataFiles,
        ]
    }

    private MetadataDetails getMetadataDetails(FastqImportInstance importInstance) {
        List<DataFile> dataFilesNotAssignedToSeqTrack = []

        List<MetaDataFile> metaDataFiles = MetaDataFile.findAllByFastqImportInstance(importInstance, [sort: "dateCreated", order: "desc"])

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
                metaDataFiles: metaDataFiles,
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

    def autoImport(String secret) {
        String expectedSecret = configService.autoImportSecret
        if (!secret || !expectedSecret || secret != expectedSecret) {
            render text: 'authentication failed', contentType: "text/plain"
            return
        }
        Authentication authentication = SecurityContextHolder.context.authentication

        try {
            List<GrantedAuthority> authorities = [
                    new SimpleGrantedAuthority(Role.ROLE_OPERATOR),
            ]
            UserDetails userDetails = new User('TicketSystem', "", authorities)
            SecurityContextHolder.context.authentication = new UsernamePasswordAuthenticationToken(userDetails, null, authorities)
            render text: doAutoImport(params.ticketNumber as String, params.ilseNumbers as String), contentType: "text/plain"
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
                problems.append("${context.problemsObject.getSortedProblemListString()}\n\n")
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

    def blacklistedIlseNumbers(BlackListedIlseCommand command) {
        if (command?.addButton) {
            if (command.validate()) {
                if (ilseSubmissionService.checkIfIlseNumberDoesNotExist(command.ilse)) {
                    ilseSubmissionService.createNewIlseSubmission(command.ilse, command.comment)
                    redirect action: 'blacklistedIlseNumbers'
                    return
                } else {
                    command.errors.rejectValue('ilse', 'code', 'ilse number exists already')
                }
            }
        }
        List<IlseSubmission> ilseSubmissions = ilseSubmissionService.getSortedBlacklistedIlseSubmissions()
        return [
                ilseSubmissions: ilseSubmissions,
                command        : command,
        ]
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

    String addButton
    Integer ilse
    String comment

    static constraints = {
        ilse nullable: false, min: 1, max: 999999
        comment nullable: false, blank: false
    }
}

@Immutable
class MetadataDetails {
    List<MetaDataFile> metaDataFiles
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
        } else {
            return ticketNumber
        }
    }
}
