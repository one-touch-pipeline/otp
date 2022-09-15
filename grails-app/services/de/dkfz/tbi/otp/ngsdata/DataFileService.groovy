/*
 * Copyright 2011-2020 The OTP authors
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
import org.hibernate.transform.ResultTransformer
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.project.Project

@Transactional
class DataFileService {

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'OTP_READ_ACCESS')")
    List<DataFile> getAllDataFilesOfProject(Project project) {
        return DataFile.withCriteria {
            seqTrack {
                sample {
                    individual {
                        eq("project", project)
                    }
                }
            }

            projections {
                property("withdrawnComment")
                property("fileWithdrawn")

                seqTrack {
                    property("sampleIdentifier")
                    sample {
                        individual {
                            property("id")
                            property("mockFullName")
                        }
                        sampleType {
                            property("id")
                            property("name")
                        }
                    }
                    seqType {
                        property("id")
                        property("displayName")
                        property("singleCell")
                        property("libraryLayout")
                    }
                }
            }
            resultTransformer(new ResultTransformer() {
                @Override
                DataFile transformTuple(Object[] tuple, String[] aliases) {
                    DataFile dataFile = new DataFile()
                    dataFile.withdrawnComment = tuple[0]
                    dataFile.fileWithdrawn = tuple[1]
                    dataFile.seqTrack = new SeqTrack()
                    dataFile.seqTrack.sampleIdentifier = tuple[2]
                    dataFile.seqTrack.sample = new Sample()
                    dataFile.seqTrack.sample.individual = new Individual()
                    dataFile.seqTrack.sample.individual.id = tuple[3] as Long
                    dataFile.seqTrack.sample.individual.mockFullName = tuple[4]
                    dataFile.seqTrack.sample.sampleType = new SampleType()
                    dataFile.seqTrack.sample.sampleType.id = tuple[5] as Long
                    dataFile.seqTrack.sample.sampleType.name = tuple[6]
                    dataFile.seqTrack.seqType = new SeqType()
                    dataFile.seqTrack.seqType.id = tuple[7] as long
                    dataFile.seqTrack.seqType.displayName = tuple[8]
                    dataFile.seqTrack.seqType.singleCell = tuple[9]
                    dataFile.seqTrack.seqType.libraryLayout = tuple[10] as SequencingReadType
                    return dataFile
                }

                @Override
                List transformList(List collection) {
                    return collection
                }
            })
        } as List<DataFile>
    }

    List<DataFile> findAllBySeqTrack(SeqTrack seqTrack) {
        return DataFile.findAllBySeqTrack(seqTrack)
    }

    List<DataFile> findAllByFastqImportInstance(FastqImportInstance importInstance) {
        return DataFile.createCriteria().list {
            createAlias('run', 'r')
            createAlias('seqTrack', 'st')
            eq('fastqImportInstance', importInstance)
            order('r.name', 'asc')
            order('st.laneId', 'asc')
            order('mateNumber', 'asc')
        } as List<DataFile>
    }
}
