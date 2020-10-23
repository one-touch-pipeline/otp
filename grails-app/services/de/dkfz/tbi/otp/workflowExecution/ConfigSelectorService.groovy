/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.workflowExecution

import grails.gorm.transactions.Transactional
import groovy.transform.TupleConstructor
import org.grails.datastore.mapping.query.api.BuildableCriteria
import org.hibernate.sql.JoinType
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

import java.time.LocalDate

@Transactional
class ConfigSelectorService {

    /**
     * Used to find Selectors similar to a given name.
     */
    List<ExternalWorkflowConfigSelector> findRelatedSelectorsByName(String name) {
        if (!name) {
            return []
        }
        return ExternalWorkflowConfigSelector.createCriteria().listDistinct {
            ilike('name', "%${name}%")
        } as List<ExternalWorkflowConfigSelector>
    }

    /**
     * Used to find ExternalWorkflowConfigSelectors related to the attributes of an MultiSelectSelectorExtendedCriteria object.
     * Multi select option. Returns all ExternalWorkflowConfigSelectors matching any of the given attributes.
     */
    List<ExternalWorkflowConfigSelector> findAllRelatedSelectors(MultiSelectSelectorExtendedCriteria relatedSelectorExtendedCriteria) {
        if (!relatedSelectorExtendedCriteria.anyValueSet()) {
            return []
        }
        return ExternalWorkflowConfigSelector.createCriteria().listDistinct {
            or {
                if (relatedSelectorExtendedCriteria.workflows) {
                    workflows {
                        'in'('id', relatedSelectorExtendedCriteria.workflows*.id)
                    }
                }
                if (relatedSelectorExtendedCriteria.workflowVersions) {
                    workflowVersions {
                        'in'('id', relatedSelectorExtendedCriteria.workflowVersions*.id)
                    }
                }
                if (relatedSelectorExtendedCriteria.projects) {
                    projects {
                        'in'('id', relatedSelectorExtendedCriteria.projects*.id)
                    }
                }
                if (relatedSelectorExtendedCriteria.seqTypes) {
                    seqTypes {
                        'in'('id', relatedSelectorExtendedCriteria.seqTypes*.id)
                    }
                }
                if (relatedSelectorExtendedCriteria.referenceGenomes) {
                    referenceGenomes {
                        'in'('id', relatedSelectorExtendedCriteria.referenceGenomes*.id)
                    }
                }
                if (relatedSelectorExtendedCriteria.libraryPreparationKits) {
                    libraryPreparationKits {
                        'in'('id', relatedSelectorExtendedCriteria.libraryPreparationKits*.id)
                    }
                }
            }
        } as List<ExternalWorkflowConfigSelector>
    }

    /**
     * Helper method for building HQL query.
     * @param propertyString refers to the Set-attributes of the MultiSelectSelectorExtendedCriteria.
     * @param connectNext indicates that the query is not finished yet and will be extended.
     */
    private static String findExactSelectorQueryHelper(
            MultiSelectSelectorExtendedCriteria multiSelectSelectorExtendedCriteria, String propertyString, Map parameters, boolean connectNext
    ) {
        String hql = """"""
        if (multiSelectSelectorExtendedCriteria.getProperty(propertyString)) {
            hql += """ewcs.${propertyString}.size = :${propertyString}Count
                    and ewcs.${propertyString}.size = (
                        select
                            count(ewcs2.id)
                        from
                            ExternalWorkflowConfigSelector as ewcs2
                            join ewcs2.${propertyString} as ${propertyString}
                        where
                            ewcs2.id = ewcs.id
                            and ${propertyString}.id in (:${propertyString})
                    )
                """
            parameters[propertyString + "Count"] = multiSelectSelectorExtendedCriteria.getProperty(propertyString).size()
            parameters[propertyString] = multiSelectSelectorExtendedCriteria.getProperty(propertyString)*.id
        } else {
            hql += """
                ewcs.${propertyString} is empty
            """
        }
        if (connectNext) {
            hql += """
            and
            """
        }
        return hql
    }

