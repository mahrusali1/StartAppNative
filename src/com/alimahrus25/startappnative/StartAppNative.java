package com.alimahrus25.startappnative;

import android.app.Activity;
import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.AndroidViewComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.util.YailList;
import com.startapp.sdk.adsbase.Ad;
import com.startapp.sdk.adsbase.SDKAdPreferences;
import com.startapp.sdk.adsbase.StartAppSDK;
import com.startapp.sdk.adsbase.adlisteners.AdEventListener;
import com.startapp.sdk.ads.nativead.NativeAdDetails;
import com.startapp.sdk.ads.nativead.NativeAdPreferences;
import com.startapp.sdk.ads.nativead.StartAppNativeAd;
import java.util.ArrayList;

@DesignerComponent(version = 4, description = "Start.io Native Ads SDK 5.2.0 for Niotron", category = ComponentCategory.EXTENSION, nonVisible = true, iconName = "aiwebres/icon.png")
@SimpleObject(external = true)
@UsesPermissions(permissionNames = "android.permission.INTERNET, android.permission.ACCESS_NETWORK_STATE")
public class StartAppNative extends AndroidNonvisibleComponent {
  private final Activity activity;
  private StartAppNativeAd nativeAd;
  private boolean initialized;

  public StartAppNative(ComponentContainer container) {
    super(container.$form());
    activity = container.$context();
  }

  @SimpleFunction(description = "Initializes Start.io SDK. Call before LoadNativeAd.")
  public void Initialize(String appId, boolean testMode) {
    if (appId == null || appId.trim().length() == 0) {
      ErrorOccurred("App ID cannot be empty");
      return;
    }
    try {
  SDKAdPreferences preferences = new SDKAdPreferences();

  StartAppSDK.init(activity, appId.trim(), preferences);
  StartAppSDK.setTestAdsEnabled(testMode);

  initialized = true;
  SdkInitialized();
} catch (Exception e) {
  ErrorOccurred(errorMessage(e));
}

  }

  @SimpleFunction(description = "Loads native ads. Primary and secondary image sizes use numeric values: 0=72x72, 1=100x100, 2=150x150, 3=340x340, 4=1200x628, 5=320x480, 6=480x320. Secondary supports only 0 to 3.")
  public void LoadNativeAd(int numberOfAds, int primaryImageSize, int secondaryImageSize) {
    if (!initialized) {
      ErrorOccurred("Call Initialize before LoadNativeAd");
      return;
    }
    if (numberOfAds < 1) numberOfAds = 1;
    if (primaryImageSize < 0 || primaryImageSize > 6) primaryImageSize = 2;
    if (secondaryImageSize < 0 || secondaryImageSize > 3) secondaryImageSize = 0;

    nativeAd = new StartAppNativeAd(activity);
    NativeAdPreferences preferences = new NativeAdPreferences();
    preferences.setAdsNumber(numberOfAds);
    preferences.setPrimaryImageSize(primaryImageSize);
    preferences.setSecondaryImageSize(secondaryImageSize);
    preferences.setAutoBitmapDownload(false);
    nativeAd.setPreferences(preferences);
    nativeAd.loadAd(new AdEventListener() {
      @Override public void onReceiveAd(Ad ad) {
        ArrayList<NativeAdDetails> ads = nativeAd.getNativeAds();
        if (ads == null || ads.size() == 0) {
          AdFailedToLoad("The SDK returned no native ads");
        } else if (ads.size() == 1) {
          AdLoaded(ads.get(0), 1);
        } else {
          AdLoaded(YailList.makeList(ads), ads.size());
        }
      }
      @Override public void onFailedToReceiveAd(Ad ad) {
        AdFailedToLoad(ad == null || ad.getErrorMessage() == null ? "Unknown error" : ad.getErrorMessage());
      }
    });
  }

  @SimpleFunction(description = "Returns 0 for 72x72, 1 for 100x100, 2 for 150x150, 3 for 340x340, 4 for 1200x628, 5 for 320x480, and 6 for 480x320.")
  public int ImageSizeCode(String size) {
    if (size == null) return 2;
    String s = size.trim().toUpperCase();
    if (s.equals("72X72")) return 0;
    if (s.equals("100X100")) return 1;
    if (s.equals("150X150")) return 2;
    if (s.equals("340X340")) return 3;
    if (s.equals("1200X628")) return 4;
    if (s.equals("320X480")) return 5;
    if (s.equals("480X320")) return 6;
    return 2;
  }

