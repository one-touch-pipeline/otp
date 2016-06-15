package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeService
import de.dkfz.tbi.otp.utils.ProcessHelperService.ProcessOutput
import org.springframework.beans.factory.annotation.Autowired


class ExecuteWgbsAlignmentJob extends AbstractExecutePanCanJob {


    @Autowired
    ExecutionService executionService

    @Autowired
    ReferenceGenomeService referenceGenomeService

    final String HEADER = "Sample\tLibrary\tPID\tReadLayout\tRun\tMate\tSequenceFile\n"


    @Override
    protected String prepareAndReturnWorkflowSpecificCValues(RoddyBamFile roddyBamFile) {
        assert roddyBamFile : "roddyBamFile must not be null"
        List<String> chromosomeNames = ReferenceGenomeEntry.findAllByReferenceGenomeAndClassificationInList(roddyBamFile.referenceGenome,
                [ReferenceGenomeEntry.Classification.CHROMOSOME, ReferenceGenomeEntry.Classification.MITOCHONDRIAL])*.name
        assert chromosomeNames : "No chromosome names could be found for reference genome ${roddyBamFile.referenceGenome}"
        return ",CHROMOSOME_INDICES:( ${chromosomeNames.join(' ')} )" +
                (roddyBamFile.referenceGenome.cytosinePositionsIndex ?
                        ",CYTOSINE_POSITIONS_INDEX:${referenceGenomeService.cytosinePositionIndexFilePath(roddyBamFile.project, roddyBamFile.referenceGenome).absolutePath}" :
                        ""
                )
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
chmod 0440 ${metadataFile.path}
"""


        ProcessOutput output = executionService.executeCommandReturnProcessOutput(realm, cmd)
        assert output.exitCode == 0
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(metadataFile)

        return "--usemetadatatable=${metadataFile.path} "
    }


    @Override
    protected void workflowSpecificValidation(RoddyBamFile roddyBamFile) {
        (["merged"] + roddyBamFile.seqTracks.collect { it.libraryDirectoryName }).unique().each {
            LsdfFilesService.ensureDirIsReadableAndNotEmpty(new File(roddyBamFile.workMethylationDirectory, it))
        }
    }
}
