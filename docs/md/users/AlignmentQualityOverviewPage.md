<!--
  ~ Copyright 2011-2019 The OTP authors
  ~
  ~ Permission is hereby granted, free of charge, to any person obtaining a copy
  ~ of this software and associated documentation files (the "Software"), to deal
  ~ in the Software without restriction, including without limitation the rights
  ~ to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  ~ copies of the Software, and to permit persons to whom the Software is
  ~ furnished to do so, subject to the following conditions:
  ~
  ~ The above copyright notice and this permission notice shall be included in all
  ~ copies or substantial portions of the Software.
  ~
  ~ THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  ~ IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  ~ FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  ~ AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  ~ LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  ~ OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  ~ SOFTWARE.
  -->

[Back to index](index.md)

## Alignment Quality Overview

The "Alignment Quality Overview", available via "ALIGNMENT QUALITY CONTROL" in the "RESULTS" menu option displays the basic alignment quality results. Dependent on the actual sequencing type, the shown tables may differ.

### WGS/WES/WGBS Data

Note that many of the statistics only use "QC-bases". QC-bases only include bases from aligned reads not marked as duplicates. Please refer to the [workflow documentation](https://github.com/DKFZ-ODCF/AlignmentAndQCWorkflows/wiki/3.-Results#qc-bases) for a detailed definition of QC-bases.

The following table explains the columns of the alignment overview table for WGS, WES, and WGBS data displayed in OTP:

| Value | Description |
|:------|:------------|
| [QC Status](https://github.com/DKFZ-ODCF/AlignmentAndQCWorkflows/wiki/3.-Results#FastQC) | Classification into QC "passed" or "failed". The actual classification also depends on the quality thresholds configured for your project. Please refer to "PROJECT" / "PROJECT CONFIG for details about QC threshold in your project. |
| [Cov. w/o N](https://github.com/DKFZ-ODCF/AlignmentAndQCWorkflows/wiki/3.-Results#genome_wo_n_coverage_qc_bases) | Ratio of mapped QC-bases and the genome size, only counting {A,C,G,T} bases |
| ChrX Cov. w/o N | The "Cov. w/o N" value only for the X-chromosome. |
| ChrY Cov. w/o N | The "Cov. w/o N" value only for the Y-chromosome. |
| Library Prep Kit | Name associated in OTP with a specific set of adapters and/or target region set (for WES data).  |
| Mapped Reads % | `#mappedReads / #allReads * 100`; numbers according to [samtools flagstat](https://www.htslib.org/doc/samtools.html) |
| Duplicates % | `#duplicates / #allReads * 100`; numbers according to [samtools flagstat](https://www.htslib.org/doc/samtools.html) |
| Properly Paired % | `#properlyPairedReads / #readsPairedInSequencing * 100`; numbers according to [samtools flagstat](https://www.htslib.org/doc/samtools.html) |
| Single % | `#singletonReads / #allReads * 100`; numbers according to [samtools flagstat](https://www.htslib.org/doc/samtools.html) |
| Insert Size Median | Median of the insert size distribution |
| Diff Chrom % | `#matesMappedToDifferentChromosomes / #allReads * 100` |
| Alignment | Alignment pipeline; currently only "bwa mem". |

### RNA-seq Data

For RNA-seq data quality values are shown that better account for the specific QC-requirements of this sequencing type. The workflow uses the [RNA-SeQC](https://github.com/broadinstitute/rnaseqc) tool, which is extensively documented [here](https://github.com/broadinstitute/rnaseqc/blob/master/Metrics.md).

 Here is a list of column names from the OTP overview table with explanations:

| Value | Description |
|:-------|------|
| QC Status | Classification into QC "passed" or "failed". The actual classification also depends on the quality thresholds configured for your project. Please refer to "PROJECT" / "PROJECT CONFIG for details about QC threshold in your project. |
| Total Read Counter | Total number of QC-passing reads according to [samtools flagstat](https://www.htslib.org/doc/samtools.html). |
| Duplicates % | Percentage of reads marked as duplicates according to [samtools flagstat](https://www.htslib.org/doc/samtools.html). |
| 3p Norm | according to [RNA-SeQC](https://github.com/broadinstitute/rnaseqc). |
| 5p Norm | according to [RNA-SeQC](https://github.com/broadinstitute/rnaseqc). |
| Chimeric Pairs | according to [RNA-SeQC](https://github.com/broadinstitute/rnaseqc). |
| Duplication Rate of Mapped | according to [RNA-SeQC](https://github.com/broadinstitute/rnaseqc). |
| End 1 Sense | according to [RNA-SeQC](https://github.com/broadinstitute/rnaseqc). |
| End 2 Sense | according to [RNA-SeQC](https://github.com/broadinstitute/rnaseqc). |
| Estimated Library Size | according to [RNA-SeQC](https://github.com/broadinstitute/rnaseqc). |
| Exonic Rate | according to [RNA-SeQC](https://github.com/broadinstitute/rnaseqc). |
| Expression Profiling Efficiency | according to [RNA-SeQC](https://github.com/broadinstitute/rnaseqc). |
| Genes Detected | according to [RNA-SeQC](https://github.com/broadinstitute/rnaseqc). |
| Intergenic Rate | according to [RNA-SeQC](https://github.com/broadinstitute/rnaseqc). |
| Intragenic Rate | according to [RNA-SeQC](https://github.com/broadinstitute/rnaseqc). |
| Mapped | according to [RNA-SeQC](https://github.com/broadinstitute/rnaseqc). |
| Mapped Unique | according to [RNA-SeQC](https://github.com/broadinstitute/rnaseqc). |
| Mapped Unique Rate of Total | according to [RNA-SeQC](https://github.com/broadinstitute/rnaseqc). |
| Mapping Rate | according to [RNA-SeQC](https://github.com/broadinstitute/rnaseqc). |
| Mean CV | according to [RNA-SeQC](https://github.com/broadinstitute/rnaseqc). |
| Unique Rate of Mapped | according to [RNA-SeQC](https://github.com/broadinstitute/rnaseqc).
| rRNA Rate | according to [RNA-SeQC](https://github.com/broadinstitute/rnaseqc). |
| Library Prep Kit | Name associated in OTP with a specific set of adapters. |
