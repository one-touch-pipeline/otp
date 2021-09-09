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

import grails.validation.Validateable
import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysis
import de.dkfz.tbi.otp.ngsdata.SeqTrack

import java.nio.file.Path

@TupleConstructor
class DataExportInput implements Validateable {

    static constraints = {
        targetFolder nullable: false, validator: { Path path ->
            if (!path.absolute) {
                return "targetFolder is not an absolute path"
            }
        }
        unixGroup nullable: false, blank: false
    }

    //************ Path to copy files. Underneath, 'PID folders' will be created. (absolute path) ************//
    Path targetFolder

    //************ Select whether analyses should be copied (true/false) ************//
    Map<PipelineType, Boolean> copyAnalyses

    //************ Check if and which files exist (true/false) ************//
    boolean checkFileStatus = true

    //************ Generate a script for file list (true/false) [checkFileStatus must be false] ************//
    boolean getFileList = false

    //************ Select new unix group ************//
    String unixGroup

    //************ Select the permissions of the files. If true group can read/execute. If false group/others can read/execute ************//
    boolean external = true

    //************ adds COPY_TARGET_BASE and COPY_CONNECTION environment variables to mkdir and rsync ************//
    boolean copyExternal = false

    //************ Data to be exported ************//
    List<SeqTrack> seqTrackList
    List<AbstractMergedBamFile> bamFileList
    Map<PipelineType, List<BamFilePairAnalysis>> analysisListMap = [:]
}
