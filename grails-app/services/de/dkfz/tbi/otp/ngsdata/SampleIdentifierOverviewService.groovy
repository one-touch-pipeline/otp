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
import grails.web.mapping.LinkGenerator
import org.apache.commons.lang.StringEscapeUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.project.Project

@Transactional
class SampleIdentifierOverviewService {

    RawSequenceFileService rawSequenceFileService

    @Autowired
    LinkGenerator linkGenerator

    /**
     * Returns the DataFiles of the given Project, grouped by a combined key of Sample and SeqType
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'OTP_READ_ACCESS')")
    Map<List, List<RawSequenceFile>> rawSequenceFilesOfProjectBySampleAndSeqType(Project project) {
        return rawSequenceFileService.getAllRawSequenceFilesOfProject(project).groupBy { [it.sample, it.seqType] }
    }

    List<Map> extractSampleIdentifiers(List<RawSequenceFile> rawSequenceFiles) {
        return rawSequenceFiles.groupBy { it.seqTrack.sampleIdentifier }.sort { it.key }
                .collect { String sampleIdentifier, List<RawSequenceFile> rawSequenceFilesPerIdentifier ->
                    return [
                            text     : sampleIdentifier,
                            withdrawn: rawSequenceFilesPerIdentifier.any { it.fileWithdrawn },
                            comments : StringEscapeUtils.escapeHtml(rawSequenceFilesPerIdentifier*.withdrawnComment.findAll().unique().join("; ")),
                    ]
                }
    }

    Map handleSampleIdentifierEntry(Map.Entry<List, List<RawSequenceFile>> entry) {
        return handleSampleIdentifierEntry(entry.key[0] as Sample, entry.key[1] as SeqType, entry.value)
    }

    Map handleSampleIdentifierEntry(Sample sample, SeqType seqType, List<RawSequenceFile> rawSequenceFiles) {
        return [
                individual      : [
                        id  : sample.individual.id,
                        name: sample.individual.pid,
                ],
                sampleType      : [
                        id  : sample.sampleType.id,
                        name: sample.sampleType.name,
                ],
                seqType         : [
                        id         : seqType.id,
                        displayText: seqType.displayNameWithLibraryLayout,
                ],
                sampleIdentifier: extractSampleIdentifiers(rawSequenceFiles),
        ]
    }
}
