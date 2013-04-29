package de.dkfz.tbi.otp.job.processing

import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import org.codehaus.groovy.grails.web.json.JSONObject
import org.springframework.util.Assert
import grails.converters.JSON
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.ngsdata.Realm

/**
 * A service to merge realm and job-cluster specific PBS options. The PBS option needs to be provided as a JSON string
 * and will be returned as PBS option string. The JSON sting has the following structure:
 * It is a map with {@link String} as key and depending of the key is the value also a String
 * or itself a Map with Strings as key and value.
 * Some examples:
 * <ul>
 * <li>"{'-l': {nodes: '1:lsdf', walltime: '48:00:00'}}"</li>
 * <li>"{'-W': {x: 'NACCESSPOLICY:SINGLEJOB'}}"</li>
 * <li>"{'-q': convey, '-A': RUNFAST, '-m': a, '-S': '/bin/bash'}"</li>
 * <li>"{'-l': {nodes: '1:ppn=6:lsdf', walltime: '48:00:00', mem: '3g', file: '50g'}, '-q': convey, '-A': RUNFAST, '-m': a, '-S': '/bin/bash'}"</li>
 * </ul>
 *
 *
 */
class PbsOptionMergingService {

    private static final String PBS_PREFIX = "PBS_"

    ProcessingOptionService processingOptionService

    /**
     * Create the merged PBS option String based on the defaults of the realm and job-cluster depending settings.
     * If no job key is given, only the default PBS option from the realm is used.
     * Otherwise the job depending option from {@link ProcessingOption} is merged into the default settings.
     * The {@link ProcessingOption} is got by name created by the prefix {@link #PBS_PREFIX} and the jobIdentifier
     * and by a soft link to the cluster about the cluster name {@link Realm#getCluster()}.
     * The PBS option can not be {@link Project} specific, because the project is not known here.
     * The PBS option have to be provided as a JSON String.
     *
     * @param realm the realm the default PBS option should be take from
     * @param jobIdentifier the job name, if the job needs specific PBS options for execution,
     *          or null, if the default PBS option of the realm is sufficient for the job.
     * @return the created PBS option String.
     * @throws NullPointerException if a job identifier is provided, but no PBS option is defined for this
     *          job identifier and the cluster ({@link Realm#cluster}) of the {@link Realm}
     */
    public String mergePbsOptions(Realm realm, String jobIdentifier = null) {
        Map mapRealm = jsonStringToMap(realm.pbsOptions)
        if (jobIdentifier != null) {
            String key = PBS_PREFIX + jobIdentifier
            String cluster = realm.cluster
            ProcessingOption processingOption = processingOptionService.findOptionObject(key, cluster, null)
            Assert.notNull(processingOption, "No processing option is defined for job ${key} and cluster ${cluster}")
            Map mapJob = jsonStringToMap(processingOption.value)
            this.mergeHelper(mapRealm, mapJob)
        }
        String ret = mapToPbsOptions(mapRealm)
        return ret
    }

    /**
     * Parse the given String as JSON string and convert it to map.
     *
     * @param jsonString the string containing as JSON structure
     * @return the created map from the string
     */
    private Map jsonStringToMap(String jsonString) {
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
    private String mapToPbsOptions(Map map) {
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