    /**
     * Used to find all ExternalWorkflowConfigSelectors exactly matching the attributes of an MultiSelectSelectorExtendedCriteria object.
     * Multi select option. There can only be a single match.
     */
    ExternalWorkflowConfigSelector findExactSelector(MultiSelectSelectorExtendedCriteria multiSelectSelectorExtendedCriteria) {
        String hql = """
            select
                ewcs
            from
                ExternalWorkflowConfigSelector as ewcs
            where
            """

        Map parameters = [:]

        hql += findExactSelectorQueryHelper(multiSelectSelectorExtendedCriteria, "workflows", parameters, true)
        hql += findExactSelectorQueryHelper(multiSelectSelectorExtendedCriteria, "workflowVersions", parameters, true)
        hql += findExactSelectorQueryHelper(multiSelectSelectorExtendedCriteria, "projects", parameters, true)
        hql += findExactSelectorQueryHelper(multiSelectSelectorExtendedCriteria, "seqTypes", parameters, true)
        hql += findExactSelectorQueryHelper(multiSelectSelectorExtendedCriteria, "referenceGenomes", parameters, true)
        hql += findExactSelectorQueryHelper(multiSelectSelectorExtendedCriteria, "libraryPreparationKits", parameters, false)

        List<ExternalWorkflowConfigSelector> result = ExternalWorkflowConfigSelector.executeQuery(hql, parameters)
        if (result == [] || result.size() >= 2) {
            return null
        }
        return result.first() as ExternalWorkflowConfigSelector
    }

    /**
     * Used to find a ExternalWorkflowConfigSelector exactly matching the attributes of an SingleSelectSelectorExtendedCriteria object.
     * Single select option.
     */
    List<ExternalWorkflowConfigSelector> findAllSelectors(SingleSelectSelectorExtendedCriteria singleSelectSelectorExtendedCriteria) {
        if (!singleSelectSelectorExtendedCriteria.anyValueSet()) {
            return []
        }
        BuildableCriteria criteria = ExternalWorkflowConfigSelector.createCriteria()
        return criteria.listDistinct {
            if (singleSelectSelectorExtendedCriteria.workflow) {
                criteria | {
                    criteria.isEmpty('workflows')
                    criteria.workflows(JoinType.LEFT_OUTER_JOIN.joinTypeValue) {
                        criteria.'in'('id', singleSelectSelectorExtendedCriteria.workflow.id)
                    }
                }
            }
            if (singleSelectSelectorExtendedCriteria.workflowVersion) {
                criteria | {
                    criteria.isEmpty('workflowVersions')
                    criteria.workflowVersions(JoinType.LEFT_OUTER_JOIN.joinTypeValue) {
                        criteria.'in'('id', singleSelectSelectorExtendedCriteria.workflowVersion.id)
                    }
                }
            }
            if (singleSelectSelectorExtendedCriteria.project) {
                criteria | {
                    criteria.isEmpty('projects')
                    criteria.projects(JoinType.LEFT_OUTER_JOIN.joinTypeValue) {
                        criteria.'in'('id', singleSelectSelectorExtendedCriteria.project.id)
                    }
                }
            }
            if (singleSelectSelectorExtendedCriteria.seqType) {
                criteria | {
                    criteria.isEmpty('seqTypes')
                    criteria.seqTypes(JoinType.LEFT_OUTER_JOIN.joinTypeValue) {
                        criteria.'in'('id', singleSelectSelectorExtendedCriteria.seqType.id)
                    }
                }
            }
            if (singleSelectSelectorExtendedCriteria.referenceGenome) {
                criteria | {
                    criteria.isEmpty('referenceGenomes')
                    criteria.referenceGenomes(JoinType.LEFT_OUTER_JOIN.joinTypeValue) {
                        criteria.'in'('id', singleSelectSelectorExtendedCriteria.referenceGenome.id)
                    }
                }
            }
            if (singleSelectSelectorExtendedCriteria.libraryPreparationKit) {
                criteria | {
                    criteria.isEmpty('libraryPreparationKits')
                    criteria.libraryPreparationKits(JoinType.LEFT_OUTER_JOIN.joinTypeValue) {
                        criteria.'in'('id', singleSelectSelectorExtendedCriteria.libraryPreparationKit.id)
                    }
                }
            }
        } as List<ExternalWorkflowConfigSelector>
    }

