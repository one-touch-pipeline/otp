/*
 * Copyright 2011-2021 The OTP authors
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

/**
 * This toaster is a bootstrap based lib which allows it to simply fire bootstrap toast messages.
 * It can be used to fire success, warning, error or info toasts.
 */
$.otp.toaster = {
  /**
     * Fires an info toast message.
     *
     * @param title: title of the toast
     * @param message: the toasts message
     */
  showInfoToast(title = 'Info', message = '') {
    this.showToast(title, message, 'info');
  },
  /**
     * Fires a success toast message.
     *
     * @param title: title of the toast
     * @param message: the toasts message
     */
  showSuccessToast(title = 'Success', message = 'Operation has been successful.') {
    this.showToast(title, message, 'success');
  },
  /**
     * Fires a warning toast message.
     *
     * @param title: title of the toast
     * @param message: the toasts message
     */
  showWarningToast(title = 'Warning', message = 'A warning occurred during the operation.') {
    this.showToast(title, message, 'warning');
  },
  /**
     * Fires an error toast message.
     *
     * @param title: title of the toast
     * @param message: the toasts message
     */
  showErrorToast(title = 'Error', message = 'Unknown error. Please try again.') {
    this.showToast(title, message, 'danger');
  },
  /**
     * Fires a toast message.
     *
     * @param title: title of the toast
     * @param message: the toasts message
     * @param state: allowed states: info, success, warning, danger. default is info
     */
  showToast(title, message, state = 'info') {
    const stateProps = this.getStateProps(state);
    const date = new Date().toLocaleTimeString('en-US', { hour12: false, hour: 'numeric', minute: 'numeric' });

    const otpToast = `<div class="toast ${stateProps.toastClass}"\
                           role="alert" aria-live="assertive" aria-atomic="true" data-autohide="false">
                        <div class="toast-header">
                          <span class="otp-toast-icon-wrapper text-${state}">${stateProps.icon}</span>
                          <strong class="mr-auto">${title}</strong>
                          <small class="text-muted otp-toast-time-txt">${date}</small>
                          <button type="button" class="ml-2 mb-1 close" data-dismiss="toast" aria-label="Close">
                            <span aria-hidden="true">&times;</span>
                          </button>
                        </div>
                        <div class="toast-body">
                          <div class="alert alert-${state}" role="alert">${message}</div>
                        </div>
                      </div>`;

    this.getToastBox().append(otpToast);

    const toast = $('.toast');

    toast.on('hidden.bs.toast', function () {
      $(this).remove();
    });
    toast.toast('show');
  },
  /**
     * This method returns the wrapper for the toaster.
     * If it not exists in the DOM, it will be created then.
     *
     * @returns {*|Window.jQuery|HTMLElement}
     */
  getToastBox() {
    const toastBoxId = 'otpToastBox';
    const toastBox = `<div id="${toastBoxId}" class="otpToastBox"></div>`;
    const getToastBoxFromDOM = () => $(`#${toastBoxId}`);

    if (getToastBoxFromDOM().length) {
      return getToastBoxFromDOM();
    }

    $('body').append(toastBox);
    return getToastBoxFromDOM();
  },
  /**
     * Convert the state into a css class name.
     *
     * @param state: info, success, warning, danger
     * @returns {string}: css class
     */
  getStateProps(state) {
    switch (state) {
      case 'info': return {
        toastClass: 'otpInfoToast',
        icon: '<i class="bi bi-info-circle"></i>'
      };
      case 'success': return {
        toastClass: 'otpSuccessToast',
        icon: '<i class="bi bi-check-circle"></i>'
      };
      case 'warning': return {
        toastClass: 'otpWarningToast',
        icon: '<i class="bi bi-exclamation-circle"></i>'
      };
      case 'danger': return {
        toastClass: 'otpErrorToast',
        icon: '<i class="bi bi-bug"></i>'
      };
      default: return {
        toastClass: 'otpInfoToast',
        icon: '<i class="bi bi-info-circle"></i>'
      };
    }
  }
};
