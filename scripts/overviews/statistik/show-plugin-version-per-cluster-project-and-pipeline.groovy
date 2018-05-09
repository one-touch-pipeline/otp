import de.dkfz.tbi.otp.dataprocessing.*

/**
 * Show the used plugin version per cluster, project and pipeline.
 *
 * Only the last calling for a sample is taken into account.
 */

String sql = """
select
    pipeline.name, 
    project.name,
    realm.jobScheduler,
    config.pluginVersion, 
    count(analysis.id) as count
from
    BamFilePairAnalysis analysis
    join analysis.samplePair.mergingWorkPackage1.sample.individual.project project
    join analysis.config config
    join config.pipeline pipeline,
    ClusterJob job
    join job.realm realm,
    ProcessParameter processParameter
where
    cast(processParameter.value as long) = analysis.id
    and processParameter.process = job.processingStep.process
    and analysis.withdrawn = false
    and pipeline.name != 'OTP_SNV'
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
group by 
    pipeline.name, 
    project.name,
    realm.jobScheduler,
    config.pluginVersion

"""

println IndelCallingInstance.executeQuery(sql).groupBy(
        [
                { it[0] },
                { it[1] },
                { it[2] },
                { it[3] },
        ]
).collect { key1, map1 ->
    "${key1}\n" +
            map1.collect { key2, map2 ->
                " ${key2}\n" +
                        map2.collect { key3, map3 ->
                            "   ${key3}\n" +
                                    map3.collect { key4, list4 ->
                                        "     ${key4} : ${list4[0][4]}"
                                    }.sort().join('\n')
                        }.sort().join('\n')
            }.sort().join('\n')
}.sort().join('\n\n')
