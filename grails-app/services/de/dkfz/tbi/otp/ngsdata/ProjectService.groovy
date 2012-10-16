package de.dkfz.tbi.otp.ngsdata

import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PostFilter
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.acls.domain.BasePermission
import org.springframework.security.acls.domain.GrantedAuthoritySid
import org.springframework.security.acls.model.Sid

import de.dkfz.tbi.otp.security.Group


/**
 * Service providing methods to access information about Projects.
 *
 */
class ProjectService {
    /**
     * Dependency Injection for aclUtilService
     */
    def aclUtilService

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

    /**
     * Creates a Project and grants permissions to Groups which have read/write privileges for Projects.
     * @param name
     * @param dirName
     * @param realmName
     * @return The created project
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public Project createProject(String name, String dirName, String realmName) {
        Project project = new Project(name: name, dirName: dirName, realmName: realmName)
        project = project.save(flush: true)
        assert(project != null)
        // add to groups
        Group.list().each { Group group ->
            if (group.readProject) {
                Sid sid = new GrantedAuthoritySid(group.role.authority)
                aclUtilService.addPermission(project, sid, BasePermission.READ)
                if (group.writeProject) {
                    aclUtilService.addPermission(project, sid, BasePermission.WRITE)
                }
            }
        }
        return project
    }
}
