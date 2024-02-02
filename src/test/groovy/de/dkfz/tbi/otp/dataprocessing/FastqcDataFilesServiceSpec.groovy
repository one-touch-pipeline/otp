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
package de.dkfz.tbi.otp.dataprocessing

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.*

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.domainFactory.FastqcDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.filestore.FilestoreService
import de.dkfz.tbi.otp.filestore.PathOption
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.utils.CreateFileHelper

import java.nio.file.Path
import java.nio.file.Paths

class FastqcDataFilesServiceSpec extends Specification implements ServiceUnitTest<FastqcDataFilesService>, DataTest, FastqcDomainFactory,
        WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                FastqcProcessedFile,
                FastqImportInstance,
                SeqPlatform,
                SeqPlatformGroup,
                ProcessingOption,
        ]
    }

    TestConfigService configService

    @TempDir
    Path tempDir

    private static final String FAST_QC = FastqcDataFilesService.FAST_QC_DIRECTORY_PART

    private static final String FASTQC_NAME = "fastqPath.zip"

    private static final String FASTQC_NAME_MD5SUM = "fastqPath.zip.md5sum"

    private static final String FASTQC_NAME_HTML = "fastqPath.html"

    SeqTrack seqTrack
    RawSequenceFile rawSequenceFile
    FastqcProcessedFile fastqcProcessedFile

    private PathOption[] pathOptions

    void setup() {
        configService = new TestConfigService(tempDir)
        service.lsdfFilesService = new LsdfFilesService()
        service.lsdfFilesService.individualService = new IndividualService()
        service.lsdfFilesService.individualService.projectService = new ProjectService()
        service.lsdfFilesService.individualService.projectService.fileSystemService = new TestFileSystemService()
        service.lsdfFilesService.individualService.projectService.configService = configService
        service.fileSystemService = new TestFileSystemService()

        seqTrack = createSeqTrack()

        rawSequenceFile = createFastqFile([seqTrack: seqTrack, project: seqTrack.project, run: seqTrack.run])
        fastqcProcessedFile = createFastqcProcessedFile([sequenceFile: rawSequenceFile])
    }

    @Unroll
    void "fastqcFileName, when fastq name is #input, then fastqc name is #output"() {
        given:
        RawSequenceFile rawSequenceFile = new FastqFile()
        rawSequenceFile.fileName = input
        FastqcProcessedFile fastqcProcessedFile = new FastqcProcessedFile([
                sequenceFile     : rawSequenceFile,
                workDirectoryName: "workDirectoryName",
        ])

        expect:
        service.fastqcFileName(fastqcProcessedFile) == output + service.FAST_QC_FILE_SUFFIX + service.FAST_QC_ZIP_SUFFIX

        where:
        input                 || output
        // no extension
        "123"                 || "123"

        // one extension
        "123.gz"              || "123"
        "123.bz2"             || "123"
        "123.txt"             || "123"
        "123.fastq"           || "123"
        "123.sam"             || "123"
        "123.bam"             || "123"
        "123.other"           || "123.other"

        // two extension, second is gz
        "123.gz.gz"           || "123.gz"
        "123.bz2.gz"          || "123"
        "123.txt.gz"          || "123"
        "123.fastq.gz"        || "123"
        "123.sam.gz"          || "123"
        "123.bam.gz"          || "123"

        // two extension, second is bz2
        "123.gz.bz2"          || "123"
        "123.bz2.bz2"         || "123"
        "123.txt.bz2"         || "123"
        "123.fastq.bz2"       || "123"
        "123.sam.bz2"         || "123"
        "123.bam.bz2"         || "123"

        // two extension, second is other
        "123.gz.other"        || "123.gz.other"
        "123.bz2.other"       || "123.bz2.other"
        "123.txt.other"       || "123.txt.other"
        "123.fastq.other"     || "123.fastq.other"
        "123.sam.other"       || "123.sam.other"
        "123.bam.other"       || "123.bam.other"

        // two extension, first is other
        "123.other.gz"        || "123.other"
        "123.other.bz2"       || "123.other"
        "123.other.txt"       || "123.other"
        "123.other.fastq"     || "123.other"
        "123.other.sam"       || "123.other"
        "123.other.bam"       || "123.other"

        // dot in name, no extension
        "123.456"             || "123.456"

        // dot in name, one extension
        "123.456.gz"          || "123.456"
        "123.456.bz2"         || "123.456"
        "123.456.txt"         || "123.456"
        "123.456.fastq"       || "123.456"
        "123.456.sam"         || "123.456"
        "123.456.bam"         || "123.456"

        // dot in name, two extension, second is gz
        "123.456.gz.gz"       || "123.456.gz"
        "123.456.bz2.gz"      || "123.456"
        "123.456.txt.gz"      || "123.456"
        "123.456.fastq.gz"    || "123.456"
        "123.456.sam.gz"      || "123.456"
        "123.456.bam.gz"      || "123.456"

        // dot in name, two extension, second is bz2
        "123.456.gz.bz2"      || "123.456"
        "123.456.bz2.bz2"     || "123.456"
        "123.456.txt.bz2"     || "123.456"
        "123.456.fastq.bz2"   || "123.456"
        "123.456.sam.bz2"     || "123.456"
        "123.456.bam.bz2"     || "123.456"

        // dot in name, two extension, second is other
        "123.456.gz.other"    || "123.456.gz.other"
        "123.456.bz2.other"   || "123.456.bz2.other"
        "123.456.txt.other"   || "123.456.txt.other"
        "123.456.fastq.other" || "123.456.fastq.other"
        "123.456.sam.other"   || "123.456.sam.other"
        "123.456.bam.other"   || "123.456.bam.other"

        // dot in name, two extension, first is other
        "123.456.other.gz"    || "123.456.other"
        "123.456.other.bz2"   || "123.456.other"
        "123.456.other.txt"   || "123.456.other"
        "123.456.other.fastq" || "123.456.other"
        "123.456.other.sam"   || "123.456.other"
        "123.456.other.bam"   || "123.456.other"

        // handle tar.bz2 (own adaption before)
        "123.tar.bz2"         || "123"
        "123.tar.gz"          || "123"
    }

    @Unroll
    void "inputFileNameAdaption, when original fastq name is #input, then adapted fastq name is #output"() {
        expect:
        service.inputFileNameAdaption(input) == output

        where:
        input          || output
        "asdf.tar"     || "asdf.tar"
        "asdf.tar.gz"  || "asdf"
        "asdf.tar.bz2" || "asdf"
        "asdf.gz"      || "asdf.gz"
        "asdf.bz2"     || "asdf"
        "asdf.bz2.tar" || "asdf.bz2.tar"
    }

    @Unroll
    void "fastqcOutputDirectory, when called with useRealPath=#useRealPath and hasPathInWorkFolder=#hasPathInWorkFolder, then return correct path"() {
        given:
        setupDataForDirectories(useRealPath, hasPathInWorkFolder)

        Path expectedPath = (useRealPath && hasPathInWorkFolder) ? Paths.get('/tmp') : fastqcPath()

        expect:
        service.fastqcOutputDirectory(fastqcProcessedFile, pathOptions) == expectedPath

        where:
        useRealPath | hasPathInWorkFolder
        false       | false
        false       | true
        true        | false
        true        | true
    }

    @Unroll
    void "fastqcOutputPath, when called with useRealPath=#useRealPath and hasPathInWorkFolder=#hasPathInWorkFolder, then return correct path"() {
        given:
        String fastqcName = service.fastqcFileName(fastqcProcessedFile)
        setupDataForDirectories(useRealPath, hasPathInWorkFolder)

        Path expectedPath = ((useRealPath && hasPathInWorkFolder) ? Paths.get('/tmp').resolve(FASTQC_NAME) : fastqcPath().resolve(fastqcName))

        expect:
        service.fastqcOutputPath(fastqcProcessedFile, pathOptions) == expectedPath

        where:
        useRealPath | hasPathInWorkFolder
        false       | false
        false       | true
        true        | false
        true        | true
    }

    @Unroll
    void "fastqcHtmlPath, when called with useRealPath=#useRealPath and hasPathInWorkFolder=#hasPathInWorkFolder, then return correct path"() {
        given:
        String fastqcHtmlName = service.fastqcFileName(fastqcProcessedFile).replaceAll(/\.zip$/, '.html')
        setupDataForDirectories(useRealPath, hasPathInWorkFolder)

        Path expectedPath = (useRealPath && hasPathInWorkFolder) ? Paths.get('/tmp').resolve(FASTQC_NAME_HTML) : fastqcPath().resolve(fastqcHtmlName)

        expect:
        service.fastqcHtmlPath(fastqcProcessedFile, pathOptions) == expectedPath

        where:
        useRealPath | hasPathInWorkFolder
        false       | false
        false       | true
        true        | false
        true        | true
    }

    @Unroll
    void "fastqcOutputMd5sumPath, when called with useRealPath=#useRealPath and hasPathInWorkFolder=#hasPathInWorkFolder, then return correct path"() {
        given:
        String fastqcMd5sumName = service.fastqcFileName(fastqcProcessedFile) + '.md5sum'
        setupDataForDirectories(useRealPath, hasPathInWorkFolder)

        Path expectedPath = ((useRealPath && hasPathInWorkFolder) ? Paths.get('/tmp').resolve(FASTQC_NAME_MD5SUM) : fastqcPath().resolve(fastqcMd5sumName))

        expect:
        service.fastqcOutputMd5sumPath(fastqcProcessedFile, pathOptions) == expectedPath

        where:
        useRealPath | hasPathInWorkFolder
        false       | false
        false       | true
        true        | false
        true        | true
    }

    void "updateFastqcProcessedFile, when file exist, then update fastqcProcessedFile"() {
        given:
        Path path = service.fastqcOutputPath(fastqcProcessedFile)
        CreateFileHelper.createFile(path)

        when:
        service.updateFastqcProcessedFile(fastqcProcessedFile)

        then:
        service.fileService = Mock(FileService) {
            1 * fileIsReadable(path) >> true
        }

        and:
        fastqcProcessedFile.fileExists
        fastqcProcessedFile.fileSize > 0
        fastqcProcessedFile.dateFromFileSystem
    }

    void "updateFastqcProcessedFile, when file not exist, then do not update fastqcProcessedFile"() {
        when:
        service.updateFastqcProcessedFile(fastqcProcessedFile)

        then:
        service.fileService = Mock(FileService) {
            1 * fileIsReadable(_) >> false
        }

        and:
        !fastqcProcessedFile.fileExists
        fastqcProcessedFile.fileSize == -1
        !fastqcProcessedFile.dateFromFileSystem
    }

    void "pathToFastQcResultFromSeqCenter, when called, then return correct path"() {
        given:
        String fastqcName = service.fastqcFileName(fastqcProcessedFile)
        Path expected = Paths.get(rawSequenceFile.initialDirectory, fastqcName)

        when:
        Path path = service.pathToFastQcResultFromSeqCenter(fastqcProcessedFile)

        then:
        path == expected
    }

    void "pathToFastQcResultMd5SumFromSeqCenter, when called, then return correct path"() {
        given:
        String fastqcName = service.fastqcFileName(fastqcProcessedFile).concat('.md5sum')
        Path expected = Paths.get(rawSequenceFile.initialDirectory, fastqcName)

        when:
        Path path = service.pathToFastQcResultMd5SumFromSeqCenter(fastqcProcessedFile)

        then:
        path == expected
    }

    private Path fastqcPath() {
        String viewByPidPath = "${configService.rootPath}/${seqTrack.project.dirName}/sequencing/${seqTrack.seqType.dirName}/view-by-pid"
        return Paths.get("${viewByPidPath}/${seqTrack.individual.pid}/${seqTrack.sampleType.dirName}/" +
                "${seqTrack.seqType.libraryLayoutDirName}/run${seqTrack.run.name}/${FAST_QC}/${fastqcProcessedFile.workDirectoryName}")
    }

    private void setupDataForDirectories(boolean useRealPath, boolean hasPathInWorkFolder) {
        fastqcProcessedFile.workflowArtefact = createWorkflowArtefact([
                producedBy: createWorkflowRun([
                        workFolder: hasPathInWorkFolder ? createWorkFolder() : null
                ]),
        ])

        pathOptions = (useRealPath ? [PathOption.REAL_PATH] : []).toArray(new PathOption[0])

        if (hasPathInWorkFolder) {
            fastqcProcessedFile.pathInWorkFolder = FASTQC_NAME
        }

        if (useRealPath && hasPathInWorkFolder) {
            service.filestoreService = Mock(FilestoreService) {
                1 * getWorkFolderPath(_) >> Paths.get('/tmp')
                0 * _
            }
        }
    }
}
