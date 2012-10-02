package de.dkfz.tbi.otp.ngsdata

import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PostFilter
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.acls.domain.BasePermission
import org.springframework.security.acls.domain.GrantedAuthoritySid
import org.springframework.security.acls.model.Sid
import org.springframework.security.core.userdetails.UserDetails

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
     * Dependency Injection of Spring Security Service - needed for ACL checks
     */
    def springSecurityService

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

    /**
     * Discovers if a user has Projects.
     * @return <code>true</code> if the user has Project(s), false otherwise
     */
    public boolean projectsAvailable() {
        if (SpringSecurityUtils.ifAllGranted("ROLE_OPERATOR")) {
            // shortcut for operator
            if (Project.count() > 0) {
                return true
            }
            return false
        }
        // for normal users
        Set<String> roles = SpringSecurityUtils.authoritiesToRoles(SpringSecurityUtils.getPrincipalAuthorities())
        if (springSecurityService.isLoggedIn()) {
            // anonymous users do not have a principal
            roles.add((springSecurityService.getPrincipal() as UserDetails).getUsername())
        }
        String query = '''
SELECT count(p.id) FROM Project AS p, AclEntry AS ace
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
            return false
        }
        if ((result[0] as Long) >= 1) {
            return true
        }
        return false
    }
}
