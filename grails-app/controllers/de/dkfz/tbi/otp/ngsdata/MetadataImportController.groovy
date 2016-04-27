package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import groovy.transform.*


class MetadataImportController {

    MetadataImportService metadataImportService


    def index(MetadataImportControllerSubmitCommand cmd) {
        MetadataValidationContext metadataValidationContext
        if (cmd.submit == "Validate") {
            metadataValidationContext = metadataImportService.validate(new File(cmd.path), cmd.directory)
        } else if (cmd.submit == "Import") {
            ValidateAndImportResult validateAndImportResult = metadataImportService.validateAndImport(new File(cmd.path), cmd.directory, cmd.align, cmd.ignoreWarnings, cmd.md5)
            metadataValidationContext = validateAndImportResult.context
            if (validateAndImportResult.runId != null) {
                redirect(controller: "run", action: "show", id: validateAndImportResult.runId)
            }
        } else {
            cmd = null
        }
        return [
                directoryStructures: metadataImportService.getSupportedDirectoryStructures(),
                cmd                : cmd,
                context            : metadataValidationContext,
                implementedValidations: metadataImportService.getImplementedValidations()
        ]
    }

    def details() {
        [data: getMetadataDetails(RunSegment.get(params.id))]
    }

    private static MetadataDetails getMetadataDetails(RunSegment importInstance) {
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
    boolean align
    boolean ignoreWarnings
}
