/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.ngsdata

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.job.processing.ProcessParameter
import de.dkfz.tbi.otp.utils.CollectionUtils

/**
 * Service to retrieve information about Runs.
 */
@CompileDynamic
@Transactional
class RunService {

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Run checkPermission(Run run) {
        return run
    }

    @PostAuthorize("hasRole('ROLE_OPERATOR')")
    Run getRun(String identifier) {
        if (!identifier) {
            return null
        }
        Run run = null
        if (identifier?.isLong()) {
            run = Run.get(identifier as Long)
        }
        return run ?: CollectionUtils.atMostOneElement(Run.findAllByName(identifier))
    }

    /**
     * Retrieves the given Run.
     * Overloaded method for convenience.
     * @param identifier Name or database Id
     * @return Run
     * */
    @PostAuthorize("hasRole('ROLE_OPERATOR')")
    Run getRun(long identifier) {
        return getRun("${identifier}")
    }

    /**
     * Retrieves the ProcessParameters for the given Run.
     * @param run The Run for which the ProcessParameter should be retrieved.
     * @return List of ProcessParameter
     * */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<ProcessParameter> retrieveProcessParameters(Run run) {
        if (!run) {
            return []
        }
        return ProcessParameter.findAllByValueAndClassName("${run.id}", run.class.name)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Collection<MetaDataFile> retrieveMetaDataFiles(Run run) {
        Collection<RawSequenceFile> rawSequenceFiles = RawSequenceFile.findAllByRun(run)
        return rawSequenceFiles ?
                (rawSequenceFiles*.fastqImportInstance ? MetaDataFile.findAllByFastqImportInstanceInList(rawSequenceFiles*.fastqImportInstance) : []) :
                []
    }

    /**
     * Retrieves information about the Sequence Track of a given Run.
     * The returned data is a Map having SeqTrack as key and another Map as value.
     * This map consists of an element "files" with a list of DataFiles for the specific
     * SeqTrack as value and an element "alignments" with a Map of the Alignments for the
     * specific SeqTrack and a list of DataFiles for each Alignment.
     * Summarized the structure looks like the following
     * <ul>
     * <li>SeqTrack:<ul>
     *   <li>files: [RawSequenceFile, RawSequenceFile]</li>
     * </ul></li>
     * <ul>
     * @param run The Run for which the Sequence Track information should be retrieved
     * @return Data Structure as described above
     * */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Map<SeqTrack, Map<String, Object>> retrieveSequenceTrackInformation(Run run) {
        Map<SeqTrack, Map<String, Object>> returnData = [:]
        if (!run) {
            return returnData
        }
        SeqTrack.findAllByRun(run, [sort: 'laneId']).each { track ->
            Map dataElement = [files: RawSequenceFile.findAllBySeqTrack(track)]
            returnData.put(track, dataElement)
        }
        returnData
    }

    /**
     * Retrieves all DataFiles for the given run with errors.
     * Error is identified by the fact that the file is not used.
     * List is sorted by file name.
     * @param run The Run for which the errornous data files need to be retrieved
     * @return List of DataFiles with errors.
     * */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<RawSequenceFile> rawSequenceFilesWithError(Run run) {
        return RawSequenceFile.findAllByRunAndUsed(run, false, [sort: "fileName"])
    }

    /**
     * Checks if a run is empty.
     * @param run to check.
     * @return boolean false or true.
     * */
    boolean isRunEmpty(Run run) {
        assert run: "The input run of the method isRunEmpty is null"
        return !(RawSequenceFile.findAllByRun(run) || SeqTrack.findAllByRun(run))
    }
}
