/*
 * Copyright 2011-2025 The OTP authors
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
import grails.gorm.transactions.Rollback
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

@Rollback
@Integration
class ExternallyProcessedBamFileIntegrationSpec extends Specification {

    ExternallyProcessedBamFile bamFile
    Project project
    Individual individual
    SampleType sampleType
    Sample sample
    SeqType seqType
    ReferenceGenome referenceGenome
    ExternalMergingWorkPackage externalMergingWorkPackage

    TestConfigService configService

    void setupData() {
        project = DomainFactory.createProject(
                name: "project",
                dirName: "project-dir",
        )

        individual = DomainFactory.createIndividual(
                pid: "patient",
                type: Individual.Type.UNDEFINED,
                project: project
        )

        sampleType = DomainFactory.createSampleType(
                name: "sample-type"
        )

        sample = DomainFactory.createSample(
                individual: individual,
                sampleType: sampleType
        )

        seqType = DomainFactory.createSeqType(
                name: "seq-type",
                libraryLayout: SequencingReadType.PAIRED,
                dirName: "seq-type-dir"
        )
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

        bamFile = DomainFactory.createExternallyProcessedBamFile(
                fileName: "FILE_NAME",
                workPackage: externalMergingWorkPackage
        )
    }

    void "test getFile returns correct file path"() {
        given:
        setupData()
        String expectedFile = "${configService.rootPath}/project-dir/sequencing/seq-type-dir/view-by-pid/patient/sample-type/paired/merged-alignment/nonOTP/analysisImport_REF_GEN/FILE_NAME"

        when:
        String otpFile = bamFile.bamFile.absolutePath

        then:
        otpFile == expectedFile
    }
}
