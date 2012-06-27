package de.dkfz.tbi.otp.ngsdata

import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.acls.domain.BasePermission
import org.springframework.security.core.userdetails.UserDetails

class IndividualService {
    def springSecurityService

    // TODO: this constant should not be here! And is also wrong!
    private static final String resourcesPath = "${home}STS/ngsTest/resources/"

    /**
     * this function loads individuals name from the provided list
     */
    void loadIndividuals() {
        // PROJECT_NAME
        Project proj = Project.findByName("PROJECT_NAME")

        File file = new File(resourcesPath + "listInd.txt")
        if (!file.canRead()) {
            // TODO: shall be exception
            log.warn "can not read ${file}"
            return
        }

        log.debug "reading list of individuals from ${file}"

        Individual ind
        file.eachLine { line, no ->
            // magic standard to DB relation
            String pid = line.substring(0, line.indexOf(','))
            String name = line.substring(line.indexOf(',') + 1)
            String mockPid = (no as String).padLeft(3, '0')

            ind = new Individual(mockFullName: name, pid: pid, mockPid: mockPid)
            ind.type = Individual.Type.REAL

            // HACK
            if (name.indexOf("Pool") != -1) {
                ind.type = Individual.Type.POOL
            }

            ind.project = proj
            ind.save()
        }
    }

    /**
     * this function load samples from the provided list
     * and attache samples to individuals
     * @return
     */
    def loadSamples() {
        // PROJECT_NAME

        // TODO: we are not in this directory
        String prefix = "${home}"

        def files = [
            "$DATA_HOME/PROJECT_NAME/map-pid-tumor-control.csv",
            "$DATA_HOME/PROJECT_NAME/map-pid-tumor-control-with-prefix.csv"
        ]

        files.each {
            Sample sample
            File file = new File(prefix + it);
            if (!file.canRead()) {
                log.debug "can not read ${file}"
                // continue
                return
            }

            log.debug "reading samples from file ${it}"

            file.eachLine { line, no ->

                // magic file format
                String name = line.substring(0, line.indexOf(','));
                String pid  = line.substring(line.indexOf(',') + 1, line.indexOf('/'))

                // find out type
                String typeText  = line.substring(line.indexOf('/') + 1)
                Sample.Type type = Sample.Type.UNKNOWN
                if (typeText.indexOf("tumor") != -1) {
                    type = Sample.Type.TUMOR
                }
                if (typeText.indexOf("control") != -1) {
                    type = Sample.Type.CONTROL
                }

                Individual ind = Individual.findByPid(pid);

                sample = Sample.findByIndividualAndType(ind, type)

                if (!sample) {
                    sample = new Sample(type: type)
                    sample.individual = ind
                    sample.save()
                }

                SampleIdentifier identifier = new SampleIdentifier(
                    name: name,
                    sample: sample
                )

                identifier.save()
                ind.save()
                sample.save()
            }
        }
    }

    /**
     * Retrieves the given Individual.
     * If the parameter can be converted to a Long it is assumed to be the database ID.
     * Otherwise it is tried to find the Individual by its mock name - as well if the Individual could not
     * be found by the database id.
     *
     * If no Individual is found null is returned.
     * @param identifier Name or database Id
     * @return Individual
     **/
    @PostAuthorize("(returnObject == null) or hasPermission(returnObject.project.id, 'de.dkfz.tbi.otp.ngsdata.Project', read) or hasRole('ROLE_ADMIN')")
    Individual getIndividual(String identifier) {
        if (!identifier) {
            return null
        }
        Individual individual = null
        if (identifier?.isLong()) {
            individual = Individual.get(identifier as Long)
        }
        if (!individual) {
            individual = Individual.findByMockFullName(identifier)
        }
        return individual
    }

