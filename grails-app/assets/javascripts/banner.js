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

$.otp = $.otp || {};
/**
 * The $.otp.poling namespace defines constants and functions for
 * OTP client side polling.
 * Current implementation applies exlusively to the news banner
 */
$.otp.polling = {
  // polling every minute
  POLL_INTERVAL: 60000,
  URL: $.otp.createLink({ controller: 'info', action: 'newsBanner' }),

  /**
   * Stop polling
   */
  stopPolling: () => {},

  /**
   * The function startPoll triggers a polling call
   * in every @param interval (ms) and will stop after calling stopPolling().
   * Keep functions asynchronous so that the js process is not blocked by polling
   * @param promiseFn function to be called
   * @param interval polling interval in milli second
   */
  startPolling: (promiseFn, interval) => promiseFn().then(() => {
    const stopOrContinue = (timeout) => new Promise((resolve) => {
      $.otp.polling.stopPolling = () => resolve(false);
      setTimeout(() => {
        resolve(true);
      }, timeout);
    });

    const asyncWrapper = (timeout) => new Promise((resolve) => resolve(stopOrContinue(timeout)));

    asyncWrapper(interval).then((res) => {
      if (res) {
        $.otp.polling.startPolling(promiseFn, interval);
      }
    });
  })
};

$.otp.banner = {
  show: () => {
    $('div.news-banner').removeClass('news-banner-hidden').addClass('news-banner-shown');
  },
  hide: () => {
    $('div.news-banner').removeClass('news-banner-shown').addClass('news-banner-hidden');
    $.otp.polling.stopPolling();
  }
};

$(document).ready(() => {
  // start polling the ProcessingOptions for the news banner text
  $.otp.polling.startPolling(() => new Promise((resolve) => {
    $.ajax({
      dataType: 'text',
      type: 'GET',
      url: $.otp.polling.URL,
      success(msg) {
        // update the text of the news banner
        if (msg && msg.length > 0) {
          const bannerText = $('div.news-banner .news-banner-body');
          if (!bannerText.is(':visible')) {
            $.otp.banner.show();
          }
          bannerText.text(msg);
          resolve();
        } else {
          $.otp.banner.hide();
        }
      },
      error() {
        $.otp.banner.hide();
      }
    });
  }), $.otp.polling.POLL_INTERVAL);
});
