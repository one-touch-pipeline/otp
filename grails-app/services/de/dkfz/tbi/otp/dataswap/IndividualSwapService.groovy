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

import de.dkfz.tbi.otp.dataprocessing.DataProcessingFilesService
import de.dkfz.tbi.otp.dataswap.data.IndividualSwapData
import de.dkfz.tbi.otp.dataswap.parameters.IndividualSwapParameters
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.Sample
import de.dkfz.tbi.otp.ngsdata.SampleIdentifier
import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.CollectionUtils

@SuppressWarnings("JavaIoPackageAccess")
@Transactional
class IndividualSwapService extends DataSwapService<IndividualSwapParameters, IndividualSwapData> {

    @Override
    protected void logSwapParameters(IndividualSwapParameters parameters) {
        parameters.log << "\n\nmove ${parameters.pidSwap.old} of ${parameters.projectNameSwap.old} to" +
                " ${parameters.pidSwap.new} of ${parameters.projectNameSwap.new} "
    }

    @Override
    protected void completeOmittedNewSwapValuesAndLog(IndividualSwapParameters parameters) {
        parameters.log << "\n  swapping samples:"
        parameters.sampleTypeSwaps = completeOmittedNewSwapValuesAndLog(parameters.sampleTypeSwaps, parameters.log)

        parameters.log << "\n  swapping datafiles:"
        parameters.dataFileSwaps = completeOmittedNewSwapValuesAndLog(parameters.dataFileSwaps, parameters.log)
    }

    @Override
    protected IndividualSwapData buildDataDTO(IndividualSwapParameters parameters) {
        Swap<Individual> individualSwap = getIndividualSwap(parameters)

        List<Sample> samples = getSamplesByIndividual(individualSwap.old, parameters)

        List<SeqTrack> seqTrackList = getSeqTracksBySampleInList(samples, parameters)

        // gather dataFiles
        List<DataFile> fastqDataFiles = getFastQDataFilesBySeqTrackInList(seqTrackList, parameters)
        List<DataFile> bamDataFiles = getBAMDataFilesBySeqTrackInList(seqTrackList, parameters)
        List<DataFile> dataFiles = [fastqDataFiles, bamDataFiles].flatten() as List<DataFile>

        return new IndividualSwapData(
                parameters: parameters,
                projectSwap: getProjectSwap(parameters),
                individualSwap: individualSwap,
                samples: samples,
                seqTrackList: seqTrackList,
                dataFiles: dataFiles,
                oldDataFileNameMap: collectFileNamesOfDataFiles(dataFiles),
                oldFastQcFileNames: getFastQcOutputFileNamesByDataFilesInList(dataFiles)
        )
    }

    @Override
    protected void logSwapData(IndividualSwapData data) {
        logAlignments(data)
    }

    @Override
    protected void performDataSwap(IndividualSwapData data) {
        swapIndividual(data)

        data.samples.each { Sample sample ->
            swapSampleAndDeleteOldIdentifier(sample, data)
        }

        createMoveDataFilesCommands(data)

        data.moveFilesCommands << "\n\n\n################ move fastq files ################\n"
        data.samples = Sample.findAllByIndividual(data.individualSwap.old)
        data.seqTrackList = data.samples ? SeqTrack.findAllBySampleInList(data.samples) : []
        List<DataFile> newDataFiles = data.seqTrackList ? DataFile.findAllBySeqTrackInList(data.seqTrackList) : []
        List<String> newFastqcFileNames = newDataFiles.collect { fastqcDataFilesService.fastqcOutputFile(it) }
        data.oldFastQcFileNames.eachWithIndex { oldFastQcFileName, i ->
            data.moveFilesCommands << copyAndRemoveFastQcFile(oldFastQcFileName, newFastqcFileNames.get(i), data)
        }

        createRemoveAnalysisAndAlignmentsCommands(data)

        data.moveFilesCommands << "\n\n\n ################ delete old Individual ################ \n"
        data.moveFilesCommands << "# rm -rf ${data.projectSwap.old.projectSequencingDirectory}/*/view-by-pid/${data.pidSwap.old}/\n"

        String processingPathToOldIndividual = dataProcessingFilesService.getOutputDirectory(
                data.individualSwap.old,
                DataProcessingFilesService.OutputDirectories.BASE
        )
        data.moveFilesCommands << "# rm -rf ${processingPathToOldIndividual}\n"
    }

