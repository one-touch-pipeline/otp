package de.dkfz.tbi.otp.ngsdata

import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.springframework.security.access.prepost.PostFilter
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.acls.domain.BasePermission
import org.springframework.security.core.userdetails.UserDetails

class IndividualService {
    def springSecurityService
    def sampleTypeService
    def sampleService
    def sampleIdentifierService

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
    @PostAuthorize("(returnObject == null) or hasPermission(returnObject.project.id, 'de.dkfz.tbi.otp.ngsdata.Project', read) or hasRole('ROLE_OPERATOR')")
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
    @PostAuthorize("(returnObject == null) or hasPermission(returnObject.project.id, 'de.dkfz.tbi.otp.ngsdata.Project', read) or hasRole('ROLE_OPERATOR')")
    Individual getIndividual(long identifier) {
        return getIndividual("${identifier}")
    }

    /**
     * Retrieves the previous Individual by database id if present.
     * @param individual The Individual for which the predecessor has to be retrieved
     * @return Previous Individual if present, otherwise null
     **/
    @PreAuthorize("hasPermission(#individual.project.id, 'de.dkfz.tbi.otp.ngsdata.Project', read) or hasRole('ROLE_OPERATOR')")
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
    @PreAuthorize("hasPermission(#individual.project.id, 'de.dkfz.tbi.otp.ngsdata.Project', read) or hasRole('ROLE_OPERATOR')")
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
        if (SpringSecurityUtils.ifAllGranted("ROLE_OPERATOR")) {
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
            List<String> columnNames = ["pid", "mockFullName", "mockPid", "project.id", "type"]
            String sortColumn = (column >=0 && column < columnNames.size()) ? columnNames.get(column) : "pid"
            return Individual.list(max: count, offset: offset, sort: sortColumn, order: sortOrder ? "asc" : "desc")
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
OR lower(i.mockPid) like :filter
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
        if (SpringSecurityUtils.ifAllGranted("ROLE_OPERATOR")) {
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
OR lower(i.mockPid) like :filter
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

    /**
     * Creates a new Individual together with depending objects
     *
     * @param project The Project for which the new Individual should be created
     * @param command IndividualCommand holding data of objects to be saved
     * @return created Individual
     * @throws IndividualCreationException
     */
    @PreAuthorize("hasPermission(#project, 'write') or hasRole('ROLE_OPERATOR')")
    public Individual createIndividual(Project project, IndividualCommand command) throws IndividualCreationException {
        Individual individual = new Individual(pid: command.pid, mockPid: command.mockPid, mockFullName: command.mockFullName, type: command.individualType, project: project)
        if (!individual.validate()) {
            throw new IndividualCreationException("Individual does not validate")
        }
        if (!individual.save(flush: true)) {
            throw new IndividualCreationException("Individual could not be saved.")
        }
        command.parseSamples().each { key, value ->
            SampleType sampleType = createSampleType(key)
            if (!sampleType) {
                throw new IndividualCreationException("SampleType could not be found nor created.")
            }
            Sample sample = createSample(individual, sampleType)
            value.each { String name ->
                SampleIdentifier sampleIdentifier = createSampleIdentifier(name, sample)
            }
        }
        return individual
    }

    /**
     * Checks whether a Individual with the given pid already exists
     * @param pid The pid to check
     * @return true if there is already a Individual with the pid, false otherwise
     */
    public boolean individualExists(String pid) {
        return (Individual.findByPid(pid) != null)
    }

    /**
     * Fetches all SampleTypes available
     * @return List of SampleTypes
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public List<String> getSampleTypeNames() {
        return SampleType.list().name
    }

    /**
     * Fetches all SampleIdentifiers available
     * @return List of SampleIdentifiers
     */
    @PostFilter("hasPermission(filterObject.sample.individual.project.id, 'de.dkfz.tbi.otp.ngsdata.Project', read) or hasRole('ROLE_OPERATOR')")
    public List<SampleIdentifier> getSampleIdentifiers() {
        return SampleIdentifier.list()
    }

    /**
     * Creates a new Sample if not existing
     * @param individual the Individual to be associated
     * @param sampleType the SampleType to be associated
     * @return the Sample
     */
    private Sample createSample(Individual individual, SampleType sampleType) {
        return Sample.findOrSaveByIndividualAndSampleType(individual, sampleType)
    }

    /**
     * Creates a new SampleIdentifier if not existing
     * @param name Name of the new Sample
     * @param sample Sample to be associated
     * @return the SampleIdentifier
     */
    private SampleIdentifier createSampleIdentifier(String name, Sample sample) {
        return SampleIdentifier.findOrSaveByNameAndSample(name, sample)
    }

    /**
     * Creates a new SampleType if not existing
     * @param name Name of the new SampleType
     * @return the SampleType
     */
    private SampleType createSampleType(String name) {
        return SampleType.findOrSaveByName(name)
    }
}
