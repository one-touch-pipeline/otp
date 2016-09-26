package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.tracking.*
import de.dkfz.tbi.otp.user.*
import grails.converters.*
import grails.validation.*
import groovy.transform.*
import org.springframework.validation.*

import java.util.regex.*

class MetadataImportController {

    MetadataImportService metadataImportService
    ProcessingOptionService processingOptionService
    RunService runService
    TrackingService trackingService
    IlseSubmissionService ilseSubmissionService

    def index(MetadataImportControllerSubmitCommand cmd) {
        MetadataValidationContext metadataValidationContext
        String errorMessage
        if (cmd.hasErrors()) {
            FieldError fieldError = cmd.errors.getFieldError()
            errorMessage = "'${fieldError.getRejectedValue()}' is not a valid value for '${fieldError.getField()}'. Error code: '${fieldError.code}'"
        }
        if (cmd.submit == "Import" && !errorMessage) {
            ValidateAndImportResult validateAndImportResult = metadataImportService.validateAndImportWithAuth(new File(cmd.path), cmd.directory, cmd.align, cmd.ignoreWarnings, cmd.md5, cmd.ticketNumber, cmd.seqCenterComment)
            metadataValidationContext = validateAndImportResult.context
            if (validateAndImportResult.metadataFile != null) {
                redirect(action: "details", id: validateAndImportResult.metadataFile.runSegment.id)
            }
        } else if (cmd.submit != null) {
            metadataValidationContext = metadataImportService.validateWithAuth(new File(cmd.path), cmd.directory)
        }
        return [
            directoryStructures: metadataImportService.getSupportedDirectoryStructures(),
            cmd                : cmd,
            errorMessage       : errorMessage,
            context            : metadataValidationContext,
            implementedValidations: metadataImportService.getImplementedValidations()
        ]
    }

    def details() {
        RunSegment runSegment = (RunSegment.get(params.id))
        [
                data: getMetadataDetails(runSegment),
                runSegment: runSegment,
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
        if (processingOptionService.findOption('autoImportEnabled', null, null) != '1') {
            throw new IllegalStateException('Automatic import is currently disabled. Set processing option autoImportEnabled to 1 to enable it.')
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
        } catch (Throwable t) {
            text.append("The following error occured during automatic import:\n${t}")
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
    String path
    String directory
    String md5
    String submit
    String ticketNumber
    String seqCenterComment
    boolean align = true
    boolean ignoreWarnings

    static constraints = {
        path(nullable:true)
        directory(nullable:true)
        md5(nullable:true)
        submit(nullable:true)
        seqCenterComment(nullable: true, validator: { val, obj ->
            return !val || obj.ticketNumber
        })
        ticketNumber(nullable:true, validator: { val, obj ->
            if (val == null) {
                return true
            }
            return OtrsTicket.ticketNumberConstraint(val) ?: true
        })
    }
    void setTicketNumber(String ticketNumber) {
        String prefix = ProcessingOptionService.getValueOfProcessingOption(TrackingService.TICKET_NUMBER_PREFIX)
        Matcher matcher = ticketNumber =~ /^\s*(((${Pattern.quote(prefix)})?#)?(?<number>(\d{16})))?\s*$/
        if (matcher.matches()) {
            this.ticketNumber = matcher.group('number') ?: null
        } else {
            this.ticketNumber = ticketNumber
        }

    }
}
