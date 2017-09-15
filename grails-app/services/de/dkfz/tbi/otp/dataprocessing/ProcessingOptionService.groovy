package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.springframework.security.access.prepost.*

import static org.springframework.util.Assert.*

class ProcessingOptionService {

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public ProcessingOption createOrUpdate(OptionName name, String type, Project project, String value) {
        ProcessingOption option = findStrict(name, type, project)
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

    private void obsoleteOption(ProcessingOption option) {
        option.dateObsoleted = new Date()
        assert(option.save(flush: true))
    }

    /**
     * This method returns an option object depending on name, type and project.
     * If the option is not found, a more general option is searched for:
     * first an option valid for all project, then option for
     * each type and finally an option with the name only.
     *
     * @param name name of the option
     * @param type type of the object, eg. "PHRED" or "ILLUMINA" for encoding option
     * @param project project to which processing object belongs
     * @return ProcessingOption object
     */
    public static ProcessingOption findOptionObject(OptionName name, String type, Project project) {
        ProcessingOption option = findStrict(name, type, project)
        if (option) {
            return option
        }
        if (project != null) {
            option = findStrict(name, type, null)
            if (option) {
                return option
            }
        }
        if (type != null) {
            option = findStrict(name, null, project)
            if (option) {
                return option
            }
        }
        if (project != null && type != null) {
            option = findStrict(name, null, null)
        }
        return option
    }

    private static ProcessingOption findStrict(OptionName name, String type, Project project) {
        return ProcessingOption.findWhere(
            name: name,
            type: type,
            project: project,
            dateObsoleted: null
        )
    }

    public static String findOption(OptionName name, String type, Project project) {
        ProcessingOption option = findOptionObject(name, type, project)
        return (option) ? option.value : null
    }

    public static String findOptionSafe(OptionName name, String type, Project project) {
        ProcessingOption option = findOptionObject(name, type, project)
        return (option) ? option.value : ""
    }

    /**
     * @throws ProcessingException if no option has been found
     */
    public static String findOptionAssure(OptionName name, String type, Project project) {
        notNull(name, "option name can not be null")
        ProcessingOption option = findOptionObject(name, type, project)
        if (!option) {
            throw new ProcessingException("no option has been found with name ${name} and type ${type} and project ${project}")
        }
        return option.value
    }

    /**
     * Return numerical value of the option or default value if option value
     * can not be cast to a number.
     */
    public static long findOptionAsNumber(OptionName name, String type, Project project, long defaultValue) {
        String value = findOptionSafe(name, type, project)
        if (value.isLong()) {
            return value.toLong()
        }
        return defaultValue
    }

    /**
     * Returns the ProcessingOption which belongs to the input parameter name.
     * If there is more than one or none ProcessingOption found by the query an Error is thrown.
     */
    static String getValueOfProcessingOption(OptionName name) {
        return CollectionUtils.exactlyOneElement(ProcessingOption.findAllByNameAndDateObsoleted(name, null)).value
    }

    /**
     * Retrieves all ProcessingOptions.
     * @return List of ProcessingOptions
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public List<ProcessingOption> listProcessingOptions() {
        return ProcessingOption.findAllByDateObsoletedIsNull()
    }
}
