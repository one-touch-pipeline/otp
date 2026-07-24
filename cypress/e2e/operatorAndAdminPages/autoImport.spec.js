/*
 * Copyright 2011-2026 The OTP authors
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

const project = 'projectUsingFastqAndUUid';
const singleIlse = 262601;
const listIlses = [262602, 262603];
const rangeIlses = [262610, 262611, 262612];
const importedIlses = [singleIlse, ...listIlses, ...rangeIlses];
const faultyIlse = 262620;
const unknownIlse = 262699;
const seqCenter = {
  name: 'DKFZ',
  dirName: 'dkfz'
};

const expectedForIlse = (ilse) => {
  'use strict';

  return {
    ilse,
    pid: `otp2626pid${ilse}`,
    sampleName: `otp2626sample${ilse}`,
    run: `otp2626_run_${ilse}`
  };
};

const requestJson = (options) => {
  'use strict';

  return cy.request({
    ...options,
    form: true
  }).then((response) => {
    expect(response.status).to.eq(200);
    expect(response.body.success, JSON.stringify(response.body)).to.eq(true);
    return response;
  });
};

const createSeqCenterIfMissing = () => {
  'use strict';

  cy.visit('/metaDataFields/seqCenters');
  cy.get('table#metadatafields-datatable tbody').then(($tbody) => {
    if ($tbody.find(`tr:contains("${seqCenter.name}")`).length === 0) {
      requestJson({
        method: 'POST',
        url: '/metaDataFields/createSeqCenter',
        body: seqCenter
      });
    }
  });
};

const getSeqCenterId = () => {
  'use strict';

  cy.visit('/metaDataFields/seqCenters');
  return cy.contains('table#metadatafields-datatable tbody tr', seqCenter.name)
    .find('input[name="seqCenter.id"]')
    .invoke('val');
};

const updateSeqCenter = (action, value) => {
  'use strict';

  return getSeqCenterId().then((seqCenterId) => requestJson({
    method: 'POST',
    url: `/metaDataFields/${action}`,
    body: {
      'seqCenter.id': seqCenterId,
      value
    }
  }));
};

const updateSampleIdentifierParser = (value) => {
  'use strict';

  return requestJson({
    method: 'POST',
    url: `/projectConfig/updateSampleIdentifierParserBeanName?project=${project}`,
    body: {
      fieldName: 'sampleIdentifierParserBeanName',
      value
    }
  });
};

const configureAutoImport = () => {
  'use strict';

  cy.loginAs('operator');
  createSeqCenterIfMissing();
  updateSeqCenter('updateAutoImportDirectory', '/home/otp/filesystem/autoimport');
  updateSeqCenter('updateAutoImportable', 'true');
  updateSampleIdentifierParser('SIMPLE');
  cy.setProcessingOption('TICKET_SYSTEM_AUTO_IMPORT_ENABLED', 'true');
};

const resetAutoImport = () => {
  'use strict';

  cy.loginAs('operator');
  updateSeqCenter('updateAutoImportable', 'false');
  updateSampleIdentifierParser('NO_PARSER');
  cy.setProcessingOption('TICKET_SYSTEM_AUTO_IMPORT_ENABLED', 'false');
};

const autoImport = (ilseNumbers, ticketNumber, failOnStatusCode) => {
  'use strict';

  return cy.env(['otp.autoimport.secret']).then((env) => {
    // An explicitly configured empty secret must not silently fall back to the dev default:
    // sending it makes the request fail loudly and expose the misconfiguration.
    const configuredSecret = env['otp.autoimport.secret'];
    const secret = configuredSecret === undefined ? 'secret' : configuredSecret;

    return cy.request({
      method: 'GET',
      url: '/metadataImport/autoImport',
      failOnStatusCode: failOnStatusCode !== false,
      qs: {
        secret,
        ticketNumber,
        ilseNumbers,
        ignoreMd5sumError: 'TRUE'
      }
    });
  });
};

const assertSuccessfulImport = (response, ilseNumbers) => {
  'use strict';

  expect(response.status).to.eq(200);
  // The anchored success prefix is mutually exclusive with the validation-failure response,
  // so no negative match on the body is needed (paths/links in the body may contain arbitrary text).
  expect(response.body).to.match(/^Automatic import succeeded :-\)/);
  ilseNumbers.forEach((ilse) => {
    expect(response.body).to.contain(`${ilse}_meta.tsv`);
  });
};

const sequenceRequestBody = (ilseNumbers) => {
  'use strict';

  return {
    iDisplayStart: 0,
    iDisplayLength: 150,
    iSortCol_0: 14,
    sSortDir_0: 'asc',
    filtering: JSON.stringify([{
      type: 'ilseIdSearch',
      value: ilseNumbers.join(',')
    }])
  };
};

const requestSequences = (ilseNumbers) => {
  'use strict';

  return cy.request({
    method: 'POST',
    url: '/sequence/dataTableSource',
    form: true,
    body: sequenceRequestBody(ilseNumbers)
  });
};

const assertForbiddenAutoImport = (qs) => {
  'use strict';

  return cy.request({
    method: 'GET',
    url: '/metadataImport/autoImport',
    failOnStatusCode: false,
    qs
  }).then((response) => {
    expect(response.status).to.eq(403);
    expect(response.body).to.contain('authentication with secret failed');
    expect(response.body).not.to.contain('Automatic import succeeded :-)');
  });
};

const assertAutoImportReset = () => {
  'use strict';

  cy.loginAs('operator');
  cy.visit('/metaDataFields/seqCenters');
  cy.contains('table#metadatafields-datatable tbody tr', seqCenter.name)
    .find('td')
    .eq(3)
    .find('.wordBreak')
    .should('contain.text', 'false');

  cy.visit(`/projectConfig/index?project=${project}`);
  cy.get('td').contains('Sample Parser')
    .siblings()
    .last()
    .find('.edit-switch-label .wordBreak')
    .should('contain.text', 'NO_PARSER');

  cy.visit('/processingOption/index');
  cy.contains('table tr', 'TICKET_SYSTEM_AUTO_IMPORT_ENABLED')
    .find('.form-control')
    .should('have.value', 'false');
};

describe('Check autoImport endpoint', { retries: 0 }, () => {
  'use strict';

  before(() => {
    configureAutoImport();
  });

  after(() => {
    resetAutoImport();
    assertAutoImportReset();
  });

  it('imports a single ILSE and returns success', () => {
    autoImport(singleIlse.toString(), '2026070800000001').then((response) => {
      assertSuccessfulImport(response, [singleIlse]);
    });
  });

  it('imports a comma-separated ILSE list', () => {
    autoImport(listIlses.join(','), '2026070800000002').then((response) => {
      assertSuccessfulImport(response, listIlses);
    });
  });

  it('imports an ILSE range', () => {
    autoImport(`${rangeIlses[0]}-${rangeIlses[2]}`, '2026070800000003').then((response) => {
      assertSuccessfulImport(response, rangeIlses);
    });
  });

  it('imported samples appear on sample overview', () => {
    cy.loginAs('operator');
    cy.intercept('POST', '**/sampleOverview/dataTableSourceLaneOverview**').as('loadSampleOverview');

    cy.visit(`/sampleOverview/index?project=${project}`);

    cy.wait('@loadSampleOverview').then((interception) => {
      const rows = interception.response.body.aaData;
      importedIlses.map(expectedForIlse).forEach((expected) => {
        const row = rows.find((sample) => sample[0] === expected.pid && sample[1] === 'tumor01');
        expect(row, expected.pid).to.exist;
        // After pid and sample type, the row holds one lane-count cell per seq type followed by
        // one bam-file cell per pipeline. The import created exactly one WGS lane and no bam
        // files, so the only populated cell must be a lane count of 1.
        expect(row.slice(2).filter(Boolean), `lane counts for ${expected.pid}`).to.deep.eq(['1']);
      });
    });
  });

  it('imported data appears on sequence page', () => {
    cy.loginAs('operator');
    cy.intercept('POST', '/sequence/dataTableSource*').as('loadSequenceTable');

    cy.visit('/sequence/index');
    cy.get('div#data-table-filter-container').find('span#select2--container')
      .contains('No Search Criteria')
      .click();
    cy.get('ul#select2--results').contains('ILSe').click({ force: true });
    cy.get('#searchCriteriaTable tr').eq(0)
      .find('input[name=ilseIdSearch]')
      .type(importedIlses.join(','));
    cy.get('#searchCriteriaTable td.search input[type=button]').click();

    cy.wait('@loadSequenceTable').then((interception) => {
      const rows = interception.response.body.aaData;
      expect(rows).to.have.length(importedIlses.length);
      importedIlses.map(expectedForIlse).forEach((expected) => {
        const row = rows.find((sequence) => sequence.ilseId.toString() === expected.ilse.toString());
        expect(row.projectName).to.eq(project);
        expect(row.pid).to.eq(expected.pid);
        expect(row.sampleName).to.eq(expected.sampleName);
        expect(row.name).to.eq(expected.run);
      });
    });
  });

  it('rejects a request with a wrong secret', () => {
    cy.loginAs('operator');
    requestSequences([singleIlse]).then((beforeResponse) => {
      assertForbiddenAutoImport({
        secret: 'wrong-secret',
        ticketNumber: '2026070800000004',
        ilseNumbers: singleIlse.toString()
      });

      requestSequences([singleIlse]).then((afterResponse) => {
        expect(afterResponse.body.iTotalRecords).to.eq(beforeResponse.body.iTotalRecords);
      });
    });
  });

  it('rejects a request without a secret', () => {
    cy.loginAs('operator');
    requestSequences([singleIlse]).then((beforeResponse) => {
      assertForbiddenAutoImport({
        ticketNumber: '2026070800000004',
        ilseNumbers: singleIlse.toString()
      });

      requestSequences([singleIlse]).then((afterResponse) => {
        expect(afterResponse.body.iTotalRecords).to.eq(beforeResponse.body.iTotalRecords);
      });
    });
  });

  it('reports a validation failure for faulty metadata', () => {
    autoImport(faultyIlse.toString(), '2026070800000005').then((response) => {
      expect(response.status).to.eq(200);
      expect(response.body).to.match(/^This metadata file failed validation:/);
      expect(response.body).to.contain(`${faultyIlse}_meta.tsv`);
    });

    cy.loginAs('operator');
    requestSequences([faultyIlse]).then((response) => {
      expect(response.body.aaData, `no sequences imported for ILSe ${faultyIlse}`).to.have.length(0);
    });
  });

  it('reports a validation failure for an unknown ILSe number', () => {
    autoImport(unknownIlse.toString(), '2026070800000006').then((response) => {
      expect(response.status).to.eq(200);
      expect(response.body).to.match(/^This metadata file failed validation:/);
      expect(response.body).to.contain('does not exist or cannot be accessed');
    });
  });

  it('rejects malformed ILSe numbers', () => {
    autoImport('not-a-number', '2026070800000007', false).then((response) => {
      expect(response.status).to.eq(500);
      expect(response.body).not.to.contain('Automatic import succeeded :-)');
    });
  });
});
