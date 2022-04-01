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

/**
 * Delete Individual and handle associated objects.
 * Only use this script if the domain object is totally new and no data is connected to it at all!
 *
 * Associated objects are: Sample, SampleIdentifier and ClusterJob.
 * Sample and SampleIdentifier are fully deleted and ClusterJobs lose their association to Individual
 */

import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils

String pid = ''
assert pid: "Enter the PID of the individual"

Individual individual = CollectionUtils.atMostOneElement(Individual.findAllByPid(pid))
assert individual: "Could not find an Individual for PID ${pid}"

List<String> output = []

List<ClusterJob> clusterJobs = ClusterJob.findAllByIndividual(individual)

List<Sample> samples = Sample.findAllByIndividual(individual)
Map<Sample, List<SampleIdentifier>> sampleIdentifiersPerSample = SampleIdentifier.findAllBySampleInList(samples).groupBy { it.sample }
Map<Sample, List<SeqTrack>> seqTracksPerSample = SeqTrack.findAllBySampleInList(samples).groupBy { it.sample }

Individual.withTransaction() {
    output << "Individual: ${individual.id}: ${individual}"

    // ClusterJobs are associated with Individual for statistical purposes. We do not delete them, but remove the association.
    output << "  - ClusterJobs:"
    clusterJobs.each { ClusterJob clusterJob ->
        output << "    * ${clusterJob.id}: ${clusterJob.clusterJobId} ${clusterJob.clusterJobName}"
        clusterJob.individual = null
        clusterJob.save(flush: true)
    }

    output << "  - Samples with Identifiers:"
    samples.each { Sample sample ->
        List<SampleIdentifier> sampleIdentifiers = sampleIdentifiersPerSample[sample]

        output << "    * ${sample.id}: ${sample.sampleType.name} ${sampleIdentifiers.collect { it.name }}"

        sampleIdentifiers*.delete(flush: true)

        List<SeqTrack> seqTracks = seqTracksPerSample[sample]
        assert !seqTracks || DataFile.findAllBySeqTrackInList(seqTracks).isEmpty(): "Found data for Sample ${sample}. Take care of the data before deleting the individual"

        sample.delete(flush: true)
    }

    individual.delete(flush: true)

    println(output.join("\n"))
    assert false: "Debug"
}

''
