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
package overviews.statistik

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.*


/**
 * Creates an overview of used seqPlatform and libraryPreparationKit and nreads per sample for the given Projects and SeqTypes.
 * The sample is defined by project name, pid, sample type, and seqType.
 * For each it shows all different seqPlatform and libraryPreparationKit, separated by semicolon.
 * Furthermore it shows all nReads values, separated by semicolon.
 */

List<Project> projects = """
""".split('\n')*.trim().findAll().collect {
    CollectionUtils.exactlyOneElement(Project.findAllByName(it))
}

List<SeqType> seqTypes = [
        SeqTypeService.wholeGenomeBisulfitePairedSeqType,
        SeqTypeService.wholeGenomeBisulfiteTagmentationPairedSeqType
]



println SeqTrack.createCriteria().list {
    sample {
        individual {
            'in'('project', projects)
        }
    }
    'in'('seqType', seqTypes)
}.groupBy { SeqTrack seqTrack ->
    [
            seqTrack.project.name,
            seqTrack.individual.pid,
            seqTrack.sampleType.name,
            seqTrack.seqType.toString(),
    ].join(',')
}.collect { String key, List<SeqTrack> seqTracks ->
    [
            key,
            seqTracks*.seqPlatform*.toString().unique().sort().join(';'),
            seqTracks*.libraryPreparationKit*.name.unique().sort().join(';'),
            seqTracks ? DataFile.findAllBySeqTrackInList(seqTracks)*.nReads.join(';') : [],
    ].join(',')
}.sort().join('\n')
