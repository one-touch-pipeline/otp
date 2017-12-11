package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.security.User
import grails.plugin.springsecurity.SpringSecurityUtils
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PostFilter
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.acls.domain.BasePermission
import org.springframework.security.core.userdetails.UserDetails
import de.dkfz.tbi.otp.utils.ReferencedClass

class IndividualService {
    def springSecurityService
    def sampleTypeService
    def sampleService
    def sampleIdentifierService
    def projectService
    CommentService commentService

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
    @PostAuthorize("hasRole('ROLE_OPERATOR') or (returnObject == null) or hasPermission(returnObject.project.id, 'de.dkfz.tbi.otp.ngsdata.Project', read)")
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

    @PostAuthorize("hasRole('ROLE_OPERATOR') or (returnObject == null) or hasPermission(returnObject.project.id, 'de.dkfz.tbi.otp.ngsdata.Project', read)")
    Individual getIndividualByMockPid(String mockPid) {
        if (!mockPid) {
            return null
        }
        Individual individual = Individual.findByMockPid(mockPid)
        return individual
    }


    /**
     * Retrieves the given Individual.
     * Overloaded method for convenience.
     * @param identifier Name or database Id
     * @return Individual
     **/
    @PostAuthorize("hasRole('ROLE_OPERATOR') or (returnObject == null) or hasPermission(returnObject.project.id, 'de.dkfz.tbi.otp.ngsdata.Project', read)")
    Individual getIndividual(long identifier) {
        return getIndividual("${identifier}")
    }

    /**
     * Retrieves list of Individual in ACL aware manner.
     * For an admin user all Individuals are returned, for a non-admin user the ACL on Project is
     * used to determine the list of Individuals to return.
     * The result set can be sorted and filtered. The filter (search) is only applied if the filter String has a length of at least three character.
     * by default it is sorted by the pid
     * @param sortOrder true for ascending, false for descending sorting
     * @param column The column to perform the sorting on
     * @param filtering Filtering restrictions
     * @param filter Filter restrictions
     * @return List of Individuals matching the criterias and ACL restricted
     **/
    public List<Individual> listIndividuals(boolean sortOrder, IndividualSortColumn column, IndividualFiltering filtering, String filter) {
        String columnName = "project"
        def c = Individual.createCriteria()
        return c.list {
            'in'('project', projectService.getAllProjects())
            if (filter.length() >= 3) {
                filter = "%${filter}%"
                or {
                    ilike("pid", filter)
                    ilike("mockFullName", filter)
                    ilike("mockPid", filter)
                    project {
                        ilike("name", filter)
                    }
                }
            }
            if (filtering.project) {
                project {
                    'in'('id', filtering.project)
                }
            }
            if (filtering.pid) {
                or {
                    filtering.pid.each {
                        ilike('pid', "%${it}%")
                    }
                }
            }
            if (filtering.mockFullName) {
                or {
                    filtering.mockFullName.each {
                        ilike('mockFullName', "%${it}%")
                    }
                }
            }
            if (filtering.mockPid) {
                or {
                    filtering.mockPid.each {
                        ilike('mockPid', "%${it}%")
                    }
                }
            }
            if (filtering.type) {
                'in'('type', filtering.type)
            }
            if (column.columnName == "project") {
                project {
                    order("name", sortOrder ? "asc" : "desc")
                }
            } else {
                order(column.columnName, sortOrder ? "asc" : "desc")
            }
        }
    }

    /**
     * Counts the Individuals the User has access to by applying the provided filtering.
     * @param filtering The filters to apply on the data
     * @param filter Restrict on this search filter if at least three characters
     * @return Number of Individuals matching the filtering
     */
    public int countIndividual(IndividualFiltering filtering, String filter) {
        if (filtering.enabled || filter.length() >= 3) {
            def c = Individual.createCriteria()
            return c.get {
                'in'('project', projectService.getAllProjects())
                if (filter.length() >= 3) {
                    filter = "%${filter}%"
                    or {
                        ilike("pid", filter)
                        ilike("mockFullName", filter)
                        ilike("mockPid", filter)
                        project {
                            ilike("name", filter)
                        }
                    }
                }
                if (filtering.project) {
                    project {
                        'in'('id', filtering.project)
                    }
                }
                if (filtering.pid) {
                    or {
                        filtering.pid.each {
                            ilike('pid', "%${it}%")
                        }
                    }
                }
                if (filtering.mockFullName) {
                    or {
                        filtering.mockFullName.each {
                            ilike('mockFullName', "%${it}%")
                        }
                    }
                }
                if (filtering.mockPid) {
                    or {
                        filtering.mockPid.each {
                            ilike('mockPid', "%${it}%")
                        }
                    }
                }
                if (filtering.type) {
                    'in'('type', filtering.type)
                }
                projections {
                    count('mockPid')
                }
            }
        } else {
            // shortcut for unfiltered results
            return Individual.countByProjectInList(projectService.getAllProjects())
        }
    }

