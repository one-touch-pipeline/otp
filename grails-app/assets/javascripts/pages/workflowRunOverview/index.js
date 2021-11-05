/*
 * Copyright 2011-2020 The OTP authors
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
$(() => {
  const tableElement = $('table#runs');
  const buttonElements = $('.toggleButton');

  const numberOfColumns = tableElement.find('thead tr:last th').length;
  const fixedColumnsLeft = 3;
  const fixedColumnsRight = 2;

  const columnsToShow = [];
  let i = fixedColumnsLeft;
  tableElement.find('thead tr:first th[colspan]').each(function () {
    columnsToShow.push(i);
    i = i + +$(this).attr('colspan') + 1;
  });

  const columnsToHide = [];
  for (let i = fixedColumnsLeft; i < numberOfColumns - fixedColumnsRight; i++) {
    if (columnsToShow.indexOf(i) === -1) {
      columnsToHide.push(i);
    }
  }

  const dataTable = tableElement.dataTable({
    aoColumnDefs: [
      { bVisible: false, aTargets: columnsToHide }
    ],
    bPaginate: false,
    bSort: false
  });

  buttonElements.on('click', (e) => {
    buttonElements.toggle();
    for (let i = fixedColumnsLeft; i < numberOfColumns - fixedColumnsRight; i++) {
      dataTable.fnSetColumnVis(i, !dataTable.fnSettings().aoColumns[i].bVisible);
    }
  });
});
