/*
 * Copyright 2011-2024 The OTP authors
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
  'use strict';

  function applyStickyHeader() {
    $('table[class="apply-sticky-header"]').each(function () {
      if ($(this).find('thead').length > 0 && $(this).find('th').length > 0) {
        // Clone <thead>
        const $w = $(window);
        const $t = $(this);
        const $thead = $t.find('thead').clone();
        const $col = $t.find('thead, tbody').clone();

        // Remove class, that header is not applied multiple times
        $t.removeClass('apply-sticky-header');

        // Add class, remove margins, reset width and wrap table
        $t.addClass('sticky-enabled')
          .css({
            margin: 0,
            width: '100%'
          }).wrap('<div class="sticky-wrap" />');

        if ($t.hasClass('overflow-y')) {
          $t.removeClass('overflow-y').parent().addClass('overflow-y');
        }

        // Create new sticky table head (basic)
        $t.after('<table class="sticky-thead" />');

        // If <tbody> contains <th>, then we create sticky column and intersect (advanced)
        if ($t.find('tbody th').length > 0) {
          $t.parent().parent().after('<table class="sticky-col" /><table class="sticky-intersect" />');
        }

        // Create shorthand for things
        const $stickyHead = $(this).siblings('.sticky-thead');
        const $stickyCol = $(this).parent().parent().siblings('.sticky-col');
        const $stickyInsct = $(this).parent().parent().siblings('.sticky-intersect');
        const $stickyWrap = $(this).parent('.sticky-wrap');

        $stickyHead.append($thead);

        $stickyCol
          .append($col)
          .find('thead tr').find('th:gt(0)').remove()
          .end()
          .end()
          .find('tbody td')
          .remove();

        $stickyInsct
          .append($col.clone())
          .find('thead tr').find('th:gt(0)').remove()
          .end()
          .end()
          .find('tbody')
          .remove();

        const setPositionAndSize = function () {
          const stickyCols = $('.sticky-col');
          const stickyIntersects = $('.sticky-intersect');
          let i;

          for (i = 0; i < stickyCols.length; i++) {
            const stickyCol = $(stickyCols[i]);
            stickyCol.css({
              left: stickyCol.parent().find('.fixed-scrollbar-container').position().left,
              top: stickyCol.parent().find('.fixed-scrollbar-container').position().top
            });
          }

          for (i = 0; i < stickyIntersects.length; i++) {
            const stickyIntersect = $(stickyIntersects[i]);
            stickyIntersect.css({
              left: stickyIntersect.parent().find('.fixed-scrollbar-container').position().left
            });
          }

          $t.find('thead th').each(function (element) {
            $stickyHead.find('th').eq(element).width($(this).width());
          }).end()
            .find('tr')
            .each(function (element) {
              $stickyCol.find('tr').eq(element).height($(this).height());
              $stickyInsct.find('tr').eq(element).height($(this).height());
            });

          // Set width of sticky table head
          $stickyHead.width($t.width());

          // Set width of sticky table col
          $stickyCol.find('th').add($stickyInsct.find('th')).width($t.find('thead th').width());
        };
        const repositionStickyHead = function () {
          // Return value of calculated allowance
          const allowance = calcAllowance();

          // Check if wrapper parent is overflowing along the y-axis
          if ($t.height() > $stickyWrap.height()) {
            // If it is overflowing (advanced layout)
            // Position sticky header based on wrapper scrollTop()
            if ($stickyWrap.scrollTop() > 0) {
              // When top of wrapping parent is out of view
              $stickyHead.css({
                opacity: 1,
                top: $stickyWrap.scrollTop()
              });
              $stickyInsct.css({
                opacity: 1,
                top: -$stickyWrap.offset().top
              });
            } else {
              // When top of wrapping parent is in view
              $stickyHead.add($stickyInsct).css({
                opacity: 0,
                top: 0
              });
            }
          } else if ($w.scrollTop() > $t.offset().top &&
            $w.scrollTop() < $t.offset().top + $t.outerHeight() - allowance) {
            // If it is not overflowing (basic layout)
            // Position sticky header based on viewport scrollTop
            // When top of viewport is in the table itself
            $stickyHead.css({
              opacity: 1,
              top: $w.scrollTop() - $t.offset().top
            });
            $stickyInsct.css({
              opacity: 1,
              top: $w.scrollTop()
            });
          } else {
            // When top of viewport is above or below table
            $stickyHead.add($stickyInsct).css({
              opacity: 0,
              top: 0
            });
          }
        };
        const calcAllowance = function () {
          let a = 0;
          // Calculate allowance
          $t.find('tbody tr:lt(3)').each(function () {
            a += $(this).height();
          });

          // Set fail safe limit (last three row might be too tall)
          // Set arbitrary limit at 0.25 of viewport height, or you can use an arbitrary pixel value
          if (a > $w.height() * 0.25) {
            a = $w.height() * 0.25;
          }

          // Add the height of sticky header
          a += $stickyHead.height();
          return a;
        };

        setPositionAndSize();

        $t.parent('.sticky-wrap').scroll(() => {
          repositionStickyHead();
        });

        $w.on('load', () => {
          setPositionAndSize();
          repositionStickyHead();
        }).on('resize', () => {
          setPositionAndSize();
          repositionStickyHead();
        }).scroll(repositionStickyHead);
      }
    });
  }

  function applyFixedHorizontalScrollbar() {
    const fixedBarTemplate = '<div class="fixed-scrollbar"><div></div></div>';
    const fixedBarCSS = {
      display: 'none',
      overflowX: 'scroll',
      position: 'fixed',
      width: '100%',
      bottom: 0
    };
    const fixedScrollbarContainer = $('.fixed-scrollbar-container');
    fixedScrollbarContainer.css('overflow', 'AUTO');

    fixedScrollbarContainer.each(function () {
      const $container = $(this);
      if ($container[0].offsetWidth < $container[0].scrollWidth) {
        const $bar = $(fixedBarTemplate).appendTo($container).css(fixedBarCSS);

        $bar.scroll(() => {
          $container.scrollLeft($bar.scrollLeft());
        });

        $bar.data('status', 'off');
      }
    });

    const fixSize = function () {
      $('.fixed-scrollbar').each(function () {
        const $bar = $(this);
        const $container = $bar.parent();

        $bar.children('div').height(1).width($container[0].scrollWidth);
        $bar.width($container.width()).scrollLeft($container.scrollLeft());
      });

      $(window).trigger('scroll.fixedbar');
    };

    $(window).on('load.fixedbar resize.fixedbar', () => {
      fixSize();
    });

    let scrollTimeout = null;

    $(window).add($('.fixed-scrollbar-container')).on('scroll.fixedbar', () => {
      clearTimeout(scrollTimeout);
      scrollTimeout = setTimeout(() => {
        $('.fixed-scrollbar-container').each(function () {
          const $container = $(this);
          const $bar = $container.children('.fixed-scrollbar');

          if ($bar.length && ($container[0].scrollWidth > $container.width())) {
            const containerOffset = {
              top: $container.offset().top,
              bottom: $container.offset().top + $container.height()
            };
            const windowOffset = {
              top: $(window).scrollTop(),
              bottom: $(window).scrollTop() + $(window).height()
            };

            if ((containerOffset.top > windowOffset.bottom) || (windowOffset.bottom > containerOffset.bottom)) {
              if ($bar.data('status') === 'on') {
                $bar.hide().data('status', 'off');
              }
            } else {
              if ($bar.data('status') === 'off') {
                $bar.show().data('status', 'on');
              }
              $bar.scrollLeft($container.scrollLeft());
            }
          } else if ($bar.data('status') === 'on') {
            $bar.hide().data('status', 'off');
          }
        });
      }, 20);
    });
  }

  const importButton = $('button#import-btn');
  const validateButton = $('button#validate-btn');
  const validateSpinner = $('#validate-spinner');
  const form = document.getElementById('metadata-import-form');
  const importSpinner = $('#import-spinner');
  const ignoreWarningCheckbox = $('#ignore-warnings-input');
  const ignoreMd5SumRow = $('#ignore-md5-sum-row');
  const ignoreWarnings = $('#ignore-warnings-label');
  const metadataFileSourceRadioButtons = $('input[name="metadataFileSource"]');
  const md5HiddenContainer = document.getElementById('md5-hidden-container');
  let md5SumsMap;

  // Render Problems and Spreadsheet when clicking on validate button
  validateButton.on('click', (e) => {
    e.preventDefault();
    const formData = new FormData(form);
    disableButtons();
    validateSpinner.show();
    document.getElementById('context-content').innerHTML = '';
    md5SumsMap = [];

    const ajaxCallList = [];
    // If drag and drop is selected send an ajax call for every file separately
    if (formData.get('metadataFileSource') === 'FILE') {
      const contentListElements = document.getElementsByName('contentList');

      let ajaxCallCounter = 0;
      contentListElements.forEach((contentListElement) => {
        const fileList = contentListElement.files;
        const loadingSpinner = contentListElement.parentNode.getElementsByClassName('content-spinner')[0];
        loadingSpinner.style.display = '';
        // Collecting all ajax calls regarding one input, for example with multiple files.
        const loadingSpinnerAjax = [];
        Array.from(fileList).forEach((file) => {
          formData.set('contentList', file);
          const ajaxCall = sendAjaxValidateCall(formData, ajaxCallCounter, file.name);
          loadingSpinnerAjax.push(ajaxCall);
          ajaxCallList.push(ajaxCall);
          ajaxCallCounter += 1;
        });
        ajaxCallCounter += 1;
        $.when(...loadingSpinnerAjax).always(() => {
          loadingSpinner.style.display = 'none';
        });
      });
    } else if (formData.get('metadataFileSource') === 'PATH') {
      formData.getAll('paths').forEach((path, index) => {
        const loadingSpinner = $('[name="paths"]').eq(index).parent().find('.path-spinner');
        loadingSpinner.show();
        formData.set('paths', path);
        const ajaxCall = sendAjaxValidateCall(formData, index, path);
        ajaxCallList.push(ajaxCall);
        ajaxCall.finally(() => {
          loadingSpinner.hide();
        });
      });
    }
    $.when(...ajaxCallList).then((...results) => {
      const maxProblemLevel = results.map((result) => result.problemLevel)
        .reduce((max, current) => ((max > current) ? max : current));
      const warningLevel = results.map((result) => result.warningLevel)
        .reduce((min, current) => ((min < current) ? min : current));
      renderMd5SumsInHiddenField();
      activateButtons(maxProblemLevel, warningLevel);
    }).catch(() => {
      activateButtons(1000, 900);
    });
  });

  importButton.on('click', (e) => {
    e.preventDefault();
    const formData = new FormData(form);
    disableButtons();
    importSpinner.show();
    document.getElementById('context-content').innerHTML = '';
    md5HiddenContainer.innerHTML = '';

    sendAjaxImportCall(formData);
  });

  const initialValidationCall = () => {
    const formData = new FormData(form);
    if (formData.get('ticketNumber') && formData.get('paths')) {
      validateButton.trigger('click');
    }
  };

  initialValidationCall();

  // Helper handler to remove clicked argument from other buttons after click,
  // to identify clicked button upon submitting of form
  $('form input[type=submit]').on('click', function () {
    $('input[type=submit]', $(this).parents('form')).removeAttr('clicked');
    $(this).attr('clicked', 'true');
  });

  function sendAjaxValidateCall(formData, index, filename) {
    return new Promise((resolve, reject) => {
      $.ajax({
        url: $.otp.createLink({
          controller: 'metadataImport',
          action: 'validatePathsOrFiles'
        }),
        type: 'POST',
        data: formData,
        success(results) {
          const { contexts } = results;
          const md5Sums = results.metadataFileMd5sums;
          const renderedContexts = contexts.map(({
            spreadsheet,
            problems,
            summary,
            maximumProblemLevel
          }) => renderContext(spreadsheet, problems, summary, filename, maximumProblemLevel, index)).join('');
          document.getElementById('context-content').innerHTML += renderedContexts;

          applyStickyHeader();
          applyFixedHorizontalScrollbar();
          addMd5SumsForRendering(md5Sums, index);
          resolve({
            problemLevel: results.problemLevel,
            warningLevel: results.warningLevel
          });
        },
        error(error) {
          try {
            $.otp.toaster.showErrorToast('Something failed', error.responseJSON.message);
          } finally {
            reject(error);
          }
        },
        cache: false,
        contentType: false,
        processData: false
      });
    });
  }

  function sendAjaxImportCall(formData) {
    const SYNCHRONIZER_TOKEN = $('#SYNCHRONIZER_TOKEN').val();
    const SYNCHRONIZER_URI = $('#SYNCHRONIZER_URI').val();
    importSpinner.show();
    $.ajax({
      url: $.otp.createLink({
        controller: 'metadataImport',
        action: 'importByPathOrContent',
        params: {
          SYNCHRONIZER_TOKEN,
          SYNCHRONIZER_URI
        }
      }),
      type: 'POST',
      data: formData,
      success(response) {
        if (response.redirect) {
          window.location.href = response.redirect;
        }
      },
      error(error) {
        resetButtons();
        $.otp.toaster.showErrorToast('Import failed', error.responseJSON.message);
      },
      cache: false,
      contentType: false,
      processData: false
    }).always(() => {
      importSpinner.hide();
    });
  }

  function renderContext(spreadsheet, problems, summary, filename, maximumProblemLevel, index) {
    let renderedContext = '';
    renderedContext += `<div id="border" class="borderMetadataImport${maximumProblemLevel}">`;
    renderedContext += `<h3>${filename}</h3>`;
    renderedContext += renderProblems(problems, summary, index);
    renderedContext += renderSpreadsheet(spreadsheet, index);
    renderedContext += '</div>';
    return renderedContext;
  }

  function renderProblems(problems, summary, index) {
    if (problems === null) {
      return 'No problems found :)';
    }

    let renderedProblems = '<ul>';
    problems.forEach((problem) => {
      renderedProblems += `<li class="${problem.level}">`;
      renderedProblems += `<span style="white-space: pre-wrap">${problem.levelAndMessage} </span>`;
      problem.affectedCells.forEach((cell) => {
        renderedProblems += `<a href="#${cell.cellAddress}_${index}">${cell.cellAddress}</a> `;
      });
      renderedProblems += '</li>';
    });
    renderedProblems += '</ul>';
    renderedProblems += renderSummary(summary);
    return renderedProblems;
  }

  function renderSummary(summary) {
    let renderedSummary = '';
    renderedSummary += '<h4>Summary</h4>';
    renderedSummary += '<ul>';
    summary.forEach((problemType) => {
      renderedSummary += `<li><span style="white-space: pre">${problemType}</span></li>`;
    });
    renderedSummary += '</ul>';
    return renderedSummary;
  }

  function renderSpreadsheet(spreadsheet, index) {
    let renderedSpreadsheet = '<div class="fixed-scrollbar-container">';

    if (spreadsheet) {
      renderedSpreadsheet += '<table class="apply-sticky-header">';
      renderedSpreadsheet += '<thead>';
      renderedSpreadsheet += '<tr>';
      renderedSpreadsheet += '<th></th>';
      if (spreadsheet.header.cells) {
        spreadsheet.header.cells.forEach((cell) => {
          renderedSpreadsheet += `<th>${cell.columnAddress}</th>`;
        });

        renderedSpreadsheet += '</tr>';
        renderedSpreadsheet += '<tr>';
        renderedSpreadsheet += `<th>${spreadsheet.header.rowAddress}</th>`;
        spreadsheet.header.cells.forEach((cell) => {
          renderedSpreadsheet += `<th class="${cell.cellProblemsName}" title="${cell.cellProblemsLevelAndMessage}">`;
          renderedSpreadsheet += `<span class="anchor" id="${cell.cellAddress}_${index}"></span>${cell.text}`;
          renderedSpreadsheet += '</th>';
        });
      }
      renderedSpreadsheet += '</tr></thead><tbody>';
      if (spreadsheet.dataRows) {
        spreadsheet.dataRows.forEach((row) => {
          renderedSpreadsheet += `<tr><th>${row.rowAddress}</th>`;
          row.cells.forEach((cell) => {
            renderedSpreadsheet += `<td class="${cell.cellProblemsName}" title="${cell.cellProblemsLevelAndMessage}">`;
            renderedSpreadsheet += `<span class="anchor" id="${cell.cellAddress}_${index}"></span> ${cell.text}</td>`;
          });
          renderedSpreadsheet += '</tr>';
        });
        renderedSpreadsheet += '</tr>';
      }
    }

    renderedSpreadsheet += '</tbody></table></div>';
    return renderedSpreadsheet;
  }

  function renderMd5SumsInHiddenField() {
    md5SumsMap.sort((a, b) => a.index - b.index);
    md5HiddenContainer.innerHTML = md5SumsMap
      .flatMap((indexedMap) => indexedMap.md5Sums)
      .map((md5Sum) => `<input name="md5" hidden value="${md5Sum}"/>`);
  }

  function addMd5SumsForRendering(md5Sums, index) {
    md5SumsMap.push({
      index,
      md5Sums
    });
  }

  ignoreWarningCheckbox.on('change', (e) => {
    e.preventDefault();
    importButton.prop('disabled', !$(e.target)[0].checked);
  }).trigger('change');

  // Show or hide file selection and disable directory structure radio buttons
  // depending on selected source of metadata files
  metadataFileSourceRadioButtons.on('change', (e) => {
    e.preventDefault();
    const target = $(e.target);
    if (!target[0].checked) {
      return;
    }
    const pathContainer = $('#path-container');
    const fileContainer = $('#file-container');
    const sameDirectoryRadioButton = $('input[value="SAME_DIRECTORY"]');
    const gpcfSpecificRadioButton = $('input[value="GPCF_SPECIFIC"]');
    const absolutePathRadioButton = $('input[value="ABSOLUTE_PATH"]');
    if (target.val() === 'FILE') {
      pathContainer.hide();
      fileContainer.show();
      sameDirectoryRadioButton.attr('disabled', true);
      gpcfSpecificRadioButton.attr('disabled', true);
      absolutePathRadioButton.prop('checked', true);
    } else if (target.val() === 'PATH') {
      fileContainer.hide();
      pathContainer.show();
      sameDirectoryRadioButton.removeAttr('disabled');
      gpcfSpecificRadioButton.removeAttr('disabled');
    }
  }).trigger('change');

  function activateButtons(problemLevel, warningLevel) {
    validateSpinner.hide();
    ignoreWarningCheckbox.prop('disabled', false);
    ignoreWarningCheckbox.prop('checked', false);

    importButton.prop('disabled', (problemLevel >= warningLevel));
    if (problemLevel === warningLevel) {
      ignoreWarnings.show();
    } else {
      ignoreWarnings.hide();
    }
    validateButton.prop('disabled', false);
    ignoreMd5SumRow.show();
  }

  function disableButtons() {
    ignoreWarningCheckbox.attr('disabled', true);
    importButton.prop('disabled', true);
    validateButton.prop('disabled', true);
  }

  $('#paths, #file-input, input[type="radio"]').on('input', () => {
    resetButtons();
  });

  resetButtons();

  function resetButtons() {
    importButton.prop('disabled', true);
    validateButton.prop('disabled', false);
    validateSpinner.hide();
    importSpinner.hide();
    ignoreWarnings.hide();
    ignoreWarningCheckbox.removeAttr('disabled');
    ignoreWarningCheckbox.removeAttr('checked');
    ignoreMd5SumRow.hide();
  }
});
