package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.tracking.*
import groovy.transform.*
import java.util.regex.*
import org.springframework.validation.*

class MetadataImportController {

    MetadataImportService metadataImportService
    RunService runService

    def index(MetadataImportControllerSubmitCommand cmd) {
        MetadataValidationContext metadataValidationContext
        String errorMessage
        if (cmd.hasErrors()) {
            FieldError fieldError = cmd.errors.getFieldError()
            errorMessage = "'${fieldError.getRejectedValue()}' is not a valid value for '${fieldError.getField()}'. Error code: '${fieldError.code}'"
        }
        if (cmd.submit == "Import" && !errorMessage) {
            ValidateAndImportResult validateAndImportResult = metadataImportService.validateAndImport(new File(cmd.path), cmd.directory, cmd.align, cmd.ignoreWarnings, cmd.md5, cmd.ticketNumber)
            metadataValidationContext = validateAndImportResult.context
            if (validateAndImportResult.metadataFile != null) {
                redirect(action: "details", id: validateAndImportResult.metadataFile.runSegment.id)
            }
        } else if (cmd.submit != null) {
            metadataValidationContext = metadataImportService.validate(new File(cmd.path), cmd.directory)
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
        [data: getMetadataDetails(RunSegment.get(params.id))]
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
    boolean align = true
    boolean ignoreWarnings

    static constraints = {
        path(nullable:true)
        directory(nullable:true)
        md5(nullable:true)
        submit(nullable:true)
        ticketNumber(nullable:true, validator: { val, obj ->
            if (val == null) {
                return true
            }
            return OtrsTicket.ticketNumberConstraint(val) ?: true
        })
    }
    void setTicketNumber(String ticketNumber) {
        // TODO: regarding the ticket number prefix see OTP-2187
        Matcher matcher = ticketNumber =~ /^\s*(((DMG )?#)?(?<number>(\d{16})))?\s*$/
        if (matcher.matches()) {
            this.ticketNumber = matcher.group('number') ?: null
        } else {
            this.ticketNumber = ticketNumber
        }

    }
}