  @SimpleFunction(description = "Registers a visible component for native-ad clicks.")
  public void RegisterContainerForClick(Object adDetails, AndroidViewComponent container) {
    if (adDetails instanceof NativeAdDetails && container != null && container.getView() != null) {
      ((NativeAdDetails) adDetails).registerViewForInteraction(container.getView());
    } else {
      ErrorOccurred("RegisterContainerForClick requires a Native Ad and a visible container");
    }
  }

  @SimpleFunction(description = "Returns the ad title.") public String GetAdTitle(Object a) { return value(a, "GetAdTitle", 0); }
  @SimpleFunction(description = "Returns the ad description.") public String GetAdDescription(Object a) { return value(a, "GetAdDescription", 1); }
  @SimpleFunction(description = "Returns the ad rating.") public String GetAdRating(Object a) { return value(a, "GetAdRating", 2); }
  @SimpleFunction(description = "Returns the primary image URL.") public String GetAdImageUrl(Object a) { return value(a, "GetAdImageUrl", 3); }
  @SimpleFunction(description = "Returns the secondary image URL.") public String GetAdSecondaryImageUrl(Object a) { return value(a, "GetAdSecondaryImageUrl", 4); }
  @SimpleFunction(description = "Returns the call-to-action text.") public String GetAdCallToAction(Object a) { return value(a, "GetAdCallToAction", 5); }
  @SimpleFunction(description = "Returns the install text.") public String GetAdInstalls(Object a) { return value(a, "GetAdInstalls", 6); }
  @SimpleFunction(description = "Returns the ad category.") public String GetAdCategory(Object a) { return value(a, "GetAdCategory", 7); }
  @SimpleFunction(description = "Returns the advertised package name.") public String GetPackageName(Object a) { return value(a, "GetPackageName", 8); }

  private String value(Object object, String method, int field) {
    if (!(object instanceof NativeAdDetails)) {
      ErrorOccurred(method + " requires a Native Ad as input");
      return "";
    }
    NativeAdDetails ad = (NativeAdDetails) object;
    switch (field) {
      case 0: return text(ad.getTitle());
      case 1: return text(ad.getDescription());
      case 2: return String.valueOf(ad.getRating());
      case 3: return text(ad.getImageUrl());
      case 4: return text(ad.getSecondaryImageUrl());
      case 5: return text(ad.getCallToAction());
      case 6: return text(ad.getInstalls());
      case 7: return text(ad.getCategory());
      default: return text(ad.getPackageName());
    }
  }

  @SimpleFunction(description = "Returns App, Market, or Unknown for the campaign action.")
  public String GetAdCampaignActionType(Object a) {
    if (!(a instanceof NativeAdDetails)) {
      ErrorOccurred("GetAdCampaignActionType requires a Native Ad as input");
      return "";
    }
    Object action = ((NativeAdDetails) a).getCampaignAction();
    if (action == null) return "Unknown";
    String name = action.toString();
    if (name.indexOf("LAUNCH_APP") >= 0) return "App";
    if (name.indexOf("OPEN_MARKET") >= 0) return "Market";
    return "Unknown";
  }

  private String text(String value) { return value == null ? "" : value; }
  private String errorMessage(Exception e) { return e.getMessage() == null ? e.toString() : e.getMessage(); }
  @SimpleEvent(description = "Fired after SDK initialization.") public void SdkInitialized() { EventDispatcher.dispatchEvent(this, "SdkInitialized"); }
  @SimpleEvent(description = "Fired when native ads are loaded.") public void AdLoaded(Object a, int count) { EventDispatcher.dispatchEvent(this, "AdLoaded", a, count); }
  @SimpleEvent(description = "Fired when native ad loading fails.") public void AdFailedToLoad(String message) { EventDispatcher.dispatchEvent(this, "AdFailedToLoad", message); }
  @SimpleEvent(description = "Fired for an invalid operation.") public void ErrorOccurred(String message) { EventDispatcher.dispatchEvent(this, "ErrorOccurred", message); }
}
