package de.dkfz.tbi.otp.ngsdata

import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PostFilter

/**
 * Service providing methods to access information about Projects.
 *
 */
class ProjectService {

    /**
     *
     * @return List of all available Projects
     */
    @PostFilter("hasPermission(filterObject, 'read') or hasRole('ROLE_OPERATOR')")
    public List<Project> getAllProjects() {
        return Project.list()
    }

    /**
     * Returns the Project in an acl aware manner
     * @param id The Id of the Project
     * @return The Project
     */
    @PostAuthorize("returnObject == null or hasPermission(returnObject, 'read') or hasRole('ROLE_OPERATOR')")
    public Project getProject(Long id) {
        return Project.get(id)
    }
}
