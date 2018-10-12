package de.dkfz.tbi.otp.egaSubmission

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.util.spreadsheet.Row
import de.dkfz.tbi.util.spreadsheet.Spreadsheet
import org.springframework.security.access.prepost.PreAuthorize

class EgaSubmissionService {

    static final String INDIVIDUAL = "Individual"
    static final String SAMPLE_TYPE = "Sample Type"
    static final String SEQ_TYPE = "Sequence Type"
    static final String EGA_SAMPLE_ALIAS = "EGA Sample Alias"
    static final String FILE_TYPE = "File Type"

    enum FileType {
        BAM,
        FASTQ,
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(params.project, 'OTP_READ_ACCESS')")
    Submission createSubmission(Map params) {
        Submission submission = new Submission( params + [
                state: Submission.State.SELECTION,
        ])
        assert submission.save(flush: true, failOnError: true)

        return submission
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void updateSubmissionState (Submission submission, Submission.State state) {
        submission.state = state
        submission.save(flush: true)
    }

    List<SeqType> seqTypeByProject(Project project) {
        List<Long> seqTypeIds = AggregateSequences.withCriteria {
            eq("projectId", project?.id)
            projections {
                groupProperty("seqTypeId")
            }
        }
        List<SeqType> seqTypes = []
        if (seqTypeIds) {
            seqTypes = SeqType.withCriteria {
                'in'("id", seqTypeIds)
                order("name")
                order("libraryLayout")
            }
        }
        return seqTypes
    }

    void saveSampleSubmissionObject(Submission submission, Sample sample, SeqType seqType) {
        SampleSubmissionObject sampleSubmissionObject = new SampleSubmissionObject(
                sample: sample,
                seqType: seqType
        ).save(flush: true)
        submission.addToSamplesToSubmit(sampleSubmissionObject)
    }

    Map<SampleSubmissionObject, Boolean> checkFastqFiles(Submission submission) {
        Map<SampleSubmissionObject, Boolean> map = [:]

        submission.samplesToSubmit.each {
            List<DataFile> dataFiles = SeqTrack.findBySampleAndSeqType(it.sample, it.seqType).dataFiles.findAll {
                !it.fileWithdrawn
            }
            if (dataFiles.empty) {
                map.put(it, false)
            } else {
                map.put(it, dataFiles.first().fileExists)
            }
        }

        return map
    }

    Map<SampleSubmissionObject, Boolean> checkBamFiles(Submission submission) {
        Map<SampleSubmissionObject, Boolean> map = [:]

        submission.samplesToSubmit.each { sampleSubmissionObject ->
            List<AbstractMergedBamFile> abstractMergedBamFiles = AbstractMergedBamFile.createCriteria().list {
                workPackage {
                    eq('sample', sampleSubmissionObject.sample)
                    eq('seqType', sampleSubmissionObject.seqType)
                }
                eq('withdrawn', false)
                eq('fileOperationStatus', AbstractMergedBamFile.FileOperationStatus.PROCESSED)
            }.findAll {
                it.isMostRecentBamFile()
            }

            map.put(sampleSubmissionObject, !abstractMergedBamFiles.empty)
        }

        return map
    }

    Map<String, String> readEgaSampleAliasesFromFile(Spreadsheet spreadsheet) {
        Map<String, String> egaSampleAliases = [:]

        spreadsheet.dataRows.each {
            egaSampleAliases.put(getIdentifierKey(it), it.getCellByColumnTitle(EGA_SAMPLE_ALIAS).text)
        }

        return egaSampleAliases
    }

    Map<String, Boolean> readBoxesFromFile(Spreadsheet spreadsheet, FileType fileType) {
        Map<String, Boolean> map = [:]

        spreadsheet.dataRows.each {
            map.put(getIdentifierKey(it), it.getCellByColumnTitle(FILE_TYPE).text.toUpperCase() as FileType == fileType)
        }

        return map
    }

    Map validateRows(Spreadsheet spreadsheet, Submission submission) {
        if (spreadsheet.dataRows.size() == submission.samplesToSubmit.size()) {
            List samplesFromDB = submission.samplesToSubmit.collect {
                getIdentifierKeyFromSampleSubmissionObject(it)
            }.sort()

            List samplesFromFile = spreadsheet.dataRows.collect {
                getIdentifierKey(it)
            }.sort()

            return [
                "valid": samplesFromDB == samplesFromFile,
                "error": "Found and expected samples are different",
            ]
        }

        return [
            "valid": false,
            "error": "There are ${spreadsheet.dataRows.size() > submission.samplesToSubmit.size() ? "more" : "less"} " +
                     "rows in the file as samples where selected",
        ]
    }

    private static String getIdentifierKey(Row row) {
        return row.getCellByColumnTitle(INDIVIDUAL).text +
               row.getCellByColumnTitle(SAMPLE_TYPE).text +
               row.getCellByColumnTitle(SEQ_TYPE).text
    }

    String getIdentifierKeyFromSampleSubmissionObject(SampleSubmissionObject sampleSubmissionObject) {
        return sampleSubmissionObject.sample.individual.displayName +
               sampleSubmissionObject.sample.sampleType.displayName +
               sampleSubmissionObject.seqType.displayName
    }

    String generateCsvFile(List<String> sampleObjectId, List<String> alias, List<FileType> fileType) {
        StringBuilder contentBody = new StringBuilder()

        sampleObjectId.eachWithIndex { it, i ->
            SampleSubmissionObject sampleSubmissionObject = SampleSubmissionObject.findById(it as Long)

            contentBody.append("${sampleSubmissionObject.sample.individual.displayName},")
            contentBody.append("${sampleSubmissionObject.sample.sampleType.displayName},")
            contentBody.append("${sampleSubmissionObject.seqType.displayName},")
            contentBody.append("${alias?.getAt(i) ?: ""},")
            contentBody.append("${fileType?.getAt(i) ?: FileType.FASTQ}\n")
        }

        String contentHeader = [
                INDIVIDUAL,
                SAMPLE_TYPE,
                SEQ_TYPE,
                EGA_SAMPLE_ALIAS,
                FILE_TYPE,
        ].join(',')

        return "${contentHeader}\n${contentBody}"
    }

    Map validateSampleInformationFormInput(List<String> sampleObjectId, List<String> alias, List<FileType> fileType) {
        List<String> errors = []
        boolean hasErrors = false
        Map<String, String> sampleAliases =  [:]
        Map<String, Boolean> fastqs = [:]
        Map<String, Boolean> bams = [:]

        if (fileType.findAll().size() != sampleObjectId.size()) {
            hasErrors = true
            errors += "For some samples files types are not selected."
        }
        if (alias.unique(false).size() != sampleObjectId.size()) {
            hasErrors = true
            errors += "Not all aliases are unique."
        }
        alias.sort(false).each {
            if (it == "" ) {
                hasErrors = true
                errors += "For some samples no alias is configured."
            }
            if (SampleSubmissionObject.findByEgaAliasName(it)) {
                hasErrors = true
                errors += "Alias ${it} already exist.".toString()
            }
        }

        alias.eachWithIndex { it, i ->
            sampleAliases.put(getIdentifierKeyFromSampleSubmissionObject(SampleSubmissionObject.findById(sampleObjectId[i] as Long)), it)
        }
        sampleObjectId.eachWithIndex { it, i ->
            fastqs.put(getIdentifierKeyFromSampleSubmissionObject(SampleSubmissionObject.findById(it as Long)), fileType[i] == FileType.FASTQ)
            bams.put(getIdentifierKeyFromSampleSubmissionObject(SampleSubmissionObject.findById(it as Long)), fileType[i] == FileType.BAM)
        }

        return [
                hasErrors: hasErrors,
                errors: errors.unique(),
                fastqs: fastqs,
                bams: bams,
                sampleAliases: sampleAliases,
        ]
    }

    boolean validateFileTypeFromInput(Spreadsheet spreadsheet) {
        List<String> fileTypes = spreadsheet.dataRows.collect {
            it.getCellByColumnTitle(FILE_TYPE).text.toUpperCase()
        }

        if (!(fileTypes.unique().size() > 2)) {
            return FileType.collect { it.toString() }.containsAll(fileTypes.unique())
        }
        return false
    }

    void updateSampleSubmissionObjects(List<String> sampleObjectId, List<String> alias, List<FileType> fileType) {
        if (sampleObjectId.size() == alias.size() && sampleObjectId.size() == fileType.size()) {
            sampleObjectId.eachWithIndex { it, i ->
                SampleSubmissionObject sampleSubmissionObject = SampleSubmissionObject.findById(it as Long)
                sampleSubmissionObject.egaAliasName = alias[i]
                sampleSubmissionObject.useBamFile = fileType[i] == FileType.BAM
                sampleSubmissionObject.useFastqFile = fileType[i] == FileType.FASTQ
                sampleSubmissionObject.save(flush: true)
            }
        }
    }
}
