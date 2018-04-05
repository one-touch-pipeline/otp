import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

/**
 * Create Statistic about SeqType and Projects for project groups
 */

"""
""".split('\n')*.trim().findAll().each {
    ProjectGroup projectGroup = CollectionUtils.exactlyOneElement(ProjectGroup.findAllByName(it))
    List list = SeqTrack.createCriteria().list {
        projections {
            groupProperty('seqType')
            sample {
                individual {
                    project {
                        eq('projectGroup', projectGroup)
                        countDistinct('id')
                    }
                    countDistinct('id')
                }
                countDistinct('id')
            }
        }
    }.sort {
        it[0].alias
    }.collect {
        "    ${it[0]}: Projecte: ${it[1]}, Individuals: ${it[2]}, Samples: ${it[3]} "
    }
    println "${projectGroup}: ${Project.countByProjectGroup(projectGroup)} Projekte"
    println list.join('\n')
}
''
