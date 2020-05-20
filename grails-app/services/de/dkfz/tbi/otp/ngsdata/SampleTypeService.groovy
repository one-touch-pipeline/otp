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
package de.dkfz.tbi.otp.ngsdata

import grails.gorm.transactions.Transactional
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.dataprocessing.AbstractMergingWorkPackage
import de.dkfz.tbi.otp.project.Project

@Transactional
class SampleTypeService {

    /**
     * return the used sample types of the project
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'OTP_READ_ACCESS')")
    List findUsedSampleTypesForProject(Project project) {
        List<SeqType> allAnalysableSeqTypes = SeqTypeService.allAnalysableSeqTypes

        List<SampleType> sampleTypes = SeqTrack.createCriteria().list {
            projections {
                sample {
                    individual {
                        eq('project', project)
                    }
                    groupProperty("sampleType")
                }
            }
            'in'("seqType", allAnalysableSeqTypes)
        }

        sampleTypes.addAll(AbstractMergingWorkPackage.createCriteria().list {
            projections {
                sample {
                    individual { eq('project', project) }
                    groupProperty("sampleType")
                }
            }
            'in'("seqType", allAnalysableSeqTypes)
        })

        return sampleTypes.unique().sort { it.name }
    }


    List<SeqTrack> getSeqTracksWithoutSampleCategory(List<SeqTrack> seqTracks) {
        List<SeqType> analysableSeqTypes = SeqTypeService.allAnalysableSeqTypes
        return seqTracks.findAll {
            it.seqType in analysableSeqTypes
        }.groupBy { SeqTrack seqTrack ->
            [
                    seqTrack.project,
                    seqTrack.sampleType,
            ]
        }.findAll { Map.Entry<List, List> entry ->
            SampleTypePerProject stp = SampleTypePerProject.findWhere(project: entry.key[0], sampleType: entry.key[1])
            (stp == null || stp.category == SampleType.Category.UNDEFINED)
        }.values().flatten()
    }
}

