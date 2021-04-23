/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.dataswap

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.dataprocessing.AlignmentPass
import de.dkfz.tbi.otp.dataprocessing.DataProcessingFilesService
import de.dkfz.tbi.otp.dataswap.data.LaneSwapData
import de.dkfz.tbi.otp.dataswap.parameters.LaneSwapParameters
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.MergedAlignmentDataFileService
import de.dkfz.tbi.otp.ngsdata.MergingAssignment
import de.dkfz.tbi.otp.ngsdata.Run
import de.dkfz.tbi.otp.ngsdata.Sample
import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.SequencingReadType
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.nio.file.Path

@SuppressWarnings("JavaIoPackageAccess")
@Transactional
class LaneSwapService extends DataSwapService<LaneSwapParameters, LaneSwapData> {

    MergedAlignmentDataFileService mergedAlignmentDataFileService

    @Override
    protected void logSwapParameters(LaneSwapParameters parameters) {
        parameters.log << "\nswap from ${parameters.pidSwap.old} ${parameters.sampleTypeSwap.old} to " +
                "${parameters.pidSwap.new} ${parameters.sampleTypeSwap.new} \n\n"
    }

    @Override
    protected void completeOmittedNewSwapValuesAndLog(LaneSwapParameters parameters) {
        parameters.log << "\n  swapping datafiles:"
        parameters.dataFileSwaps = completeOmittedNewSwapValuesAndLog(parameters.dataFileSwaps, parameters.log)
    }

    @Override
    protected LaneSwapData buildDataDTO(LaneSwapParameters parameters) {
        Swap<Individual> individualSwap = getIndividualSwap(parameters)
        Swap<SampleType> sampleTypeSwap = getSampleTypeSwap(parameters)
        Run run = getRunByName(parameters)
        Swap sampleSwap = getSampleSwap(parameters, individualSwap, sampleTypeSwap)
        Swap<SequencingReadType> sequencingReadTypeSwap = getSequencingReadTypeSwaps(parameters)
        List<SeqTrack> seqTrackList = getSeqTracksBySampleAndRunAndLaneIdInList(parameters, sampleSwap, run)
        List<DataFile> dataFiles = getFastQDataFilesBySeqTrackInList(seqTrackList, parameters)

        return new LaneSwapData(
                parameters: parameters,
                projectSwap: getProjectSwap(parameters),
                individualSwap: individualSwap,
                sampleTypeSwap: sampleTypeSwap,
                run: run,
                sampleSwap: sampleSwap,
                sequencingReadTypeSwap: sequencingReadTypeSwap,
                seqTypeSwap: getSeqTypeSwap(parameters, sequencingReadTypeSwap),
                seqTrackList: seqTrackList,
                dataFiles: dataFiles,
                oldDataFileNameMap: collectFileNamesOfDataFiles(dataFiles),
                oldFastQcFileNames: getFastQcOutputFileNamesByDataFilesInList(dataFiles)
        )
    }

    @Override
    protected void logSwapData(LaneSwapData data) {
        List<AlignmentPass> alignmentsPasses = AlignmentPass.findAllBySeqTrackInList(data.seqTrackList)
        if (alignmentsPasses) {
            data.log << "Alignments found for SeqTracks ${data.seqTrackList}\n\n"
        }
    }

