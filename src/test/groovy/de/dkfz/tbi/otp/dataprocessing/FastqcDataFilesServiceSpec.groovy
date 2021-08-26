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

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflowExecution.WorkflowArtefact

class FastqcDataFilesServiceSpec extends Specification implements ServiceUnitTest<FastqcDataFilesService>, DataTest, DomainFactoryCore,
        WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                DataFile,
                FastqcProcessedFile,
                FileType,
                Individual,
                Project,
                Realm,
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
        ]
    }

    TestConfigService configService

    SeqTrack seqTrack
    DataFile dataFile
    Realm realm

    void setup() {
        configService = new TestConfigService()
        service.lsdfFilesService = new LsdfFilesService()
        service.fileSystemService = new TestFileSystemService()

        realm = DomainFactory.createRealm()

        seqTrack = DomainFactory.createSeqTrack()
        seqTrack.project.realm = realm
        assert seqTrack.project.save(flush: true)

        dataFile = DomainFactory.createDataFile([seqTrack: seqTrack, project: seqTrack.project, run: seqTrack.run])
    }

    void "test fastqcFileName"() {
        given:
        DataFile dataFile = new DataFile()
        dataFile.fileName = input

        expect:
        service.fastqcFileName(dataFile) == output + service.FASTQC_FILE_SUFFIX + service.FASTQC_ZIP_SUFFIX

        where:
        input                 || output
        //no extension
        "123"                 || "123"

        //one extension
        "123.gz"              || "123"
        "123.bz2"             || "123"
        "123.txt"             || "123"
        "123.fastq"           || "123"
        "123.sam"             || "123"
        "123.bam"             || "123"
        "123.other"           || "123.other"

        //two extension, second is gz
        "123.gz.gz"           || "123.gz"
        "123.bz2.gz"          || "123"
        "123.txt.gz"          || "123"
        "123.fastq.gz"        || "123"
        "123.sam.gz"          || "123"
        "123.bam.gz"          || "123"

        //two extension, second is bz2
        "123.gz.bz2"          || "123"
        "123.bz2.bz2"         || "123"
        "123.txt.bz2"         || "123"
        "123.fastq.bz2"       || "123"
        "123.sam.bz2"         || "123"
        "123.bam.bz2"         || "123"

        //two extension, second is other
        "123.gz.other"        || "123.gz.other"
        "123.bz2.other"       || "123.bz2.other"
        "123.txt.other"       || "123.txt.other"
        "123.fastq.other"     || "123.fastq.other"
        "123.sam.other"       || "123.sam.other"
        "123.bam.other"       || "123.bam.other"

        //two extension, first is other
        "123.other.gz"        || "123.other"
        "123.other.bz2"       || "123.other"
        "123.other.txt"       || "123.other"
        "123.other.fastq"     || "123.other"
        "123.other.sam"       || "123.other"
        "123.other.bam"       || "123.other"

        //dot in name, no extension
        "123.456"             || "123.456"

        //dot in name, one extension
        "123.456.gz"          || "123.456"
        "123.456.bz2"         || "123.456"
        "123.456.txt"         || "123.456"
        "123.456.fastq"       || "123.456"
        "123.456.sam"         || "123.456"
        "123.456.bam"         || "123.456"


        //dot in name, two extension, second is gz
        "123.456.gz.gz"       || "123.456.gz"
        "123.456.bz2.gz"      || "123.456"
        "123.456.txt.gz"      || "123.456"
        "123.456.fastq.gz"    || "123.456"
        "123.456.sam.gz"      || "123.456"
        "123.456.bam.gz"      || "123.456"

        //dot in name, two extension, second is bz2
        "123.456.gz.bz2"      || "123.456"
        "123.456.bz2.bz2"     || "123.456"
        "123.456.txt.bz2"     || "123.456"
        "123.456.fastq.bz2"   || "123.456"
        "123.456.sam.bz2"     || "123.456"
        "123.456.bam.bz2"     || "123.456"

        //dot in name, two extension, second is other
        "123.456.gz.other"    || "123.456.gz.other"
        "123.456.bz2.other"   || "123.456.bz2.other"
        "123.456.txt.other"   || "123.456.txt.other"
        "123.456.fastq.other" || "123.456.fastq.other"
        "123.456.sam.other"   || "123.456.sam.other"
        "123.456.bam.other"   || "123.456.bam.other"

        //dot in name, two extension, first is other
        "123.456.other.gz"    || "123.456.other"
        "123.456.other.bz2"   || "123.456.other"
        "123.456.other.txt"   || "123.456.other"
        "123.456.other.fastq" || "123.456.other"
        "123.456.other.sam"   || "123.456.other"
        "123.456.other.bam"   || "123.456.other"

        //handle tar.bz2 (own adaption before)
        "123.tar.bz2"         || "123"
        "123.tar.gz"          || "123"
    }

    void "test inputFileNameAdaption"() {
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

    void "test fastqcOutputDirectory"() {
        given:
        String fastqc = DataProcessingFilesService.OutputDirectories.FASTX_QC.toString().toLowerCase()

        String viewByPidPath = "${configService.getRootPath()}/${seqTrack.project.dirName}/sequencing/${seqTrack.seqType.dirName}/view-by-pid"
        String expectedPath = "${viewByPidPath}/${seqTrack.individual.pid}/${seqTrack.sampleType.dirName}/${seqTrack.seqType.libraryLayoutDirName}/run${seqTrack.run.name}/${fastqc}"

        expect:
        service.fastqcOutputDirectory(seqTrack) == expectedPath
    }

    void "test getAndUpdateFastqcProcessedFile"() {
        given:
        FastqcProcessedFile fastqcProcessedFile = DomainFactory.createFastqcProcessedFile(dataFile: dataFile)

        expect:
        service.getAndUpdateFastqcProcessedFile(fastqcProcessedFile.dataFile) == fastqcProcessedFile
    }

    void "test updateFastqcProcessedFile, with existing FastqcProcessedFile"() {
        given:
        WorkflowArtefact workflowArtefact = createWorkflowArtefact()
        FastqcProcessedFile fastqcProcessedFile = DomainFactory.createFastqcProcessedFile(dataFile: dataFile)

        expect:
        service.getAndUpdateFastqcProcessedFile(fastqcProcessedFile.dataFile, workflowArtefact) == fastqcProcessedFile
        fastqcProcessedFile.workflowArtefact == workflowArtefact
    }

    void "test pathToFastQcResultFromSeqCenter"() {
        expect:
        service.pathToFastQcResultFromSeqCenter(dataFile).toString() == "${dataFile.initialDirectory}/${service.fastqcFileName(dataFile)}"
    }
}
