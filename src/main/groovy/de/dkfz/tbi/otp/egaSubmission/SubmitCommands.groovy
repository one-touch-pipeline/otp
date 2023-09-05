/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.egaSubmission

import grails.databinding.BindUsing
import grails.validation.Validateable
import groovy.transform.CompileDynamic
import org.springframework.web.multipart.MultipartFile

@CompileDynamic
trait SubmitCommands {
}

@CompileDynamic
class NewSubmissionControllerSubmitCommand implements Validateable {
    String egaBox
    String submissionName
    String studyName
    EgaSubmission.StudyType studyType
    String studyAbstract
    String pubMedId
    String submit

    static constraints = {
        egaBox blank: false
        submissionName blank: false
        studyName blank: false
        studyAbstract blank: false
    }
}

@CompileDynamic
class UpdateSubmissionStateSubmitCommand implements Validateable {
    EgaSubmission submission
    EgaSubmission.State state

    static constraints = {
        state(validator: { val, obj ->
            if (val != EgaSubmission.State.SELECTION) {
                if (!obj.submission.samplesToSubmit) {
                    return 'no.samples'
                }
                if (!obj.submission.bamFilesToSubmit && !obj.submission.rawSequenceFilesToSubmit) {
                    return 'no.files'
                }
            }
        })
    }

    void setValue(String value) {
        this.state = value as EgaSubmission.State
    }
}

@CompileDynamic
class UpdatePubMedIdSubmitCommand implements Validateable {
    EgaSubmission submission
    String pubMedId

    void setValue(String value) {
        this.pubMedId = value
    }
}

@CompileDynamic
class SelectSamplesControllerSubmitCommand implements Validateable {
    EgaSubmission submission
    String csv
    String next
    List<String> sampleAndSeqType
    List<String> samplesWithMissingFile

    static constraints = {
        csv nullable: true
        samplesWithMissingFile validator: { samplesWithMissingFile, obj, errors ->
            List<List<String>> tokenizedSamplesWithMissingFile = samplesWithMissingFile*.tokenize(',')

            Set<String> selectedSampleIds = obj.sampleAndSeqType.toSet()

            List<String> intersection = tokenizedSamplesWithMissingFile
                    .findAll { selectedSampleIds.contains(it[0]) }
                    .collect { it[1..-1].join(",") }

            if (intersection) {
                intersection.each {
                    errors.rejectValue(
                            "samplesWithMissingFile", "default.message.error.placeholder", [it].toArray(), "missing file")
                }
            }
        }
    }
}

@CompileDynamic
class UploadFormSubmitCommand extends MultiPartFileCommand implements Validateable {
    EgaSubmission submission
    String upload

    static constraints = {
        upload matches: "Upload.*"
    }
}

@CompileDynamic
class SampleInformationFormsSubmitCommand implements Validateable {
    EgaSubmission submission
    String csv
    String next
    String back
    List<String> sampleObjectId = []
    List<EgaSubmissionService.FileType> fileType = []
    List<String> egaSampleAlias = []

    static constraints = {
        next nullable: true
        csv nullable: true
        back nullable: true
    }
}

@CompileDynamic
class SelectFilesRawSequenceFilesFormSubmitCommand implements Validateable {
    EgaSubmission submission
    String saveSelection
    String download
    String saveAliases
    List<Boolean> selectBox = []
    List<String> fastqFile = []
    List<String> egaSample = []
    List<String> egaFileAlias = []

    static constraints = {
        download nullable: true
        saveSelection nullable: true
        saveAliases nullable: true
    }
}

@CompileDynamic
class SelectFilesBamFilesFormSubmitCommand implements Validateable {
    EgaSubmission submission
    String saveSelection
    String saveAliases
    String download
    List<String> fileId
    List<String> egaFileAlias
    List<String> egaSampleAlias

    static constraints = {
        download nullable: true
        saveAliases nullable: true
        saveSelection nullable: true
        egaFileAlias nullable: true
    }
}

@CompileDynamic
class GenerateFilesToUploadFileSubmitCommand implements Validateable {
    EgaSubmission submission
    String save
}

@CompileDynamic
class SampleMetadataFormSubmitCommand implements Validateable {
    EgaSubmission submission
    String download
}

@CompileDynamic
class SelectSampleDownloadCommand implements Validateable {
    @BindUsing({ obj, source ->
        source['selectedSamples'].collect {
            new EgaMapKey(it["individualName"] as String,
                    it["individualUuid"] as String,
                    it["seqTypeName"] as String,
                    it["sequencingReadType"] as String,
                    it["singleCell"] as String,
                    it["sampleTypeName"] as String,)
        }
    })
    List<EgaMapKey> selectedSamples
}

@CompileDynamic
class MultiPartFileCommand {
    MultipartFile file
}

@CompileDynamic
class UploadCsvFileCommand extends MultiPartFileCommand implements Validateable {

    static constraints = {
        file validator: { val, obj ->
            if (val == null) {
                return false
            }
            if (val.empty) {
                return false
            }
            val.originalFilename?.toLowerCase()?.endsWith('csv')
        }
    }
}
