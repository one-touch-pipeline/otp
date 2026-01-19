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
package de.dkfz.tbi.otp.infrastructure.alignment

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFileService
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerConfig
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.ngsdata.*

import java.nio.file.Paths

class CellRangerLinkFileServiceSpec extends Specification implements ServiceUnitTest<CellRangerLinkFileService>, DataTest, CellRangerFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                CellRangerConfig,
                CellRangerMergingWorkPackage,
                FastqFile,
                FastqImportInstance,
                FileType,
                Individual,
                ReferenceGenomeProjectSeqType,
                Sample,
                SampleType,
                SingleCellBamFile,
        ]
    }

    SingleCellBamFile bamFile

    void setup() {
        bamFile = createBamFile()
        service.abstractBamFileService = Mock(AbstractBamFileService) {
            getBaseDirectory(_) >> Paths.get("/base-dir")
        }
    }

    void "test getBamFile"() {
        expect:
        service.getBamFile(bamFile) == Paths.get("/base-dir", bamFile.workDirectoryName, bamFile.bamFileName)
    }

    void "test getBaiFile"() {
        expect:
        service.getBaiFile(bamFile) == Paths.get("/base-dir", bamFile.workDirectoryName, bamFile.baiFileName)
    }

    void "test getSampleDirectory"() {
        expect:
        service.getSampleDirectory(bamFile) == Paths.get("/base-dir", bamFile.workDirectoryName, "cell-ranger-input", bamFile.id.toString())
    }

    void "test getOutputDirectory"() {
        expect:
        service.getOutputDirectory(bamFile) == Paths.get("/base-dir", bamFile.workDirectoryName, bamFile.id.toString())
    }

    void "test getResultDirectory"() {
        expect:
        service.getResultDirectory(bamFile) == Paths.get("/base-dir", bamFile.workDirectoryName, bamFile.id.toString(), "outs")
    }

    void "test getFileMappingForLinks"() {
        expect:
        service.getFileMappingForLinks(bamFile) == [
                "web_summary.html"             : "web_summary.html",
                "metrics_summary.csv"          : "metrics_summary.csv",
                (bamFile.bamFileName)          : "possorted_genome_bam.bam",
                (bamFile.baiFileName)          : "possorted_genome_bam.bam.bai",
                (bamFile.md5SumFileName)       : "possorted_genome_bam.md5sum",
                "filtered_feature_bc_matrix.h5": "filtered_feature_bc_matrix.h5",
                "raw_feature_bc_matrix.h5"     : "raw_feature_bc_matrix.h5",
                "molecule_info.h5"             : "molecule_info.h5",
                "cloupe.cloupe"                : "cloupe.cloupe",
                "filtered_feature_bc_matrix"   : "filtered_feature_bc_matrix",
                "raw_feature_bc_matrix"        : "raw_feature_bc_matrix",
                "analysis"                     : "analysis",
        ]
    }

    void "test getLinkedResultFiles"() {
        expect:
        service.getLinkedResultFiles(bamFile) == [
                Paths.get("/base-dir", bamFile.workDirectoryName, "web_summary.html"),
                Paths.get("/base-dir", bamFile.workDirectoryName, "metrics_summary.csv"),
                Paths.get("/base-dir", bamFile.workDirectoryName, bamFile.bamFileName),
                Paths.get("/base-dir", bamFile.workDirectoryName, bamFile.baiFileName),
                Paths.get("/base-dir", bamFile.workDirectoryName, bamFile.md5SumFileName),
                Paths.get("/base-dir", bamFile.workDirectoryName, "filtered_feature_bc_matrix.h5"),
                Paths.get("/base-dir", bamFile.workDirectoryName, "raw_feature_bc_matrix.h5"),
                Paths.get("/base-dir", bamFile.workDirectoryName, "molecule_info.h5"),
                Paths.get("/base-dir", bamFile.workDirectoryName, "cloupe.cloupe"),
                Paths.get("/base-dir", bamFile.workDirectoryName, "filtered_feature_bc_matrix"),
                Paths.get("/base-dir", bamFile.workDirectoryName, "raw_feature_bc_matrix"),
                Paths.get("/base-dir", bamFile.workDirectoryName, "analysis"),
        ]
    }

    void "test getQualityAssessmentCsvFile"() {
        expect:
        service.getQualityAssessmentCsvFile(bamFile) ==
                Paths.get("/base-dir", bamFile.workDirectoryName, bamFile.id.toString(), "outs", "metrics_summary.csv")
    }

    void "test getWebSummaryResultFile"() {
        expect:
        service.getWebSummaryResultFile(bamFile) == Paths.get("/base-dir", bamFile.workDirectoryName, bamFile.id.toString(), "outs", "web_summary.html")
    }

    void "test getPathForFurtherProcessing, should return final directory"() {
        expect:
        service.getPathForFurtherProcessing(bamFile) == Paths.get("/base-dir", bamFile.workDirectoryName, bamFile.bamFileName)
    }

    void "test getPathForFurtherProcessing, when not set in mergingWorkPackage, should throw exception"() {
        given:
        bamFile.fileOperationStatus = AbstractBamFile.FileOperationStatus.DECLARED
        bamFile.md5sum = null
        bamFile.save(flush: true)
        bamFile.mergingWorkPackage.bamFileInProjectFolder = null
        bamFile.mergingWorkPackage.save(flush: true)

        when:
        service.getPathForFurtherProcessing(bamFile)

        then:
        thrown(IllegalStateException)
    }
}