    /**
     * Retrieves the given Individual.
     * Overloaded method for convenience.
     * @param identifier Name or database Id
     * @return Individual
     **/
    @PostAuthorize("(returnObject == null) or hasPermission(returnObject.project.id, 'de.dkfz.tbi.otp.ngsdata.Project', read) or hasRole('ROLE_ADMIN')")
    Individual getIndividual(long identifier) {
        return getIndividual("${identifier}")
    }

    /**
     * Retrieves the previous Individual by database id if present.
     * @param individual The Individual for which the predecessor has to be retrieved
     * @return Previous Individual if present, otherwise null
     **/
    @PreAuthorize("hasPermission(#individual.project.id, 'de.dkfz.tbi.otp.ngsdata.Project', read) or hasRole('ROLE_ADMIN')")
    Individual previousIndividual(Individual individual) {
        if (!individual) {
            return null
        }
        // TODO: navigate to Individual in ACL aware manner
        return Individual.findByIdLessThan(individual.id, [sort: "id", order: "desc"])
    }

    /**
     * Retrieves the next Individual by database id if present.
     * @param individual The Individual for which the successor has to be retrieved
     * @return Next Individual if present, otherwise null
     **/
    @PreAuthorize("hasPermission(#individual.project.id, 'de.dkfz.tbi.otp.ngsdata.Project', read) or hasRole('ROLE_ADMIN')")
    Individual nextIndividual(Individual individual) {
        if (!individual) {
            return null
        }
        // TODO: navigate to Individual in ACL aware manner
        return Individual.findByIdGreaterThan(individual.id, [sort: "id", order: "asc"])
    }

    /**
     * Retrieves list of Individual in ACL aware manner.
     * For an admin user all Individuals are returned, for a non-admin user the ACL on Project is
     * used to determine the list of Individuals to return.
     * The result set can be paginated, sorted and filtered. The filter (search) is only applied if
     * the filter String has a length of at least three character.
     * The sorting is applied using the sort order and the column which is an integer identifying the
     * following columns:
     * <ul>
     * <li><strong>0:</strong> pid</li>
     * <li><strong>1:</strong> mock full name</li>
     * <li><strong>2:</strong> mock pid</li>
     * <li><strong>3:</strong> project name</li>
     * <li><strong>4:</strong> type</li>
     * </ul>
     * by default it is sorted by the pid
     * @param offset Offset in result list of pagination
     * @param count The number of Individuals to return in this query
     * @param sortOrder true for ascending, false for descending sorting
     * @param column the column to sort on, see above for mapping
     * @param filter The search filter
     * @return List of Individuals matching the criterias and ACL restricted
     **/
    List<Individual> listIndividuals(int offset, int count, boolean sortOrder, int column, String filter) {
        if (SpringSecurityUtils.ifAnyGranted("ROLE_ADMIN")) {
            if (filter.length() >= 3) {
                String query = '''
SELECT i FROM Individual as i
WHERE
lower(i.pid) like :filter
OR lower(i.mockFullName) like :filter
OR lower(i.mockPid) like :filter
OR lower(i.project.name) like :filter
OR lower(i.type) like :filter
'''
                query = query + "ORDER BY "
                switch (column) {
                case 1:
                    query = query + "i.mockFullName"
                    break
                case 2:
                    query = query + "i.mockPid"
                    break
                case 3:
                    query = query + "i.project.id"
                    break
                case 4:
                    query = query + "i.type"
                    break
                case 0:
                default:
                    query = query + "i.pid"
                    break
                }
                query = query + " ${sortOrder ? 'asc' : 'desc'}"
                Map params = [
                    filter: "%${filter.toLowerCase()}%",
                    max: count, offset: offset]
                return Individual.executeQuery(query, params)
            }
            return Individual.list(max: count, offset: offset, sort: "pid", order: sortOrder ? "asc" : "desc")
        }

        Set<String> roles = SpringSecurityUtils.authoritiesToRoles(SpringSecurityUtils.getPrincipalAuthorities())
        if (springSecurityService.isLoggedIn()) {
            // anonymous users do not have a principal
            roles.add((springSecurityService.getPrincipal() as UserDetails).getUsername())
        }

        String query = '''
SELECT i FROM Individual as i, AclEntry AS ace
JOIN i.project AS p
JOIN ace.aclObjectIdentity AS aoi
JOIN aoi.aclClass AS ac
JOIN ace.sid AS sid
WHERE
aoi.objectId = p.id
AND ac.className = :className
AND sid.sid IN (:roles)
AND ace.mask IN (:permissions)
AND ace.granting = true
'''
        if (filter.length() >= 3) {
            query += '''
AND (
lower(i.pid) like :filter
OR lower(i.mockFullName) like :filter
OR lower(i.mockPid like) :filter
OR lower(i.project.name) like :filter
OR lower(i.type) like :filter
)
'''
        }
        query = query + "ORDER BY "
        switch (column) {
        case 1:
            query = query + "i.mockFullName"
            break
        case 2:
            query = query + "i.mockPid"
            break
        case 3:
            query = query + "i.project.id"
            break
        case 4:
            query = query + "i.type"
            break
        case 0:
        default:
            query = query + "i.pid"
            break
        }
        query = query + " ${sortOrder ? 'asc' : 'desc'}"
        Map params = [
            className: Project.class.getName(),
            permissions: [BasePermission.READ.getMask(), BasePermission.ADMINISTRATION.getMask()],
            roles: roles,
            max: count, offset: offset]
        if (filter.length() >= 3) {
            params.put("filter", "%${filter.toLowerCase()}%");
        }
        return Individual.executeQuery(query, params)
    }

