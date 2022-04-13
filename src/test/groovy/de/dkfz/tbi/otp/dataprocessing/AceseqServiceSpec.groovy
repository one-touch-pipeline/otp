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
package de.dkfz.tbi.otp.dataprocessing

import grails.testing.services.ServiceUnitTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Unroll

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CreateFileHelper

import java.nio.file.Path

class AceseqServiceSpec extends AbstractBamFileAnalysisServiceSpec implements ServiceUnitTest<AceseqService> {

    final String pathPart = 'cnv_results'

    @Rule
    TemporaryFolder temporaryFolder

    AceseqInstance instance
    Path instancePath

    void setup() {
        Path temporaryFile = temporaryFolder.newFolder().toPath()

        this.instance = DomainFactory.createAceseqInstanceWithRoddyBamFiles()

        Path vbpPath = temporaryFile.resolve("${instance.project.dirName}/sequencing/${instance.seqType.dirName}/view-by-pid/${instance.individual.pid}")

        service.individualService = Mock(IndividualService) {
            getViewByPidPath(_, _) >> vbpPath
        }

        instancePath = vbpPath.resolve("cnv_results/${instance.seqType.libraryLayoutDirName}/" +
                "${instance.sampleType1BamFile.sampleType.dirName}_${instance.sampleType2BamFile.sampleType.dirName}/" +
                "${instance.instanceName}"
        )
    }

    @Override
    BamFilePairAnalysis getNewInstance() {
        return DomainFactory.createAceseqInstanceWithRoddyBamFiles()
    }

    @Unroll()
    void "test getPlot with #plot, tests if Path is in a valid form"() {
        expect:
        service.getPlot(instance, plot) == instancePath.resolve("plots/${plot == PlotType.ACESEQ_WG_COVERAGE ? 'control_' : ''}${instance.individual.pid}_${name}")

        where:
        plot                            | name
        PlotType.ACESEQ_GC_CORRECTED    | "gc_corrected.png"
        PlotType.ACESEQ_QC_GC_CORRECTED | "qc_rep_corrected.png"
        PlotType.ACESEQ_WG_COVERAGE     | "wholeGenome_coverage.png"
    }

    void "test getPlot with TCN_DISTANCE_COMBINED_STAR"() {
        expect:
        service.getPlot(instance, PlotType.ACESEQ_TCN_DISTANCE_COMBINED_STAR) == instancePath.resolve("${instance.individual.pid}_tcn_distances_combined_star.png")
    }

    @Unroll()
    void "test getPlots with #plots, tests if Path is in a valid form"() {
        given:
        Path expectedPath = instancePath.resolve("${instance.individual.pid}_${name}")

        CreateFileHelper.createFile(expectedPath)

        AceseqQc qcInstance = DomainFactory.createAceseqQcWithExistingAceseqInstance(instance)
        qcInstance.ploidyFactor = '1.0'
        qcInstance.tcc = tcc
        qcInstance.save(flush: true)

        expect:
        service.getPlots(instance, plots) == [expectedPath]

        where:
        plots                 | name                        | tcc
        PlotType.ACESEQ_ALL   | "plot_XX_ALL.png"           | 2.0
        PlotType.ACESEQ_EXTRA | "plot_1.0extra_2_XX.png"    | 2.0
        PlotType.ACESEQ_EXTRA | "plot_1.0extra_0.1_XX.png"  | 0.1
        PlotType.ACESEQ_EXTRA | "plot_1.0extra_0.42_XX.png" | 0.42
        PlotType.ACESEQ_EXTRA | "plot_1.0extra_0_XX.png"    | 0
    }

    void "test getQcJsonFile"() {
        expect:
        service.getQcJsonFile(instance) == instancePath.resolve("cnv_${instance.individual.pid}_parameter.json")
    }
}
