/*
 * Copyright 2011-2022 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
$(document).ready(() => {
  $('#select-bulk').change(function () {
    const table = $('#selectSamplesTable').DataTable();
    const { checked } = this;
    table.rows().eq(0).each((rowIdx) => {
      table.cell(rowIdx, 0).node().children[0].checked = checked;
    });
  });
});

// eslint-disable-next-line no-unused-vars
function downloadCsvOfSelectedSamples() {
  const table = $('#selectSamplesTable').DataTable();

  const selectedRows = table.rows().data();

  if (selectedRows.length) {
    const selectedSamples = [];

    for (let i = 0; i < selectedRows.length; i++) {
      selectedSamples.push({
        individualName: selectedRows[i].individual,
        sampleTypeName: selectedRows[i].sampleType,
        seqTypeName: selectedRows[i].seqTypeDisplayName,
        sequencingReadType: selectedRows[i].sequencingReadType,
        singleCell: selectedRows[i].singleCellDisplayName
      });
    }

    let filename = '';

    fetch($.otp.createLink({
      controller: 'egaSubmission',
      action: 'selectSamplesCsvDownload'
    }), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        selectedSamples
      })
    }).then((response) => {
      // eslint-disable-next-line prefer-destructuring
      filename = response.headers.get('content-disposition').split('filename=')[1];
      return response.blob();
    }).then((blob) => {
      const link = document.createElement('a');
      link.style.display = 'none';
      link.href = window.URL.createObjectURL(blob);
      link.download = filename;
      link.click();
    }).catch(() => {
      $.otp.warningMessage('error occurred while generating the csv export file');
    });
  } else {
    $.otp.infoMessage('please select at least one sample before exporting.');
  }
}

// eslint-disable-next-line no-unused-vars
function uploadCsvOfSelectedSamples() {
  const { files } = $('#file').get(0);

  if (files.length) {
    const fileData = new FormData();
    fileData.append('file', files[0]);

    fetch($.otp.createLink({
      controller: 'egaSubmission',
      action: 'selectSamplesCsvUpload'
    }), {
      method: 'POST',
      body: fileData
    }).then((response) => response.json()
    ).then((json) => {
      const table = $('#selectSamplesTable').DataTable();

      const header = ['Individual', 'Sequence Type Name', 'Sequencing Read Type', 'Single Cell', 'Sample Type'];
      const foundSamples = new Set([]);

      table.rows().eq(0).each((rowIdx) => {
        table.cell(rowIdx, 0).node().children[0].checked = false; // always new selection
        json.forEach((sample) => {
          if (table.cell(rowIdx, 1).data() === sample[header[0]] &&
            table.cell(rowIdx, 2).data() === sample[header[1]] &&
            table.cell(rowIdx, 3).data() === sample[header[2]] &&
            table.cell(rowIdx, 4).data() === sample[header[3]] &&
            table.cell(rowIdx, 5).data() === sample[header[4]]) {
            foundSamples.add(sample);
            table.cell(rowIdx, 0).node().children[0].checked = true;
          }
        });
      });

      if (json.length !== foundSamples.size) {
        const difference = json.map((sample, index) => {
          if (![...foundSamples].includes(sample)) {
            return `${index + 1} `;
          }
          return undefined;
        }).filter((element) => element);
        const prefix = 'csv file uploaded with warnings:<br />';
        $.otp.warningMessage(`${prefix} following rows of uploaded file can not be found in table: ${difference}`);
      } else {
        $.otp.infoMessage('csv file successfully uploaded');
      }
    }).catch(() => {
      $.otp.warningMessage('error occurred while uploading the csv file');
    });
  } else {
    $.otp.infoMessage('please select a csv file to upload.');
  }
}
