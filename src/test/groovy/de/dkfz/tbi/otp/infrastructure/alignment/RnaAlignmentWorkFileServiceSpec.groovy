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

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.roddyRna.RoddyRnaFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.filestore.FilestoreService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflowExecution.WorkflowArtefact

import java.nio.file.Paths

class RnaAlignmentWorkFileServiceSpec extends Specification
        implements ServiceUnitTest<RnaAlignmentWorkFileService>, DataTest, RoddyRnaFactory, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AbstractBamFile,
                RawSequenceFile,
                FastqFile,
                FileType,
                Individual,
                LibraryPreparationKit,
                MergingCriteria,
                Pipeline,
                Project,
                ReferenceGenome,
                ReferenceGenomeProjectSeqType,
                RnaRoddyBamFile,
                RoddyWorkflowConfig,
                Run,
                FastqImportInstance,
                Sample,
                SampleType,
                SeqCenter,
                SeqPlatform,
                SeqPlatformGroup,
                SeqPlatformModelLabel,
                SeqTrack,
                SeqType,
                SoftwareTool,
                MergingWorkPackage,
                WorkflowArtefact,
        ]
    }

    RnaRoddyBamFile bamFile

    void setupNonUuid() {
        bamFile = createBamFile()
        service.abstractBamFileService = Mock(AbstractBamFileService) {
            1 * getBaseDirectory(_) >> Paths.get("/base-dir")
        }
    }

    void setupUuid() {
        bamFile = createBamFile([
                workflowArtefact: createWorkflowArtefact([
                        producedBy: createWorkflowRun([
                                workFolder: createWorkFolder(),
                        ]),
                ]),
        ])
        service.filestoreService = Mock(FilestoreService) {
            1 * getWorkFolderPath(_) >> Paths.get("/base-dir-uuid")
        }
    }

    void "test getMergedQADirectory"() {
        given:
        setupNonUuid()

        expect:
        service.getMergedQADirectory(bamFile) == Paths.get("/base-dir", bamFile.workDirectoryName, "qualitycontrol")
    }

    void "test getSingleLaneQADirectories"() {
        expect:
        service.getSingleLaneQADirectories(bamFile) == [:]
    }

    void "test getCorrespondingWorkChimericBamFile"() {
        given:
        setupNonUuid()

        expect:
        service.getCorrespondingChimericBamFile(bamFile) ==
                Paths.get("/base-dir", bamFile.workDirectoryName, "${bamFile.sampleType.dirName}_${bamFile.individual.pid}_chimeric_merged.mdup.bam")
    }

    void "test getArribaFusionPlotPdf"() {
        given:
        setupNonUuid()

        expect:
        service.getArribaFusionPlotPdf(bamFile) ==
                Paths.get("/base-dir", bamFile.workDirectoryName, "fusions_arriba", "${bamFile.sampleType.dirName}_${bamFile.individual.pid}.fusions.pdf")
    }

    void "test getMergedQADirectory for uuid structure"() {
        given:
        setupUuid()

        expect:
        service.getMergedQADirectory(bamFile) == Paths.get("/base-dir-uuid", "qualitycontrol")
    }

    void "test getCorrespondingWorkChimericBamFile for uuid structure"() {
        given:
        setupUuid()

        expect:
        service.getCorrespondingChimericBamFile(bamFile) ==
                Paths.get("/base-dir-uuid", "${bamFile.sampleType.dirName}_${bamFile.individual.pid}_chimeric_merged.mdup.bam")
    }

    void "test getArribaFusionPlotPdf for uuid structure"() {
        given:
        setupUuid()

        expect:
        service.getArribaFusionPlotPdf(bamFile) ==
                Paths.get("/base-dir-uuid", "fusions_arriba", "${bamFile.sampleType.dirName}_${bamFile.individual.pid}.fusions.pdf")
    }
}
