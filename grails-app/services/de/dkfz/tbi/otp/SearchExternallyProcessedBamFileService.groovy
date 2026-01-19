/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedBamFile
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

@Transactional
@PreAuthorize("hasRole('ROLE_OPERATOR')")
class SearchExternallyProcessedBamFileService {

    SeqTypeService seqTypeService

    @CompileDynamic
    Set<ExternallyProcessedBamFile> getAllExternallyProcessedBamFilesByProjectAndSeqTypes(Project project, Set<SeqType> seqTypes) {
        if (!(project && seqTypes)) {
            throw new IllegalArgumentException("Invalid inputs: project and seqTypes must be specified")
        }

        return ExternallyProcessedBamFile.withCriteria {
            workPackage {
                sample {
                    individual {
                        eq('project', project)
                    }
                }
                'in'("seqType", seqTypes)
            }
        }
    }

    @CompileDynamic
    Set<ExternallyProcessedBamFile> getAllExternallyProcessedBamFilesByIndividualsAndSeqTypes(Set<Individual> individuals, Set<SeqType> seqTypes) {
        if (!(individuals && seqTypes)) {
            throw new IllegalArgumentException("Invalid inputs: Pids and seqTypes must be specified")
        }

        return ExternallyProcessedBamFile.withCriteria {
            workPackage {
                sample {
                    'in'('individual', individuals)
                }
                'in'("seqType", seqTypes)
            }
        }
    }

    @CompileDynamic
    List<ExternallyProcessedBamFile> getExternallyProcessedBamFilesByMultiInput(String pid, String sampleTypeName, String seqTypeName,
                                                                                String readTypeName, Boolean singleCell) throws AssertionError {
        SequencingReadType libraryLayout = SequencingReadType.getByName(readTypeName)
        assert libraryLayout: "${readTypeName} is not a valid sequencingReadType"
        SeqType seqTypeByImportAlias = seqTypeService.findByImportAlias(seqTypeName, [libraryLayout: libraryLayout, singleCell: singleCell])

        return ExternallyProcessedBamFile.createCriteria().list {
            workPackage {
                sample {
                    individual {
                        eq("pid", pid)
                    }
                    sampleType {
                        eq("name", sampleTypeName)
                    }
                }
                seqType {
                    or {
                        eq("name", seqTypeName)
                        eq("displayName", seqTypeName)
                        if (seqTypeByImportAlias) {
                            idEq(seqTypeByImportAlias.id)
                        }
                    }
                    eq("libraryLayout", libraryLayout)
                    eq("singleCell", singleCell)
                }
            }
        } as List<ExternallyProcessedBamFile>
    }
}
