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
package de.dkfz.tbi.otp.infrastructure.alignment

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.pipelines.externalBam.ExternalBamFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.filestore.FilestoreService
import de.dkfz.tbi.otp.workflowExecution.WorkflowArtefact

import java.nio.file.Path
import java.nio.file.Paths

class ExternalAlignmentWorkFileServiceSpec extends Specification
        implements ServiceUnitTest<ExternalAlignmentWorkFileService>, DataTest, ExternalBamFactory, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ExternalMergingWorkPackage,
                ExternallyProcessedBamFile,
                WorkflowArtefact,
        ]
    }

    private ExternallyProcessedBamFile bamFile
    private String importDir

    void setup() {
        importDir = TestCase.uniqueNonExistentPath
    }

    void setupNonUuid(int count = 1) {
        String bamName = "bamFile.bam"
        bamFile = createBamFile(fileName: bamName, importedFrom: Paths.get(importDir, bamName))
        service.abstractBamFileService = Mock(AbstractBamFileService) {
            count * getBaseDirectory(_) >> Paths.get("/base-dir")
        }
    }

    void setupUuid(int count = 1) {
        String bamName = "bamFileUuid.bam"
        bamFile = createBamFile([
                fileName        : bamName,
                importedFrom    : Paths.get(importDir, bamName),
                workflowArtefact: createWorkflowArtefact([
                        producedBy: createWorkflowRun([
                                workFolder: createWorkFolder(),
                        ]),
                ]),
        ])
        service.filestoreService = Mock(FilestoreService) {
            count * getWorkFolderPath(_) >> Paths.get("/base-dir-uuid")
        }
    }

    void "test getBamFile"() {
        given:
        setupNonUuid()

        expect:
        service.getBamFile(bamFile) == Paths.get("/base-dir", "nonOTP", "analysisImport_${bamFile.referenceGenome.name}", bamFile.bamFileName)
    }

    void "test getBaiFile"() {
        given:
        setupNonUuid()

        expect:
        service.getBaiFile(bamFile) == Paths.get("/base-dir", "nonOTP", "analysisImport_${bamFile.referenceGenome.name}", bamFile.baiFileName)
    }

    void "test getFurtherFiles"() {
        given:
        setupNonUuid(2)

        List<String> files = [
                'file1',
                'file2',
        ]
        bamFile.furtherFiles = files as Set

        List<Path> expected = files.collect {
            Paths.get("/base-dir", "nonOTP", "analysisImport_${bamFile.referenceGenome.name}", it)
        }

        expect:
        service.getFurtherFiles(bamFile) == expected
    }

    void "test getBamMaxReadLengthFile"() {
        given:
        setupNonUuid()

        expect:
        service.getBamMaxReadLengthFile(bamFile) ==
                Paths.get("/base-dir", "nonOTP", "analysisImport_${bamFile.referenceGenome.name}", "${bamFile.bamFileName}.maxReadLength")
    }

    void "test getNonOtpFolder"() {
        given:
        setupNonUuid()

        expect:
        service.getNonOtpFolder(bamFile) == Paths.get("/base-dir", "nonOTP")
    }

    void "test getImportFolder"() {
        given:
        setupNonUuid()

        expect:
        service.getDirectoryPath(bamFile) == Paths.get("/base-dir", "nonOTP", "analysisImport_${bamFile.referenceGenome.name}")
    }

    void "test getBamFile for uuid structure"() {
        given:
        setupUuid()

        expect:
        service.getBamFile(bamFile) == Paths.get("/base-dir-uuid", bamFile.bamFileName)
    }

    void "test getBaiFile for uuid structure"() {
        given:
        setupUuid()

        expect:
        service.getBaiFile(bamFile) == Paths.get("/base-dir-uuid", bamFile.baiFileName)
    }

    void "test getFurtherFiles for uuid structure"() {
        given:
        setupUuid(2)

        List<String> files = [
                'file1',
                'file2',
        ]
        bamFile.furtherFiles = files as Set

        List<Path> expected = files.collect {
            Paths.get("/base-dir-uuid", it)
        }

        expect:
        service.getFurtherFiles(bamFile) == expected
    }

    void "test getBamMaxReadLengthFile for uuid structure"() {
        given:
        setupUuid()

        expect:
        service.getBamMaxReadLengthFile(bamFile) == Paths.get("/base-dir-uuid", "${bamFile.bamFileName}.maxReadLength")
    }

    void "test getNonOtpFolder for uuid structure"() {
        given:
        setupUuid()

        expect:
        service.getNonOtpFolder(bamFile) == Paths.get("/base-dir-uuid")
    }

    void "test getImportFolder for uuid structure"() {
        given:
        setupUuid()

        expect:
        service.getDirectoryPath(bamFile) == Paths.get("/base-dir-uuid")
    }
}
