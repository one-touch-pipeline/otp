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

## Workflows & Sequencing Types

Currently, the following bioinformatic workflows are supported by OTP:

  * [Roddy Alignment Workflow](https://github.com/DKFZ-ODCF/AlignmentAndQCWorkflows/)
  * [Roddy RNA-seq Alignment Workflow](https://github.com/DKFZ-ODCF/RNAseqWorkflow)
  * [Roddy SNV Calling Workflow](https://github.com/DKFZ-ODCF/SNVCallingWorkflow)
  * [Roddy Sophia Structural Variation Calling Workflow](https://github.com/DKFZ-ODCF/SophiaWorkflow)
  * [Roddy InDel Calling Workflow](https://github.com/DKFZ-ODCF/IndelCallingWorkflow)
  * [Roddy ACEseq CNV Calling workflow](https://github.com/DKFZ-ODCF/ACEseqWorkflow)
  * [YAPSA Mutational Signature Workflow](https://github.com/eilslabs/YAPSA)

A CellRanger single-cell RNA-seq workflow is in preparation.

Each of these workflows is only supported for specific sequencing types:

| Sequencing Type | Description  | Display Name (shown in GUI)  | Workflows  |
|:----------------|:-------------|:-----------------------------|:-----------|
| ChIP Seq                                            | ChIPseq DNA sequencing (antibody and target needed for import)                                 | ChIP     | Alignment                                             |
| EXON                                                | Whole exome sequencing (please provide the target files/enrichment kit as early as possible)   | EXOME    | Alignment, SNV, INDEL, Mutational signatures          |
| RNA                                                 | RNA-seq                                                                                        | RNA      | Alignment                                             |
| WHOLE_GENOME                                        | Whole genome sequencing                                                                        | WGS      | Alignment, SNV, INDEL, SV, CNV, Mutational signatures |
| WHOLE_GENOME_BISULFITE                              | Whole genome bisulfite sequencing                                                              | WGBS     | Alignment                                             |
| WHOLE_GENOME_BISULFITE_TAGMENTATION                 | Whole genome bisulfite tagmantation sequencing                                                 | WGBS_TAG | Alignment                                             |