    @Override
    protected void performDataSwap(LaneSwapData data) {
        List<SeqTrack> seqTracksOfOldSample = SeqTrack.findAllBySampleAndSeqType(data.sampleSwap.old, data.seqTypeSwap.old)
        Path bashScriptToMoveFiles = createMoveFileScript(data)[0]

        if (AlignmentPass.findAllBySeqTrackInList(data.seqTrackList)) {
            seqTracksOfOldSample.each {
                deleteProcessingInformationOfSeqTrack(it, bashScriptToMoveFiles, data)
            }
            bashScriptToMoveFiles << "# delete analyses stuff\n"
            data.dirsToDelete.flatten()*.path.each {
                bashScriptToMoveFiles << "#rm -rf ${it}\n"
            }
            assert AlignmentPass.findAllBySeqTrackInList(data.seqTrackList).isEmpty(): "There are alignments for ${data.seqTrackList}, which can not be deleted"
        }

        swapSeqTracks(data)

        bashScriptToMoveFiles << "\n\n#copy and remove fastq files\n"
        bashScriptToMoveFiles << renameDataFiles(data)

        // files need to be already renamed at this point
        List<String> newFastQcFileNames = getFastQcOutputFileNamesByDataFilesInList(
                getFastQDataFilesBySeqTrackInList(data.seqTrackList, data.parameters).sort { it.id }
        )

        bashScriptToMoveFiles << "\n\n#copy and delete fastqc files\n\n"

        data.oldFastQcFileNames.eachWithIndex { oldFastQcFileName, i ->
            bashScriptToMoveFiles << copyAndRemoveFastQcFile(oldFastQcFileName, newFastQcFileNames.get(i), data)
        }

        List<MergingAssignment> mergingAssignments = MergingAssignment.findAllBySeqTrackInList(data.seqTrackList)
        data.log << "\n${mergingAssignments.size()} MergingAssignment found"
        deletionService.deleteAllMergingAssignmentsWithAlignmentDataFilesAndSeqScans(mergingAssignments)

        checkForRemainingSeqTracks(bashScriptToMoveFiles, data)
    }

    @Override
    protected void createSwapComments(LaneSwapData data) {
        individualService.createComment("Lane swap",
                [
                        individual   : data.individualSwap.old,
                        project      : data.projectSwap.old.name,
                        sample       : data.sampleSwap.old,
                        pid          : data.individualSwap.old.pid,
                        sampleType   : data.sampleTypeSwap.old.name,
                        seqType      : data.seqTypeSwap.old.name,
                        singleCell   : data.seqTypeSwap.old.singleCell,
                        libraryLayout: data.seqTypeSwap.old.libraryLayout,
                ],
                [
                        individual   : data.individualSwap.new,
                        project      : data.projectSwap.new.name,
                        sample       : data.sampleSwap.new,
                        pid          : data.individualSwap.new.pid,
                        sampleType   : data.sampleTypeSwap.new.name,
                        seqType      : data.seqTypeSwap.new.name,
                        singleCell   : data.seqTypeSwap.new.singleCell,
                        libraryLayout: data.seqTypeSwap.new.libraryLayout,
                ],
                "run: ${data.run.name}\nlane: ${data.lanes}"
        )
        createCommentForSwappedDatafiles(data)
    }

    /**
     * Swap all seqTracks to new seqType and sampleType.
     *
     * @param seqTrack which should be swapped.
     * @param data DTO containing all entities necessary to perform a swap.
     * @return swapped seqTrack
     */
    private void swapSeqTracks(LaneSwapData data) {
        data.seqTrackList = data.seqTrackList.collect {
            if (data.seqTypeSwap.old.hasAntibodyTarget != data.seqTypeSwap.new.hasAntibodyTarget) {
                throw new UnsupportedOperationException("Old and new SeqTypes (old: ${data.seqTypeSwap.old};" +
                        " new: ${data.seqTypeSwap.new}) differ in antibody target usage and " +
                        "thus can not be swapped, as we would be missing the antibody target information.")
            }
            it.swapped = true
            it.seqType = data.seqTypeSwap.new
            it.sample = data.sampleSwap.new
            assert it.save(flush: true)
            return it
        }
    }

