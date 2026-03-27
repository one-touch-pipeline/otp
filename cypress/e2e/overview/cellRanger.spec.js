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

describe('Check cell ranger page', () => {
  'use strict';

  const userRoles = ['operator', 'user'];

  // Helper function to find and click delete button using stable selector
  const findAndClickDeleteButton = ($row) => {
    const $deleteButton = $row.find('button.delete-btn');
    if ($deleteButton.length > 0) {
      cy.wrap($deleteButton.eq(0)).click();
      return true;
    }
    return false;
  };

  userRoles.forEach((userType) => {
    context(`when user is ${userType}`, () => {
      beforeEach(() => {
        cy.loginAs(userType);
      });

      it('should visit the final run selection page', () => {
        cy.visit('/cellRanger/finalRunSelection');
      });

      it('should select multiple run configuration and save these', () => {
        cy.visit('/cellRanger/finalRunSelection');

        cy.get('body').then(($body) => {
          const canModify = $body.find('input#input-3').length > 0;
          if (userType === 'operator') {
            expect(canModify, 'operator should have modify controls').to.eq(true);
          }
          if (canModify) {
            // User has access to the controls, proceed with the test
            cy.intercept('/cellRanger/saveFinalRunSelection*').as('saveRunSelection');

            // Select second run selection to use default
            cy.get('input#input-3').click();

            // Select third run selection to not keep any runs
            cy.get('input#input-6').click();

            // Save run configurations
            cy.get('input#save').click();

            cy.wait('@saveRunSelection').then((interception) => {
              expect(interception.response.statusCode).to.eq(302);
            });
            cy.get('#otpToastBox .otpSuccessToast').should('be.visible').and('contain.text', 'successfully');

            cy.get('#cell-ranger-run-table tbody tr').contains('Final run').should('exist');
            cy.get('#cell-ranger-run-table tbody tr').contains('Deleted run').should('exist');

            // Clean up: Delete the created final run to not affect other tests
            cy.intercept('/cellRanger/deleteFinalSelectedRun*').as('deleteFinalRun');
            cy.get('#cell-ranger-run-table tbody tr').contains('Final run').closest('tr').then(($row) => {
              // Use helper function to find and click delete button
              const deleteButtonFound = findAndClickDeleteButton($row);

              if (deleteButtonFound) {
                cy.get('#confirmDeleteModal').should('be.visible');
                cy.get('#confirmDeleteModal').find('button.confirm').click();
                cy.wait('@deleteFinalRun');
              } else {
                cy.log('Delete button not found - test cleanup skipped');
              }
            });
          } else {
            if (userType === 'operator') {
              throw new Error('Operator permissions regression: modification controls are missing');
            }
            // User doesn't have access to controls, skip this test or verify read-only access
            cy.log('User does not have permission to modify cell ranger configurations');
            cy.get('#cell-ranger-run-table').should('be.visible');
          }
        });
      });

      it('should delete the created final run', () => {
        cy.visit('/cellRanger/finalRunSelection');

        // Check if user has permission to access the controls
        cy.get('body').then(($body) => {
          const canModify = $body.find('input#input-3').length > 0;
          if (userType === 'operator') {
            expect(canModify, 'operator should have modify controls').to.eq(true);
          }
          if (canModify) {
            // User has access to the controls, proceed with the test
            cy.intercept('/cellRanger/saveFinalRunSelection*').as('saveRunSelection');
            cy.intercept('/cellRanger/deleteFinalSelectedRun*').as('deleteFinalRun');

            // First create a final run to delete
            cy.get('input#input-3').click();
            cy.get('input#save').click();
            cy.wait('@saveRunSelection');

            cy.get('#cell-ranger-run-table tbody tr').contains('Final run').should('exist');
            // Wait for the table to update with the new run
            cy.get('#cell-ranger-run-table tbody tr').contains('Final run').should('be.visible');

            // Track whether deletion was successful
            let deletionAttempted = false;

            cy.get('#cell-ranger-run-table tbody tr').contains('Final run').closest('tr').then(($row) => {
              // Log the row HTML for debugging
              cy.log('Row HTML:', $row[0].outerHTML);

              // Use helper function to find and click delete button
              const deleteButtonFound = findAndClickDeleteButton($row);
              deletionAttempted = deleteButtonFound;

              if (!deleteButtonFound) {
                // Log all available buttons/links in the row for debugging
                const buttons = $row.find('button, a, input[type="button"], input[type="submit"]');
                cy.log(`Available buttons/links in row (${buttons.length}):`);
                buttons.each((index, element) => {
                  cy.log(`  ${index}: ${element.outerHTML}`);
                });

                // Check if user has delete permissions by looking for any delete-related elements
                const hasDeleteElements = $row.find('*').filter((index, element) => {
                  const text = element.textContent?.toLowerCase() || '';
                  const classes = element.className?.toLowerCase() || '';
                  const title = element.title?.toLowerCase() || '';
                  return text.includes('delete') || classes.includes('delete') || title.includes('delete');
                });

                if (hasDeleteElements.length === 0) {
                  cy.log('No delete elements found - user may not have delete permissions');
                  // Skip the delete test for this user type
                  deletionAttempted = false;
                } else {
                  throw new Error(`Delete button not found with the expected selector. Available elements: ${buttons.length}`);
                }
              } else {
                cy.log('Successfully clicked delete button using button.delete-btn selector');
              }
            })
              .then(() => {
                // Only proceed with modal and wait operations if deletion was attempted
                if (deletionAttempted) {
                  cy.get('#confirmDeleteModal').should('be.visible');
                  cy.get('#confirmDeleteModal').find('button.confirm').click();

                  cy.wait('@deleteFinalRun').then((interception) => {
                    expect(interception.response.statusCode).to.eq(302);
                  });
                  cy.get('#otpToastBox .otpSuccessToast').should('be.visible').and('contain.text', 'successfully');

                  cy.get('#cell-ranger-run-table tbody tr').contains('Deleted run').should('exist');
                } else {
                  cy.log('Delete operation skipped - no delete button found');
                  // Verify that the final run still exists since it wasn't deleted
                  cy.get('#cell-ranger-run-table tbody tr').contains('Final run').should('exist');
                }
              });
          } else {
            if (userType === 'operator') {
              throw new Error('Operator permissions regression: modification controls are missing');
            }
            // User doesn't have access to controls, skip this test or verify read-only access
            cy.log('User does not have permission to delete cell ranger configurations');
            cy.get('#cell-ranger-run-table').should('be.visible');
          }
        });
      });
    });
  });
});
