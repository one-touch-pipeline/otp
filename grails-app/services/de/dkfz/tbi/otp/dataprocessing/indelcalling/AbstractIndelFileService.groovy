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
package de.dkfz.tbi.otp.dataprocessing.indelcalling

import de.dkfz.tbi.otp.dataprocessing.ArtefactFileService
import de.dkfz.tbi.otp.dataprocessing.RoddyResultServiceTrait

import java.nio.file.Path

@SuppressWarnings('AbstractClassWithoutAbstractMethod')
abstract trait AbstractIndelFileService implements ArtefactFileService<IndelCallingInstance>, RoddyResultServiceTrait<IndelCallingInstance> {

    private final static String INDEL_RESULTS_PREFIX = 'indel_'

    List<Path> getResultFilePathsToValidate(IndelCallingInstance instance) {
        return ["${INDEL_RESULTS_PREFIX}${instance.individual.pid}.vcf.gz", "${INDEL_RESULTS_PREFIX}${instance.individual.pid}.vcf.raw.gz"].collect {
            getDirectoryPath(instance).resolve(it)
        } as List<Path>
    }

    Path getCombinedPlotPath(IndelCallingInstance instance) {
        return getDirectoryPath(instance).resolve("screenshots/${INDEL_RESULTS_PREFIX}somatic_functional_combined.pdf")
    }

    Path getCombinedPlotPathTiNDA(IndelCallingInstance instance) {
        return getDirectoryPath(instance).resolve("snvs_${instance.individual.pid}.GTfiltered_gnomAD.Germline.Rare.Rescue.png")
    }

    Path getIndelQcJsonFile(IndelCallingInstance instance) {
        return getDirectoryPath(instance).resolve("indel.json")
    }

    Path getSampleSwapJsonFile(IndelCallingInstance instance) {
        return getDirectoryPath(instance).resolve("checkSampleSwap.json")
    }

}