    /**
     * Calls deletionService and create bash commands to remove processing information, alignments and merging data.
     *
     * @param seqTrack as parameters for searching for the corresponding processing information.
     * @param bashScriptToMoveFiles where the commands will be added.
     * @param data DTO containing all entities necessary to perform a swap.
     */
    private void deleteProcessingInformationOfSeqTrack(SeqTrack seqTrack, Path bashScriptToMoveFiles, LaneSwapData data) {
        bashScriptToMoveFiles << "\n\n#Delete Alignment- & Merging stuff from ${seqTrack} and retrigger Alignment.\n"
        data.dirsToDelete.push(deletionService.deleteAllProcessingInformationAndResultOfOneSeqTrack(seqTrack, !data.linkedFilesVerified) as File)

        String baseDir = dataProcessingFilesService.getOutputDirectory(data.individualSwap.old, DataProcessingFilesService.OutputDirectories.ALIGNMENT)
        String middleDir = processedAlignmentFileService.getRunLaneDirectory(seqTrack)
        bashScriptToMoveFiles << "# rm -rf '${baseDir}/${middleDir}'\n"

        String mergingResultDir = dataProcessingFilesService.getOutputDirectory(data.individualSwap.old,
                DataProcessingFilesService.OutputDirectories.MERGING)
        bashScriptToMoveFiles << "# rm -rf '${mergingResultDir}/${data.sampleTypeSwap.old.name}/" +
                "${data.seqTypeSwap.old.name}/${data.seqTypeSwap.old.libraryLayout}'\n"

        String projectDir = configService.rootPath
        String mergedAlignmentDir = mergedAlignmentDataFileService.buildRelativePath(data.seqTypeSwap.old, data.sampleSwap.old)
        bashScriptToMoveFiles << "# rm -rf '${projectDir}/${mergedAlignmentDir}'\n"
    }

    /**
     * Check if there are any remaining SeqTracks for this sample/seqType combination left and create commands to remove them.
     *
     * @param bashScriptToMoveFiles where the commands will be added.
     * @param data DTO containing all entities necessary to perform a swap.
     */
    private void checkForRemainingSeqTracks(Path bashScriptToMoveFiles, LaneSwapData data) {
        if (SeqTrack.findAllBySampleAndSeqType(data.sampleSwap.old, data.seqTypeSwap.old).empty) {
            bashScriptToMoveFiles << "\n #There are no seqTracks belonging to the sample ${data.sampleSwap.old} -> delete it on the filesystem\n\n"
            File basePath = data.projectSwap.old.projectSequencingDirectory
            bashScriptToMoveFiles << "#rm -rf '${basePath}/${data.seqTypeSwap.old.dirName}/" +
                    "view-by-pid/${data.individualSwap.old.pid}/${data.sampleTypeSwap.old.dirName}/" +
                    "${data.seqTypeSwap.old.libraryLayoutDirName}'\n"
        }
    }

    /**
     * Gathers the new and the old sampleType by their names and create a new swap with the entities.
     *
     * @param parameters which contain the names of the old and new SampleType.
     * @return Swap contain the entities of the old and new SampleType.
     */
    private Swap<SampleType> getSampleTypeSwap(LaneSwapParameters parameters) {
        return new Swap(
                CollectionUtils.exactlyOneElement(SampleType.findAllByName(parameters.sampleTypeSwap.old),
                        "old sample type ${parameters.sampleTypeSwap.old} not found"),
                CollectionUtils.exactlyOneElement(SampleType.findAllByName(parameters.sampleTypeSwap.new),
                        "new sample type ${parameters.sampleTypeSwap.new} not found")
        )
    }

    /**
     * Gathers the new and the old SequencingReadType by their name and create a new swap with the entities.
     *
     * @param parameters which contain the names of the old and new SequencingReadType.
     * @return Swap contain the entities of the old and new SequencingReadType.
     */
    @SuppressWarnings('AvoidFindWithoutAll')
    private Swap<SequencingReadType> getSequencingReadTypeSwaps(LaneSwapParameters parameters) {
        return new Swap<SequencingReadType>(
                SequencingReadType.findByName(parameters.sequencingReadTypeSwap.old),
                SequencingReadType.findByName(parameters.sequencingReadTypeSwap.new)
        )
    }

