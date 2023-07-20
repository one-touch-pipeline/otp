/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.dataswap

import grails.test.hibernate.HibernateSpec
import grails.testing.services.ServiceUnitTest
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.ExternalMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedBamFile
import de.dkfz.tbi.otp.dataswap.data.LaneSwapData
import de.dkfz.tbi.otp.domainFactory.pipelines.externalBam.ExternalBamFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

import java.nio.file.Path
import java.nio.file.Paths

class LaneSwapServiceHibernateSpec extends HibernateSpec implements ServiceUnitTest<LaneSwapService>, ExternalBamFactory {

    @Override
    List<Class> getDomainClasses() {
        return [
                RawSequenceFile,
                Project,
                SampleType,
                SeqType,
                Sample,
                Individual,
                SeqTrack,
                ExternallyProcessedBamFile,
                ExternalMergingWorkPackage,
        ]
    }

    @Unroll
    void "cleanupLeftOvers, when Sample has still connected #title it should do nothing"() {
        given:
        service.fileService = Mock(FileService)

        final Individual individual = createIndividual()
        final Sample sample = createSample(individual: individual)
        final SeqType seqType = DomainFactory.createRnaPairedSeqType()
        if (seqTrackRemain) {
            createSeqTrack(sample: sample)
        }
        if (bamRemain) {
            createBamFile(
                    workPackage: createMergingWorkPackage(
                            sample: sample
                    )
            )
        }

        final LaneSwapData data = new LaneSwapData([
                individualSwap: new Swap(individual, null),
                sampleSwap    : new Swap(sample, null),
                seqTypeSwap   : new Swap(seqType, null),
        ])

        when:
        service.cleanupLeftOvers(data)

        then:
        Sample.count
        Individual.count
        seqTrackRemain && SeqTrack.count || !SeqTrack.count
        bamRemain && ExternallyProcessedBamFile.count || !ExternallyProcessedBamFile.count

        where:
        bamRemain | seqTrackRemain | title
        true      | false          | "ExternallyProcessedBamFiles"
        false     | true           | "SeqTracks"
        true      | true           | "SeqTracks or ExternallyProcessedBamFiles"
    }

    void "cleanupLeftOvers, when Sample has no connected SeqTracks or ExternallyProcessedBamFiles it should delete sample"() {
        given:
        service.fileService = Mock(FileService)

        final Individual individual = createIndividual()
        final Sample sample = createSample(individual: individual)
        createSample(individual: individual)
        final Path vbpPath = Paths.get("/vbpPath/")
        final Path sampleDir = Paths.get("/samplePath")

        final LaneSwapData data = new LaneSwapData([
                individualSwap        : new Swap(individual, null),
                sampleSwap            : new Swap(sample, null),
                cleanupIndividualPaths: [vbpPath],
                cleanupSampleTypePaths: [sampleDir],
        ])

        when:
        service.cleanupLeftOvers(data)

        then:
        Sample.count == 1
        Individual.count == 1

        String bashScriptSnippet = data.moveFilesCommands.join("\n")
        bashScriptSnippet.contains("################ cleanup empty sample and pid directories ################")
        bashScriptSnippet.contains("rm -rf ${sampleDir}")
        !bashScriptSnippet.contains("rm -rf ${vbpPath}\n")
    }

    void "cleanupLeftOvers, when Sample has no connected SeqTracks or ExternallyProcessedBamFiles and Individual has no more samples it should delete sample and individual"() {
        given:
        service.fileService = Mock(FileService)

        final Individual individual = createIndividual()
        final Sample sample = createSample(individual: individual)
        final Path vbpPath = Paths.get("/vbpPath/")
        final Path sampleDir = Paths.get("/samplePath")

        final LaneSwapData data = new LaneSwapData([
                individualSwap        : new Swap(individual, null),
                sampleSwap            : new Swap(sample, null),
                cleanupIndividualPaths: [vbpPath],
                cleanupSampleTypePaths: [sampleDir],
        ])

        when:
        service.cleanupLeftOvers(data)

        then:
        !Sample.count
        !Individual.count

        String bashScriptSnippet = data.moveFilesCommands.join("\n")
        bashScriptSnippet.contains("################ cleanup empty sample and pid directories ################")
        bashScriptSnippet.contains("rm -rf ${sampleDir}")
        bashScriptSnippet.contains("rm -rf ${vbpPath}")
    }
}
