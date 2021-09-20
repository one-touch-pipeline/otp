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
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile
import de.dkfz.tbi.otp.dataswap.data.SampleSwapData
import de.dkfz.tbi.otp.dataswap.parameters.SampleSwapParameters
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.Sample
import de.dkfz.tbi.otp.ngsdata.SampleIdentifier
import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.CollectionUtils

@SuppressWarnings("JavaIoPackageAccess")
@Transactional
class SampleSwapService extends DataSwapService<SampleSwapParameters, SampleSwapData> {

    @Override
    protected void logSwapParameters(SampleSwapParameters parameters) {
        parameters.log << "\n\nmove ${parameters.pidSwap.old} ${parameters.sampleTypeSwap.old} of ${parameters.projectNameSwap.old} " +
                "to ${parameters.pidSwap.new} ${parameters.sampleTypeSwap.new} of ${parameters.projectNameSwap.new} "
    }

    @Override
    protected void completeOmittedNewSwapValuesAndLog(SampleSwapParameters parameters) {
        parameters.log << "\n  swapping datafiles:"
        parameters.dataFileSwaps = completeOmittedNewSwapValuesAndLog(parameters.dataFileSwaps, parameters.log)
    }

    @Override
    protected SampleSwapData buildDataDTO(SampleSwapParameters parameters) {
        Swap<Individual> individualSwap = getIndividualSwap(parameters)
        Swap<SampleType> sampleTypeSwap = getSampleTypeSwap(parameters)

        Sample sample = getSampleByIndividualAndSampleType(individualSwap.old, sampleTypeSwap.old, parameters)
        List<SeqTrack> seqTrackList = getSeqTracksBySample(sample, parameters)

        List<DataFile> fastqDataFiles = getFastQDataFilesBySeqTrackInList(seqTrackList, parameters)
        List<DataFile> bamDataFiles = getBAMDataFilesBySeqTrackInList(seqTrackList, parameters)
        List<DataFile> dataFiles = [fastqDataFiles, bamDataFiles].flatten() as List<DataFile>

        return new SampleSwapData(
                parameters: parameters,
                projectSwap: getProjectSwap(parameters),
                individualSwap: individualSwap,
                sampleTypeSwap: sampleTypeSwap,
                sample: sample,
                seqTrackList: seqTrackList,
                fastqDataFiles: fastqDataFiles,
                dataFiles: dataFiles,
                oldDataFileNameMap: collectFileNamesOfDataFiles(dataFiles),
                oldFastQcFileNames: getFastQcOutputFileNamesByDataFilesInList(dataFiles),
                seqTrackService: seqTrackService
        )
    }

    @Override
    protected void logSwapData(SampleSwapData data) {
        logAlignments(data)
    }

    @Override
    protected void performDataSwap(SampleSwapData data) {
        List<AlignmentPass> alignmentPasses = AlignmentPass.findAllBySeqTrackInList(data.seqTrackList)

        if (data.seqTrackList && alignmentPasses) {
            data.moveFilesBashScript << "\n\n\n ################ delete old aligned & merged files ################ \n"
            alignmentPasses.each { AlignmentPass alignmentPass ->
                String baseDirAlignment = dataProcessingFilesService.getOutputDirectory(data.individualSwap.old,
                        DataProcessingFilesService.OutputDirectories.ALIGNMENT)
                String middleDirAlignment = processedAlignmentFileService.getRunLaneDirectory(alignmentPass.seqTrack)
                String oldPathToAlignedFiles = "${baseDirAlignment}/${middleDirAlignment}"
                data.moveFilesBashScript << "#rm -rf ${oldPathToAlignedFiles}\n"
            }

            String baseDirMerging = dataProcessingFilesService.getOutputDirectory(data.individualSwap.old,
                    DataProcessingFilesService.OutputDirectories.MERGING)
            String oldProcessingPathToMergedFiles = "${baseDirMerging}/${data.sampleTypeSwap.old.name}"
            data.moveFilesBashScript << "#rm -rf ${oldProcessingPathToMergedFiles}\n"

            List<ProcessedMergedBamFile> processedMergedBamFiles = ProcessedMergedBamFile.createCriteria().list {
                mergingPass {
                    mergingSet {
                        mergingWorkPackage {
                            eq("sample", data.sample)
                        }
                    }
                }
            }
            List<ProcessedMergedBamFile> latestProcessedMergedBamFiles = processedMergedBamFiles.findAll {
                it.mergingPass.isLatestPass() && it.mergingSet.isLatestSet()
            }
            latestProcessedMergedBamFiles.each { ProcessedMergedBamFile latestProcessedMergedBamFile ->
                String oldProjectPathToMergedFiles = latestProcessedMergedBamFile.baseDirectory.absolutePath
                data.moveFilesBashScript << "#rm -rf ${oldProjectPathToMergedFiles}\n"
            }
        }

        swapSample(data)

        createMoveDataFilesCommands(data)

        SampleIdentifier.findAllBySample(data.sample)*.delete(flush: true)

        List<String> newFastQcFileNames = data.fastqDataFiles.collect { fastqcDataFilesService.fastqcOutputFile(it) }
        data.moveFilesBashScript << "\n\n\n################ move fastqc files ################\n"
        data.oldFastQcFileNames.eachWithIndex { oldFastQcFileName, i ->
            data.moveFilesBashScript << copyAndRemoveFastQcFile(oldFastQcFileName, newFastQcFileNames.get(i), data)
        }

        createRemoveAnalysisAndAlignmentsCommands(data)
    }

