#!/usr/bin/env bash

# Copyright 2011-2024 The OTP authors
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.

set -o pipefail
set -e

POSSIBLE_SEQ_TYPES=( WGS WES )


if (( $# < 4 )) ; then
   printf "USAGE:  %s -f PROJECT_FOLDER -t SEQ_TYPE[$(IFS=\| ; echo "${POSSIBLE_SEQ_TYPES[*]}")] -p PID -s SAMPLE\n" $(basename $0) >&2
   exit 1
fi


########################################################################################################################

fflag=false
wflag=false
pflag=false
sflag=false
while getopts 'f:t:p:s:' OPTION; do
	case ${OPTION} in
		f)	PROJECT_FOLDER="${OPTARG}"
				if [[ ! -d "${PROJECT_FOLDER}" ]]; then
					echo "Did not find project folder: ${PROJECT_FOLDER}"
					exit 2
				fi
				fflag=true
		;;
		t)	SEQ_TYPE="${OPTARG}"
				if [[ ! " ${POSSIBLE_SEQ_TYPES[@]} " =~ " ${SEQ_TYPE} " ]]; then
					printf "Invalid sequencing type: -w [$(IFS=\| ; echo "${POSSIBLE_SEQ_TYPES[*]}")]\n" >&2
					exit 2
				fi
				case $SEQ_TYPE in
						WGS)
								SEQ_TYPE_FOLDER=whole_genome_sequencing
						;;
						WES)
								SEQ_TYPE_FOLDER=exon_sequencing
						;;
				esac
				tflag=true
		;;
		p)	PID="${OPTARG}"
				pflag=true
		;;
		s)	SAMPLE="${OPTARG}"
				sflag=true
		;;

		\?)	print_usage
			exit 2
		;;
	esac
done

if ! $fflag ; then
    printf "PROJECT_FOLDER (-f) is missing.\n" >&2
    print_usage
    exit 3
fi

if ! $tflag ; then
    printf "SEQ_TYPE is missing: (-t [$(IFS=\| ; echo "${POSSIBLE_SEQ_TYPES[*]}")])\n" >&2
    print_usage
    exit 3
fi
if ! $pflag ; then
    printf "PID (-p) is missing.\n" >&2
    print_usage
    exit 3
fi

if ! $sflag ; then
    printf "SAMPLE (-s) is missing.\n" >&2
    print_usage
    exit 3
fi

########################################################################################################################

VBP_FOLDER=${PROJECT_FOLDER}/sequencing/${SEQ_TYPE_FOLDER}/view-by-pid
BAM2FASTQ_PP_FOLDER=${PROJECT_FOLDER}/sequencing/${SEQ_TYPE_FOLDER}/bam2fastq_per_pid
PID_FOLDER=${VBP_FOLDER}/${PID}
	if [[ ! -d "${PID_FOLDER}" ]]; then
		echo "Did not find PID folder: ${PID_FOLDER}"
		exit 2
	fi
SAMPLE_FOLDER=${PID_FOLDER}/${SAMPLE}
	if [[ ! -d "${SAMPLE_FOLDER}" ]]; then
		echo "Did not find SAMPLE folder: ${SAMPLE_FOLDER}"
		exit 2
	fi
RUN_FOLDER_ARRAY=( `ls -d ${SAMPLE_FOLDER}/paired/run*` )


########################################################################################################################

for RUN_FOLDER in ${RUN_FOLDER_ARRAY[@]}; do
  SEQ_FOLDER=${RUN_FOLDER}/sequence
  FASTQ_FILES=`ls ${SEQ_FOLDER}`
  for FASTQ_FILE in ${FASTQ_FILES[@]}; do
    FASTQ_FILE=${SEQ_FOLDER}/${FASTQ_FILE}
    if [ ! -f ${FASTQ_FILE} ]; then
      #echo "${FASTQ_FILE} is a dead link!"
      CORE_LINK=`readlink ${FASTQ_FILE}`
      #echo "TARGET: ${CORE_LINK}"
      RUN=`echo ${CORE_LINK} | perl -pe 's/.+\/core\/(run.+?)\/.+\.fastq\.gz/$1/'`
      #echo "RUN: ${RUN}"

      BAM2FASTQ_FOLDER=${BAM2FASTQ_PP_FOLDER}/${PID}/bam2fastq_biobambam/${SAMPLE}_${PID}_merged.mdup.bam_fastqs
      FASTQ_BASENAME=`basename ${FASTQ_FILE} | perl -pe 's/(.+)_R\d.fastq.gz/$1/'`
      BAM2FASTQ_RESULT_FOLDER=${BAM2FASTQ_FOLDER}/${RUN}_${FASTQ_BASENAME}/sequence

      LINKSOURCE_BASENAME=`basename ${CORE_LINK} | perl -pe 's/(.+R\d\.)(fastq\.gz)/$1sorted.$2/'`
      LINKSOURCE_BASENAME=${SAMPLE}_${PID}_merged.mdup_${RUN}_${LINKSOURCE_BASENAME}
      LINKSOURCE_PATH=${BAM2FASTQ_RESULT_FOLDER}/${LINKSOURCE_BASENAME}

      echo "WILL DO: ln -sf ${LINKSOURCE_PATH} ${CORE_LINK}"
      ln -sf ${LINKSOURCE_PATH} ${CORE_LINK}
    fi
  done
done
