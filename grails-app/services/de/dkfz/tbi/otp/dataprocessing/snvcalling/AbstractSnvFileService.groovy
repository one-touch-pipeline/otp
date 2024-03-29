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
package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.ngsdata.IndividualService

import java.nio.file.Path

@SuppressWarnings('AbstractClassWithoutAbstractMethod')
abstract trait AbstractSnvFileService implements ArtefactFileService<AbstractSnvCallingInstance>, RoddyResultServiceTrait<RoddySnvCallingInstance> {

    FileService fileService
    IndividualService individualService

    private final static String SNV_RESULTS_PREFIX = 'snvs_'

    Path getSnvCallingResult(AbstractSnvCallingInstance instance) {
        return getDirectoryPath(instance).resolve("${SNV_RESULTS_PREFIX}${instance.individual.pid}_raw.vcf.gz")
    }

    Path getSnvDeepAnnotationResult(AbstractSnvCallingInstance instance) {
        return getDirectoryPath(instance).resolve("${SNV_RESULTS_PREFIX}${instance.individual.pid}.vcf.gz")
    }

    Path getCombinedPlotPath(AbstractSnvCallingInstance instance) {
        return getDirectoryPath(instance).resolve("${SNV_RESULTS_PREFIX}${instance.individual.pid}_allSNVdiagnosticsPlots.pdf")
    }

    Path getResultRequiredForRunYapsa(BamFilePairAnalysis bamFilePairAnalysis) {
        final Path workDirectory = getDirectoryPath(bamFilePairAnalysis.samplePair.findLatestSnvCallingInstance())
        final String minConfScore = /[0-9]/
        final String matcherForFileRequiredForRunYapsa =
                /.*${SNV_RESULTS_PREFIX}${individualService.getEscapedPid(bamFilePairAnalysis.individual)}_somatic_snvs_conf_${minConfScore}_to_10.vcf/
        return fileService.findFileInPath(workDirectory, matcherForFileRequiredForRunYapsa)
    }
}
