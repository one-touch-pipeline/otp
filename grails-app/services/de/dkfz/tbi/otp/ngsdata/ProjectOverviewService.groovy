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

import de.dkfz.tbi.otp.project.Project

@Transactional
class ProjectOverviewService {

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'OTP_READ_ACCESS')")
    List overviewProjectQuery(Project project) {
        List seq = AggregateSequences.withCriteria {
            eq("projectId", project?.id)
            property("individualId")
            property("mockPid")
            property("sampleTypeName")
            property("seqTypeDisplayName")
            property("libraryLayout")
            property("singleCell")
            property("seqPlatformId")
            property("seqCenterName")
            property("laneCount")
            property("sum_N_BasePairsGb")
            property("projectName")
            order("mockPid")
            order("sampleTypeName")
            order("seqTypeDisplayName")
            order("libraryLayout")
            order("seqPlatformId")
            order("seqCenterName")
            order("laneCount")
        }
        List queryList = []
        for (def track in seq) {
            def queryListSingleRow = [
                    track.mockPid,
                    track.sampleTypeName,
                    track.seqTypeDisplayName,
                    track.libraryLayout,
                    track.singleCell,
                    track.seqCenterName,
                    SeqPlatform.get(track.seqPlatformId).toString(),
                    track.laneCount,
                    track.sum_N_BasePairsGb,
            ]
            queryList.add(queryListSingleRow)
        }
        return queryList
    }

    /**
     * Returns the sampleIdentifier strings in a project, in a map keyed by Individual+SampleType+SeqType.
     * <p>
     * Key is the three-item list <pre>[ 'Individual.mockFullName', 'sampleType.name', 'SeqType layout single/bulk']</pre>
     * Value is a combined list of sample identifiers for this combination, taken from the SeqTracks.</p>
     * <p>
     * Example result:
     * <pre>
     * [
     *   [ 'indivA', 'tumor',   'WGS Paired bulk' ] : [ 'sampleIdA1', 'sampleIdA2', 'sampleIdA3' ]
     *   [ 'indivA', 'control', 'WGS Paired bulk' ] : [ 'sampleIdA4', 'sampleIdA5', 'sampleIdA6' ]
     *   [ 'indivB', 'tumor',   'WGS Paired bulk' ] : [ 'sampleIdB1', 'sampleIdB2', 'sampleIdB3' ]
     *   [ 'indivB', 'control', 'WGS Paired bulk' ] : [ 'sampleIdB4', 'sampleIdB5', 'sampleIdB6' ]
     * ]
     * </pre>
     * </p>
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Map<List<String>, List<String>> listSampleIdentifierByProject(Project project) {
        return SeqTrack.createCriteria().list {
            projections {
                sample {
                    individual {
                        eq('project', project)
                        property('mockFullName')
                    }
                    sampleType {
                        property('name')
                    }
                }
                property('seqType')
                property('sampleIdentifier')
            }
        }
        .groupBy { it[0..2] } // group by Individual, SampleType, SeqType
        .collectEntries { k, v ->
            // replace SeqType-object with desired display string
            List<String> newKey = [k[0], k[1], k[2].getDisplayNameWithLibraryLayout()]

            // keep only SampleId (last element) in the value; project+SampleType+SeqType are already in the key.
            List<String> newVal = v.collect { it[-1] }.sort().unique()

            [ (newKey): newVal]
        }
    }

    List patientsAndSamplesGBCountPerProject(Project project) {
        List seq = AggregateSequences.withCriteria {
            eq("projectId", project?.id)
            projections {
                groupProperty("seqTypeDisplayName")
                groupProperty("libraryLayout")
                groupProperty("singleCell")
                countDistinct("mockPid")
                count("sampleId")
                sum("sum_N_BasePairsGb")
            }
            order("seqTypeDisplayName")
        }
        return seq
    }

    Long individualCountByProject(Project project) {
        List seq = AggregateSequences.withCriteria {
            eq("projectId", project?.id)
            projections { countDistinct("mockPid") }
        }
        return seq[0]
    }

    List sampleTypeNameCountBySample(Project project) {
        List seq = AggregateSequences.withCriteria {
            eq("projectId", project?.id)
            projections {
                groupProperty("sampleTypeName")
                countDistinct("sampleId")
            }
        }
        return seq
    }

    List centerNameRunId(Project project) {
        List seq = Sequence.withCriteria {
            eq("projectId", project?.id)
            projections {
                groupProperty("seqCenterName")
                countDistinct("runId")
            }
            order("seqCenterName")
        }
        return seq
    }

    List centerNameRunIdLastMonth(Project project) {
        Calendar cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -6)
        Date date = cal.getTime()
        List seq = Sequence.withCriteria {
            eq("projectId", project?.id)
            gt("dateExecuted", date)
            projections {
                groupProperty("seqCenterName")
                countDistinct("runId")
            }
            order("seqCenterName")
        }
        return seq
    }
}