    @Override
    protected void createSwapComments(IndividualSwapData data) {
        individualService.createComment("Individual swap", [
                individual: data.individualSwap.old,
                project   : data.projectSwap.old.name,
                pid       : data.pidSwap.old,
        ], [
                individual: data.individualSwap.new,
                project   : data.projectSwap.new.name,
                pid       : data.pidSwap.new,
        ])

        createCommentForSwappedDatafiles(data)
    }

    @Override
    protected Swap<Individual> getIndividualSwap(IndividualSwapParameters parameters) {
        return new Swap<Individual>(
                CollectionUtils.exactlyOneElement(Individual.findAllByPid(parameters.pidSwap.old),
                        "old individual ${parameters.pidSwap.old} not found"),
                CollectionUtils.atMostOneElement(Individual.findAllByPid(parameters.pidSwap.new))
        )
    }

    /**
     * Swap Individual to new Project and set new pid.
     *
     * @param data DTO containing all entities necessary to perform a swap.
     */
    private void swapIndividual(IndividualSwapData data) {
        data.log << "\n  changing ${data.individualSwap.old.project} to ${data.projectSwap.new} for ${data.individualSwap.old}"
        Individual individual = data.individualSwap.new ? data.individualSwap.new : data.individualSwap.old
        individual.project = data.projectSwap.new
        individual.pid = data.pidSwap.new
        individual.mockPid = data.pidSwap.new
        individual.mockFullName = data.pidSwap.new
        individual.save(flush: true)
        data.individualSwap = new Swap<>(data.individualSwap.old, individual)
    }

    /**
     * Swap sample to new SampleType and delete old SampleIdentifier.
     *
     * @param sample sample to swap
     * @param data DTO containing all entities necessary to perform a swap.
     */
    private void swapSampleAndDeleteOldIdentifier(Sample sample, IndividualSwapData data) {
        SampleType newSampleType = CollectionUtils.exactlyOneElement(SampleType.findAllByName(
                data.sampleTypeSwaps.find { it.old == sample.sampleType.name }.new
        ))
        data.log << "\n    change ${sample.sampleType.name} to ${newSampleType.name}"
        SampleIdentifier.findAllBySample(sample)*.delete(flush: true)
        sample.sampleType = newSampleType
        sample.save(flush: true)
    }

    /**
     * Gathers samples by given individual and log found entities.
     *
     * @param individual as parameters for searching for corresponding sample.
     * @param sampleType as parameters for searching for corresponding sample.
     * @param parameters containing the StringBuilder for logging
     * @return found list of samples
     */
    private List<Sample> getSamplesByIndividual(Individual individual, IndividualSwapParameters parameters) {
        List<Sample> samples = Sample.findAllByIndividual(individual)
        parameters.log << "\n  samples (${samples.size()}): ${samples}"
        return samples
    }

    /**
     * Gathers seq tracks by given samples and log found entities.
     *
     * @param sample as parameters for searching for corresponding seq tracks.
     * @param parameters containing the StringBuilder for logging
     * @return found list of seq tracks
     */
    private List<SeqTrack> getSeqTracksBySampleInList(List<Sample> samples, IndividualSwapParameters parameters) {
        List<SeqTrack> seqTrackList = samples ? SeqTrack.findAllBySampleInList(samples) : []
        logListEntries(seqTrackList, "seqtracks", parameters.log)
        return seqTrackList
    }
}
