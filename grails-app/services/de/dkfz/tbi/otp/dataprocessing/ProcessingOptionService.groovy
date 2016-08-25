package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.job.processing.ProcessingException;
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import grails.plugin.springsecurity.SpringSecurityUtils
import org.springframework.security.acls.domain.BasePermission
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.access.prepost.PreAuthorize
import static org.springframework.util.Assert.*

class ProcessingOptionService {

    /**
     * Dependency injection of Project service
     */
    def projectService
    /**
     * Dependency Injection of Spring Security Service - needed for ACL checks
     */
    def springSecurityService

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public ProcessingOption createOrUpdate(String name, String type, Project project, String value, String comment) {
        ProcessingOption option = findStrict(name, type, project)
        if (option) {
            if (option.value == value && option.comment == comment) {
                return option
            }
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
    public static ProcessingOption findOptionObject(String name, String type, Project project) {
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

    private static ProcessingOption findStrict(String name, String type, Project project) {
        return ProcessingOption.findWhere(
            name: name,
            type: type,
            project: project,
            dateObsoleted: null
        )
    }

    public static String findOption(String name, String type, Project project) {
        ProcessingOption option = findOptionObject(name, type, project)
        return (option) ? option.value : null
    }

    public String findOptionSafe(String name, String type, Project project) {
        ProcessingOption option = findOptionObject(name, type, project)
        return (option) ? option.value : ""
    }

    /**
     * @throws ProcessingException if no option has been found
     */
    public static String findOptionAssure(String name, String type, Project project) {
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
    public long findOptionAsNumber(String name, String type, Project project, long defaultValue) {
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
    static String getValueOfProcessingOption(String name) {
        return CollectionUtils.exactlyOneElement(ProcessingOption.findAllByNameAndDateObsoleted(name, null)).value
    }

    /**
     * Retrieves the ProcessingOption if a user has access to at least one Project.
     * @return List of matching ProcessingOption
     */
    public List<ProcessingOption> listProcessingOptions() {
        if (!projectService.projectsAvailable()) {
            return []
        }
        return ProcessingOption.getAll()
    }

    /**
     * Counts ProcessingOptions which are available to the user.
     * @return The number of ProcessingOptions
     */
    public int countProcessingOption() {
        if (SpringSecurityUtils.ifAllGranted("ROLE_OPERATOR")) {
            // shortcut for operator
            return ProcessingOption.count()
        }
        // for normal users
        Set<String> roles = SpringSecurityUtils.authoritiesToRoles(SpringSecurityUtils.getPrincipalAuthorities())
        if (springSecurityService.isLoggedIn()) {
            // anonymous users do not have a principal
            roles.add((springSecurityService.getPrincipal() as UserDetails).getUsername())
        }
        String query = '''
SELECT count(p.id) FROM ProcessingOption AS p, AclEntry AS ace
JOIN ace.aclObjectIdentity AS aoi
JOIN aoi.aclClass AS ac
JOIN ace.sid AS sid
WHERE
aoi.objectId = p.id
AND sid.sid IN (:roles)
AND ace.mask IN (:permissions)
AND ace.granting = true
'''
        Map params = [
            permissions: [BasePermission.READ.getMask(), BasePermission.ADMINISTRATION.getMask()],
            roles: roles
        ]
        List result = Project.executeQuery(query, params)
        if (!result) {
            return 0
        }
        return result[0]
    }
}
