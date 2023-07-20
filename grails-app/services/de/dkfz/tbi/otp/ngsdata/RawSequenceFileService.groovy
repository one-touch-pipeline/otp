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
import org.hibernate.transform.ResultTransformer
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.project.Project

@CompileDynamic
@Transactional
class RawSequenceFileService {

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'OTP_READ_ACCESS')")
    List<RawSequenceFile> getAllRawSequenceFilesOfProject(Project project) {
        return RawSequenceFile.withCriteria {
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
                            property("pid")
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
                RawSequenceFile transformTuple(Object[] tuple, String[] aliases) {
                    RawSequenceFile file = new FastqFile()
                    file.withdrawnComment = tuple[0]
                    file.fileWithdrawn = tuple[1]
                    file.seqTrack = new SeqTrack()
                    file.seqTrack.sampleIdentifier = tuple[2]
                    file.seqTrack.sample = new Sample()
                    file.seqTrack.sample.individual = new Individual()
                    file.seqTrack.sample.individual.id = tuple[3] as Long
                    file.seqTrack.sample.individual.pid = tuple[4]
                    file.seqTrack.sample.sampleType = new SampleType()
                    file.seqTrack.sample.sampleType.id = tuple[5] as Long
                    file.seqTrack.sample.sampleType.name = tuple[6]
                    file.seqTrack.seqType = new SeqType()
                    file.seqTrack.seqType.id = tuple[7] as long
                    file.seqTrack.seqType.displayName = tuple[8]
                    file.seqTrack.seqType.singleCell = tuple[9]
                    file.seqTrack.seqType.libraryLayout = tuple[10] as SequencingReadType
                    return file
                }

                @Override
                List transformList(List collection) {
                    return collection
                }
            })
        } as List<RawSequenceFile>
    }

    List<RawSequenceFile> findAllBySeqTrack(SeqTrack seqTrack) {
        return RawSequenceFile.findAllBySeqTrack(seqTrack)
    }

    List<RawSequenceFile> findAllByFastqImportInstance(FastqImportInstance importInstance) {
        return RawSequenceFile.createCriteria().list {
            createAlias('run', 'r')
            createAlias('seqTrack', 'st')
            eq('fastqImportInstance', importInstance)
            order('r.name', 'asc')
            order('st.laneId', 'asc')
            order('mateNumber', 'asc')
        } as List<RawSequenceFile>
    }
}
