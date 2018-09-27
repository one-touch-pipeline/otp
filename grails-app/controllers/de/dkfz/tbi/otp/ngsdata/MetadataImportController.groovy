package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.otp.tracking.*
import de.dkfz.tbi.otp.user.*
import grails.converters.*
import grails.validation.*
import groovy.transform.*

import java.nio.file.*
import java.util.regex.*

class MetadataImportController {

    static allowedMethods = [
            // TODO: incomplete (OTP-2887)
            index: "GET",
            validateOrImport: "POST",
            details: "GET",
            multiDetails: "GET",
            autoImport: "GET",
    ]

    MetadataImportService metadataImportService
    ProcessingOptionService processingOptionService
    RunService runService
    TrackingService trackingService
    IlseSubmissionService ilseSubmissionService

    FileSystemService fileSystemService

    def index(MetadataImportControllerSubmitCommand cmd) {
        boolean isValidated = false
        int problems = 0
        List<MetadataValidationContext> metadataValidationContexts = []

        if (flash.mvc) {
            metadataValidationContexts = flash.mvc
        } else if (cmd.paths) {
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
            flash.message = "Error"
            flash.errors = cmd.errors
        } else if (cmd.submit == "Import") {
            FileSystem fs = fileSystemService.getFilesystemForFastqImport()

            List<MetadataImportService.PathWithMd5sum> pathWithMd5sums = cmd.paths.withIndex().collect {
                return new MetadataImportService.PathWithMd5sum(fs.getPath(it.first), cmd.md5.get(it.second))
            }
            List<ValidateAndImportResult> validateAndImportResults = metadataImportService.validateAndImportWithAuth(
                    pathWithMd5sums, cmd.directory, cmd.align, cmd.ignoreWarnings, cmd.ticketNumber,
                    cmd.seqCenterComment, cmd.automaticNotification
            )
            metadataValidationContexts = validateAndImportResults*.context
            boolean allValid = validateAndImportResults.every {
                it.metadataFile
            }
            if (allValid) {
                if (validateAndImportResults.size() == 1) {
                    redirect(action: "details", id: validateAndImportResults.first().metadataFile.runSegment.id)
                } else {
                    redirect(action: "multiDetails", params: [metaDataFiles: validateAndImportResults*.metadataFile*.id])
                }
                return
            }
        }
        flash.mvc = metadataValidationContexts
        redirect(action: "index", params: [
                paths                : cmd.paths,
                directory            : cmd.directory,
                ticketNumber         : cmd.ticketNumber,
                seqCenterComment     : cmd.seqCenterComment,
                align                : cmd.align,
                automaticNotification: cmd.automaticNotification,
        ])
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

    private StringBuilder doAutoImport(String otrsTicketNumber, String ilseNumbers) {
        boolean autoImport = processingOptionService.findOptionAsBoolean(OptionName.TICKET_SYSTEM_AUTO_IMPORT_ENABLED)
        if (!autoImport) {
            throw new IllegalStateException('Automatic import is currently disabled. Set processing option autoImportEnabled to "true" to enable it.')
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
                            'directory'   : MetadataImportService.MIDTERM_ILSE_DIRECTORY_STRUCTURE_BEAN_NAME,
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
    ProcessingOptionService processingOptionService

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
