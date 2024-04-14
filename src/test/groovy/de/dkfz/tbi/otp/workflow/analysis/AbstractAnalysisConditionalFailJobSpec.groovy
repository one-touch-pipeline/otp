/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.workflow.analysis

import grails.testing.gorm.DataTest
import spock.lang.*

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaConfig
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.RoddySnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.shared.WorkflowException
import de.dkfz.tbi.otp.workflowExecution.*

import java.nio.file.Path
import java.nio.file.Paths

abstract class AbstractAnalysisConditionalFailJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory, IsRoddy {

    AbstractAnalysisConditionalFailJob job
    BamFilePairAnalysis instance
    WorkflowStep workflowStep

    @Shared
    AbstractBamFile bamFile1

    @Shared
    AbstractBamFile bamFile2

    Map<String, SeqType> seqTypeMap

    final static Path BASE_DIR = Paths.get("/baseDir")
    final static Path FASTA_DIR = Paths.get("fasta.fa")

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AbstractMergingWorkPackage,
                BamFilePairAnalysis,
                BedFile,
                FastqFile,
                MergingWorkPackage,
                Project,
                RawSequenceFile,
                RoddySnvCallingInstance,
                SampleTypePerProject,
                SeqTrack,
                ReferenceGenomeProjectSeqType,
                RunYapsaConfig,
                RunYapsaInstance,
                SophiaInstance,
                RoddyBamFile,
                Workflow,
                WorkflowRun,
        ]
    }

    void setup() {
        seqTypeMap = [
                (SeqTypeNames.WHOLE_GENOME.seqTypeName): createSeqType([
                        name         : SeqTypeNames.WHOLE_GENOME.seqTypeName,
                        libraryLayout: SequencingReadType.PAIRED,
                ]),
                (SeqTypeNames.EXOME.seqTypeName)       : createSeqType([
                        name         : SeqTypeNames.EXOME.seqTypeName,
                        libraryLayout: SequencingReadType.PAIRED,
                        needsBedFile : true
                ]),
        ]
    }

    void setupWithSeqType(String seqTypeName) {
        SeqType seqType = seqTypeMap[seqTypeName]

        Individual individual = createIndividual()

        bamFile1 = createBamFile([
                seqTracks: [createSeqTrack(seqType: seqType), createSeqTrack(seqType: seqType)],
                workPackage: createMergingWorkPackage([
                        seqType: seqType,
                        sample: createSample([individual: individual]),
                ]),

        ])
        bamFile2 = createBamFile([
                seqTracks: [createSeqTrack(seqType: seqType), createSeqTrack(seqType: seqType)],
                workPackage: createMergingWorkPackage([
                        seqType: seqType,
                        sample: createSample([individual: individual]),
                ]),
        ])

        createFastqFile([
                seqTrack      : bamFile1.containedSeqTracks.first(),
                sequenceLength: '30',
        ])
        createFastqFile([
                seqTrack      : bamFile2.containedSeqTracks.first(),
                sequenceLength: '30',
        ])
    }

    void setupMocking() {
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            1 * getInputArtefact(workflowStep, "TUMOR_BAM") >> bamFile1
            1 * getInputArtefact(workflowStep, "CONTROL_BAM") >> bamFile2
            1 * getOutputArtefact(workflowStep, "ANALYSIS_OUTPUT") >> instance
            0 * _
        }
        job.abstractBamFileService = Mock(AbstractBamFileService) {
            1 * getBaseDirectory(bamFile1) >> BASE_DIR
            1 * getBaseDirectory(bamFile2) >> BASE_DIR
            0 * _
        }
        job.referenceGenomeService = Mock(ReferenceGenomeService) {
            1 * fastaFilePath(_) >> FASTA_DIR.toFile()
            0 * _
        }
        job.fileService = Mock(FileService)
    }

    void "check seqType: #desc, everything is ok, then no exception shall be thrown"() {
        given:
        setupWithSeqType(seqTypeName)
        setupMocking()
        1 * job.fileService.isFileReadableAndNotEmpty(BASE_DIR.resolve(bamFile1.bamFileName)) >> true
        1 * job.fileService.isFileReadableAndNotEmpty(BASE_DIR.resolve(bamFile2.bamFileName)) >> true
        1 * job.fileService.isFileReadableAndNotEmpty(FASTA_DIR) >> true

        when:
        job.check(workflowStep)

        then:
        noExceptionThrown()

        where:
        desc    | seqTypeName
        "WGS"   | SeqTypeNames.WHOLE_GENOME.seqTypeName
        "EXOME" | SeqTypeNames.EXOME.seqTypeName
    }

    @Unroll
    void "check if #name, then throw exception with error messages containing check errors"() {
        given:
        setupWithSeqType(SeqTypeNames.WHOLE_GENOME.seqTypeName)
        setupMocking()

        1 * job.fileService.isFileReadableAndNotEmpty(BASE_DIR.resolve(bamFile1.bamFileName)) >> bam1
        1 * job.fileService.isFileReadableAndNotEmpty(BASE_DIR.resolve(bamFile2.bamFileName)) >> bam2
        1 * job.fileService.isFileReadableAndNotEmpty(FASTA_DIR) >> fasta

        when:
        job.check(workflowStep)

        then:
        final WorkflowException exception = thrown()
        exception.message.contains(errmsg())
        exception.message.split('\n').size() == errsize

        where:
        name                       | bam1  | bam2  | fasta || errsize | errmsg
        "tumor bam file missing"   | false | true  | true  || 1       | { bamFile1.bamFileName }
        "control bam file missing" | true  | false | true  || 1       | { bamFile2.bamFileName }
        "ref genome missing"       | true  | true  | false || 1       | { "reference genome file" }
        "all missing"              | false | false | false || 3       | { "reference genome file" }
    }
}
