package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.security.access.prepost.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class ProcessingOptionService {

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    ProcessingOption createOrUpdate(OptionName name, String value, String type = null, Project project = null) {
        ProcessingOption option = findOption(name, type, project)
        if (option) {
            if (option.value == value) {
                return option
            }
            obsoleteOption(option)
        }
        option = new ProcessingOption(
            name: name,
            type: type,
            project: project,
            value: value,
        )
        assert(option.save(flush: true))
        return option
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void obsoleteOptionByName(OptionName name, String type = null, Project project = null) {
        ProcessingOption option = findOption(name, type, project)
        if (option) {
            if (option.name.necessity == Necessity.REQUIRED &&
                    !option.name.isDeprecated()
            ) {
                throw new ProcessingException("Required options can't be obsoleted")
            }
            obsoleteOption(option)
        }
    }

    private static ProcessingOption findOption(OptionName name, String type = null, Project project = null) {
        return singleElement(ProcessingOption.findAllWhere(
                name: name,
                type: type,
                project: project,
                dateObsoleted: null
        ), true)
    }

    private void obsoleteOption(ProcessingOption option) {
        option.dateObsoleted = new Date()
        assert(option.save(flush: true))
    }

    String findOptionAsString(OptionName name, String type = null) {
        ProcessingOption option = findOption(name, type)
        return option?.value != null ? option?.value : name.defaultValue
    }
    int findOptionAsInteger(OptionName name, String type = null) {
        Integer.parseInt(findOptionAsString(name, type))
    }
    double findOptionAsDouble(OptionName name, String type = null) {
        Double.parseDouble(findOptionAsString(name, type))
    }
    boolean findOptionAsBoolean(OptionName name, String type = null) {
        findOptionAsString(name, type) == "true"
    }
    List<String> findOptionAsList(OptionName name, String type = null) {
        findOptionAsString(name, type)?.split(',')*.trim()
    }


    @Deprecated
    static String findOptionSafe(OptionName name, String type, Project project) {
        ProcessingOption option = findOption(name, type, project)
        return option?.value != null ? option?.value : name.defaultValue
    }

    /**
     * Return numerical value of the option or default value if option value
     * can not be cast to a number.
     */
    @Deprecated
    static long findOptionAsNumber(OptionName name, String type, Project project) {
        String value = findOptionSafe(name, type, project)
        return value.toLong()
    }

    /**
     * Retrieves all ProcessingOptions.
     * @return List of ProcessingOptions
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    List<ProcessingOption> listProcessingOptions() {
        ProcessingOption.findAllByDateObsoletedIsNull().findAll {
            !it.name.isDeprecated()
        }
    }
}
