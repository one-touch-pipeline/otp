import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.ngsdata.*

/**
 * Show all used roddy plugins per pipeline and project.
 *
 * Only the last calling for a sample is taken into account.
 */

String sql = """
select
    analysis
from
    BamFilePairAnalysis analysis
where
    analysis.withdrawn = false
    and analysis.config.pipeline.name != 'OTP_SNV'
and
    analysis.id = (
        select
            max(id)
        from
            BamFilePairAnalysis analysis2
        where
            analysis2.samplePair = analysis.samplePair
            and analysis2.config.pipeline = analysis.config.pipeline
    )
"""

println IndelCallingInstance.executeQuery(sql).groupBy(
        [
                { BamFilePairAnalysis analysis ->
                    analysis.config.pipeline
                },
                { BamFilePairAnalysis analysis ->
                    analysis.project
                },
                { BamFilePairAnalysis analysis ->
                    analysis.seqType
                },
                { BamFilePairAnalysis analysis ->
                    analysis.config.pluginVersion
                }

        ]
).collect { Pipeline pipeline, Map map1 ->
    "${pipeline}\n" +
            map1.collect { Project project, Map map2 ->
                "    ${project}\n" +
                        map2.collect { SeqType seqType, Map map3 ->
                            "        ${seqType.displayNameWithLibraryLayout} (${RoddyWorkflowConfig.getLatestForProject(project, seqType, pipeline)?.pluginVersion})\n" +
                                    map3.collect { String pluginVersion, List list ->
                                        "            ${pluginVersion}: ${list.size()}".toString()
                                    }.sort().join('\n')
                        }.sort().join('\n')
            }.sort().join('\n\n')
}.sort().join('\n\n\n')
