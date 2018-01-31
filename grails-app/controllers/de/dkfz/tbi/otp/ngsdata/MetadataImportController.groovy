package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.otp.tracking.*
import de.dkfz.tbi.otp.user.*
import grails.converters.*
import grails.validation.*
import groovy.transform.*
import org.codehaus.groovy.grails.web.mapping.*
import org.springframework.beans.factory.annotation.*
import org.springframework.validation.*

import java.util.regex.*

class MetadataImportController {

    MetadataImportService metadataImportService
    ProcessingOptionService processingOptionService
    RunService runService
    TrackingService trackingService
    IlseSubmissionService ilseSubmissionService

    def index(MetadataImportControllerSubmitCommand cmd) {
        boolean isValidated = false
        int problems = 0
        List<MetadataValidationContext> metadataValidationContexts = []
        List<MetaDataFile> metaDataFiles = []
        String errorMessage
        if (cmd.hasErrors()) {
            FieldError fieldError = cmd.errors.getFieldError()
            errorMessage = "'${fieldError.getRejectedValue()}' is not a valid value for '${fieldError.getField()}'. Error code: '${fieldError.code}'"
        }
        if (cmd.submit == "Import" && !errorMessage) {
            cmd.paths.eachWithIndex { path, idx ->
                ValidateAndImportResult validateAndImportResult = metadataImportService.validateAndImportWithAuth(new File(path), cmd.directory, cmd.align, cmd.ignoreWarnings, cmd.md5.get(idx), cmd.ticketNumber, cmd.seqCenterComment, cmd.automaticNotification)
                metadataValidationContexts.add(validateAndImportResult.context)
                if (validateAndImportResult.metadataFile != null) {
                    metaDataFiles.add(validateAndImportResult.metadataFile)
                }
            }
            if (metaDataFiles) {
                if (metaDataFiles.size() == 1) {
                    redirect(action: "details", id: metaDataFiles.first().runSegment.id)
                } else {
                    redirect(action: "multiDetails", params: [metaDataFiles: metaDataFiles.id])
                }

            }
        } else if (cmd.submit != null) {
            cmd.paths.each { path ->
                MetadataValidationContext mvc = metadataImportService.validateWithAuth(new File(path), cmd.directory)
                metadataValidationContexts.add(mvc)
                if (mvc.maximumProblemLevel.intValue() > problems) {
                    problems = mvc.maximumProblemLevel.intValue()
                }
            }
            isValidated = true
        }

        return [
                directoryStructures   : metadataImportService.getSupportedDirectoryStructures(),
                cmd                   : cmd,
                errorMessage          : errorMessage,
                contexts              : metadataValidationContexts,
                implementedValidations: metadataImportService.getImplementedValidations(),
                isValidated           : isValidated,
                problems              : problems,
        ]
    }

    def details() {
        RunSegment runSegment = (RunSegment.get(params.id))
        [
                data      : getMetadataDetails(runSegment),
                runSegment: runSegment,
        ]
    }

    def multiDetails() {
        List<MetaDataFile> metaDataFiles = params.metaDataFiles.collect {
            MetaDataFile.get(it)
        }
        [
                metaDataFiles: metaDataFiles,
        ]
    }

    private MetadataDetails getMetadataDetails(RunSegment importInstance) {
        List<DataFile> dataFilesNotAssignedToSeqTrack = []

        List<MetaDataFile> metaDataFiles = MetaDataFile.findAllByRunSegment(importInstance, [sort: "dateCreated", order: "desc"])

        List<DataFile> dataFiles = DataFile.createCriteria().list {
            createAlias('run', 'r')
            createAlias('seqTrack', 'st')
            eq('runSegment', importInstance)
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

    def assignOtrsTicketToRunSegment() {
        def dataToRender = [:]

        try {
            trackingService.assignOtrsTicketToRunSegment(params.value, params.id as Long)
            dataToRender.put("success", true)
        } catch (UserException e) {
            dataToRender.put("error", e.toString())
        }

        render dataToRender as JSON
    }

    def autoImport() {
        render text: doAutoImport(params.ticketNumber, params.ilseNumbers), contentType: "text/plain"
    }

    StringBuilder doAutoImport(String otrsTicketNumber, String ilseNumbers) {
        String autoImport = processingOptionService.findOption(OptionName.TICKET_SYSTEM_AUTO_IMPORT_ENABLED, null, null)
        if (!(autoImport && (autoImport == '1' || autoImport.toUpperCase().trim() == 'TRUE'))) {
            throw new IllegalStateException('Automatic import is currently disabled. Set processing option autoImportEnabled to "1" or "true" to enable it.')
        }
        StringBuilder text = new StringBuilder()
        try {
            Collection<ValidateAndImportResult> results =
                    metadataImportService.validateAndImportMultiple(otrsTicketNumber, ilseNumbers)
            text.append('Automatic import succeeded :-)')
            results.each {
                text.append("\n\n${it.context.metadataFile}:\n")
                text.append(g.createLink(action: 'details', id: it.metadataFile.runSegment.id, absolute: 'true'))
            }
        } catch (MultiImportFailedException e) {
            if (e.failedValidations.size() == 1) {
                text.append('This metadata file failed validation:')
            } else {
                text.append('These metadata files failed validation:')
            }
            e.failedValidations.each {
                text.append("\n${it.metadataFile}")
            }
            text.append("\n\nClick here for manual import:")
            text.append("\n" + g.createLink(
                    action: 'index',
                    absolute: 'true',
                    params: [
                            'ticketNumber': otrsTicketNumber,
                            'paths'       : e.allPaths,
                            'directory'   : MetadataImportService.MIDTERM_ILSE_DIRECTORY_STRUCTURE_BEAN_NAME
                    ])
            )
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

@Validateable
class BlackListedIlseCommand {

    String addButton
    Integer ilse
    String comment

    static constraints = {
        ilse nullable: false, min: 1000, max: 999999
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
    List<String> paths
    String directory
    List<String> md5
    String submit
    String ticketNumber
    String seqCenterComment
    boolean align = true
    boolean automaticNotification = true
    boolean ignoreWarnings

    static constraints = {
        paths(nullable: true)
        directory(nullable: true)
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

    void setTicketNumber(String ticketNumber) {
        String prefix = ProcessingOptionService.findOptionSafe(OptionName.TICKET_SYSTEM_NUMBER_PREFIX, null, null)
        Matcher matcher = ticketNumber =~ /^\s*(((${Pattern.quote(prefix)})?#)?(?<number>(\d{16})))?\s*$/
        if (matcher.matches()) {
            this.ticketNumber = matcher.group('number') ?: null
        } else {
            this.ticketNumber = ticketNumber
        }

    }
}
