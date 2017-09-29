//Copyright (c) 2017 Łukasz Nawrot
//Email: lukasz@nawrot.me
//License: MIT (http://opensource.org/licenses/MIT)

package me.nawrot.cordova.plugin.vungle;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import android.annotation.TargetApi;
import android.app.Activity;
import android.util.Log;

import android.util.Log;
import android.view.View;

import com.vungle.publisher.VungleAdEventListener;
import com.vungle.publisher.VungleInitListener;
import com.vungle.publisher.VunglePub;

public class VunglePlugin extends CordovaPlugin {
	private static final String LOG_TAG = "VunglePlugin";
	final VunglePub vunglePub = VunglePub.getInstance();

	private String appId;
	private String placement;
	private boolean isTest;

	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		super.initialize(cordova, webView);
	}

	@Override
	public boolean execute(String action, JSONArray inputs, CallbackContext callbackContext) throws JSONException {
    PluginResult result = null;

		if (Actions.SETUP.equals(action)) {
			JSONObject options = inputs.optJSONObject(0);
			result = setup(options, callbackContext);

		} else if (Actions.IS_READY.equals(action)) {
			result = isReady(callbackContext);

		} else if (Actions.LOAD_VIDEO.equals(action)) {
			result = loadVideoAd(callbackContext);

		} else if (Actions.SHOW_VIDEO.equals(action)) {
			result = showVideoAd(callbackContext);

		} else {
			Log.d(LOG_TAG, String.format("Invalid action passed: %s", action));
			result = new PluginResult(Status.INVALID_ACTION);
		}

		if (result != null) {
			callbackContext.sendPluginResult(result);
		}

		return true;
	}

	@Override
	public void onPause(boolean multitasking) {
		super.onPause(multitasking);
		vunglePub.onPause();
	}

	@Override
	public void onResume(boolean multitasking) {
		super.onResume(multitasking);
		vunglePub.onResume();
	}

	@Override
	public void onDestroy() {
		vunglePub.removeEventListeners(vungleListener);
		super.onDestroy();
	}


	private PluginResult setup(final JSONObject options, final CallbackContext callbackContext) {
		cordova.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				_setup(options, callbackContext);
			}
		});

		return null;
	}

	private void _setup(final JSONObject options, final CallbackContext callbackContext) {
		if (options == null) {
			return;
		}

		if (options.has("appId")) {
			this.appId = options.optString("appId");
		} else {
			callbackContext.error("Wrong options passed!");
			return;
		}

		if (options.has("placement")) {
			this.placement = options.optString("placement");
		}

		vunglePub.init(cordova.getActivity(), this.appId, new String[] { this.placement }, new VungleInitListener() {
        @Override
        public void onSuccess() {
            Log.d(LOG_TAG, "init success");
            vunglePub.clearAndSetEventListeners(vungleListener);
            callbackContext.success();
        }

        @Override
        public void onFailure(Throwable error) {
            Log.d(LOG_TAG, "init failure: " );
            callbackContext.error("Error occured!");
        }
    });
	}

	private PluginResult isReady(final CallbackContext callbackContext) {
		cordova.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				boolean result = vunglePub.isAdPlayable(placement);
				callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));
			}
		});

		return null;
	}

	private PluginResult loadVideoAd(final CallbackContext callbackContext) {
		cordova.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				_loadVideoAd();
			}
		});
		callbackContext.success();

		return null;
	}

	private void _loadVideoAd() {
		vunglePub.loadAd(placement);
	}

	private PluginResult showVideoAd(final CallbackContext callbackContext) {
		final CallbackContext delayCallback = callbackContext;
		cordova.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				boolean result = _showVideoAd();
				if (result) {
					delayCallback.success();
				} else {
					delayCallback.error("Ad is not ready!");
				}
			}
		});

		return null;
	}

	private boolean _showVideoAd() {
		if (!vunglePub.isAdPlayable(placement)) {
			return false;
		}

		vunglePub.playAd(placement, null);
		return true;
	}

	private final VungleAdEventListener vungleListener = new VungleAdEventListener() {
		@Override
	  public void onAdEnd(String placementReferenceId, boolean wasSuccessFulView, boolean wasCallToActionClicked) {
	      Log.d(LOG_TAG, "onAdEnd: " + placementReferenceId + " ,wasSuccessfulView: " + wasSuccessFulView + " ,wasCallToActionClicked: " + wasCallToActionClicked);
	      JSONObject data = new JSONObject();
	      try {
	        data.put("result", wasSuccessFulView);
	        data.put("clicked", wasCallToActionClicked);
	      } catch (JSONException e) {
	        e.printStackTrace();
	      }
	      fireEvent("vungle.finish", data);
	  }

	  @Override
	  public void onAdStart(String placementReferenceId) {
	      Log.d(LOG_TAG, "onAdStart: " + placementReferenceId);
	      fireEvent("vungle.start", null);
	  }

	  @Override
	  public void onUnableToPlayAd(String placementReferenceId, String reason) {
	      Log.d(LOG_TAG, "onUnableToPlayAd: " + placementReferenceId + " ,reason: " + reason);

	      JSONObject data = new JSONObject();
	      try {
	        data.put("error", reason);
	      } catch (JSONException e) {
	        e.printStackTrace();
	      }
	      fireEvent("vungle.error", data);
	  }

	  @Override
	  public void onAdAvailabilityUpdate(String placementReferenceId, boolean isAdAvailable) {
	      Log.d(LOG_TAG, "onAdAvailabilityUpdate: " + placementReferenceId + " isAdAvailable: " + isAdAvailable);

	      final boolean ready = isAdAvailable;
	      if (!ready) {
	        return;
	      }

	      fireEvent("vungle.ready", null);
	  }
  };

  private void fireEvent(String eventName, JSONObject jsonObj) {
    String data = "";
    if (jsonObj != null) {
      data = jsonObj.toString();
    }

    StringBuilder js = new StringBuilder();
    js.append("javascript:cordova.fireDocumentEvent('");
    js.append(eventName);
    js.append("'");
    if (data != null && !"".equals(data)) {
      js.append(",");
      js.append(data);
    }
    js.append(");");

    final String code = js.toString();

    cordova.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
    		webView.loadUrl(code);
			}
		});
  }
}