    /**
     * Creates a new Individual together with depending objects
     *
     * @param project The Project for which the new Individual should be created
     * @param command IndividualCommand holding data of objects to be saved
     * @return created Individual
     * @throws IndividualCreationException
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'write')")
    public Individual createIndividual(Project project, IndividualCommand command, List<SamplesParser> parsedSamples) throws IndividualCreationException {
        Individual individual = new Individual(
                        pid: command.pid,
                        mockPid: command.mockPid,
                        mockFullName: command.mockFullName,
                        internIdentifier: command.internIdentifier,
                        type: command.individualType, project: project
                        )
        if (!individual.validate()) {
            throw new IndividualCreationException("Individual does not validate")
        }
        if (!individual.save(flush: true)) {
            throw new IndividualCreationException("Individual could not be saved.")
        }
        createOrUpdateSamples(individual, parsedSamples)
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
    public List<String> getSampleTypeNames() {
        return SampleType.list([sort: "name", order: "asc"])*.name
    }

    /**
     * Fetches all SampleIdentifiers available
     * @return List of SampleIdentifiers
     */
    @PostFilter("hasRole('ROLE_OPERATOR') or hasPermission(filterObject.sample.individual.project.id, 'de.dkfz.tbi.otp.ngsdata.Project', read)")
    public List<SampleIdentifier> getSampleIdentifiers() {
        return SampleIdentifier.list()
    }

    /**
     * Fetches all SampleIdentifiers belonging to the given {@link Individual} and of the {@link SampleType}
     * @param individualId The id of the {@link Individual} the {@link SampleIdentifier} shall be retrieved
     * @param sType The {@link SampleType}'s name of which {@link SampleIdentifier}s are fetched
     * @return List of SampleIdentifiers
     */
    @PostFilter("hasRole('ROLE_OPERATOR') or hasPermission(filterObject.sample.individual.project.id, 'de.dkfz.tbi.otp.ngsdata.Project', read)")
    public List<SampleIdentifier> getSampleIdentifiers(Long individualId, String sType) {
        List<SampleIdentifier> sampleIdentifiers = SampleIdentifier.withCriteria {
            sample {
                and {
                    individual {
                        eq('id', individualId)
                    }
                    sampleType {
                        ilike("name", sType)
                    }
                }
            }
        }
        return sampleIdentifiers
    }

    /**
     * Updates the given Individual with the new given value.
     * Creates a ChangeLog entry for this update.
     * @param individual The Individual to update
     * @param key The key to be updated
     * @param value The new value to set
     * @throws ChangelogException In case the Changelog Entry could not be created
     * @throws IndividualUpdateException In case the Individual could not be updated
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR') or ((#individual != null) and hasPermission(#individual.project.id, 'de.dkfz.tbi.otp.ngsdata.Individual', write))")
    void updateField(Individual individual, String key, String value) throws ChangelogException, IndividualUpdateException {
        ReferencedClass clazz = ReferencedClass.findOrSaveByClassName(individual.class.getName())
        // To check if the key handed over matches the field name
        // value is not used later because the method returns upper case name
        if (!individual.getDomainClass().getFieldName(key)) {
            throw new IndividualUpdateException(individual)
        }
        ChangeLog changelog = new ChangeLog(rowId: individual.id, referencedClass: clazz, columnName: key, fromValue: "${individual[key]}", toValue: value, comment: "-", source: ChangeLog.Source.MANUAL)
        if (!changelog.save()) {
            throw new ChangelogException("Creation of changelog failed, errors: " + changelog.errors.toString())
        }
        individual[key] = value
        if (!individual.save(flush: true)) {
            throw new IndividualUpdateException(individual)
        }
    }

    /**
     * Creates and updates all given aspects of Samples
     *
     * The Samples are handed over as List of {@link SamplesParser}.
     * Then there is extracted the information, which is
     * for new Samples the identifier and the type and for
     * Samples to be updated the id as well. As the parameter
     * String can be mixed from new and to be updated the method
     * handles both cases.
     *
     * @param individual The {@link Individual} the Samples are to be associated
     * @param parsedSamples List of SamplesParser containing the Samples
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#individual, 'write')")
    void createOrUpdateSamples(Individual individual, List<SamplesParser> parsedSamples) {
        parsedSamples.each { SamplesParser parsedSample ->
            SampleType sampleType = createSampleType(parsedSample.type)
            if (!sampleType) {
                throw new IndividualCreationException("SampleType could not be found nor created.")
            }
            Sample sample = Sample.findOrSaveByIndividualAndSampleType(individual, sampleType)
            for (entry in parsedSample.updateEntries) {
                updateSampleIdentifier(entry.value, entry.key as long)
            }
            parsedSample.newEntries.each { String sampleIdentifier ->
                createSampleIdentifier(sampleIdentifier, sample)
            }
        }
    }

    /**
     * Triggers the creation of a new {@link Sample} if not yet existent
     * @param individual The {@link Individual} to which the new {@link Sample} belongs to
     * @param type The type of the new {@link Sample}
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#individual, 'write')")
    void createSample(Individual individual, String type) {
        SampleType sampleType = createSampleType(type)
        Sample.findOrSaveByIndividualAndSampleType(individual, sampleType)
    }

    /**
     * Creates a new {@link SampleIdentifier} if not existing, otherwise the existent {@link SampleIdentifier} is returned
     * @param name Name of the new {@link Sample}
     * @param sample {@link Sample} to be associated
     * @return the {@link SampleIdentifier}
     */
    private SampleIdentifier createSampleIdentifier(String name, Sample sample) {
        return SampleIdentifier.findOrSaveByNameAndSample(name, sample)
    }

