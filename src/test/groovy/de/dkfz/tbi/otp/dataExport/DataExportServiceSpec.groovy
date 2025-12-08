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
package de.dkfz.tbi.otp.dataExport

import grails.testing.gorm.DataTest
import spock.lang.*

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqInstance
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingInstance
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaConfig
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.*

import java.nio.file.*
import java.util.regex.Matcher
import java.util.regex.Pattern

class DataExportServiceSpec extends Specification implements DataTest, IsRoddy {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                RawSequenceFile,
                SampleType,
                SeqType,
                Sample,
                Individual,
                SeqTrack,
                Pipeline,
                MergingWorkPackage,
                SampleTypePerProject,
                SamplePair,
                RoddyWorkflowConfig,
                ProcessingOption,
                RoddyBamFile,
                ExternalMergingWorkPackage,
                ExternallyProcessedBamFile,
                IndelCallingInstance,
                SnvCallingInstance,
                SnvConfig,
                RunYapsaInstance,
                RunYapsaConfig,
                AceseqInstance,
                SophiaInstance,
        ]
    }

    @Shared
    DataExportService service = new DataExportService()

    final static List<String> TEST_PID_LIST = ["pid_1", "pid_2"]
    final static String TEST_UNIX_GROUP = "test"
    final static String TEST_BASE_FOLDER = "/tmp/target"

    final static String LINK_PATTERN = "for file in [\$]\\(ls -d ([^()])*\\); do base=[\$]\\(basename [\$]file\\) ln -sf [\$]file (\\/[a-zA-Z0-9\\-+_.]*)*[\$]base; done;"

    @TempDir
    Path tempDir

    @Shared
    Path targetFolder = Paths.get(TEST_BASE_FOLDER)

    TestConfigService configService

    void setup() {
        configService = new TestConfigService()

        DomainFactory.createRnaPairedSeqType()
        DomainFactory.createRnaSingleSeqType()
        DomainFactory.createExomeSeqType()
    }

    void cleanup() {
        configService.clean()
    }

    /**
     * Helper method to set up Files.exists() mock consistently across tests
     */
    private void setupFilesMock(boolean fileExists) {
        GroovyMock([global: true], Files)
        Files.exists(_ as Path) >> fileExists
    }

    private DataExportInput createDataFileInput(boolean checkFileStatus, boolean getFileList, DataExportInput.Mode mode = DataExportInput.Mode.COPY_INTERNAL) {
        // two seqTracks
        List<SeqTrack> seqTrackList = TEST_PID_LIST.collect {
            createSeqTrackWithOneFastqFile(sample: createSample(
                    individual: createIndividual(
                            pid: it,
                    )
            )
            )
        }

        return new DataExportInput([
                targetFolder   : targetFolder,
                checkFileStatus: checkFileStatus,
                getFileList    : getFileList,
                unixGroup      : TEST_UNIX_GROUP,
                external       : false,
                mode           : mode,
                seqTrackList   : seqTrackList,
                bamFileList    : [],
                analysisListMap: [:],
        ])
    }

    @Unroll
    void "exportRawSequenceFiles, combination of different inputs, should return correct scripts"() {
        given:
        final DataExportInput dataExportInput = createDataFileInput(checkFileStatus, getFileList, mode)
        Path finalFile = tempDir.resolve('finalFile')
        Files.createFile(finalFile)

        service.rawSequenceDataWorkFileService = Mock(RawSequenceDataWorkFileService) {
            getFileFinalPathCount * getFilePath(_) >> finalFile
        }
        service.rawSequenceDataViewFileService = Mock(RawSequenceDataViewFileService) {
            getFilePathInViewByPidCount * getDirectoryPath(_) >> Paths.get('/vbp/path')
        }
        service.individualService = Mock(IndividualService) {
            getFilePathInViewByPidCount * getViewByPidPath(_, _) >> Paths.get('/vbp/')
        }
        service.fileSystemService = Mock(FileSystemService) {
            getRemoteFileSystem() >> new TestFileSystemService().remoteFileSystem
        }

        when:
        DataExportOutput output = service.exportRawSequenceFiles(dataExportInput)

        then:
        switch (cases) {
            case 1:
                assert ["echo", "mkdir -p", "rsync \${RSYNC_LOG} -upL"].every { output.bashScript.contains(it) } /* codenarc-disable-line GStringExpressionWithinString */
                assert output.listScript.contains("ls -l")
                assert output.consoleLog.empty
                break
            case 2:
                assert ["echo", "mkdir -p", "rsync \${RSYNC_LOG} -upL"].every { output.bashScript.contains(it) } /* codenarc-disable-line GStringExpressionWithinString */
                assert output.listScript.empty
                assert output.consoleLog.empty
                break
            case 3:
                assert !["echo", "mkdir", "rsync", "ln -s"].every { output.bashScript.contains(it) }
                assert output.listScript.empty
                assert output.consoleLog.contains('** FASTQ **')
                TEST_PID_LIST.each {
                    assert output.consoleLog.contains(it)
                }
                break
            case 4:
                assert !["echo", "mkdir", "rsync", "ln -s"].every { output.bashScript.contains(it) }
                assert output.listScript.empty
                assert output.consoleLog.contains('** FASTQ **')
                TEST_PID_LIST.each {
                    assert output.consoleLog.contains(it)
                }
                break
            case 5:
                assert ["echo", "mkdir", "ln -s"].every { output.bashScript.contains(it) }
                assert output.listScript.contains("ls -l")
                assert output.consoleLog.empty
                break
        }

        where:
        checkFileStatus | getFileList | getFileFinalPathCount | getFilePathInViewByPidCount | mode                               || cases
        false           | true        | 2                     | 2                           | DataExportInput.Mode.COPY_INTERNAL || 1
        false           | false       | 2                     | 2                           | DataExportInput.Mode.COPY_INTERNAL || 2
        true            | true        | 2                     | 0                           | DataExportInput.Mode.COPY_INTERNAL || 3
        true            | false       | 2                     | 0                           | DataExportInput.Mode.COPY_INTERNAL || 4
        false           | true        | 2                     | 2                           | DataExportInput.Mode.LINK_INTERNAL || 5
    }

    private DataExportInput createBamFileInput(boolean checkFileStatus, boolean getFileList, boolean external = false,
                                               DataExportInput.Mode mode = DataExportInput.Mode.COPY_INTERNAL) {
        List<AbstractBamFile> bamFileList = [
                // RoddyBamFile:
                DomainFactory.createRoddyBamFile([
                        workPackage: DomainFactory.createMergingWorkPackage([
                                pipeline: DomainFactory.createPanCanPipeline(),
                                seqType : DomainFactory.createWholeGenomeSeqType(),
                        ])
                ]),
                // ExternallyProcessedBamFile:
                DomainFactory.createExternallyProcessedBamFile([
                        workPackage: DomainFactory.createExternalMergingWorkPackage([
                                pipeline: DomainFactory.createExternallyProcessedPipelineLazy(),
                                seqType : DomainFactory.createRnaSingleSeqType(),
                        ])
                ]),
        ]

        return new DataExportInput([
                targetFolder   : targetFolder,
                checkFileStatus: checkFileStatus,
                getFileList    : getFileList,
                unixGroup      : TEST_UNIX_GROUP,
                external       : external,
                mode           : mode,
                copyAnalyses   : [:],
                seqTrackList   : [],
                bamFileList    : bamFileList,
                analysisListMap: [:],
        ])
    }

    @SuppressWarnings("LineLength")
    @Unroll
    @IgnoreIf({ System.getProperty("os.name").toLowerCase().contains("windows") })
    void "exportBamFiles, combination of different inputs, should return correct scripts"() {
        given:
        DataExportInput dataExportInput = createBamFileInput(checkFileStatus, getFileList, external, mode)

        setupFilesMock(fileExists)
        service.fileSystemService = Mock(FileSystemService) {
            getRemoteFileSystem() >> new TestFileSystemService().remoteFileSystem
        }

        String copyConnection = dataExportInput.mode == DataExportInput.Mode.COPY_EXTERNAL ? /\$\{COPY_CONNECTION\}/ : ""
        String copyTargetBase = dataExportInput.mode == DataExportInput.Mode.COPY_EXTERNAL ? /\$\{COPY_TARGET_BASE\}/ : ""

        String bashScriptBase = "\\[\\[ -n \"(.{2}ECHO_LOG.)\" \\]\\] && echo (/[a-zA-Z0-9\\-+_.]*)*\\n(mkdir -p (.{2}COPY_TARGET_BASE.)?(/[a-zA-Z0-9\\-+_.]*)*\\n)"
        String bashScriptRsync = "rsync (.{2}RSYNC_LOG.) -u(r)?pL ${copyConnection}(\\/[a-zA-Z0-9\\-+_.*]*)* ${copyTargetBase}(\\/[a-zA-Z0-9\\-+_.*]*)*"
        String bashScriptLink = LINK_PATTERN

        String bashScriptPatternStr = bashScriptBase + '?' +
                (mode == DataExportInput.Mode.LINK_INTERNAL ? bashScriptLink : bashScriptRsync)

        Pattern bashScriptPattern = Pattern.compile(bashScriptPatternStr)
        Pattern listScriptPattern = ~/ls -l (\/[a-zA-Z0-9\-+_.]*)*/
        Pattern consoleLogPattern = fileExists ?
                ~/Found BAM files \d\n\n([a-zA-Z0-9()-_ ]*){${dataExportInput.bamFileList.size()}}/ :
                ~/WARNING: BAM File ([a-zA-Z0-9()-_ ]*)/

        when:
        DataExportOutput output = service.exportBamFiles(dataExportInput)

        Matcher bashScriptMatcher = output.bashScript =~ bashScriptPattern
        Matcher listScriptMatcher = output.listScript =~ listScriptPattern
        Matcher consoleLogMatcher = output.consoleLog =~ consoleLogPattern

        then:
        switch (cases) {
            case 1:
                assert bashScriptMatcher.find()
                assert bashScriptMatcher.size() == dataExportInput.bamFileList.size() * 2

                assert listScriptMatcher.find()
                assert listScriptMatcher.size() == dataExportInput.bamFileList.size() * 2

                assert output.consoleLog.empty
                break
            case 2:
                assert bashScriptMatcher.find()
                assert bashScriptMatcher.size() == dataExportInput.bamFileList.size() * 2

                assert output.listScript.empty
                assert output.consoleLog.empty
                break
            case 3:
                assert !bashScriptMatcher.find()

                assert output.listScript.empty

                assert consoleLogMatcher.find()
                int expectedMatches = fileExists ? 1 : dataExportInput.bamFileList.size()
                assert consoleLogMatcher.size() == expectedMatches
                break
        }

        where:
        checkFileStatus | getFileList | external | fileExists | mode                               || cases
        false           | true        | false    | true       | DataExportInput.Mode.COPY_INTERNAL || 1
        false           | true        | true     | true       | DataExportInput.Mode.COPY_EXTERNAL || 1
        false           | true        | false    | true       | DataExportInput.Mode.LINK_INTERNAL || 1
        false           | true        | true     | true       | DataExportInput.Mode.LINK_INTERNAL || 1
        false           | false       | false    | true       | DataExportInput.Mode.COPY_INTERNAL || 2
        false           | false       | true     | true       | DataExportInput.Mode.COPY_EXTERNAL || 2
        false           | false       | true     | true       | DataExportInput.Mode.LINK_INTERNAL || 2
        true            | true        | false    | true       | DataExportInput.Mode.COPY_INTERNAL || 3
        true            | true        | true     | true       | DataExportInput.Mode.COPY_EXTERNAL || 3
        true            | false       | false    | false      | DataExportInput.Mode.COPY_INTERNAL || 3
        true            | false       | false    | false      | DataExportInput.Mode.LINK_INTERNAL || 3
    }

    @Unroll
    void "exportBamFiles, if checkFileStatus=false && getFileList=true && is RNA, should return correct scripts"() {
        given:
        DataExportInput dataExportInput = createBamFileInput(false, true, false, mode)
        dataExportInput.copyAnalyses.put(pipeline, true)

        setupFilesMock(true)
        service.fileSystemService = new TestFileSystemService()

        when:
        DataExportOutput output = service.exportBamFiles(dataExportInput)

        then:
        output.bashScript.contains("mkdir -p")
        output.bashScript.contains(pattern)

        output.listScript.contains("ls -l")

        output.consoleLog.empty

        where:
        mode                               | pipeline                   || pattern
        DataExportInput.Mode.COPY_INTERNAL | PipelineType.INDEL         || 'rsync ${RSYNC_LOG} -upL' /* codenarc-disable-line GStringExpressionWithinString */
        DataExportInput.Mode.COPY_INTERNAL | PipelineType.RNA_ANALYSIS  || 'rsync ${RSYNC_LOG} -urpL --exclude=*roddyExec* --exclude=.*' /* codenarc-disable-line GStringExpressionWithinString */
        DataExportInput.Mode.COPY_EXTERNAL | PipelineType.INDEL         || 'rsync ${RSYNC_LOG} -upL' /* codenarc-disable-line GStringExpressionWithinString */
        DataExportInput.Mode.COPY_EXTERNAL | PipelineType.RNA_ANALYSIS  || 'rsync ${RSYNC_LOG} -urpL --exclude=*roddyExec* --exclude=.*' /* codenarc-disable-line GStringExpressionWithinString */
        DataExportInput.Mode.LINK_INTERNAL | PipelineType.INDEL         || 'do base=$(basename $file) ln -sf $file'
        DataExportInput.Mode.LINK_INTERNAL | PipelineType.RNA_ANALYSIS  || 'do base=$(basename $file) ln -sf $file'
    }

    private DataExportInput createAnalysisInput(boolean checkFileStatus, boolean getFileList, DataExportInput.Mode mode = DataExportInput.Mode.COPY_INTERNAL) {
        Map<PipelineType, List<BamFilePairAnalysis>> analysisListMap = [
                (PipelineType.INDEL)    : [
                        DomainFactory.createIndelCallingInstanceWithRoddyBamFiles(),
                        DomainFactory.createIndelCallingInstanceWithRoddyBamFiles(),
                ],
                (PipelineType.SOPHIA)   : [
                        DomainFactory.createSophiaInstanceWithRoddyBamFiles(),
                        DomainFactory.createSophiaInstanceWithRoddyBamFiles(),
                ],
                (PipelineType.SNV)      : [
                        DomainFactory.createSnvInstanceWithRoddyBamFiles(),
                        DomainFactory.createSnvInstanceWithRoddyBamFiles(),
                ],
                (PipelineType.ACESEQ)   : [
                        DomainFactory.createAceseqInstanceWithRoddyBamFiles(),
                        DomainFactory.createAceseqInstanceWithRoddyBamFiles(),
                ],
                (PipelineType.RUN_YAPSA): [
                        DomainFactory.createRunYapsaInstanceWithRoddyBamFiles(),
                        DomainFactory.createRunYapsaInstanceWithRoddyBamFiles(),
                ],
        ]

        return new DataExportInput([
                targetFolder   : targetFolder,
                checkFileStatus: checkFileStatus,
                getFileList    : getFileList,
                unixGroup      : TEST_UNIX_GROUP,
                external       : false,
                mode           : mode,
                copyAnalyses   : [
                        (PipelineType.INDEL)    : true,
                        (PipelineType.SNV)      : true,
                        (PipelineType.SOPHIA)   : true,
                        (PipelineType.ACESEQ)   : true,
                        (PipelineType.RUN_YAPSA): true,
                ],
                seqTrackList   : [],
                bamFileList    : [],
                analysisListMap: analysisListMap,
        ])
    }

    @Unroll
    @IgnoreIf({ System.getProperty("os.name").toLowerCase().contains("windows") })
    void "exportAnalyses, combination of different inputs, should return correct scripts"() {
        given:
        DataExportInput dataExportInput = createAnalysisInput(checkFileStatus, getFileList, mode)

        final String instancePath = TEST_BASE_FOLDER + "/instance/path"
        AbstractAnalysisWorkFileService<? extends BamFilePairAnalysis> abstractBamFileAnalysisService =
                Mock(AbstractAnalysisWorkFileService)
        _ * abstractBamFileAnalysisService.getDirectoryPath(_) >> Paths.get(instancePath)
        service.analysisWorkFileServiceFactoryService = Mock(AnalysisWorkFileServiceFactoryService)
        _ * service.analysisWorkFileServiceFactoryService.getService(_) >> abstractBamFileAnalysisService

        String copyConnection = dataExportInput.mode == DataExportInput.Mode.COPY_EXTERNAL ? /[\$]\{COPY_CONNECTION\}/ : ""
        String copyTargetBase = dataExportInput.mode == DataExportInput.Mode.COPY_EXTERNAL ? /[\$]\{COPY_TARGET_BASE\}/ : ""

        String bashScriptBase = "\\[\\[ -n \"(.{2}ECHO_LOG.)\" \\]\\] && echo (\\/[^\\/ \\n]*)+\\nmkdir -p ${copyTargetBase}(\\/[^\\/ \\n]*)+\\n"

        String bashScriptRsyncPattern = "rsync (.{2}RSYNC_LOG.) -urpL --exclude=\\*roddyExec\\* --exclude=\\*bam\\* ${copyConnection}(\\/[^\\/ \\n]*)+ ${copyTargetBase}(\\/[^\\/ \\n]*)+\\n"
        String bashScriptLnPattern = LINK_PATTERN
        Pattern listScriptPattern = ~/ls -l --ignore=\"\*roddyExec\*\" ${instancePath}\n/

        Pattern consoleLogPattern = ~/Found following [a-zA-Z]* analyses:\n(\s*pid_\d\s*[a-zA-Z0-9-\s]*:\s*instance-\d*\n){2}/

        when:
        DataExportOutput output = service.exportAnalysisFiles(dataExportInput)

        Matcher bashScriptMatcher = output.bashScript =~ Pattern.compile(bashScriptBase + (mode == DataExportInput.Mode.LINK_INTERNAL ? bashScriptLnPattern : bashScriptRsyncPattern))
        Matcher listScriptMatcher = output.listScript =~ listScriptPattern
        Matcher consoleLogMatcher = output.consoleLog =~ consoleLogPattern

        then:
        switch (cases) {
            case 1:
                assert bashScriptMatcher.find()
                assert bashScriptMatcher.size() == dataExportInput.analysisListMap.size() * 2

                assert listScriptMatcher.find()
                assert listScriptMatcher.size() == dataExportInput.analysisListMap.size() * 2

                assert output.consoleLog.empty
                break
            case 2:
                assert bashScriptMatcher.find()
                assert bashScriptMatcher.size() == dataExportInput.analysisListMap.size() * 2

                assert output.listScript.empty

                assert output.consoleLog.empty
                break
            case 3:
                assert !["echo", "mkdir", "rsync"].every { output.bashScript.contains(it) }

                assert output.listScript.empty

                assert consoleLogMatcher.find()
                assert consoleLogMatcher.size() == dataExportInput.analysisListMap.size()
                break
        }

        where:
        checkFileStatus | getFileList | mode                               | cases
        false           | true        | DataExportInput.Mode.COPY_INTERNAL | 1
        false           | true        | DataExportInput.Mode.COPY_EXTERNAL | 1
        false           | true        | DataExportInput.Mode.LINK_INTERNAL | 1
        false           | false       | DataExportInput.Mode.COPY_INTERNAL | 2
        false           | false       | DataExportInput.Mode.LINK_INTERNAL | 2
        false           | false       | DataExportInput.Mode.COPY_EXTERNAL | 2
        true            | true        | DataExportInput.Mode.COPY_INTERNAL | 3
        true            | true        | DataExportInput.Mode.COPY_EXTERNAL | 3
        true            | true        | DataExportInput.Mode.LINK_INTERNAL | 3
        true            | false       | DataExportInput.Mode.COPY_INTERNAL | 3
        true            | false       | DataExportInput.Mode.COPY_EXTERNAL | 3
        true            | false       | DataExportInput.Mode.LINK_INTERNAL | 3
    }

    @IgnoreIf({ System.getProperty("os.name").toLowerCase().contains("windows") })
    void "exportBamFiles, when linking entire directory for RNA analysis, should not create individual qualitycontrol links"() {
        given: "RNA BAM file with RNA_ANALYSIS enabled and LINK_INTERNAL mode"
        RoddyBamFile rnaBamFile = createBamFile([
                workPackage: createMergingWorkPackage([
                        seqType : DomainFactory.createRnaPairedSeqType(),
                ])
        ])

        DataExportInput dataExportInput = new DataExportInput([
                targetFolder   : targetFolder,
                checkFileStatus: false,
                getFileList    : false,
                unixGroup      : TEST_UNIX_GROUP,
                external       : false,
                mode           : DataExportInput.Mode.LINK_INTERNAL,
                copyAnalyses   : [(PipelineType.RNA_ANALYSIS): true],
                seqTrackList   : [],
                bamFileList    : [rnaBamFile],
                analysisListMap: [:],
        ])

        and: "Files exist for both BAM and qualitycontrol"
        setupFilesMock(true)

        and: "Mock file system service"
        service.fileSystemService = new TestFileSystemService()

        when: "Export BAM files"
        DataExportOutput output = service.exportBamFiles(dataExportInput)

        then: "Script should link entire directory but NOT link qualitycontrol individually"
        // Should contain linking for entire basePath
        output.bashScript.contains('$(ls -d /') && output.bashScript.contains('/* | grep -v roddyExec)')
        output.bashScript.contains('for file in $(ls -d')

        // Should NOT contain individual qualitycontrol linking
        !output.bashScript.contains('qualitycontrol/*')

        // Count the number of link operations - should only be 1 (for entire directory)
        (output.bashScript =~ /for file in \$\(ls -d/).size() == 1

        and: "No console output for non-check mode"
        output.consoleLog.empty
    }

    @IgnoreIf({ System.getProperty("os.name").toLowerCase().contains("windows") })
    void "exportBamFiles, when NOT linking entire directory, should create individual qualitycontrol links"() {
        given: "Non-RNA BAM file with LINK_INTERNAL mode (regular BAM export)"
        RoddyBamFile wgsBamFile = createBamFile([
                workPackage: createMergingWorkPackage([
                        seqType : DomainFactory.createWholeGenomeSeqType(),
                ])
        ])

        DataExportInput dataExportInput = new DataExportInput([
                targetFolder   : targetFolder,
                checkFileStatus: false,
                getFileList    : false,
                unixGroup      : TEST_UNIX_GROUP,
                external       : false,
                mode           : DataExportInput.Mode.LINK_INTERNAL,
                copyAnalyses   : [:],
                seqTrackList   : [],
                bamFileList    : [wgsBamFile],
                analysisListMap: [:],
        ])

        and: "Files exist for both BAM and qualitycontrol"
        setupFilesMock(true)

        and: "Mock file system service"
        service.fileSystemService = new TestFileSystemService()

        when: "Export BAM files"
        DataExportOutput output = service.exportBamFiles(dataExportInput)

        then: "Script should link individual files AND qualitycontrol separately"
        // Should contain linking for individual BAM files
        output.bashScript.contains('$(ls -d /')
        output.bashScript.contains('.bam*)')

        // Should ALSO contain individual qualitycontrol linking
        output.bashScript.contains('qualitycontrol/*')

        // Count the number of link operations - should be 2 (BAM file + qualitycontrol)
        (output.bashScript =~ /for file in \$\(ls -d/).size() == 2

        and: "No console output for non-check mode"
        output.consoleLog.empty
    }

    void "exportBamFiles, when using COPY mode for RNA analysis, should handle qualitycontrol correctly"() {
        given: "RNA BAM file with RNA_ANALYSIS enabled and COPY_INTERNAL mode"
        RoddyBamFile rnaBamFile = createBamFile([
                workPackage: createMergingWorkPackage([
                        seqType : DomainFactory.createRnaPairedSeqType(),
                ])
        ])

        DataExportInput dataExportInput = new DataExportInput([
                targetFolder   : targetFolder,
                checkFileStatus: false,
                getFileList    : false,
                unixGroup      : TEST_UNIX_GROUP,
                external       : false,
                mode           : DataExportInput.Mode.COPY_INTERNAL,
                copyAnalyses   : [(PipelineType.RNA_ANALYSIS): true],
                seqTrackList   : [],
                bamFileList    : [rnaBamFile],
                analysisListMap: [:],
        ])

        and: "Files exist for both BAM and qualitycontrol"
        setupFilesMock(true)

        and: "Mock file system service"
        service.fileSystemService = new TestFileSystemService()

        when: "Export BAM files"
        DataExportOutput output = service.exportBamFiles(dataExportInput)

        then: "Script should use rsync for entire directory AND qualitycontrol separately (COPY mode behavior)"
        // Should contain rsync for entire basePath
        output.bashScript.contains('rsync ${RSYNC_LOG} -urpL --exclude=*roddyExec* --exclude=.*') /* codenarc-disable-line GStringExpressionWithinString */

        // Should NOT contain rsync for qualitycontrol (already copied with above command)
        !output.bashScript.contains('qualitycontrol/*')

        // Should NOT contain any linking operations
        !output.bashScript.contains('for file in $(ls -d')

        and: "No console output for non-check mode"
        output.consoleLog.empty
    }
}