    @Override
    protected void createSwapComments(SampleSwapData data) {
        individualService.createComment("Sample swap", [
                individual: data.individualSwap.old,
                project   : data.projectSwap.old.name,
                pid       : data.individualSwap.old.pid,
                sampleType: data.sampleTypeSwap.old.name,
        ], [
                individual: data.individualSwap.new,
                project   : data.projectSwap.new.name,
                pid       : data.individualSwap.new.pid,
                sampleType: data.sampleTypeSwap.new.name,
        ])
        createCommentForSwappedDatafiles(data)
    }

    /**
     * Swap sample to new individual and sampleType.
     *
     * @param data DTO containing all entities necessary to perform a swap.
     */
    private void swapSample(SampleSwapData data) {
        data.sample.individual = data.individualSwap.new
        data.sample.sampleType = data.sampleTypeSwap.new
        data.sample.save(flush: true)
    }

    /**
     * Gathers the new and the old sampleType by their names and create a new swap with the entities.
     *
     * @param parameters which contain the names of the old and new SampleType.
     * @return Swap contain the entities of the old and new SampleType.
     */
    private Swap<SampleType> getSampleTypeSwap(SampleSwapParameters parameters) {
        return new Swap(
                CollectionUtils.exactlyOneElement(SampleType.findAllByName(parameters.sampleTypeSwap.old),
                        "old sample type ${parameters.sampleTypeSwap.old} not found"),
                CollectionUtils.exactlyOneElement(SampleType.findAllByName(parameters.sampleTypeSwap.new),
                        "new sample type ${parameters.sampleTypeSwap.new} not found")
        )
    }

    /**
     * Gathers sample by given individual and sampleType and log found entity.
     *
     * @param individual as parameters for searching for corresponding sample.
     * @param sampleType as parameters for searching for corresponding sample.
     * @param parameters containing the StringBuilder for logging
     * @return found sample
     */
    private Sample getSampleByIndividualAndSampleType(Individual individual, SampleType sampleType, SampleSwapParameters parameters) {
        Sample sample = CollectionUtils.exactlyOneElement(Sample.findAllByIndividualAndSampleType(individual, sampleType),
                "The old Sample (${individual} ${sampleType}) does not exist")
        parameters.log << "\n  sample: ${sample}"
        return sample
    }

    /**
     * Gathers seq tracks by given sample and log found entities.
     *
     * @param sample as parameters for searching for corresponding seq tracks.
     * @param parameters containing the StringBuilder for logging
     * @return found list of seq tracks
     */
    private List<SeqTrack> getSeqTracksBySample(Sample sample, SampleSwapParameters parameters) {
        List<SeqTrack> seqTrackList = sample ? SeqTrack.findAllBySample(sample) : []
        logListEntries(seqTrackList, "seqtracks", parameters.log)
        return seqTrackList
    }
}

