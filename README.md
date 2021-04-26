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

# One Touch Pipeline

One Touch Pipeline (OTP) is a data management platform for running bioinformatics pipelines in a high-throughput setting, and 
for organising the resulting data and metadata.

It started as a collaborative in-house tool at the German Cancer Research Center and University of Heidelberg,
to coordinate the thousands of genomes and petabytes of data handled there.
As both are publicly funded bodies, the code is made open source under the philosophy of
["public money - public code"](https://publiccode.eu/).

We, the OTP team, hope that it will help you manage your own petabytes of genome data!

## Why OTP?

We made OTP to help us manage _routine_ analysis in a scalable, automated way.
We process thousands of cancer genomics samples with multiple pipelines,
across hundreds of projects, in varying combinations and with different parameters.
More than any person could manage by hand.

OTP automates the complete digital process from import of raw sequence data,
via alignment and identification of genomic events,
to notifying project members their analyses are finished.  
Once you've set up a project configuration to your liking,
all it takes is the "one touch" of importing some FASTQ files, and OTP takes care of the rest.  
(Disclaimer: OTP is a powerful tool, but no tool can protect you from _all_ the chaos
of handling hundreds of projects. Some hand-holding will always be required.)

Our goals are:

* support human data managers with as much automation as possible
* reduce time until sequences can be analyzed by bioinformatics experts, by executing routine operations more reliably and quickly
* store all information in one organised system

In achievement of these goals OTP provides, among others:

* automatic import of FASTQ and their metadata
* automated processing and analyses:
  * BWA alignment
  * InDel calling, SNV calling,
  * identifying Copy Number Variations (CNV) and Structural Variations (SV)
  * mutational signature analysis
  * processing single-cell data is in preparation
* basic error recovery, and error reporting when it can't.
* display of, and gating on, important quality control values and analysis results
* user- and access management at the project level

If you want to learn more, you may also be interested in our publication:  
**Reisinger et al. "OTP: An automatized system for managing and processing NGS data" (2017)**  
Journal of Biotechnology, vol 261, p53-62  
DOI: [10.1016/j.jbiotec.2017.08.006](https://doi.org/10.1016/j.jbiotec.2017.08.006)

## Contributing

We're still getting a feel for how to develop OTP in the open, after almost a decade of in-house development. Please be patient while we figure things out!

Some things on our current to-do list:

* run our CI/CD in the open
* rebuild our review process with tooling accessible to the wider public.

## License

This project is licensed under the [MIT license](LICENSE).

Some bundled dependencies included in this repository have their own license, please see the [LICENSE](LICENSE) file for details.

### ... of your contributions

Unless you explicitly state otherwise, any contribution intentionally submitted
for inclusion in OTP by you, shall be licensed as MIT, without any additional
terms or conditions.

## Our Partners

<table><tr>
<td><a href="https://www.denbi.de/"><img src="https://otp.dkfz.de/otp/assets/non-free/denbi-4d5d1416811ca00fcf89cf71836f0e55.png" alt="de.NBI" width="300" align="left"></a></td>
<td>The development of OTP is supported by the 
<a href="https://www.denbi.de/">German Network for Bioinformatic Infrastructure (de.NBI)</a>. By completing <a href="https://www.surveymonkey.de/r/denbi-service?sc=hd-hub&tool=otp">this very short survey</a> you support our efforts to improve this tool.
<strong>Your opinion matters!</strong></td>
</tr></table>

In addition to de.NBI, thanks also go to our other partners:

[<img src="https://otp.dkfz.de/otp/assets/non-free/dktk.jpg" alt="DKTK" width="190"/>](https://www.dkfz.de/en/dktk/) &nbsp; &nbsp; &nbsp; &nbsp;
[<img src="https://otp.dkfz.de/otp/assets/non-free/hipo.png" alt="HIPO" width="150"/>](https://www.hipo-heidelberg.org/hipo2/) &nbsp; &nbsp; &nbsp; &nbsp;
[<img src="https://otp.dkfz.de/otp/assets/non-free/nct.jpg" alt="NCT" width="360"/>](https://www.nct-heidelberg.de/) &nbsp; &nbsp; &nbsp; &nbsp;
[<img src="https://otp.dkfz.de/otp/assets/non-free/kitz.png" alt="KiTZ" width="200"/>](https://www.kitz-heidelberg.de/) &nbsp; &nbsp; &nbsp; &nbsp;
[<img src="https://otp.dkfz.de/otp/assets/non-free/logoDeep.png" alt="DEEP" width="100"/>](http://www.deutsches-epigenom-programm.de/) &nbsp; &nbsp; &nbsp; &nbsp;
[<img src="https://otp.dkfz.de/otp/assets/non-free/ihec.png" alt="IHEC" width="220"/>](http://ihec-epigenomes.org/) &nbsp; &nbsp; &nbsp; &nbsp;
[<img src="https://daco.icgc.org/assets/site/images/ICGC_Logo.svg" alt="CACO" width="600"/>](https://daco.icgc.org/)
