/*
Show all currently used roddy plugins inclusive count of using.
Plugins not used anymore are not shown.
*/
SELECT DISTINCT
    c.plugin_version,
    COUNT(id)
FROM
     config_per_project_and_seq_type c
WHERE
    c.obsolete_date IS NULL
    AND c.individual_id is null
GROUP BY
    c.plugin_version
ORDER BY
    c.plugin_version
