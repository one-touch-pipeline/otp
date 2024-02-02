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
package de.dkfz.tbi.otp.ngsdata

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.project.Project

@CompileDynamic
@Transactional
class SampleOverviewService {

    /**
     * @param project the project for filtering the result
     * @return all SeqTypes used in the project
     */
    List<SeqType> seqTypeByProject(Project project) {
        List<Long> seqTypeIds = AggregateSequences.withCriteria {
            eq("projectId", project?.id)
            projections {
                groupProperty("seqTypeId")
            }
        }
        List<SeqType> seqTypes = []
        if (seqTypeIds) {
            seqTypes = SeqType.withCriteria {
                'in'("id", seqTypeIds)
                order("name")
                order("libraryLayout")
            }
        }
        return seqTypes
    }

    List<String> sampleTypeByProject(Project project) {
        List<String> sampleTypes = AggregateSequences.withCriteria {
            eq("projectId", project?.id)
            projections {
                groupProperty("sampleTypeName")
            }
            order("sampleTypeName", "asc")
        }
        return sampleTypes
    }

    /**
     * fetch and return all combination of {@link Individual} (as pid) and of Sample type name with the number of lanes depend of {@link SeqType}
     * as list.
     * <br> Example:[
     * [pid: patient1, sampleTypeName: sampleTypeName1, seqType: sampleType1, laneCount: laneCount1],
     * [pid: patient1, sampleTypeName: sampleTypeName1, seqType: sampleType2, laneCount: laneCount2],
     * [pid: patient1, sampleTypeName: sampleTypeName2, seqType: sampleType1, laneCount: laneCount3],
     * [pid: patient2, sampleTypeName: sampleTypeName1, seqType: sampleType1, laneCount: laneCount4],
     * ...]
     * @param project the project for filtering the result
     * @return all combination of  name of {@link Individual}(pid) and sampleTypeName with with the number of lanes depend of {@link SeqType}  as list
     */
    List<Map> laneCountForSeqtypesPerPatientAndSampleType(Project project) {
        List lanes = AggregateSequences.withCriteria {
            eq("projectId", project?.id)
            projections {
                groupProperty("pid")
                groupProperty("sampleTypeName")
                groupProperty("seqTypeId")
                sum("laneCount")
            }
        }

        Map<Long, SeqType> seqTypes = [:]

        return lanes.collect {
            SeqType seqType = seqTypes[it[2]]
            if (!seqType) {
                seqType = SeqType.get(it[2])
                seqTypes.put(it[2], seqType)
            }
            [
                    pid           : it[0],
                    sampleTypeName: it[1],
                    seqType       : seqType,
                    laneCount     : it[3],
            ]
        }
    }

    List<Map> withdrawnLaneCountForSeqTypesPerPatientAndSampleType(Project project) {
        List lanes = Sequence.withCriteria {
            eq("projectId", project?.id)
            eq("fileWithdrawn", true)
            projections {
                groupProperty("pid")
                groupProperty("sampleTypeName")
                groupProperty("seqTypeId")
                count()
            }
        }

        Map<Long, SeqType> seqTypes = [:]

        return lanes.collect {
            SeqType seqType = seqTypes[it[2]]
            if (!seqType) {
                seqType = SeqType.get(it[2])
                seqTypes.put(it[2], seqType)
            }
            [
                    pid           : it[0],
                    sampleTypeName: it[1],
                    seqType       : seqType,
                    withdrawnCount: it[3],
            ]
        }
    }

    Collection<AbstractBamFile> abstractBamFilesInProjectFolder(Project project) {
        if (!project) {
            return []
        }
        return AbstractBamFile.executeQuery("""
from
        AbstractBamFile abstractBamFile
where
        workPackage.sample.individual.project = :project
        and workPackage.bamFileInProjectFolder = abstractBamFile
        and fileOperationStatus = :fileOperationStatus
""", [project: project, fileOperationStatus: AbstractBamFile.FileOperationStatus.PROCESSED])
    }
}
