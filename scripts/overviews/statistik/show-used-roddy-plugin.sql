/*
  Show all currently used roddy plugins.
  Plugins not used anymore are not shown.
 */
SELECT DISTINCT
  config_per_project.plugin_version
FROM
  config_per_project
WHERE
  config_per_project.obsolete_date IS NULL
ORDER BY
  config_per_project.plugin_version
