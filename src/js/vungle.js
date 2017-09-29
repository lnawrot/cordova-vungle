import exec from 'cordova/exec'

const PLUGIN = 'VunglePlugin';
const DEFAULT_OPTIONS = {
  appId: null,
  placement: '',
  isTest: false,
};

const isFunction = (f) => typeof f === 'function';
export function setup(opts, successCallback, failureCallback) {
  if (typeof opts === 'object') {
    const options = {
      ...DEFAULT_OPTIONS,
      ...opts,
    };

    if (options.appId === null) {
      if (isFunction(failureCallback)) {
        failureCallback(`"appId" must be specified.`);
      }
      return;
    }

    exec(successCallback, failureCallback, PLUGIN, 'setup', [options]);
  } else if (isFunction(failureCallback)) {
    failureCallback('Options must be specified.');
  }
}

export function isReady(successCallback, failureCallback) {
  exec(successCallback, failureCallback, PLUGIN, 'isReady', []);
}

export function loadVideoAd(successCallback, failureCallback) {
  exec(successCallback, failureCallback, PLUGIN, 'loadVideoAd', []);
}

export function showVideoAd(successCallback, failureCallback) {
  exec(successCallback, failureCallback, PLUGIN, 'showVideoAd', []);
}