    /**
     * Updates the {@link SampleIdentifier} identified by the given id
     * @param name Name of the {@link Sample} to be updated
     * @param sample Sample to be associated
     * @param id The id of the {@link SampleIdentifier} to be updated
     * @return the updated {@link SampleIdentifier}
     */
    private SampleIdentifier updateSampleIdentifier(String name, long id) {
        SampleIdentifier sampleIdentifier = SampleIdentifier.get(id)
        if (sampleIdentifier) {
            sampleIdentifier.name = name
            sampleIdentifier.save(flush: true)
        }
        return sampleIdentifier
    }

    /**
     * Creates a new SampleType if not existing, otherwise the existent {@link SampleType} is returned
     * @param name Name of the new SampleType
     * @return the SampleType
     */
    private SampleType createSampleType(String name) {
        return SampleType.findOrSaveByName(name)
    }
    /**
     * show the List of Individual per Project
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'read')")
    public List findAllMockPidsByProject(Project project) {
        List seq = Sequence.withCriteria {
            eq("projectId", project.id)
            projections {
                groupProperty("mockPid")
            }
            order("mockPid")
        }
        return seq
    }

    /**
     * Checks for missing values, as well as input maps with different key-sets and calls {@link IndividualService#saveComment(de.dkfz.tbi.otp.ngsdata.Individual, java.lang.String, java.util.Date)}}
     * to finally store the comment for the specific individual in the DB
     * Both input-maps have to contain an {@link Individual}, e.g. Map[individual: individual, ...]
     * @param operation a String that describes the specific operation, e.g. "sample-swap"
     * @param oldProperties a Map that contains the old properties of the individual/sample/lane
     * @param newProperties a Map that contains the new properties of the individual/sample/lane
     * @param additionalInformation a String with additional information that will be displayed between header and old/new properties
     */
    public void createComment (String operation, Map oldProperties, Map newProperties, String additionalInformation = null) {
        assert oldProperties
        assert oldProperties.individual
        assert newProperties
        assert newProperties.individual
        assert oldProperties.keySet() == newProperties.keySet()

        Date date = new Date()

        String output = createCommentString(operation, oldProperties, newProperties, date, additionalInformation)
        commentService.saveComment(newProperties.individual, oldProperties.individual.comment?.comment ? "${output}\n${oldProperties.individual.comment.comment}" : "${output}")
    }

    /**
     * Creates the comment-String used when comment fields on individual pages automatically get filled through swap-scripts
     * The String gets build just with the difference of both properties-maps, equal values will be ignored
     * @param operation a String that describes the specific operation, e.g. "sample-swap"
     * @param oldProperties a Map that contains the old properties of the individual/sample/lane
     * @param newProperties a Map that contains the new properties of the individual/sample/lane
     * @param date a Date-object
     * @param additionalInformation a String with additional information that will be displayed between header and old/new properties
     * @return the comment-String
     */
    private String createCommentString(String operation, Map oldProperties, Map newProperties, Date date, String additionalInformation) {
        Map diff = oldProperties - newProperties

        String output = "== ${operation} - ${date.format("yyyy-MM-dd hh:mm")} ==\n"
        if(additionalInformation) {
            output += "${additionalInformation}\n"
        }

        String outputOld = "Old:\n"
        String outputNew = "New:\n"

        diff.keySet().each { key ->
            outputOld += "${key}: ${oldProperties.get(key)}\n"
            outputNew += "${key}: ${newProperties.get(key)}\n"
        }

        return "${output}${outputOld}${outputNew}"
    }
}
