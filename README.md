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

* get our issue tracker outside the firewall (not as easy as it sounds, given the personal nature of most genomic data)
* run our CI/CD in the open
* rebuild our review process with tooling accessible to the wider public.

## License

This project is licensed under the [MIT license](LICENSE).

Some bundled dependencies included in this repository have their own license, please see the [LICENSE](LICENSE) file for details.

### ... of your contributions

Unless you explicitly state otherwise, any contribution intentionally submitted
for inclusion in OTP by you, shall be licensed as MIT, without any additional
terms or conditions.

