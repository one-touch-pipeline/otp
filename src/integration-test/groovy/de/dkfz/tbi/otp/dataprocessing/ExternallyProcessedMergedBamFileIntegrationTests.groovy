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
package de.dkfz.tbi.otp.dataprocessing

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.junit.Test

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

import static org.junit.Assert.assertNotNull

@Rollback
@Integration
class ExternallyProcessedMergedBamFileIntegrationTests {

    ExternallyProcessedMergedBamFile bamFile
    Project project
    Individual individual
    SampleType sampleType
    Sample sample
    SeqType seqType
    SeqPlatform seqPlatform
    SeqCenter seqCenter
    Run run
    ReferenceGenome referenceGenome
    ExternalMergingWorkPackage externalMergingWorkPackage

    TestConfigService configService

    void setupData() {
        configService = new TestConfigService()

        project = DomainFactory.createProject(
                        name: "project",
                        dirName: "project-dir",
                        )
        assertNotNull(project.save([flush: true]))

        individual = DomainFactory.createIndividual(
                        pid: "patient",
                        mockPid: "mockPid",
                        mockFullName: "mockFullName",
                        type: Individual.Type.UNDEFINED,
                        project: project
                        )
        assertNotNull(individual.save([flush: true]))

        sampleType = DomainFactory.createSampleType(
                        name: "sample-type"
                        )
        assertNotNull(sampleType.save([flush: true]))

        sample = DomainFactory.createSample(
                        individual: individual,
                        sampleType: sampleType
                        )
        assertNotNull(sample.save([flush: true]))

        seqType = DomainFactory.createSeqType(
                        name: "seq-type",
                        libraryLayout: SequencingReadType.PAIRED,
                        dirName: "seq-type-dir"
                        )
        assertNotNull(seqType.save([flush: true]))
        seqType.refresh()

        referenceGenome = DomainFactory.createReferenceGenome(
                name: "REF_GEN"
        )

        externalMergingWorkPackage = DomainFactory.createExternalMergingWorkPackage(
                pipeline: DomainFactory.createExternallyProcessedPipelineLazy(),
                sample: sample,
                seqType: seqType,
                referenceGenome: referenceGenome
        )

        bamFile = DomainFactory.createExternallyProcessedMergedBamFile(
                type: AbstractBamFile.BamType.SORTED,
                fileName: "FILE_NAME",
                workPackage: externalMergingWorkPackage
        )
    }

    void cleanup() {
        configService.clean()
    }

    @Test
    void testGetFile() {
        setupData()
        String otpFile = bamFile.getBamFile().absolutePath
        String expectedFile = "${configService.getRootPath()}/project-dir/sequencing/seq-type-dir/view-by-pid/patient/sample-type/paired/merged-alignment/nonOTP/analysisImport_REF_GEN/FILE_NAME"
        assert otpFile == expectedFile
    }
}
