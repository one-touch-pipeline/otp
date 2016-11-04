package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.job.jobs.AutoRestartableJob
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.ProcessHelperService.ProcessOutput
import org.springframework.beans.factory.annotation.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*


class ExecuteWgbsAlignmentJob extends AbstractRoddyAlignmentJob implements AutoRestartableJob {


    @Autowired
    ExecutionService executionService

    @Autowired
    ReferenceGenomeService referenceGenomeService

    @Autowired
    AdapterFileService adapterFileService

    final String HEADER = "Sample\tLibrary\tPID\tReadLayout\tRun\tMate\tSequenceFile\n"


    @Override
    protected List<String> prepareAndReturnWorkflowSpecificCValues(RoddyBamFile roddyBamFile) {
        assert roddyBamFile : "roddyBamFile must not be null"

        List<String> cValues = prepareAndReturnAlignmentCValues(roddyBamFile)

        cValues.add(getChromosomeIndexParameter(roddyBamFile.referenceGenome))

        if (roddyBamFile.referenceGenome.cytosinePositionsIndex) {
            cValues.add("CYTOSINE_POSITIONS_INDEX:${referenceGenomeService.cytosinePositionIndexFilePath(roddyBamFile.project, roddyBamFile.referenceGenome).absolutePath}")
        } else {
            throw new RuntimeException("Cytosine position index for reference genome ${roddyBamFile.referenceGenome} is not defined.")
        }

        AdapterFile adapterFile = exactlyOneElement(roddyBamFile.seqTracks*.adapterFile.unique())
        if (adapterFile) {
            cValues.add("CLIP_INDEX:${adapterFileService.fullPath(adapterFile)}")
        }

        return cValues
    }


    @Override
    protected String prepareAndReturnWorkflowSpecificParameter(RoddyBamFile roddyBamFile) {
        assert roddyBamFile : "roddyBamFile must not be null"

        final Realm realm = configService.getRealmDataManagement(roddyBamFile.project)

        File metadataFile = roddyBamFile.getWorkMetadataTableFile()
        LsdfFilesService.ensureDirIsReadable(metadataFile.parentFile)

        StringBuilder builder = new StringBuilder()

        builder << HEADER

        builder << DataFile.findAllBySeqTrackInList(roddyBamFile.seqTracks).sort { it.mateNumber }.collect { DataFile dataFile ->
            [dataFile.sampleType.dirName, // it is correct that the header is 'Sample', this is because of the different names for the same things
             dataFile.seqTrack.getLibraryDirectoryName(),
             dataFile.individual.pid,
             dataFile.seqType.libraryLayoutDirName,
             dataFile.run.dirName,
             dataFile.mateNumber,
             lsdfFilesService.getFileViewByPidPath(dataFile),
            ].join("\t")
        }.join("\n")

        String cmd = """
cat <<EOD > ${metadataFile.path}
${builder.toString()}
EOD
chmod 0444 ${metadataFile.path}
"""


        ProcessOutput output = executionService.executeCommandReturnProcessOutput(realm, cmd)
        assert output.exitCode == 0
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(metadataFile)

        return "--usemetadatatable=${metadataFile.path} "
    }


    @Override
    protected void workflowSpecificValidation(RoddyBamFile roddyBamFile) {
        LsdfFilesService.ensureDirIsReadableAndNotEmpty(new File(roddyBamFile.workMethylationDirectory, "merged"))
        if (roddyBamFile.hasMultipleLibraries()) {
            roddyBamFile.seqTracks.collect { it.libraryDirectoryName }.unique().each {
                LsdfFilesService.ensureDirIsReadableAndNotEmpty(new File(roddyBamFile.workMethylationDirectory, it))
            }
        }
    }
}
