/*
 * Copyright 2011-2020 The OTP authors
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

package rollout.docker

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService

/**
 * Calls each Script from ../ReferenceGenome with few exceptions to
 * create ReferenceGenomes for Docker setup.
 */

GroovyShell shell = new GroovyShell()
ProcessingOptionService processingOptionService = ctx.processingOptionService
String basePath = processingOptionService.findOptionAsString(ProcessingOption.OptionName.BASE_PATH_REFERENCE_GENOME)

def folder = new File('scripts/ReferenceGenome')

folder.eachFileRecurse(groovy.io.FileType.FILES, { file ->

    def constraints =
            (
                    file.name.endsWith(".groovy")
                            && file.name.startsWith("LoadReferenceGenome")
            ) && (
                    !file.name.contains("hs37d5_Bovine_Phix")
                            && !file.name.contains("GRCh38_decoy_ebv_phiX_alt_hla_chr")
            )

    if (constraints) {
        def script = shell.parse(file)

        File refGenomePath = new File(basePath + "/" + script.getProperty("path"))
        refGenomePath.mkdirs()
        refGenomePath.setReadable(true, false);
        refGenomePath.setExecutable(true, false);
        refGenomePath.setWritable(true, false);

        println("run: ${script.getProperty("path")}")
        run(file)

        File statsPath = new File(refGenomePath.getAbsolutePath() + "/stats")
        statsPath.mkdirs()
        (script.getProperty("statSizeFileNames") as List).each {
            File statsFile = new File(statsPath.getAbsolutePath() + it)
            statsFile.createNewFile()
            statsFile.text = "Text content"
        }
    }
})
println("created")
