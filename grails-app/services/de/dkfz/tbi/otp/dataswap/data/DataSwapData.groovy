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
package de.dkfz.tbi.otp.dataswap.data

import grails.validation.Validateable
import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.dataswap.Swap
import de.dkfz.tbi.otp.dataswap.parameters.DataSwapParameters
import de.dkfz.tbi.otp.project.Project

import java.nio.file.Path

@TupleConstructor
class DataSwapData<P extends DataSwapParameters> implements Validateable {

    static Closure constraints = {
        parameters nullable: false
        individualSwap validator: { individualSwap, obj ->
            if (individualSwap.old.project != obj.projectSwap.old) {
                return "old individual ${individualSwap.old.pid} should be in project" +
                        " ${obj.projectSwap.old.name}, but was in ${individualSwap.old.project}"
            }
            if (individualSwap.new.project != obj.projectSwap.new) {
                return "new individual ${individualSwap.new.pid} should be in project" +
                        "  ${obj.projectSwap.new.name}, but was in ${individualSwap.new.project}"
            }
        }
        seqTrackList nullable: false
        dataFiles nullable: false, minSize: 1, validator: { dataFiles, obj ->
            List<String> dataFilesGiven = obj.dataFileSwaps*.old.sort()
            List<String> dataFilesFound = dataFiles*.fileName.sort()
            List<String> difference = dataFilesGiven - dataFilesFound
            if (dataFilesGiven != dataFilesFound) {
                return "DataFiles: ${difference} not found, but found: ${dataFilesFound}"
            }
        }
    }

    P parameters

    Swap<Project> projectSwap
    Swap<Individual> individualSwap
    List<SeqTrack> seqTrackList
    List<DataFile> fastqDataFiles
    List<DataFile> dataFiles
    Map<DataFile, Map<String, String>> oldDataFileNameMap
    List<String> oldFastQcFileNames
    List<File> dirsToDelete = []

    Swap<String> getProjectNameSwap() {
        return parameters.projectNameSwap
    }

    Swap<String> getPidSwap() {
        return parameters.pidSwap
    }

    List<Swap<String>> getDataFileSwaps() {
        return parameters.dataFileSwaps
    }

    String getBashScriptName() {
        return parameters.bashScriptName
    }

    StringBuilder getLog() {
        return parameters.log
    }

    void setLog(StringBuilder log) { parameters.log = log }

    boolean getFailOnMissingFiles() {
        return parameters.failOnMissingFiles
    }

    Path getScriptOutputDirectory() {
        return parameters.scriptOutputDirectory
    }

    boolean getLinkedFilesVerified() {
        return parameters.linkedFilesVerified
    }
}
