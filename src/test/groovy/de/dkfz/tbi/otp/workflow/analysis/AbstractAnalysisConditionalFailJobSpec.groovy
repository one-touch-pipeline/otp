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
package de.dkfz.tbi.otp.workflow.analysis

import grails.testing.gorm.DataTest
import groovy.transform.TupleConstructor
import spock.lang.*

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaConfig
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.RoddySnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.infrastructure.alignment.AlignmentWorkFileServiceFactoryService
import de.dkfz.tbi.otp.infrastructure.alignment.PanCancerWorkFileService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.shared.WorkflowException
import de.dkfz.tbi.otp.workflowExecution.*

import java.nio.file.Files
import java.nio.file.Path

abstract class AbstractAnalysisConditionalFailJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory, IsRoddy {

    AbstractAnalysisConditionalFailJob job
    BamFilePairAnalysis instance
    WorkflowStep workflowStep

    @Shared
    AbstractBamFile bamFile1

    @Shared
    AbstractBamFile bamFile2

    Map<String, SeqType> seqTypeMap

    @TempDir
    Path tmpDir

    Path baseDir
    Path bamFile1Path
    Path bamFile2Path

    @Shared
    Path fastaDir

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
                seqTracks  : [createSeqTrack(seqType: seqType), createSeqTrack(seqType: seqType)],
                workPackage: createMergingWorkPackage([
                        seqType: seqType,
                        sample : createSample([
                                individual: individual,
                                sampleType: createSampleType([
                                        name: "tumor",
                                ]),
                        ]),
                ]),

        ])
        bamFile2 = createBamFile([
                seqTracks  : [createSeqTrack(seqType: seqType), createSeqTrack(seqType: seqType)],
                workPackage: createMergingWorkPackage([
                        seqType: seqType,
                        sample : createSample([
                                individual: individual,
                                sampleType: createSampleType([
                                        name: "control",
                                ]),
                        ]),
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
        baseDir = tmpDir.resolve("baseDir")
        bamFile1Path = tmpDir.resolve("bam1.bam")
        bamFile2Path = tmpDir.resolve("bam2.bam")
        fastaDir = tmpDir.resolve("fasta.fa")

        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            1 * getInputArtefact(workflowStep, "TUMOR_BAM") >> bamFile1
            1 * getInputArtefact(workflowStep, "CONTROL_BAM") >> bamFile2
            1 * getOutputArtefact(workflowStep, "ANALYSIS_OUTPUT") >> instance
            0 * _
        }
        job.alignmentWorkFileServiceFactoryService = Mock(AlignmentWorkFileServiceFactoryService) {
            1 * getService(bamFile1) >> Mock(PanCancerWorkFileService) {
                1 * getBamFile(bamFile1) >> bamFile1Path
                0 * _
            }
            1 * getService(bamFile2) >> Mock(PanCancerWorkFileService) {
                1 * getBamFile(bamFile2) >> bamFile2Path
                0 * _
            }
            0 * _
        }
        job.referenceGenomeService = Mock(ReferenceGenomeService) {
            1 * fastaFilePath(_) >> fastaDir.toFile()
            0 * _
        }
        job.fileService = Mock(FileService) {
            _ * fileIsReadable(_) >> { Path path ->
                Files.isReadable(path)
            }
            0 * _
        }
    }

    /**
     * Allows subclass to do needed additional file system setup
     */
    // most subclasses do not need this, therefore an empty implementation is provided
    @SuppressWarnings("EmptyMethodInAbstractClass")
    void setupFileSystem() {
    }

    void "check seqType: #desc, everything is ok, then no exception shall be thrown"() {
        given:
        setupWithSeqType(seqTypeName)
        setupMocking()
        setupFileSystem()

        Files.createDirectories(baseDir)
        Files.createFile(bamFile1Path)
        Files.createFile(bamFile2Path)
        Files.createFile(fastaDir)

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
    @IgnoreIf({ System.getProperty("os.name").toLowerCase().contains("windows") })
    void "check if #name, then throw exception with error messages containing check errors"() {
        given:
        setupWithSeqType(SeqTypeNames.WHOLE_GENOME.seqTypeName)
        setupMocking()
        setupFileSystem()

        Files.createDirectories(baseDir)
        bam1.closure(bamFile1Path)
        bam2.closure(bamFile2Path)
        fasta.closure(fastaDir)

        when:
        job.check(workflowStep)

        then:
        final WorkflowException exception = thrown()
        exception.message.contains(errmsg1()) && exception.message.contains(errmsg2)
        exception.message.split('\n').size() == errsize

        where:
        name                               | bam1                              | bam2                              | fasta                             || errsize | errmsg1                  | errmsg2
        "tumor bam file missing"           | FilePreparation.NONE_EXISTENCE    | FilePreparation.FILE              | FilePreparation.FILE              || 1       | { bamFile1.bamFileName } | "does not exist"
        "tumor bam file is a directory"    | FilePreparation.DIRECTORY         | FilePreparation.FILE              | FilePreparation.FILE              || 1       | { bamFile1.bamFileName } | "is not a regular file"
        "tumor bam file is not readable"   | FilePreparation.NOT_READABLE_FILE | FilePreparation.FILE              | FilePreparation.FILE              || 1       | { bamFile1.bamFileName } | "is not readable"
        "control bam file missing"         | FilePreparation.FILE              | FilePreparation.NONE_EXISTENCE    | FilePreparation.FILE              || 1       | { bamFile2.bamFileName } | "does not exist"
        "control bam file is a directory"  | FilePreparation.FILE              | FilePreparation.DIRECTORY         | FilePreparation.FILE              || 1       | { bamFile2.bamFileName } | "is not a regular file"
        "control bam file is not readable" | FilePreparation.FILE              | FilePreparation.NOT_READABLE_FILE | FilePreparation.FILE              || 1       | { bamFile2.bamFileName } | "is not readable"
        "ref genome missing"               | FilePreparation.FILE              | FilePreparation.FILE              | FilePreparation.NONE_EXISTENCE    || 1       | { fastaDir.toString() }  | "does not exist"
        "ref genome is a file"             | FilePreparation.FILE              | FilePreparation.FILE              | FilePreparation.DIRECTORY         || 1       | { fastaDir.toString() }  | "is not a regular file"
        "ref genome is not readable"       | FilePreparation.FILE              | FilePreparation.FILE              | FilePreparation.NOT_READABLE_FILE || 1       | { fastaDir.toString() }  | "is not readable"
    }

    @TupleConstructor
    enum FilePreparation {
        NONE_EXISTENCE({ Path path ->
            // do nothing
            path
        }),
        FILE({ Path path ->
            Files.createFile(path)
        }),
        DIRECTORY({ Path path ->
            Files.createDirectory(path)
        }),
        NOT_READABLE_FILE({ Path path ->
            Files.createFile(path)
            Files.setPosixFilePermissions(path, [] as Set)
        }),

        final Closure<Path> closure
    }
}
