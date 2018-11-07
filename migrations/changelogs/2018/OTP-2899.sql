-- The attributes of the QA Object do not match with the columns provided in the QA output file.
-- The list of available values in the file takes precedence over the list in the domain object.
-- The QA Object has to be adapted to the output file.

-- in Domain but not in QA File:
ALTER TABLE abstract_quality_assessment DROP COLUMN q30bases_in_sample_index;

-- in QA File but not in Domain:
ALTER TABLE abstract_quality_assessment
    ADD reads_mapped_to_genome              DOUBLE PRECISION,
    ADD reads_mapped_confidently_to_genome  DOUBLE PRECISION,
    ADD reads_mapped_antisense_to_gene      DOUBLE PRECISION;