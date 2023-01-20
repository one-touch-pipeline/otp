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
package de.dkfz.tbi.otp.dataExport

import grails.testing.gorm.DataTest
import spock.lang.*

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaConfig
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.*

import java.nio.file.*
import java.util.regex.Matcher
import java.util.regex.Pattern

class DataExportServiceSpec extends Specification implements DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                DataFile,
                SampleType,
                SeqType,
                Sample,
                Individual,
                SeqTrack,
                Pipeline,
                MergingWorkPackage,
                ProcessedBamFile,
                ProcessedMergedBamFile,
                AlignmentPass,
                MergingSet,
                MergingPass,
                MergingSetAssignment,
                SampleTypePerProject,
                SamplePair,
                RoddyWorkflowConfig,
                ProcessingThresholds,
                ProcessingOption,
                RoddyBamFile,
                ExternalMergingWorkPackage,
                ExternallyProcessedMergedBamFile,
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

    @Shared
    Path targetFolder = Paths.get(TEST_BASE_FOLDER)

    TestConfigService configService

    void setup() {
        configService = new TestConfigService()
        GroovyMock([global: true], SeqTypeService)
        SeqTypeService.rnaSingleSeqType >> DomainFactory.createRnaSingleSeqType()
        SeqTypeService.rnaPairedSeqType >> DomainFactory.createRnaPairedSeqType()
    }

    void cleanup() {
        configService.clean()
    }

    private DataExportInput createDataFileInput(boolean checkFileStatus, boolean getFileList) {
        //two seqTracks
        List<SeqTrack> seqTrackList = TEST_PID_LIST.collect {
            createSeqTrackWithOneDataFile(sample: createSample(
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
                copyExternal   : false,
                seqTrackList   : seqTrackList,
                bamFileList    : [],
                analysisListMap: [:],
        ])
    }

    @Unroll
    void "exportDataFiles, combination of different inputs, should return correct scripts"() {
        given:
        final DataExportInput dataExportInput = createDataFileInput(checkFileStatus, getFileList)

        GroovyMock([global: true], Files)
        Files.exists(_) >> true

        service.lsdfFilesService = Mock(LsdfFilesService) {
            getFileFinalPathCount * getFileFinalPath(_) >> 'finalFile'
            getFilePathInViewByPidCount * getFileViewByPidPathAsPath(_) >> Paths.get('/vbp/path/somePid')
        }
        service.individualService = Mock(IndividualService) {
            getFilePathInViewByPidCount * getViewByPidPath(_, _) >> Paths.get('/vbp/')
        }
        service.fileSystemService = Mock(FileSystemService) {
            getRemoteFileSystemOnDefaultRealm() >> new TestFileSystemService().remoteFileSystemOnDefaultRealm
        }

        when:
        DataExportOutput output = service.exportDataFiles(dataExportInput)

        then:
        switch (cases) {
            case 1:
                assert output.bashScript.contains("mkdir -p")
                assert output.bashScript.contains('rsync ${RSYNC_LOG} -upL') /* codenarc-disable-line GStringExpressionWithinString */

                assert output.listScript.contains("ls -l")

                assert output.consoleLog.empty
                break
            case 2:
                assert output.bashScript.contains("mkdir -p")
                assert output.bashScript.contains('rsync ${RSYNC_LOG} -upL') /* codenarc-disable-line GStringExpressionWithinString */

                assert output.listScript.empty

                assert output.consoleLog.empty
                break
            case 3:
                assert !["echo", "mkdir", "rsync"].every { output.bashScript.contains(it) }

                assert output.listScript.empty

                assert output.consoleLog.contains('** FASTQ **')
                TEST_PID_LIST.every {
                    assert output.consoleLog.contains(it)
                }
                break
            case 4:
                assert !["echo", "mkdir", "rsync"].every { output.bashScript.contains(it) }
                assert output.listScript.empty

                assert output.consoleLog.contains('** FASTQ **')
                TEST_PID_LIST.every {
                    assert output.consoleLog.contains(it)
                }
                break
        }

        where:
        checkFileStatus | getFileList | getFileFinalPathCount | getFilePathInViewByPidCount || cases
        false           | true        | 2                     | 2                           || 1
        false           | false       | 2                     | 2                           || 2
        true            | true        | 2                     | 0                           || 3
        true            | false       | 2                     | 0                           || 4
    }

    private DataExportInput createBamFileInput(boolean checkFileStatus, boolean getFileList, boolean external = false, boolean copyExternal = false) {
        List<AbstractMergedBamFile> bamFileList = [
                //RoddyBamFile:
                DomainFactory.createRoddyBamFile([
                        workPackage: DomainFactory.createMergingWorkPackage([
                                pipeline: DomainFactory.createPanCanPipeline(),
                                seqType : DomainFactory.createWholeGenomeSeqType(),
                        ])
                ]),
                //ProcessedMergedBamFile:
                DomainFactory.createProcessedMergedBamFile([
                        workPackage: DomainFactory.createMergingWorkPackage([
                                pipeline: DomainFactory.createDefaultOtpPipeline(),
                                seqType : DomainFactory.createWholeGenomeSeqType(),
                        ])
                ]),
                //ExternallyProcessedMergedBamFile:
                DomainFactory.createExternallyProcessedMergedBamFile([
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
                copyExternal   : copyExternal,
                copyAnalyses   : [:],
                seqTrackList   : [],
                bamFileList    : bamFileList,
                analysisListMap: [:],
        ])
    }

    @SuppressWarnings("LineLength")
    @Unroll
    void "exportBamFiles, combination of different inputs, should return correct scripts"() {
        given:
        DataExportInput dataExportInput = createBamFileInput(checkFileStatus, getFileList, external, copyExternal)

        GroovyMock([global: true], Files)
        Files.exists(_) >> fileExists

        service.fileSystemService = Mock(FileSystemService) {
            getRemoteFileSystemOnDefaultRealm() >> new TestFileSystemService().remoteFileSystemOnDefaultRealm
        }

        String copyConnection = copyExternal ? /\$\{COPY_CONNECTION\}/ : ""
        String copyTargetBase = copyExternal ? /\$\{COPY_TARGET_BASE\}/ : ""

        Pattern bashScriptPattern = ~/\[\[ -n "(.{2}ECHO_LOG.)" \]\] && echo (\/[a-zA-Z0-9\-+_.]*)*\n(mkdir -p (.{2}COPY_TARGET_BASE.)?(\/[a-zA-Z0-9\-+_.]*)*\n)?rsync (.{2}RSYNC_LOG.) -u(r)?pL ${copyConnection}(\/[a-zA-Z0-9\-+_.*]*)* ${copyTargetBase}(\/[a-zA-Z0-9\-+_.*]*)*/
        Pattern listScriptPattern = ~/ls -l (\/[a-zA-Z0-9\-+_.]*)*/
        Pattern consoleLogPattern = fileExists ?
                ~/Found BAM files \d\n\n([a-zA-Z0-9\(\)-_ ]*){${dataExportInput.bamFileList.size()}}/ :
                ~/WARNING: BAM File ([a-zA-Z0-9\(\)-_ ]*)/

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
                assert consoleLogMatcher.size() == fileExists ? 1 : dataExportInput.bamFileList.size()
                break
            case 4:
                assert !bashScriptMatcher.find()

                assert output.listScript.empty

                assert consoleLogMatcher.find()
                assert consoleLogMatcher.size() == fileExists ? 1 : dataExportInput.bamFileList.size()
                break
        }

        where:
        checkFileStatus | getFileList | external | copyExternal | fileExists || cases
        false           | true        | false    | false        | true       || 1
        false           | true        | true     | true         | true       || 1
        false           | false       | false    | false        | true       || 2
        false           | false       | true     | true         | true       || 2
        true            | true        | false    | false        | true       || 3
        true            | true        | true     | true         | true       || 3
        true            | false       | false    | false        | false      || 3
        true            | false       | false    | false        | true       || 4
        true            | false       | true     | true         | true       || 4
        true            | false       | false    | false        | false      || 4
    }

    void "exportBamFiles, if checkFileStatus=false && getFileList=true && is RNA, should return correct scripts"() {
        given:
        DataExportInput dataExportInput = createBamFileInput(false, true)
        dataExportInput.copyAnalyses.put(PipelineType.RNA_ANALYSIS, true)

        GroovyMock([global: true], Files)
        Files.exists(_) >> true

        service.fileSystemService = Mock(FileSystemService) {
            getRemoteFileSystemOnDefaultRealm() >> new TestFileSystemService().remoteFileSystemOnDefaultRealm
        }

        when:
        DataExportOutput output = service.exportBamFiles(dataExportInput)

        then:
        output.bashScript.contains("mkdir -p")
        output.bashScript.contains('rsync ${RSYNC_LOG} -urpL --exclude=*roddyExec* --exclude=.*') /* codenarc-disable-line GStringExpressionWithinString */

        output.listScript.contains("ls -l")

        output.consoleLog.empty
    }

    void "exportBamFiles, if external bam = true, should return correct scripts"() {
        given:
        DataExportInput dataExportInput = createBamFileInput(false, true)
        dataExportInput.copyAnalyses.put(PipelineType.RNA_ANALYSIS, true)

        GroovyMock([global: true], Files)
        Files.exists(_) >> true

        service.fileSystemService = Mock(FileSystemService) {
            getRemoteFileSystemOnDefaultRealm() >> new TestFileSystemService().remoteFileSystemOnDefaultRealm
        }

        when:
        DataExportOutput output = service.exportBamFiles(dataExportInput)

        then:
        output.bashScript.contains("mkdir -p")
        output.bashScript.contains('rsync ${RSYNC_LOG} -urpL --exclude=*roddyExec* --exclude=.*') /* codenarc-disable-line GStringExpressionWithinString */

        output.listScript.contains("ls -l")

        output.consoleLog.empty
    }

    private DataExportInput createAnalysisInput(boolean checkFileStatus, boolean getFileList) {
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
                copyExternal   : false, //not relevant
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
    void "exportAnalyses, combination of different inputs, should return correct scripts"() {
        given:
        DataExportInput dataExportInput = createAnalysisInput(checkFileStatus, getFileList)

        GroovyMock([global: true], Files)
        Files.exists(_) >> true

        final String instancePath = TEST_BASE_FOLDER + "/instance/path"
        service.fileService = Mock(FileService) {
            toFile(_) >> new File(instancePath)
        }
        AbstractBamFileAnalysisService<? extends BamFilePairAnalysis> abstractBamFileAnalysisService =
                Mock(AbstractBamFileAnalysisService)
        abstractBamFileAnalysisService.getWorkDirectory(_) >> targetFolder
        service.bamFileAnalysisServiceFactoryService = Mock(BamFileAnalysisServiceFactoryService)
        service.bamFileAnalysisServiceFactoryService.getService(_) >> abstractBamFileAnalysisService

        Pattern bashScriptPattern = ~/\[\[ -n "(.{2}ECHO_LOG.)" \]\] && echo ${instancePath}\nmkdir -p (.{2}COPY_TARGET_BASE.)?[a-zA-Z0-9-_\/]*\nrsync (.{2}RSYNC_LOG.) -urpL --exclude=\*roddyExec\* --exclude=\*bam\*/
        Pattern listScriptPattern = ~/ls -l --ignore=\"\*roddyExec\*\" ${instancePath}\n/
        Pattern consoleLogPattern = ~/Found following [a-zA-Z]* analyses:\n(\s*pid_\d\s*[a-zA-Z0-9-\s]*:\s*instance-\d*\n){2}/

        when:
        DataExportOutput output = service.exportAnalysisFiles(dataExportInput)

        Matcher bashScriptMatcher = output.bashScript =~ bashScriptPattern
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
            case 4:
                assert !["echo", "mkdir", "rsync"].every { output.bashScript.contains(it) }

                assert output.listScript.empty

                assert consoleLogMatcher.find()
                assert consoleLogMatcher.size() == dataExportInput.analysisListMap.size()
                break
        }

        where:
        checkFileStatus | getFileList || cases
        false           | true        || 1
        false           | false       || 2
        true            | true        || 3
        true            | false       || 4
    }
}