    List<ExternalWorkflowConfigSelector> findAllSelectorsSortedByPriority(SingleSelectSelectorExtendedCriteria singleSelectSelectorExtendedCriteria) {
        if (singleSelectSelectorExtendedCriteria.anyValueSet()) {
            return findAllSelectors(singleSelectSelectorExtendedCriteria).sort()
        }
        return []
    }

    List<ExternalWorkflowConfigSelector> getAll() {
        ExternalWorkflowConfigSelector.all.sort { a, b -> String.CASE_INSENSITIVE_ORDER.compare(a.name, b.name) }
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    ExternalWorkflowConfigSelector create(CreateCommand cmd) {
        ExternalWorkflowConfigFragment fragment = new ExternalWorkflowConfigFragment(
                name: cmd.fragmentName,
                configValues: cmd.value,
        ).save(flush: true)

        ExternalWorkflowConfigSelector selector = new ExternalWorkflowConfigSelector(
                name: cmd.selectorName,
                workflows: cmd.workflows as Set,
                workflowVersions: cmd.workflowVersions as Set,
                projects: cmd.projects as Set,
                seqTypes: cmd.seqTypes as Set,
                referenceGenomes: cmd.referenceGenomes as Set,
                libraryPreparationKits: cmd.libraryPreparationKits as Set,
                externalWorkflowConfigFragment: fragment,
                selectorType: cmd.type,
                basePriority: cmd.priority,
                fineTuningPriority: 0,
        ).save(flush: true)
        return selector
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    ExternalWorkflowConfigSelector update(UpdateCommand cmd) {
        cmd.selector.workflows = cmd.workflows as Set
        cmd.selector.workflowVersions = cmd.workflowVersions as Set
        cmd.selector.projects = cmd.projects as Set
        cmd.selector.seqTypes = cmd.seqTypes as Set
        cmd.selector.referenceGenomes = cmd.referenceGenomes as Set
        cmd.selector.libraryPreparationKits = cmd.libraryPreparationKits as Set

        cmd.selector.name = cmd.selectorName
        cmd.selector.basePriority = cmd.priority
        cmd.selector.selectorType = cmd.type

        if (cmd.fragment.name != cmd.fragmentName ||
                cmd.fragment.configValues != cmd.value) {
            cmd.fragment.deprecationDate = LocalDate.now()
            ExternalWorkflowConfigFragment fragment = new ExternalWorkflowConfigFragment(
                    name: cmd.fragmentName,
                    configValues: cmd.value,
                    previous: cmd.fragment,
            ).save(flush: true)
            cmd.selector.externalWorkflowConfigFragment = fragment
        }

        cmd.selector.save(flush: true)
        return cmd.selector
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    void deprecate(ExternalWorkflowConfigFragment fragment) {
        fragment.deprecationDate = LocalDate.now()
        fragment.save(flush: true)
        fragment.selector.ifPresent { it.delete(flush: true) }
    }
}

@TupleConstructor
class MultiSelectSelectorExtendedCriteria {
    Set<Workflow> workflows
    Set<WorkflowVersion> workflowVersions
    Set<Project> projects
    Set<SeqType> seqTypes
    Set<ReferenceGenome> referenceGenomes
    Set<LibraryPreparationKit> libraryPreparationKits

    boolean anyValueSet() {
        Map p = this.properties
        p.remove("class")
        p.any { it.value != null }
    }
}

@TupleConstructor
class SingleSelectSelectorExtendedCriteria {
    Workflow workflow
    WorkflowVersion workflowVersion
    Project project
    SeqType seqType
    ReferenceGenome referenceGenome
    LibraryPreparationKit libraryPreparationKit

    boolean anyValueSet() {
        Map p = this.properties
        p.remove("class")
        p.any { it.value != null }
    }
}
