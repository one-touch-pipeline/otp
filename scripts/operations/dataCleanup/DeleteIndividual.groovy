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
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.DeletionService

import java.nio.file.Path

/**
 * Script to delete individuals with all data.
 *
 * It deletes all data of the individual in OTP database and generates a bash script to delete it on the files system.
 * The file name is constructed by all names, but truncated to 100 chars.
 *
 * The individuals are given by there pid.
 *
 * Please note, that the script needs some time per pid, depending of the amount of data. This can cause proxy timeout. But a proxy timeout does not stop the
 * request in OTP, it will continue to run until it succeeds or an error is thrown.
 */


// input area
//----------------------

/**
 * input area for pids, one pid per line.
 * All values are trimmed, empty lines and lines starting with '#' are ignored.
 */
String pids = """
#pid
#pid2


"""


/**
 * Flag to indicate, if it should be checked for files only linked and for external bam files.
 * If checked and some found, an exception is thrown
 */
boolean check = true


//script area
//-----------------------------

List<Individual> individuals = pids.split('\n')*.trim().findAll { String line ->
    line && !line.startsWith('#')
}.collect {
    CollectionUtils.exactlyOneElement(Individual.findAllByPid(it), "Could not find pid '${it}'")
}.unique()

String combinedPids = individuals*.pid.join('__')
String fileName = "Delete_${combinedPids.size() < 110 ? combinedPids : combinedPids.substring(0, 100) + '_and_others'}.sh"

DeletionService deletionService = ctx.deletionService
FileService fileService = ctx.fileService

Path baseOutputDir = ConfigService.getInstance().getScriptOutputPath().toPath().resolve('sample_swap')

Individual.withTransaction {
    List<String> allFilesToRemove = [
            "#!/bin/bash",
            "",
            "set -evx",
    ]

    individuals.each {
        println "Delete: ${it} of project ${it.project}"
        allFilesToRemove << "\n\n#${it}"
        allFilesToRemove << deletionService.deleteIndividual(it, check)[0]
    }

    Path deleteFileCmd = fileService.createOrOverwriteScriptOutputFile(baseOutputDir, fileName)

    deleteFileCmd << allFilesToRemove.join('\n')

    println ''
    println "Deletion file is written to:"
    println deleteFileCmd

    assert false: "DEBUG: transaction intentionally failed to rollback changes"
}
