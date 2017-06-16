package de.dkfz.tbi.otp.job.processing

import com.google.common.base.CaseFormat
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.ngsdata.*
import grails.converters.*
import org.codehaus.groovy.grails.web.converters.exceptions.*
import org.codehaus.groovy.grails.web.json.*
import org.springframework.util.*

/**
 * A service to merge realm and job-cluster specific PBS options. The PBS option needs to be provided as a JSON string
 * and will be returned as PBS option string. The JSON sting has the following structure:
 * It is a map with {@link String} as key and depending of the key is the value also a String
 * or itself a Map with Strings as key and value.
 * Some examples:
 * <ul>
 * <li>"{'-l': {nodes: '1', walltime: '48:00:00'}}"</li>
 * <li>"{'-W': {x: 'NACCESSPOLICY:SINGLEJOB'}}"</li>
 * <li>"{'-q': convey, '-A': RUNFAST, '-m': a, '-S': '/bin/bash'}"</li>
 * <li>"{'-l': {nodes: '1:ppn=6', walltime: '48:00:00', mem: '3g', file: '50g'}, '-q': convey, '-A': RUNFAST, '-m': a, '-S': '/bin/bash'}"</li>
 * </ul>
 *
 *
 */
class PbsOptionMergingService {

    static final String PBS_PREFIX = "CLUSTER_SUBMISSIONS_OPTION"

    ProcessingOptionService processingOptionService

    public String mergePbsOptions(ProcessingStep processingStep, Realm realm, String... additionalJsonOptions) {
        String cluster = realm.cluster
        ProcessParameterObject parameterObject = processingStep.processParameterObject
        assert parameterObject
        Project project = parameterObject.project
        SeqType seqType = parameterObject.seqType
        OptionName optionName = OptionName."${PBS_PREFIX}_${CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, processingStep.nonQualifiedJobClass)}"

        // default options for the realm
        Map options = jsonStringToMap(realm.defaultJobSubmissionOptions)

        // options for the job class
        options = mergeMapWithJsonString(options, processingOptionService.findOption(optionName, cluster, project))
        if (seqType) {
            assert seqType.processingOptionName
            // options for the job class and SeqType
            options = mergeMapWithJsonString(options, processingOptionService.findOption(
                    optionName, seqType.alias, project))
        }

        // additional options
        additionalJsonOptions.each {
            options = mergeMapWithJsonString(options, it)
        }

        return mapToPbsOptions(options)
    }

    /**
     * Parse the given String as JSON string and convert it to map.
     *
     * @param jsonString the string containing as JSON structure
     * @return the created map from the string
     */
    public static Map jsonStringToMap(String jsonString) {
        Assert.notNull(jsonString, "The string is a null pointer")
        if (jsonString.isEmpty()) {
            throw new IllegalArgumentException("The string is empty")
        }
        try {
            JSONObject jsonObject = JSON.parse(jsonString)
            return jsonObject as Map
        } catch (ConverterException e) {
            throw new IllegalArgumentException("The string is no valid JSON string", e)
        }
    }

    /**
     * Convert the given map to an PBS option string
     *
     * @param map the map to convert
     * @return the string of the PBS options
     */
    public String mapToPbsOptions(Map map) {
        Assert.notNull(map, "The map is a null pointer")
        if (!(map instanceof Map)) {
            throw new RuntimeException("The parameter 'map' is not of type Map")
        }
        StringBuilder sb = new StringBuilder(1000)
        map.each { String outerKey, outerValue ->
            if (outerValue instanceof Map) {
                outerValue.each { String innerKey, innerValue ->
                    sb.append(outerKey).append(" ")
                    sb.append(innerKey).append("=")
                    sb.append(innerValue).append(" ")
                }
            } else {
                sb.append(outerKey).append(" ")
                sb.append(outerValue).append(" ")
            }
        }
        return sb.toString()
    }

    private Map mergeMapWithJsonString(Map<String, ?> baseMap, String jsonOptionsToMerge) {
        if (jsonOptionsToMerge) {
            return mergeHelper(baseMap, jsonStringToMap(jsonOptionsToMerge))
        } else {
            return baseMap
        }
    }

    /**
     * A helper to merge two Map hierarchy together. The method work on the given maps and change the state.
     *
     * @param baseMap the map the other should be merged into
     * @param mapToMerge the map to merge into the other
     * @return a reference to baseMap
     */
    private Map mergeHelper(Map<String, ?> baseMap, Map<String, ?> mapToMerge) {
        Assert.notNull(baseMap, "The base map may not be null")
        Assert.notNull(mapToMerge, "The map to merge may not be null")
        mapToMerge.each { String outerKey, mergedValue ->
            if (!baseMap.containsKey(outerKey)) {
                //key to merge is not present yet, so add value
                baseMap.put(outerKey, mergedValue)
            } else if (!(mergedValue instanceof Map)) {
                //key to merge is present, but has a simple value --> override
                baseMap.put(outerKey, mergedValue)
            } else {
                Map baseValue = baseMap.get(outerKey)
                if (!(baseValue instanceof Map)) {
                    throw new RuntimeException("Can not merge the map '${mergedValue}' into the single value '${baseValue}' for key: ${outerKey}")
                }
                //key to merge is present and has sub keys --> merge sub keys
                mergeHelper(baseMap.get(outerKey), mergedValue)
            }
        }
        return baseMap
    }
}
