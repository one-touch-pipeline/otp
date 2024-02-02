<!--
  ~ Copyright 2011-2024 The OTP authors
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

## Workflows & Sequencing Types

Currently, OTP supports the following bioinformatic workflows:

  * [Roddy Alignment Workflow](https://github.com/DKFZ-ODCF/AlignmentAndQCWorkflows/)
  * [Roddy RNA-seq Alignment Workflow](https://github.com/DKFZ-ODCF/RNAseqWorkflow)
  * [Roddy SNV Calling Workflow](https://github.com/DKFZ-ODCF/SNVCallingWorkflow)
  * [Roddy Sophia Structural Variation Calling Workflow](https://github.com/DKFZ-ODCF/SophiaWorkflow)
  * [Roddy InDel Calling Workflow](https://github.com/DKFZ-ODCF/IndelCallingWorkflow)
  * [Roddy ACEseq CNV Calling workflow](https://github.com/DKFZ-ODCF/ACEseqWorkflow)
  * [YAPSA Mutational Signature Workflow](https://bioconductor.org/packages/release/bioc/html/YAPSA.html)
  * [CellRanger Single-Cell RNA-seq Workflow](https://support.10xgenomics.com/single-cell-gene-expression/software/pipelines/latest/what-is-cell-ranger)

These workflows run a number of quality control steps and report indicators that should support you in judging the quality of your data. Please refer to the documentation of the workflows for more information. For questions please contact the workflow's authors.

The workflows mostly only support paired-end data, except for the bulk RNA-seq workflow, which also supports single-end sequencing data. Furthermore, OTP supports each of these workflows only for specific sequencing types:

| Sequencing Type | Comment      | Display Name (shown in GUI)  | Workflows  |
|:----------------|:-------------|:-----------------------------|:-----------|
| ChIP-seq DNA sequencing                        | antibody target is needed            | ChIP      | Alignment                                             |
| Whole exome sequencing                         | target-file/enrichment kit is needed | EXOME     | Alignment, SNV, INDEL, Mutational signatures          |
| bulk RNA-seq                                   |                                      | RNA       | RNA-seq                                               |
| single-cell RNA-seq                            |                                      | 10x_scRNA | CellRanger                                            |
| Whole genome sequencing                        |                                      | WGS       | Alignment, SNV, INDEL, SV, CNV, Mutational signatures |
| Whole genome bisulfite sequencing              |                                      | WGBS      | Alignment                                             |
| Whole genome bisulfite tagmentation sequencing |                                      | WGBS_TAG  | Alignment                                             |
