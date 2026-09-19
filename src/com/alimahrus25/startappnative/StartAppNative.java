package com.alimahrus25.startappnative;

import android.app.Activity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.annotations.UsesActivities;
import com.google.appinventor.components.annotations.UsesBroadcastReceivers;
import com.google.appinventor.components.annotations.UsesContentProviders;
import com.google.appinventor.components.annotations.UsesServices;
import com.google.appinventor.components.annotations.androidmanifest.ActivityElement;
import com.google.appinventor.components.annotations.androidmanifest.ActionElement;
import com.google.appinventor.components.annotations.androidmanifest.IntentFilterElement;
import com.google.appinventor.components.annotations.androidmanifest.ProviderElement;
import com.google.appinventor.components.annotations.androidmanifest.ReceiverElement;
import com.google.appinventor.components.annotations.androidmanifest.ServiceElement;
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

import com.startapp.sdk.ads.banner.Banner;
import com.startapp.sdk.ads.banner.BannerListener;

import com.startapp.sdk.ads.nativead.NativeAdDetails;
import com.startapp.sdk.ads.nativead.NativeAdPreferences;
import com.startapp.sdk.ads.nativead.StartAppNativeAd;

import java.util.ArrayList;

@DesignerComponent(
    version = 6,
    description = "Start.io Native Ads and Banner SDK 5.1.0 for Niotron",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    iconName = "aiwebres/icon.png"
)
@SimpleObject(external = true)
@UsesPermissions(
    permissionNames =
        "android.permission.INTERNET, " +
        "android.permission.ACCESS_NETWORK_STATE, " +
        "android.permission.ACCESS_WIFI_STATE, " +
        "android.permission.RECEIVE_BOOT_COMPLETED, " +
        "android.permission.BLUETOOTH, " +
        "android.permission.AD_ID, " +
        "com.google.android.gms.permission.AD_ID, " +
        "com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE, " +
        "android.permission.ACCESS_ADSERVICES_TOPICS"
)
@UsesActivities(activities = {
    @ActivityElement(
        name = "com.startapp.sdk.adsbase.consent.ConsentActivity",
        configChanges = "orientation|screenSize|screenLayout|keyboardHidden",
        theme = "@android:style/Theme.Translucent"
    ),
    @ActivityElement(
        name = "com.startapp.sdk.ads.list3d.List3DActivity",
        theme = "@android:style/Theme"
    ),
    @ActivityElement(
        name = "com.startapp.sdk.ads.interstitials.OverlayActivity",
        configChanges = "orientation|screenSize|screenLayout|keyboardHidden",
        theme = "@android:style/Theme.Translucent"
    )
})
@UsesServices(services = {
    @ServiceElement(
        name = "com.startapp.sdk.cachedservice.BackgroundService",
        exported = "false"
    ),
    @ServiceElement(
        name = "com.startapp.sdk.jobs.SchedulerService",
        permission = "android.permission.BIND_JOB_SERVICE",
        exported = "true"
    )
})
@UsesBroadcastReceivers(receivers = {
    @ReceiverElement(
        name = "com.startapp.sdk.adsbase.remoteconfig.BootCompleteListener",
        exported = "true",
        intentFilters = {
            @IntentFilterElement(
                actionElements = {
                    @ActionElement(
                        name = "android.intent.action.BOOT_COMPLETED"
                    )
                }
            )
        }
    )
})
@UsesContentProviders(providers = {
    @ProviderElement(
        name = "com.startapp.sdk.adsbase.StartAppInitProvider",
        authorities = "${applicationId}.startappinitprovider",
        exported = "false"
    )
})
public class StartAppNative
    extends AndroidNonvisibleComponent {

    private final Activity activity;

    // =========================================================
    // NATIVE
    // =========================================================

    private StartAppNativeAd nativeAd;
    private boolean initialized;

    // =========================================================
    // BANNER
    // =========================================================

    private Banner banner;
    private boolean bannerLoaded;
    private FrameLayout bannerContainer;

    public StartAppNative(ComponentContainer container) {
        super(container.$form());
        activity = container.$context();
    }

    // =========================================================
    // INITIALIZE
    // =========================================================

    @SimpleFunction(
        description =
            "Initializes Start.io SDK. " +
            "Call before LoadNativeAd or LoadBanner."
    )
    public void Initialize(
        String appId,
        boolean testMode
    ) {

        AdDebug("Initialize: started");

        if (
            appId == null ||
            appId.trim().length() == 0
        ) {

            AdDebug(
                "Initialize: App ID is empty"
            );

            ErrorOccurred(
                "App ID cannot be empty"
            );

            return;
        }

        try {

            final String normalizedAppId =
                appId.trim();

            AdDebug(
                "Initialize: creating SDKAdPreferences"
            );

            final SDKAdPreferences preferences =
                new SDKAdPreferences();

            AdDebug(
                "Initialize: calling " +
                "StartAppSDK.setTestAdsEnabled"
            );

            StartAppSDK.setTestAdsEnabled(
                testMode
            );

            AdDebug(
                "Initialize: testMode = " +
                String.valueOf(testMode)
            );

            AdDebug(
                "Initialize: calling " +
                "StartAppSDK.initParams"
            );

            StartAppSDK.initParams(
                activity,
                normalizedAppId
            )
                .setSdkAdPrefs(
                    preferences
                )
                .setAccountId(
                    normalizedAppId
                )
                .setCallback(
                    new Runnable() {
                        @Override
                        public void run() {

                            initialized = true;

                            AdDebug(
                                "Initialize: SDK callback received"
                            );

                            AdDebug(
                                "Initialize: initialized = true"
                            );

                            SdkInitialized();

                            AdDebug(
                                "Initialize: " +
                                "SdkInitialized event dispatched"
                            );
                        }
                    }
                )
                .init();

            AdDebug(
                "Initialize: StartAppSDK.initParams().init() completed"
            );

        } catch (Exception e) {

            AdDebug(
                "Initialize: EXCEPTION = " +
                errorMessage(e)
            );

            ErrorOccurred(
                errorMessage(e)
            );
        }
    }

    // =========================================================
    // BANNER
    // =========================================================

    @SimpleFunction(
        description =
            "Loads a Start.io Banner using the " +
            "default 320x50 dp size."
    )
    public void LoadBanner() {

        LoadBannerSize(
            320,
            50
        );
    }

    @SimpleFunction(
        description =
            "Loads a Start.io Banner. " +
            "Width and height are in dp."
    )
    public void LoadBannerSize(
        int widthDp,
        int heightDp
    ) {

        AdDebug(
            "LoadBanner: started"
        );

        if (!initialized) {

            AdDebug(
                "LoadBanner: FAILED - SDK not initialized"
            );

            BannerFailedToLoad(
                "Call Initialize before LoadBanner"
            );

            ErrorOccurred(
                "Call Initialize before LoadBanner"
            );

            return;
        }

        if (widthDp <= 0) {
            widthDp = 320;
        }

        if (heightDp <= 0) {
            heightDp = 50;
        }

        final int finalWidthDp =
            widthDp;

        final int finalHeightDp =
            heightDp;

        AdDebug(
            "LoadBanner: size = " +
            String.valueOf(finalWidthDp) +
            "x" +
            String.valueOf(finalHeightDp) +
            " dp"
        );

        try {

            RemoveBannerFromScreen();

            /*
             * =====================================================
             * 1. CREATE BANNER
             * =====================================================
             *
             * Match the APK test flow:
             *
             *     new Banner(activity)
             *
             * The listener is attached separately below.
             */
            AdDebug(
                "LoadBanner: creating Banner(activity)"
            );

            banner =
                new Banner(
                    activity
                );

            AdDebug(
                "LoadBanner: Banner created"
            );

            /*
             * =====================================================
             * 2. SET BANNER LISTENER
             * =====================================================
             */
            banner.setBannerListener(
                new BannerListener() {

                    @Override
                    public void onReceiveAd(
                        View view
                    ) {

                        bannerLoaded = true;

                        AdDebug(
                            "Banner onReceiveAd: callback received"
                        );

                        try {

                            if (banner != null) {

                                banner.setVisibility(
                                    View.VISIBLE
                                );

                                banner.showBanner();

                            }

                            AdDebug(
                                "Banner onReceiveAd: " +
                                "banner shown"
                            );

                            BannerLoaded();

                        } catch (Exception e) {

                            AdDebug(
                                "Banner onReceiveAd: " +
                                "EXCEPTION = " +
                                errorMessage(e)
                            );

                            BannerFailedToLoad(
                                "Banner display exception: " +
                                errorMessage(e)
                            );
                        }
                    }

                    @Override
                    public void onFailedToReceiveAd(
                        View view
                    ) {

                        bannerLoaded = false;

                        AdDebug(
                            "Banner onFailedToReceiveAd: " +
                            "callback received"
                        );

                        try {

                            String error =
                                banner != null
                                    ? banner.getErrorMessage()
                                    : null;

                            if (
                                error != null &&
                                error.trim().length() > 0
                            ) {

                                AdDebug(
                                    "Banner SDK error = " +
                                    error
                                );

                                BannerFailedToLoad(
                                    "Banner request failed: " +
                                    error
                                );

                            } else {

                                AdDebug(
                                    "Banner SDK error message is EMPTY"
                                );

                                BannerFailedToLoad(
                                    "Banner request failed " +
                                    "(SDK error message is empty)"
                                );
                            }

                        } catch (Exception e) {

                            AdDebug(
                                "Banner getErrorMessage " +
                                "EXCEPTION = " +
                                errorMessage(e)
                            );

                            BannerFailedToLoad(
                                "Banner request failed: " +
                                errorMessage(e)
                            );
                        }
                    }

                    @Override
                    public void onImpression(
                        View view
                    ) {

                        AdDebug(
                            "Banner onImpression"
                        );

                        BannerImpression();
                    }

                    @Override
                    public void onClick(
                        View view
                    ) {

                        AdDebug(
                            "Banner onClick"
                        );

                        BannerClicked();
                    }
                }
            );

            AdDebug(
                "LoadBanner: BannerListener set"
            );

            /*
             * =====================================================
             * 3. HIDE BANNER BEFORE LOAD
             * =====================================================
             */
            banner.setVisibility(
                View.GONE
            );

            /*
             * =====================================================
             * 4. ADD BANNER TO SCREEN
             * =====================================================
             *
             * The View is attached before loadAd(), matching
             * the APK test flow.
             */
            AdDebug(
                "LoadBanner: adding Banner to screen"
            );

            AddBannerToScreen(
                finalWidthDp,
                finalHeightDp
            );

            AdDebug(
                "LoadBanner: Banner added to screen"
            );

            /*
             * =====================================================
             * 5. LOAD AD
             * =====================================================
             *
             * The APK test flow uses loadAd() without dimensions.
             * The View dimensions are already supplied by
             * AddBannerToScreen().
             */
            AdDebug(
                "LoadBanner: calling banner.loadAd()"
            );

            banner.loadAd();

            AdDebug(
                "LoadBanner: banner.loadAd() returned"
            );

        } catch (Exception e) {

            AdDebug(
                "LoadBanner: EXCEPTION = " +
                errorMessage(e)
            );

            BannerFailedToLoad(
                errorMessage(e)
            );

            ErrorOccurred(
                errorMessage(e)
            );
        }
    }

    @SimpleFunction(
        description =
            "Shows the Start.io Banner."
    )
    public void ShowBanner() {

        if (banner == null) {

            AdDebug(
                "ShowBanner: no banner"
            );

            BannerFailedToLoad(
                "No banner has been created"
            );

            return;
        }

        try {

            banner.setVisibility(
                View.VISIBLE
            );

            /*
             * Start.io Banner API.
             */
            banner.showBanner();

            AdDebug(
                "ShowBanner: banner shown"
            );

        } catch (Exception e) {

            AdDebug(
                "ShowBanner: EXCEPTION = " +
                errorMessage(e)
            );

            ErrorOccurred(
                errorMessage(e)
            );
        }
    }

    @SimpleFunction(
        description =
            "Hides the Start.io Banner."
    )
    public void HideBanner() {

        if (banner == null) {

            AdDebug(
                "HideBanner: no banner"
            );

            return;
        }

        try {

            /*
             * Start.io Banner API.
             */
            banner.hideBanner();

            banner.setVisibility(
                View.GONE
            );

            AdDebug(
                "HideBanner: banner hidden"
            );

        } catch (Exception e) {

            AdDebug(
                "HideBanner: EXCEPTION = " +
                errorMessage(e)
            );

            ErrorOccurred(
                errorMessage(e)
            );
        }
    }

    /*
     * Adds the Banner View to the bottom-center
     * of the Activity.
     */
    private void AddBannerToScreen(
        int widthDp,
        int heightDp
    ) {

        if (banner == null) {
            return;
        }

        if (bannerContainer == null) {

            FrameLayout content =
                activity.findViewById(
                    android.R.id.content
                );

            if (content == null) {

                throw new IllegalStateException(
                    "Activity content view is NULL"
                );
            }

            bannerContainer =
                new FrameLayout(
                    activity
                );

            FrameLayout.LayoutParams
                containerParams =
                new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                );

            containerParams.gravity =
                Gravity.BOTTOM |
                Gravity.CENTER_HORIZONTAL;

            content.addView(
                bannerContainer,
                containerParams
            );
        }

        /*
         * Prevent the same Banner View from
         * having two parents.
         */
        if (banner.getParent() != null) {

            ((ViewGroup) banner.getParent())
                .removeView(banner);
        }

        FrameLayout.LayoutParams
            bannerParams =
            new FrameLayout.LayoutParams(
                dpToPx(widthDp),
                dpToPx(heightDp)
            );

        bannerParams.gravity =
            Gravity.BOTTOM |
            Gravity.CENTER_HORIZONTAL;

        bannerContainer.addView(
            banner,
            bannerParams
        );
    }

    private void RemoveBannerFromScreen() {

        if (
            banner != null &&
            banner.getParent() != null
        ) {

            ((ViewGroup) banner.getParent())
                .removeView(banner);
        }

        bannerLoaded = false;
    }

    private int dpToPx(int dp) {

        float density =
            activity
                .getResources()
                .getDisplayMetrics()
                .density;

        return Math.round(
            dp * density
        );
    }

    // =========================================================
    // NATIVE AD
    // =========================================================

    @SimpleFunction(
        description =
            "Loads native ads. Primary and secondary " +
            "image sizes use numeric values: " +
            "0=72x72, 1=100x100, 2=150x150, " +
            "3=340x340, 4=1200x628, 5=320x480, " +
            "6=480x320. Secondary supports only 0 to 3."
    )
    public void LoadNativeAd(
        int numberOfAds,
        int primaryImageSize,
        int secondaryImageSize
    ) {

        AdDebug(
            "LoadNativeAd: started"
        );

        if (!initialized) {

            AdDebug(
                "LoadNativeAd: FAILED - SDK not initialized"
            );

            ErrorOccurred(
                "Call Initialize before LoadNativeAd"
            );

            return;
        }

        AdDebug(
            "LoadNativeAd: SDK is initialized"
        );

        if (numberOfAds < 1) {
            numberOfAds = 1;
        }

        if (
            primaryImageSize < 0 ||
            primaryImageSize > 6
        ) {

            primaryImageSize = 2;
        }

        if (
            secondaryImageSize < 0 ||
            secondaryImageSize > 3
        ) {

            secondaryImageSize = 0;
        }

        AdDebug(
            "LoadNativeAd: numberOfAds = " +
            String.valueOf(numberOfAds)
        );

        AdDebug(
            "LoadNativeAd: primaryImageSize = " +
            String.valueOf(primaryImageSize)
        );

        AdDebug(
            "LoadNativeAd: secondaryImageSize = " +
            String.valueOf(secondaryImageSize)
        );

        try {

            AdDebug(
                "LoadNativeAd: creating StartAppNativeAd"
            );

            nativeAd =
                new StartAppNativeAd(
                    activity.getApplicationContext()
                );

            AdDebug(
                "LoadNativeAd: StartAppNativeAd created"
            );

            NativeAdPreferences preferences =
                new NativeAdPreferences();

            AdDebug(
                "LoadNativeAd: NativeAdPreferences created"
            );

            preferences.setAdsNumber(
                numberOfAds
            );

            AdDebug(
                "LoadNativeAd: setAdsNumber completed"
            );

            preferences.setPrimaryImageSize(
                primaryImageSize
            );

            AdDebug(
                "LoadNativeAd: " +
                "setPrimaryImageSize completed"
            );

            preferences.setSecondaryImageSize(
                secondaryImageSize
            );

            AdDebug(
                "LoadNativeAd: " +
                "setSecondaryImageSize completed"
            );

            preferences.setAutoBitmapDownload(
                true
            );

            AdDebug(
                "LoadNativeAd: " +
                "setAutoBitmapDownload(true) completed"
            );

            nativeAd.setPreferences(
                preferences
            );

            AdDebug(
                "LoadNativeAd: setPreferences completed"
            );

            AdDebug(
                "LoadNativeAd: calling nativeAd.loadAd()"
            );

            nativeAd.loadAd(
                new AdEventListener() {

                    @Override
                    public void onReceiveAd(
                        Ad ad
                    ) {

                        AdDebug(
                            "onReceiveAd: callback received"
                        );

                        if (ad == null) {

                            AdDebug(
                                "onReceiveAd: " +
                                "Ad object is NULL"
                            );

                        } else {

                            AdDebug(
                                "onReceiveAd: " +
                                "Ad object received"
                            );
                        }

                        try {

                            ArrayList<NativeAdDetails> ads =
                                nativeAd.getNativeAds();

                            if (ads == null) {

                                AdDebug(
                                    "onReceiveAd: " +
                                    "getNativeAds() returned NULL"
                                );

                                AdFailedToLoad(
                                    "getNativeAds() returned null"
                                );

                                return;
                            }

                            AdDebug(
                                "onReceiveAd: " +
                                "native ads count = " +
                                String.valueOf(
                                    ads.size()
                                )
                            );

                            if (ads.size() == 0) {

                                AdDebug(
                                    "onReceiveAd: " +
                                    "native ads list is EMPTY"
                                );

                                AdFailedToLoad(
                                    "SDK returned 0 native ads"
                                );

                            } else if (
                                ads.size() == 1
                            ) {

                                NativeAdDetails detail =
                                    ads.get(0);

                                if (detail == null) {

                                    AdDebug(
                                        "onReceiveAd: " +
                                        "first NativeAdDetails is NULL"
                                    );

                                    AdFailedToLoad(
                                        "NativeAdDetails is null"
                                    );

                                    return;
                                }

                                AdDebug(
                                    "onReceiveAd: " +
                                    "NativeAdDetails received"
                                );

                                AdLoaded(
                                    detail,
                                    1
                                );

                                AdDebug(
                                    "onReceiveAd: " +
                                    "AdLoaded event dispatched"
                                );

                            } else {

                                AdDebug(
                                    "onReceiveAd: " +
                                    "multiple native ads received"
                                );

                                AdLoaded(
                                    YailList.makeList(
                                        ads
                                    ),
                                    ads.size()
                                );

                                AdDebug(
                                    "onReceiveAd: " +
                                    "AdLoaded event dispatched, " +
                                    "count = " +
                                    String.valueOf(
                                        ads.size()
                                    )
                                );
                            }

                        } catch (Exception e) {

                            AdDebug(
                                "onReceiveAd: EXCEPTION = " +
                                errorMessage(e)
                            );

                            AdFailedToLoad(
                                "onReceiveAd exception: " +
                                errorMessage(e)
                            );
                        }
                    }

                    @Override
                    public void onFailedToReceiveAd(
                        Ad ad
                    ) {

                        AdDebug(
                            "onFailedToReceiveAd: " +
                            "callback received"
                        );

                        if (ad == null) {

                            AdDebug(
                                "onFailedToReceiveAd: " +
                                "Ad object = NULL"
                            );

                        } else {

                            AdDebug(
                                "onFailedToReceiveAd: " +
                                "Ad object exists"
                            );

                            try {

                                String error =
                                    ad.getErrorMessage();

                                if (
                                    error != null &&
                                    error.trim().length() > 0
                                ) {

                                    AdDebug(
                                        "onFailedToReceiveAd: " +
                                        "SDK error = " +
                                        error
                                    );

                                } else {

                                    AdDebug(
                                        "onFailedToReceiveAd: " +
                                        "SDK error message is EMPTY"
                                    );
                                }

                            } catch (Exception e) {

                                AdDebug(
                                    "onFailedToReceiveAd: " +
                                    "getErrorMessage EXCEPTION = " +
                                    errorMessage(e)
                                );
                            }
                        }

                        Log.e(
                            "StartAppNative",
                            "Native ad loading failed"
                        );

                        AdFailedToLoad(
                            "Native ad request failed"
                        );
                    }
                }
            );

            AdDebug(
                "LoadNativeAd: nativeAd.loadAd() returned"
            );

        } catch (Exception e) {

            AdDebug(
                "LoadNativeAd: EXCEPTION = " +
                errorMessage(e)
            );

            ErrorOccurred(
                errorMessage(e)
            );
        }
    }

    // =========================================================
    // NATIVE IMAGE SIZE
    // =========================================================

    @SimpleFunction(
        description =
            "Returns 0 for 72x72, 1 for 100x100, " +
            "2 for 150x150, 3 for 340x340, " +
            "4 for 1200x628, 5 for 320x480, " +
            "and 6 for 480x320."
    )
    public int ImageSizeCode(
        String size
    ) {

        if (size == null) {
            return 2;
        }

        String s =
            size.trim().toUpperCase();

        if (s.equals("72X72")) {
            return 0;
        }

        if (s.equals("100X100")) {
            return 1;
        }

        if (s.equals("150X150")) {
            return 2;
        }

        if (s.equals("340X340")) {
            return 3;
        }

        if (s.equals("1200X628")) {
            return 4;
        }

        if (s.equals("320X480")) {
            return 5;
        }

        if (s.equals("480X320")) {
            return 6;
        }

        return 2;
    }

    // =========================================================
    // NATIVE CLICK REGISTRATION
    // =========================================================

    @SimpleFunction(
        description =
            "Registers a visible component for native-ad clicks."
    )
    public void RegisterContainerForClick(
        Object adDetails,
        AndroidViewComponent container
    ) {

        if (
            adDetails instanceof NativeAdDetails &&
            container != null &&
            container.getView() != null
        ) {

            ((NativeAdDetails) adDetails)
                .registerViewForInteraction(
                    container.getView()
                );

        } else {

            ErrorOccurred(
                "RegisterContainerForClick requires " +
                "a Native Ad and a visible container"
            );
        }
    }

    // =========================================================
    // NATIVE GETTERS
    // =========================================================

    @SimpleFunction(
        description =
            "Returns the ad title."
    )
    public String GetAdTitle(
        Object a
    ) {

        return value(
            a,
            "GetAdTitle",
            0
        );
    }

    @SimpleFunction(
        description =
            "Returns the ad description."
    )
    public String GetAdDescription(
        Object a
    ) {

        return value(
            a,
            "GetAdDescription",
            1
        );
    }

    @SimpleFunction(
        description =
            "Returns the ad rating."
    )
    public String GetAdRating(
        Object a
    ) {

        return value(
            a,
            "GetAdRating",
            2
        );
    }

    @SimpleFunction(
        description =
            "Returns the primary image URL."
    )
    public String GetAdImageUrl(
        Object a
    ) {

        return value(
            a,
            "GetAdImageUrl",
            3
        );
    }

    @SimpleFunction(
        description =
            "Returns the secondary image URL."
    )
    public String GetAdSecondaryImageUrl(
        Object a
    ) {

        return value(
            a,
            "GetAdSecondaryImageUrl",
            4
        );
    }

    @SimpleFunction(
        description =
            "Returns the call-to-action text."
    )
    public String GetAdCallToAction(
        Object a
    ) {

        return value(
            a,
            "GetAdCallToAction",
            5
        );
    }

    @SimpleFunction(
        description =
            "Returns the install text."
    )
    public String GetAdInstalls(
        Object a
    ) {

        return value(
            a,
            "GetAdInstalls",
            6
        );
    }

    @SimpleFunction(
        description =
            "Returns the ad category."
    )
    public String GetAdCategory(
        Object a
    ) {

        return value(
            a,
            "GetAdCategory",
            7
        );
    }

    @SimpleFunction(
        description =
            "Returns the advertised package name."
    )
    public String GetPackageName(
        Object a
    ) {

        return value(
            a,
            "GetPackageName",
            8
        );
    }

    private String value(
        Object object,
        String method,
        int field
    ) {

        if (
            !(object instanceof NativeAdDetails)
        ) {

            ErrorOccurred(
                method +
                " requires a Native Ad as input"
            );

            return "";
        }

        NativeAdDetails ad =
            (NativeAdDetails) object;

        switch (field) {

            case 0:
                return text(
                    ad.getTitle()
                );

            case 1:
                return text(
                    ad.getDescription()
                );

            case 2:
                return String.valueOf(
                    ad.getRating()
                );

            case 3:
                return text(
                    ad.getImageUrl()
                );

            case 4:
                return text(
                    ad.getSecondaryImageUrl()
                );

            case 5:
                return text(
                    ad.getCallToAction()
                );

            case 6:
                return text(
                    ad.getInstalls()
                );

            case 7:
                return text(
                    ad.getCategory()
                );

            default:
                return text(
                    ad.getPackageName()
                );
        }
    }

    @SimpleFunction(
        description =
            "Returns App, Market, or Unknown " +
            "for the campaign action."
    )
    public String GetAdCampaignActionType(
        Object a
    ) {

        if (
            !(a instanceof NativeAdDetails)
        ) {

            ErrorOccurred(
                "GetAdCampaignActionType requires " +
                "a Native Ad as input"
            );

            return "";
        }

        Object action =
            ((NativeAdDetails) a)
                .getCampaignAction();

        if (action == null) {
            return "Unknown";
        }

        String name =
            action.toString();

        if (
            name.indexOf(
                "LAUNCH_APP"
            ) >= 0
        ) {

            return "App";
        }

        if (
            name.indexOf(
                "OPEN_MARKET"
            ) >= 0
        ) {

            return "Market";
        }

        return "Unknown";
    }

    private String text(
        String value
    ) {

        return value == null
            ? ""
            : value;
    }

    private String errorMessage(
        Exception e
    ) {

        return e.getMessage() == null
            ? e.toString()
            : e.getMessage();
    }

    // =========================================================
    // EVENTS
    // =========================================================

    @SimpleEvent(
        description =
            "Provides debug information during " +
            "SDK, Banner, and Native Ad loading."
    )
    public void AdDebug(
        String message
    ) {

        EventDispatcher.dispatchEvent(
            this,
            "AdDebug",
            message
        );
    }

    @SimpleEvent(
        description =
            "Fired after SDK initialization."
    )
    public void SdkInitialized() {

        EventDispatcher.dispatchEvent(
            this,
            "SdkInitialized"
        );
    }

    // =========================================================
    // BANNER EVENTS
    // =========================================================

    @SimpleEvent(
        description =
            "Fired when a Banner is successfully loaded."
    )
    public void BannerLoaded() {

        EventDispatcher.dispatchEvent(
            this,
            "BannerLoaded"
        );
    }

    @SimpleEvent(
        description =
            "Fired when Banner loading fails."
    )
    public void BannerFailedToLoad(
        String message
    ) {

        EventDispatcher.dispatchEvent(
            this,
            "BannerFailedToLoad",
            message
        );
    }

    @SimpleEvent(
        description =
            "Fired when the Banner generates an impression."
    )
    public void BannerImpression() {

        EventDispatcher.dispatchEvent(
            this,
            "BannerImpression"
        );
    }

    @SimpleEvent(
        description =
            "Fired when the Banner is clicked."
    )
    public void BannerClicked() {

        EventDispatcher.dispatchEvent(
            this,
            "BannerClicked"
        );
    }

    // =========================================================
    // NATIVE EVENTS
    // =========================================================

    @SimpleEvent(
        description =
            "Fired when native ads are loaded."
    )
    public void AdLoaded(
        Object a,
        int count
    ) {

        EventDispatcher.dispatchEvent(
            this,
            "AdLoaded",
            a,
            count
        );
    }

    @SimpleEvent(
        description =
            "Fired when native ad loading fails."
    )
    public void AdFailedToLoad(
        String message
    ) {

        EventDispatcher.dispatchEvent(
            this,
            "AdFailedToLoad",
            message
        );
    }

    @SimpleEvent(
        description =
            "Fired for an invalid operation."
    )
    public void ErrorOccurred(
        String message
    ) {

        EventDispatcher.dispatchEvent(
            this,
            "ErrorOccurred",
            message
        );
    }
}
