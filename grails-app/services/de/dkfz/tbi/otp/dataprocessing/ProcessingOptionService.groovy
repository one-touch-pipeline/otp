package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.Project

class ProcessingOptionService {

    public ProcessingOption createOrUpdate(String name, String type, Project project, String value, String comment) {
        ProcessingOption option = findStrict(name, type, project)
        if (option) {
            obsoleteOption(option)
        }
        option = new ProcessingOption(
            name: name,
            type: type,
            project: project,
            value: value,
            comment: comment
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
    public ProcessingOption findOptionObject(String name, String type, Project project) {
        ProcessingOption option = findStrict(name, type, project)
        if (option) {
            return option
        }
        option = findStrict(name, type, null)
        if (option) {
            return option
        }
        option = findStrict(name, null, project)
        if (option) {
            return option
        }
        option = findStrict(name, null, null)
        return option
    }

    private ProcessingOption findStrict(String name, String type, Project project) {
        return ProcessingOption.findWhere(
            name: name,
            type: type,
            project: project,
            dateObsoleted: null
        )
    }

    public String findOption(String name, String type, Project project) {
        ProcessingOption option = findOptionObject(name, type, project)
        return (option) ? option.value : null
    }

    public String findOptionSafe(String name, String type, Project project) {
        ProcessingOption option = findOptionObject(name, type, project)
        return (option) ? option.value : ""
    }

    /**
     * Return numerical value of the option or default value if option value
     * can not be cast to a number.
     */
    public long findOptionAsNumber(String name, String type, Project project, long defaultValue) {
        String value = findOptionSafe(name, type, project)
        if (value.isLong()) {
            return value.toLong()
        }
        return defaultValue
    }
}
