#!/bin/bash
#
# Copyright 2011-2026 The OTP authors
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
#

# Measures the write performance of the storage the docker daemon of the job runs on.
#
# The two startup phases that differ the most between runners, extracting the images and applying
# the dev dump, are both write bound. The benchmark therefore writes the same way those phases do:
# - into the writable layer of a container, which is where 'docker pull' extracts the image layers
# - into a named volume, which is where postgres keeps its data directory
# - as many small files, which is the access pattern of extracting an image layer
#
# The results are written to `logs/io-benchmark.log`, which is part of the artifacts of the job.
# Run the pipeline once with a single cypress job and once with all of them, to tell storage that is
# slow in general from storage that only saturates under the parallel load of the whole matrix.

set -e -o pipefail

# A small image, so that pulling it does not dominate the runtime of the benchmark. It comes from
# docker hub, which limits anonymous pulls, so point the variable somewhere else if that becomes a
# problem.
BENCHMARK_IMAGE="${BENCHMARK_IMAGE:-alpine:3.22}"
# the job id keeps the volume unique, should a job ever share its docker daemon with another one
BENCHMARK_VOLUME="otp-io-benchmark-${CI_JOB_ID:-local}"
LOG_FILE=logs/io-benchmark.log

trap 'docker volume rm --force "${BENCHMARK_VOLUME}" > /dev/null 2>&1 || true' EXIT

# Runs one measurement in a container and reports the wall clock time it took.
# The time is taken outside of the container, since the date of busybox has no sub second
# resolution. It therefore also covers creating and removing the container, which is why the
# baseline below measures a container that does nothing at all.
measure() {
    local description="$1"
    local script="$2"
    local start end

    printf '\n--- %s\n' "${description}" >> "${LOG_FILE}"

    start=$(date +%s%3N)
    docker run --rm --volume "${BENCHMARK_VOLUME}:/volume" "${BENCHMARK_IMAGE}" sh -c "${script}" >> "${LOG_FILE}" 2>&1 \
        || echo 'this measurement failed, see the output above' >> "${LOG_FILE}"
    end=$(date +%s%3N)

    printf '=> %s ms\n' "$((end - start))" >> "${LOG_FILE}"
}

{
    echo "runner:      ${CI_RUNNER_DESCRIPTION:-unknown}"
    echo "job:         ${CI_JOB_ID:-local}, suite '${SUITE:-unknown}', concurrent id ${CI_CONCURRENT_ID:-unknown}"
    echo "parallelism: job ${CI_NODE_INDEX:-1} of ${CI_NODE_TOTAL:-1} of this matrix"
    echo "image:       ${BENCHMARK_IMAGE}"
} > "${LOG_FILE}"

printf '\n--- pull the benchmark image\n' >> "${LOG_FILE}"
pullStart=$(date +%s%3N)
docker pull --quiet "${BENCHMARK_IMAGE}" >> "${LOG_FILE}" 2>&1
printf '=> %s ms\n' "$(($(date +%s%3N) - pullStart))" >> "${LOG_FILE}"

docker volume create "${BENCHMARK_VOLUME}" > /dev/null

measure 'baseline, a container that does nothing' \
    'true'

# `conv=fsync` makes `dd` wait until the data reached the disk, so that the rate it reports is the one
# of the storage and not the one of the page cache
measure 'write 512MB into the writable container layer, the way an image is extracted' \
    'dd if=/dev/zero of=/layerFile bs=1M count=512 conv=fsync
     rm -f /layerFile'

measure 'write 512MB into a named volume, the way postgres writes its data directory' \
    'dd if=/dev/zero of=/volume/volumeFile bs=1M count=512 conv=fsync
     rm -f /volume/volumeFile'

# The throughput above says nothing about how long a single write takes to be acknowledged, which is
# what a database commit waits for. `oflag=direct` bypasses the page cache, so every one of the 4k
# blocks goes to the storage on its own and the rate dd reports is the rate of small synchronous
# writes. This is the number that decides how fast the dev dump can be imported.
measure 'write 1000 blocks of 4k synchronously, the way a database commits' \
    'dd if=/dev/zero of=/volume/syncFile bs=4k count=1000 oflag=direct conv=fsync
     rm -f /volume/syncFile'

# many small files stress the metadata handling of the filesystem instead of its throughput, which
# is what makes extracting an image layer slow
measure 'create and sync 30000 small files in the writable container layer' \
    'set -e
     mkdir /layerDirectory
     i=0
     while [ ${i} -lt 30000 ]
     do
         echo "content of file ${i}" > "/layerDirectory/file${i}"
         i=$((i + 1))
     done
     sync
     rm -rf /layerDirectory'

cat "${LOG_FILE}"
