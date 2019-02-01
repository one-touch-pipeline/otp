update cluster_job
set check_status = 'FINISHED'
where id in (select cj.id
             from cluster_job cj
                    join processing_step step on cj.processing_step_id = step.id
                    join process p on step.process_id = p.id
             where p.finished);

update cluster_job
set check_status = 'CHECKING'
where check_status is null;

alter table cluster_job
  alter column check_status set not null;