    /**
     * Counts the Individual in an ACL aware manner.
     * For an admin all individuals are included.
     * @param filter Restrict on this search filter if at least three characters
     * @return Number of Individuals in ACL aware manner
     **/
    int countIndividual(String filter) {
        if (SpringSecurityUtils.ifAnyGranted("ROLE_ADMIN")) {
            if (filter.length() >= 3) {
                String query = '''
SELECT COUNT(DISTINCT i.id) FROM Individual as i
WHERE
lower(i.pid) like :filter
OR lower(i.mockFullName) like :filter
OR lower(i.mockPid) like :filter
OR lower(i.project.name) like :filter
OR lower(i.type) like :filter
'''
                Map params = [
                    filter: "%${filter.toLowerCase()}%"]
                return Individual.executeQuery(query, params)[0] as Integer
            }
            return Individual.count()
        }

        Set<String> roles = SpringSecurityUtils.authoritiesToRoles(SpringSecurityUtils.getPrincipalAuthorities())
        if (springSecurityService.isLoggedIn()) {
            // anonymous users do not have a principal
            roles.add((springSecurityService.getPrincipal() as UserDetails).getUsername())
        }

        String query = '''
SELECT COUNT(DISTINCT i.id) FROM Individual as i, AclEntry AS ace
JOIN i.project AS p
JOIN ace.aclObjectIdentity AS aoi
JOIN aoi.aclClass AS ac
JOIN ace.sid AS sid
WHERE
aoi.objectId = p.id
AND ac.className = :className
AND sid.sid IN (:roles)
AND ace.mask IN (:permissions)
AND ace.granting = true
'''
        if (filter.length() >= 3) {
            query += '''
AND (
lower(i.pid) like :filter
OR lower(i.mockFullName) like :filter
OR lower(i.mockPid like) :filter
OR lower(i.project.name) like :filter
OR lower(i.type) like :filter
)
'''
        }
        Map params = [
            className: Project.class.getName(),
            permissions: [BasePermission.READ.getMask(), BasePermission.ADMINISTRATION.getMask()],
            roles: roles]

        if (filter.length() >= 3) {
            params.put("filter", "%${filter.toLowerCase()}%");
        }
        return Individual.executeQuery(query, params)[0] as Integer
    }
}
