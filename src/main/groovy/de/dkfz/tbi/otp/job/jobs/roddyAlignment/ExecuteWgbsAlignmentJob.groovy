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
package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.job.jobs.AutoRestartableJob
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.utils.ProcessOutput

@CompileDynamic
@Component
@Scope("prototype")
@Slf4j
class ExecuteWgbsAlignmentJob extends AbstractRoddyAlignmentJob implements AutoRestartableJob {

    @Autowired
    RemoteShellHelper remoteShellHelper

    @Autowired
    ReferenceGenomeService referenceGenomeService

    static final String HEADER = "Sample\tLibrary\tPID\tReadLayout\tRun\tMate\tSequenceFile\n"

    @Override
    @SuppressWarnings("ThrowRuntimeException") // ignored: will be removed with the old workflow system
    protected List<String> prepareAndReturnWorkflowSpecificCValues(RoddyBamFile roddyBamFile) {
        assert roddyBamFile : "roddyBamFile must not be null"

        List<String> cValues = prepareAndReturnAlignmentCValues(roddyBamFile)

        cValues.add(getChromosomeIndexParameterWithMitochondrium(roddyBamFile.referenceGenome))

        if (roddyBamFile.referenceGenome.cytosinePositionsIndex) {
            cValues.add("CYTOSINE_POSITIONS_INDEX:${referenceGenomeService.cytosinePositionIndexFilePath(roddyBamFile.referenceGenome).absolutePath}")
        } else {
            throw new RuntimeException("Cytosine position index for reference genome ${roddyBamFile.referenceGenome} is not defined.")
        }

        return cValues
    }

    @Override
    protected String prepareAndReturnWorkflowSpecificParameter(RoddyBamFile roddyBamFile) {
        assert roddyBamFile : "roddyBamFile must not be null"

        final Realm realm = roddyBamFile.project.realm

        File metadataFile = roddyBamFile.workMetadataTableFile
        LsdfFilesService.ensureDirIsReadable(metadataFile.parentFile)

        StringBuilder builder = new StringBuilder()

        builder << HEADER

        builder << DataFile.findAllBySeqTrackInListAndIndexFile(roddyBamFile.seqTracks as List, false).sort { it.mateNumber }.collect { DataFile dataFile ->
            File file = new File(lsdfFilesService.getFileViewByPidPath(dataFile))
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)
            assert dataFile.fileSize == file.length()

            [dataFile.sampleType.dirName, // it is correct that the header is 'Sample', this is because of the different names for the same things
             dataFile.seqTrack.libraryDirectoryName,
             dataFile.individual.pid,
             dataFile.seqType.libraryLayoutDirName,
             dataFile.run.dirName,
             dataFile.mateNumber,
             lsdfFilesService.getFileViewByPidPath(dataFile),
            ].join("\t")
        }.join("\n")

        String cmd = """
set -e

if [ -e "${metadataFile.path}" ]; then
    echo "File ${metadataFile.path} already exists, deleting it."
    rm ${metadataFile.path}*
fi

cat <<EOD > ${metadataFile.path}
${builder}
EOD

chmod 0444 ${metadataFile.path}
"""

        ProcessOutput output = remoteShellHelper.executeCommandReturnProcessOutput(realm, cmd)
        assert output.exitCode == 0
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(metadataFile)

        return "--usemetadatatable=${metadataFile.path}"
    }

    @Override
    protected void workflowSpecificValidation(RoddyBamFile roddyBamFile) {
        LsdfFilesService.ensureDirIsReadableAndNotEmpty(new File(roddyBamFile.workMethylationDirectory, "merged"))
        if (roddyBamFile.hasMultipleLibraries()) {
            roddyBamFile.seqTracks*.libraryDirectoryName.unique().each {
                LsdfFilesService.ensureDirIsReadableAndNotEmpty(new File(roddyBamFile.workMethylationDirectory, it))
            }
        }
    }
}
