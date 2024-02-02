/*
 * Copyright 2011-2024 The OTP authors
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
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataswap.data.SampleSwapData
import de.dkfz.tbi.otp.dataswap.parameters.SampleSwapParameters
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.nio.file.Path

@SuppressWarnings("JavaIoPackageAccess")
@CompileDynamic
@Transactional
class SampleSwapService extends AbstractDataSwapService<SampleSwapParameters, SampleSwapData> {

    AbstractBamFileService abstractBamFileService

    @Override
    protected void logSwapParameters(SampleSwapParameters parameters) {
        parameters.log << "\n\nmove ${parameters.pidSwap.old} ${parameters.sampleTypeSwap.old} of ${parameters.projectNameSwap.old} " +
                "to ${parameters.pidSwap.new} ${parameters.sampleTypeSwap.new} of ${parameters.projectNameSwap.new} "
    }

    @Override
    protected void completeOmittedNewSwapValuesAndLog(SampleSwapParameters parameters) {
        parameters.log << "\n  swapping datafiles:"
        parameters.rawSequenceFileSwaps = completeOmittedNewSwapValuesAndLog(parameters.rawSequenceFileSwaps, parameters.log)
    }

    @Override
    protected SampleSwapData buildDataDTO(SampleSwapParameters parameters) {
        Swap<Individual> individualSwap = getIndividualSwap(parameters)
        Swap<SampleType> sampleTypeSwap = getSampleTypeSwap(parameters)

        Sample sample = getSampleByIndividualAndSampleType(individualSwap.old, sampleTypeSwap.old, parameters)
        List<SeqTrack> seqTrackList = getSeqTracksBySample(sample, parameters)
        List<RawSequenceFile> rawSequenceFiles = getRawSequenceFilesBySeqTrackInList(seqTrackList, parameters)

        List<Path> individualPaths = seqTrackList*.seqType.unique().collect {
            individualService.getViewByPidPath(individualSwap.old, it)
        }
        List<Path> sampleDirs = rawSequenceFiles.collect {
            lsdfFilesService.getSampleTypeDirectory(it)
        }

        return new SampleSwapData(
                parameters: parameters,
                projectSwap: getProjectSwap(parameters),
                individualSwap: individualSwap,
                sampleTypeSwap: sampleTypeSwap,
                sample: sample,
                seqTrackList: seqTrackList,
                rawSequenceFiles: rawSequenceFiles,
                oldRawSequenceFileNameMap: collectFileNamesOfRawSequenceFiles(rawSequenceFiles),
                oldFastQcFileNames: getFastQcOutputFileNamesByRawSequenceFilesInList(rawSequenceFiles),
                seqTrackService: seqTrackService,
                cleanupIndividualPaths: individualPaths,
                cleanupSampleTypePaths: sampleDirs,
        )
    }

    @Override
    protected void performDataSwap(SampleSwapData data) {
        swapSample(data)

        createMoveRawSequenceFilesCommands(data)

        SampleIdentifier.findAllBySample(data.sample)*.delete(flush: true)

        Map<FastqcProcessedFile, String> newFastQcFileNames = getFastQcOutputFileNamesByRawSequenceFilesInList(data.rawSequenceFiles)
        data.moveFilesCommands << "\n\n\n################ move fastqc files ################\n"
        data.oldFastQcFileNames.each { fastqcProcessedFile, oldFastQcFileName ->
            data.moveFilesCommands << copyAndRemoveFastQcFile(oldFastQcFileName, newFastQcFileNames[fastqcProcessedFile], data)
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
        createCommentForSwappedRawSequenceFiles(data)
    }

    @Override
    protected void cleanupLeftOvers(SampleSwapData data) {
        data.moveFilesCommands << "\n\n################ cleanup empty sample and pid directories ################\n\n"
        cleanupLeftOverSamples(data)
    }

    /**
     * Swap sample to new individual and sampleType.
     *
     * @param data DTO containing all entities necessary to perform a swap.
     */
    private void swapSample(SampleSwapData data) {
        // copy the species if the new individual has no species defined
        // and the sample doesn't exist
        if (!data.individualSwap.new.species && !SeqTrack.withCriteria {
            sample {
                eq('individual', data.individualSwap.new)
            }
        }) {
            data.individualSwap.new.species = data.individualSwap.old.species
        }

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
