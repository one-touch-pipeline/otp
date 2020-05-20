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
package de.dkfz.tbi.otp.dataprocessing

import grails.gorm.transactions.Transactional
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils

@Transactional
class ProcessingThresholdsService {

    /**
     * @return List of ProcessingThresholds for an project
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'OTP_READ_ACCESS')")
    List<ProcessingThresholds> findByProject(Project project) {
        return ProcessingThresholds.findAllByProject(project)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    ProcessingThresholds createUpdateOrDelete(Project project, SampleType sampleType, SeqType seqType, Integer numberOfLanes, Double coverage) {
        ProcessingThresholds processingThresholds = CollectionUtils.atMostOneElement(ProcessingThresholds.findAllByProjectAndSampleTypeAndSeqType(project, sampleType, seqType))
        if (!numberOfLanes && !coverage) {
            if (processingThresholds) {
                processingThresholds.delete(flush: true)
            }
            return null
        }
        if (processingThresholds) {
            processingThresholds.numberOfLanes = numberOfLanes
            processingThresholds.coverage = coverage
        } else {
            processingThresholds = new ProcessingThresholds(
                            project: project,
                            sampleType: sampleType,
                            seqType: seqType,
                            numberOfLanes: numberOfLanes,
                            coverage: coverage,
            )
        }
        processingThresholds.save(flush: true)
        return processingThresholds
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'OTP_READ_ACCESS')")
    ProcessingThresholds findByProjectAndSampleTypeAndSeqType(Project project, SampleType sampleType, SeqType seqType) {
        return ProcessingThresholds.findByProjectAndSampleTypeAndSeqType(project, sampleType, seqType)
    }

    List<SeqTrack> getSeqTracksWithoutProcessingThreshold(List<SeqTrack> seqTracks) {
        List<SeqType> analysableSeqTypes = SeqTypeService.allAnalysableSeqTypes
        seqTracks.findAll { SeqTrack seqTrack ->
            (seqTrack.seqType in analysableSeqTypes &&
                    !findByProjectAndSampleTypeAndSeqType(seqTrack.project, seqTrack.sampleType, seqTrack.seqType))
        }
    }
}
