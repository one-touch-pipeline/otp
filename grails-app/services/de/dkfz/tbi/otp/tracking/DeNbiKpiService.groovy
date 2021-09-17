/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.tracking

import grails.gorm.transactions.Transactional
import groovy.transform.ToString

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.AceseqInstance
import de.dkfz.tbi.otp.dataprocessing.IndelCallingInstance
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.AbstractSnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.ngsdata.UserProjectRole

/**
 * Service to provide different Key Performance Indicators (KPIs) for de.NBI
 */
@Transactional
class DeNbiKpiService {

    /**
     * Generate de.NBI KPI for panCan projects in the given date range.
     *
     * @param from date
     * @param to date
     * @return DeNbiKpi
     */
    DeNbiKpi getPanCanKpi(Date from, Date to) {
        List<String> panCanProjects = RoddyBamFile.createCriteria().list {
            lt("dateCreated", to)
            gt("dateCreated", from)
            workPackage {
                seqType {
                    ne("name", "RNA")
                    eq("singleCell", false)
                }
            }
            projections {
                workPackage {
                    sample {
                        individual {
                            project {
                                property('name')
                            }
                        }
                    }
                }
            }
        } as List<String>

        return new DeNbiKpi("PanCan", panCanProjects.size(), panCanProjects.unique())
    }

    /**
     * Generate de.NBI KPI for rna alignment projects in the given date range.
     *
     * @param from date
     * @param to date
     * @return DeNbiKpi
     */
    DeNbiKpi getRnaAlignmentKpi(Date from, Date to) {
        List<String> rnaAlignmentProjects = RnaRoddyBamFile.createCriteria().list {
            lt("dateCreated", to)
            gt("dateCreated", from)
            workPackage {
                seqType {
                    eq("singleCell", false)
                    eq("name", "RNA")
                }
            }
            projections {
                workPackage {
                    sample {
                        individual {
                            project {
                                property('name')
                            }
                        }
                    }
                }
            }
        } as List<String>

        return new DeNbiKpi("Rna alignment", rnaAlignmentProjects.size(), rnaAlignmentProjects.unique())
    }

    /**
     * Generate de.NBI KPI for cellRanger projects in the given date range.
     *
     * @param from date
     * @param to date
     * @return DeNbiKpi
     */
    DeNbiKpi getCellRangerKpi(Date from, Date to) {
        List<String> cellRangerProjects = AbstractMergedBamFile.createCriteria().list {
            lt("dateCreated", to)
            gt("dateCreated", from)
            workPackage {
                seqType {
                    eq("singleCell", true)
                }
            }
            projections {
                workPackage {
                    sample {
                        individual {
                            project {
                                property('name')
                            }
                        }
                    }
                }
            }
        } as List<String>

        return new DeNbiKpi("CellRanger", cellRangerProjects.size(), cellRangerProjects.unique())
    }

    /**
     * Generate de.NBI KPI for snv calling projects in the given date range.
     *
     * @param from date
     * @param to date
     * @return DeNbiKpi
     */
    DeNbiKpi getSnvCallingKpi(Date from, Date to) {
        Closure searchClosure = getBamFilePairAnalysisProjectSearchClosure(from, to)
        List<String> snvCallingProjects = AbstractSnvCallingInstance.createCriteria().list(searchClosure) as List<String>
        return new DeNbiKpi("Snv", snvCallingProjects.size(), snvCallingProjects.unique())
    }

    /**
     * Generate de.NBI KPI for indel projects in the given date range.
     *
     * @param from date
     * @param to date
     * @return DeNbiKpi
     */
    DeNbiKpi getIndelKpi(Date from, Date to) {
        Closure searchClosure = getBamFilePairAnalysisProjectSearchClosure(from, to)
        List<String> indelProjects = IndelCallingInstance.createCriteria().list(searchClosure) as List<String>
        return new DeNbiKpi("Indel", indelProjects.size(), indelProjects.unique())
    }

