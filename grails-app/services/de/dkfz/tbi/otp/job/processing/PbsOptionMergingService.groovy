package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.ngsdata.*
import grails.converters.*
import org.codehaus.groovy.grails.web.converters.exceptions.*
import org.codehaus.groovy.grails.web.json.*


class PbsOptionMergingService {

    ProcessingOptionService processingOptionService

    /**
     * Method to read options for job submission to the cluster job scheduler from the database.
     * Options are read in the following order: from Realm, from job-specific processing option,
     * from job- and seqType-specific processing option; if an options is specified more than once,
     * the last value is used.
     *
     * Options are Map<JobSubmissionOption, String> serialized as JSON
     */
    Map<JobSubmissionOption, String> readOptionsFromDatabase(ProcessingStep processingStep, Realm realm) {
        ProcessParameterObject parameterObject = processingStep.processParameterObject
        assert parameterObject
        Project project = parameterObject.project
        SeqType seqType = parameterObject.seqType
        String jobClass = processingStep.nonQualifiedJobClass

        // default options for the realm
        Map<JobSubmissionOption, String> options = convertJsonStringToMap(realm.defaultJobSubmissionOptions)

        // options for the job class
        options.putAll(convertJsonStringToMap(
                processingOptionService.findOption(OptionName.CLUSTER_SUBMISSIONS_OPTION, "${jobClass}", project))
        )

        if (seqType) {
            assert seqType.processingOptionName
            // options for the job class and SeqType
            options.putAll(convertJsonStringToMap(
                    processingOptionService.findOption(OptionName.CLUSTER_SUBMISSIONS_OPTION, "${jobClass}_${seqType.processingOptionName}", project))
            )
        }

        return options
    }

    static Map<JobSubmissionOption, String> convertJsonStringToMap(String jsonString) {
        if (!jsonString) {
            return [:]
        }
        JSONElement jsonElement
        try {
            jsonElement = JSON.parse(jsonString)
        } catch (ConverterException e) {
            throw new IllegalArgumentException("The string is no valid JSON string", e)
        }
        if (!(jsonElement instanceof JSONObject)) {
            throw new IllegalArgumentException("The JSON string doesn't contain a map")
        }
        if (jsonElement.isEmpty()) {
            throw new IllegalArgumentException("The JSON map is empty")
        }
        jsonElement.each { k, v ->
            if (!(v instanceof String)) {
                throw new IllegalArgumentException("Values of map are not strings")
            }
        }
        return jsonElement.collectEntries {
            k, v -> [(JobSubmissionOption.valueOf(k as String)): v]
        }
    }

    static boolean validateJsonString(String s) {
        try {
            convertJsonStringToMap(s)
            return true
        } catch (Exception e) {
            return false
        }
    }
}