    /**
     * Gathers the new and the old SeqType by their name, sequencingReadTypes and singleCell flag and create a new swap with the entities.
     *
     * @param parameters which contain the names, sequencingReadTypes and singleCell flags of the old and new SeqType.
     * @return Swap contain the entities of the old and new SeqType.
     */
    private Swap<SeqType> getSeqTypeSwap(LaneSwapParameters parameters, Swap<SequencingReadType> sequencingReadTypeSwap) {
        return new Swap<SeqType>(
                CollectionUtils.exactlyOneElement(SeqType.findAllByNameAndLibraryLayoutAndSingleCell(
                        parameters.seqTypeSwap.old, sequencingReadTypeSwap.old, parameters.singleCellSwap.old),
                        "The old seqtype ${parameters.seqTypeSwap.old} ${parameters.sequencingReadTypeSwap.old} " +
                                "${parameters.singleCellSwap.old} does not exist"),
                CollectionUtils.exactlyOneElement(SeqType.findAllByNameAndLibraryLayoutAndSingleCell(
                        parameters.seqTypeSwap.new, sequencingReadTypeSwap.new, parameters.singleCellSwap.new),
                        "The new seqtype ${parameters.seqTypeSwap.new} ${parameters.sequencingReadTypeSwap.old}  " +
                                "${parameters.singleCellSwap.old} does not exist"
                )
        )
    }

    /**
     * Gathers the new and the old Sample by their Individual snd SampleType and create a new swap with the entities.
     * If parameters flag sampleNeedsToBeCreated is set a new Sample will be created.
     *
     * @param parameters which contain the sampleNeedsToBeCreated flag.
     * @param individualSwap as parameters for searching for the corresponding sample.
     * @param sampleTypeSwap as parameters for searching for the corresponding sample.
     * @return Swap contain the entities of the old and new Sample.
     */
    private Swap<Sample> getSampleSwap(LaneSwapParameters parameters, Swap<Individual> individualSwap, Swap<SampleType> sampleTypeSwap) {
        Sample oldSample = CollectionUtils.exactlyOneElement(Sample.findAllByIndividualAndSampleType(individualSwap.old, sampleTypeSwap.old),
                "The old Sample (${individualSwap.old} ${sampleTypeSwap.old}) does not exist")

        Sample newSample

        List<Sample> sampleList = Sample.findAllByIndividualAndSampleType(individualSwap.new, sampleTypeSwap.new)
        if (parameters.sampleNeedsToBeCreated) {
            assert sampleList.isEmpty(): "The new Sample (${individualSwap.new} ${sampleTypeSwap.new}) does exist, but should not"
            newSample = new Sample(individual: individualSwap.new, sampleType: sampleTypeSwap.new).save(flush: true)
        } else {
            newSample = CollectionUtils.exactlyOneElement(sampleList,
                    "The new Sample (${individualSwap.new} ${sampleTypeSwap.new}) does not exist")
        }

        return new Swap<Sample>(oldSample, newSample)
    }

    /**
     * Gathers seq tracks by Sample, Run and lane ids and log found entities.
     *
     * @param parameters which contain the lane ids.
     * @param sampleSwap as parameters for searching for corresponding seq tracks.
     * @param run as parameters for searching for corresponding seq tracks.
     * @return found list of seq tracks.
     */
    private List<SeqTrack> getSeqTracksBySampleAndRunAndLaneIdInList(LaneSwapParameters parameters, Swap<Sample> sampleSwap, Run run) {
        List<SeqTrack> seqTrackList = SeqTrack.findAllBySampleAndRunAndLaneIdInList(sampleSwap.old, run, parameters.lanes)
        logListEntries(seqTrackList, "seqtracks", parameters.log)
        return seqTrackList
    }

    /**
     * Gathers Run by name.
     *
     * @param parameters which contain the run name.
     * @return found Run.
     */
    private Run getRunByName(LaneSwapParameters parameters) {
        return CollectionUtils.exactlyOneElement(Run.findAllByName(parameters.runName),
                "The run (${parameters.runName}) does not exist"
        )
    }
}