    /**
     * Generate de.NBI KPI for sophia projects in the given date range.
     *
     * @param from date
     * @param to date
     * @return DeNbiKpi
     */
    DeNbiKpi getSophiaKpi(Date from, Date to) {
        Closure searchClosure = getBamFilePairAnalysisProjectSearchClosure(from, to)
        List<String> sophiaProjects = SophiaInstance.createCriteria().list(searchClosure) as List<String>
        return new DeNbiKpi("Sophia", sophiaProjects.size(), sophiaProjects.unique())
    }

    /**
     * Generate de.NBI KPI for aceseq projects in the given date range.
     *
     * @param from date
     * @param to date
     * @return DeNbiKpi
     */
    DeNbiKpi getAceseqKpi(Date from, Date to) {
        Closure searchClosure = getBamFilePairAnalysisProjectSearchClosure(from, to)
        List<String> aceseqProjects = AceseqInstance.createCriteria().list(searchClosure) as List<String>
        return new DeNbiKpi("Aceseq", aceseqProjects.size(), aceseqProjects.unique())
    }

    /**
     * Generate de.NBI KPI for runYapsa projects in the given date range.
     *
     * @param from date
     * @param to date
     * @return DeNbiKpi
     */
    DeNbiKpi getRunYapsaKpi(Date from, Date to) {
        Closure searchClosure = getBamFilePairAnalysisProjectSearchClosure(from, to)
        List<String> runYapsaProjects = RunYapsaInstance.createCriteria().list(searchClosure) as List<String>
        return new DeNbiKpi("runYapsa", runYapsaProjects.size(), runYapsaProjects.unique())
    }

    /**
     * Generate de.NBI KPI for cluster job projects in the given date range.
     *
     * @param from date
     * @param to date
     * @return DeNbiKpi
     */
    DeNbiKpi getClusterJobKpi(Date from, Date to) {
        List<String> clusterJobProjects = ClusterJob.createCriteria().list {
            lt("dateCreated", to)
            gt("dateCreated", from)
        }.collect { ClusterJob clusterJob ->
            if (clusterJob.oldSystem) {
                return clusterJob.individual.project.name
            } else {
                return clusterJob.workflowStep.workflowRun.project.name
            }
        }

        return new DeNbiKpi("otp cluster jobs", clusterJobProjects.size(), clusterJobProjects.unique())
    }

    /**
     * Generate a de.NBI KPI which is defined as sum of other de.NBI KPIs like `roddy` or `otp workflow`.
     *
     * @param kpiName name of the sum
     * @param kpiList de.NBI KPIs
     * @return DeNbiKpi
     */
    DeNbiKpi generateSumKpi(String kpiName, DeNbiKpi... kpiList) {
        return new DeNbiKpi(kpiName, kpiList*.count.sum() as int, kpiList*.projects.flatten().unique() as List<String>)
    }

    /**
     * Count the number of users in the given projects. Only enabled users are counted.
     *
     * @param projectNames
     * @return number of users
     */
    int countUsersInProjects(List<String> projectNames) {
        if (!projectNames) {
            return 0
        }

        return UserProjectRole.createCriteria().list {
            eq("enabled", true)
            user {
                eq("enabled", true)
            }
            project {
                'in'('name', projectNames)
            }
        }*.user.unique().size()
    }

    /**
     * Generate a hibernate search criteria closure to get a list of all
     * projects of a BamFilePairAnalysis for a given date range.
     *
     * @param from Date
     * @param to Date
     * @return hibernate Criteria
     */
    private Closure getBamFilePairAnalysisProjectSearchClosure(Date from, Date to) {
        return {
            lt("dateCreated", to)
            gt("dateCreated", from)
            projections {
                samplePair {
                    mergingWorkPackage1 {
                        sample {
                            individual {
                                project {
                                    property('name')
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Key Performance Indicator (KPI) for de.NBI
 */
@ToString
class DeNbiKpi {
    String name
    int count
    int projectCount
    List<String> projects

    DeNbiKpi(String name, int count, List<String> projects) {
        this.name = name
        this.count = count
        this.projects = projects
        this.projectCount = projects.size()
    }
}
