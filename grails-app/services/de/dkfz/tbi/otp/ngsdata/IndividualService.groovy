/*
 * Copyright 2011-2024 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.ngsdata

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.TimeFormats

import java.nio.file.Path

@CompileDynamic
@Transactional
class IndividualService {

    ConfigService configService
    SampleIdentifierService sampleIdentifierService
    ProjectService projectService
    CommentService commentService

    @PostAuthorize("hasRole('ROLE_OPERATOR') or (returnObject == null) or hasPermission(returnObject.project, 'OTP_READ_ACCESS')")
    Individual getIndividual(long id) {
        return Individual.get(id)
    }

    @PostAuthorize("hasRole('ROLE_OPERATOR') or (returnObject == null) or hasPermission(returnObject.project, 'OTP_READ_ACCESS')")
    Individual getIndividualByPid(String pid) {
        return CollectionUtils.atMostOneElement(Individual.findAllByPid(pid))
    }

    /**
     * Retrieves list of individuals in permission aware manner.
     * The result set can be sorted and filtered. The filter (search) is only applied if the filter String has a length of at least three character.
     * by default it is sorted by the pid
     * @param sortOrder true for ascending, false for descending sorting
     * @param column The column to perform the sorting on
     * @param filtering Filtering restrictions
     * @param filter Filter restrictions
     * @return List of Individuals matching the criteria
     * */
    List<Individual> listIndividuals(boolean sortOrder, IndividualColumn column, IndividualFiltering filtering, String filterString) {
        List projects = projectService.allProjects
        if (!projects) {
            return []
        }
        def c = Individual.createCriteria()
        return c.list {
            'in'('project', projects)
            if (filterString.length() >= 3) {
                String filter = "%${filterString}%"
                or {
                    ilike("pid", filter)
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
    int countIndividual(IndividualFiltering filtering, String filterString) {
        if (filtering.enabled || filterString.length() >= 3) {
            def c = Individual.createCriteria()
            return c.get {
                'in'('project', projectService.allProjects)
                if (filterString.length() >= 3) {
                    String filter = "%${filterString}%"
                    or {
                        ilike("pid", filter)
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
                if (filtering.type) {
                    'in'('type', filtering.type)
                }
                projections {
                    count('pid')
                }
            }
        }
        // shortcut for unfiltered results
        List<Project> projects = projectService.allProjects
        return projects ? Individual.countByProjectInList(projects) : 0
    }

    /**
     * Creates a new Individual together with depending objects
     *
     * @param cmd IndividualCommand holding data of objects to be saved
     * @return created Individual
     * @throws IndividualCreationException This method may throw an IndividualCreationException.
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Individual createIndividual(IndividualCommand cmd) throws IndividualCreationException {
        Individual individual = new Individual(
                pid: cmd.identifier,
                type: cmd.type,
                project: cmd.individualProject,
        )
        individual.save(flush: true)
        createSamples(individual, cmd.samples)
        return individual
    }

    /**
     * Checks whether a Individual with the given pid already exists
     * @param pid The pid to check
     * @return true if there is already a Individual with the pid, false otherwise
     */
    boolean individualExists(String pid) {
        return (CollectionUtils.atMostOneElement(Individual.findAllByPid(pid)) != null)
    }

    /**
     * Fetches all SampleTypes available
     * @return List of SampleTypes
     */
    List<String> getSampleTypeNames() {
        return SampleType.list([sort: "name", order: "asc"])*.name
    }

    /**
     * Updates the given Individual with the new given value.
     * @param individual The Individual to update
     * @param key The key to be updated
     * @param value The new value to set
     * @throws IndividualUpdateException In case the Individual could not be updated
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void updateField(Individual individual, String key, String value) throws IndividualUpdateException {
        individual[key] = value
        if (!individual.save(flush: true)) {
            throw new IndividualUpdateException(individual)
        }
    }

    /**
     * Creates Samples
     * The Samples are handed over as List of {@link SampleCommand}.
     *
     * @param individual The {@link Individual} the Samples are to be associated
     * @param samples List of SampleCommand containing the Samples
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void createSamples(Individual individual, List<SampleCommand> samples) {
        samples.each { SampleCommand sampleCommand ->
            SampleType sampleType = createSampleType(sampleCommand.sampleType)
            if (!sampleType) {
                throw new IndividualCreationException("SampleType could not be found nor created.")
            }
            Sample sample = createSample(individual, sampleType)
            sampleCommand.sampleIdentifiers.each { String sampleIdentifier ->
                if (sampleIdentifier.trim() != "") {
                    sampleIdentifierService.getOrCreateSampleIdentifier(sampleIdentifier, sample)
                }
            }
        }
    }

    /**
     * Triggers the creation of a new {@link Sample} if not yet existent
     * @param individual The {@link Individual} to which the new {@link Sample} belongs to
     * @param type The type of the new {@link Sample}
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void createSample(Individual individual, String type) {
        SampleType sampleType = createSampleType(type)
        createSample(individual, sampleType)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Sample createSample(Individual individual, SampleType sampleType) {
        Sample sample = CollectionUtils.atMostOneElement(Sample.findAllByIndividualAndSampleType(individual, sampleType))
        if (!sample) {
            sample = new Sample(individual: individual, sampleType: sampleType)
            sample.save(flush: true)
        }
        return sample
    }

    /**
     * Creates a new SampleType if not existing, otherwise the existent {@link SampleType} is returned
     * @param name Name of the new SampleType
     * @return the SampleType
     */
    private SampleType createSampleType(String name) {
        SampleType sampleType = SampleTypeService.findSampleTypeByName(name)
        if (!sampleType) {
            sampleType = new SampleType(name: name)
            sampleType.save(flush: true)
        }
        return sampleType
    }

    /**
     * Checks for missing values, as well as input maps with different key-sets and calls
     * {@link IndividualService#saveComment(de.dkfz.tbi.otp.ngsdata.Individual, java.lang.String, java.util.Date)}
     * to finally store the comment for the specific individual in the DB
     * Both input-maps have to contain an {@link Individual}, e.g. Map[individual: individual, ...]
     * @param operation a String that describes the specific operation, e.g. "sample-swap"
     * @param oldProperties a Map that contains the old properties of the individual/sample/lane
     * @param newProperties a Map that contains the new properties of the individual/sample/lane
     * @param additionalInformation a String with additional information that will be displayed between header and old/new properties
     */
    void createComment(String operation, Map oldProperties, Map newProperties, String additionalInformation = null) {
        Closure<Individual> assertAndGetIndividual = { Map properties ->
            assert properties
            assert properties.individual
            return (properties.individual as Individual).refresh()
        }

        Individual oldIndividual = assertAndGetIndividual(oldProperties)
        Individual newIndividual = assertAndGetIndividual(newProperties)
        assert oldProperties.keySet() == newProperties.keySet()

        String output = createCommentString(operation, oldProperties, newProperties, additionalInformation)
        boolean sameIndividual = (oldIndividual == newIndividual)

        /* The target of the comment is the new individual, the old individual remains unchanged.
         * The comment is build in this order:
         * - the active comment of this individual
         * - whatever new comment has been created
         * - the comment of the old individual (if it isnt the same individual)
         */
        String revisedComment = """\
            |${newIndividual.comment?.comment ?: ""}
            |
            |${output}
            |
            |${sameIndividual ? "" : oldIndividual.comment?.comment ?: ""}""".stripMargin().trim()

        commentService.saveComment(newIndividual, revisedComment)
    }

    /**
     * Creates the comment-String used when comment fields on individual pages automatically get filled through swap-scripts
     * The String gets build just with the difference of both properties-maps, equal values will be ignored
     * @param operation a String that describes the specific operation, e.g. "sample-swap"
     * @param oldProperties a Map that contains the old properties of the individual/sample/lane
     * @param newProperties a Map that contains the new properties of the individual/sample/lane
     * @param additionalInformation a String with additional information that will be displayed between header and old/new properties
     * @return the comment-String
     */
    String createCommentString(String operation, Map oldProperties, Map newProperties, String additionalInformation) {
        Map diff = oldProperties - newProperties

        String output = "== ${operation} - ${TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedZonedDateTime(configService.zonedDateTime)} ==\n"
        if (additionalInformation) {
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

    /**
     * returns the folder viewByPid without the pid
     * Example: ${project}/sequencing/exon_sequencing/view-by-pid
     */
    Path getViewByPidPathBase(Individual individual, final SeqType seqType) {
        return projectService.getSequencingDirectory(individual.project).resolve(seqType.dirName).resolve('view-by-pid')
    }

    /**
     * returns the folder viewByPid with the pid
     * Example: ${project}/sequencing/exon_sequencing/view-by-pid/${pid}*/
    Path getViewByPidPath(Individual individual, final SeqType seqType) {
        return getViewByPidPathBase(individual, seqType).resolve(individual.pid)
    }

    String getEscapedPid(Individual individual) {
        String toEscapedChars = /[+]/
        return individual.pid.replaceAll(toEscapedChars, /\\$0/)
    }
}
