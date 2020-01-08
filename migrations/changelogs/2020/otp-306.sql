ALTER TABLE abstract_merging_work_package
ADD COLUMN status varchar(255);

UPDATE abstract_merging_work_package
SET status = 'UNSET'
WHERE class = 'de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage'
;

ALTER TABLE abstract_merging_work_package
ADD COLUMN informed timestamp WITH TIME ZONE;
