package com.onecat.godotcas;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import org.godotengine.godot.Dictionary;
import org.godotengine.godot.Godot;
import org.godotengine.godot.plugin.GodotPlugin;
import org.godotengine.godot.plugin.SignalInfo;
import org.godotengine.godot.plugin.UsedByGodot;

import com.cleveradssolutions.sdk.AdContentInfo;
import com.cleveradssolutions.sdk.AdFormat;
import com.cleveradssolutions.sdk.OnAdImpressionListener;
import com.cleveradssolutions.sdk.screen.CASAppOpen;
import com.cleveradssolutions.sdk.screen.CASInterstitial;
import com.cleveradssolutions.sdk.screen.CASRewarded;
import com.cleveradssolutions.sdk.screen.OnRewardEarnedListener;
import com.cleveradssolutions.sdk.screen.ScreenAdContentCallback;
import com.cleversolutions.ads.AdError;
import com.cleversolutions.ads.AdSize;
import com.cleversolutions.ads.AdStatusHandler;
import com.cleversolutions.ads.AdViewListener;
import com.cleversolutions.ads.ConsentFlow;
import com.cleversolutions.ads.InitialConfiguration;
import com.cleversolutions.ads.InitializationListener;
import com.cleversolutions.ads.TargetingOptions;
import com.cleversolutions.ads.android.CAS;
import com.cleversolutions.ads.android.CAS.ManagerBuilder;
import com.cleversolutions.ads.android.CASBannerView;

@SuppressWarnings({"unused"})
public class GodotCas extends GodotPlugin {
    private final Activity activity;
    private FrameLayout layout = null;
    private boolean signalsRegistered = false;

    private String casId;
    private boolean useBuiltInConsentManager = true;
    private boolean testAdMode = false;
    private String tenjinKey = null;
    private boolean fbLDUEnabled = false;
    private int debugGeography = ConsentFlow.DebugGeography.DISABLED;
    private boolean consentForceTesting = false;
    private Set<String> testDevices = new HashSet<>();

    private CASInterstitial interstitialAd;
    private CASRewarded rewardedAd;
    private CASBannerView bannerAd;
    private CASAppOpen appOpenAd;

    public GodotCas(Godot godot) {
        super(godot);
        activity = getActivity();
    }

    @Override
    public View onMainCreate(Activity activity) {
        layout = new FrameLayout(activity);
        return layout;
    }

    @Override
    public void onMainDestroy() {
        if (interstitialAd != null) { interstitialAd.destroy(); }
        if (rewardedAd != null) { rewardedAd.destroy(); }
        if (bannerAd != null) { bannerAd.destroy(); }
        if (appOpenAd != null) { appOpenAd.destroy(); }
        super.onMainDestroy();
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "GodotCas";
    }

    @NonNull
    @Override
    public Set<SignalInfo> getPluginSignals() {
        Set<SignalInfo> signalInfoSet = new HashSet<>();
        // Basic
        signalInfoSet.add(new SignalInfo("initialization_finished", String.class, String.class, Boolean.class, Integer.class));
        signalInfoSet.add(new SignalInfo("consent_flow_status_changed", Integer.class));
        // Interstitial
        signalInfoSet.add(new SignalInfo("interstitial_loaded", Dictionary.class));
        signalInfoSet.add(new SignalInfo("interstitial_failed_to_load", String.class, String.class));
        signalInfoSet.add(new SignalInfo("interstitial_failed_to_show", String.class, String.class));
        signalInfoSet.add(new SignalInfo("interstitial_showed", Dictionary.class));
        signalInfoSet.add(new SignalInfo("interstitial_clicked", Dictionary.class));
        signalInfoSet.add(new SignalInfo("interstitial_dismissed", Dictionary.class));
        signalInfoSet.add(new SignalInfo("interstitial_impression", Dictionary.class));
        // Rewarded
        signalInfoSet.add(new SignalInfo("rewarded_loaded", Dictionary.class));
        signalInfoSet.add(new SignalInfo("rewarded_failed_to_load", String.class, String.class));
        signalInfoSet.add(new SignalInfo("rewarded_failed_to_show", String.class, String.class));
        signalInfoSet.add(new SignalInfo("rewarded_showed", Dictionary.class));
        signalInfoSet.add(new SignalInfo("rewarded_clicked", Dictionary.class));
        signalInfoSet.add(new SignalInfo("rewarded_dismissed", Dictionary.class));
        signalInfoSet.add(new SignalInfo("reward_earned", Dictionary.class));
        signalInfoSet.add(new SignalInfo("rewarded_impression", Dictionary.class));
        // Banner
        signalInfoSet.add(new SignalInfo("banner_loaded"));
        signalInfoSet.add(new SignalInfo("banner_failed", String.class));
        signalInfoSet.add(new SignalInfo("banner_clicked"));
        signalInfoSet.add(new SignalInfo("banner_presented", Dictionary.class));
        signalInfoSet.add(new SignalInfo("banner_impression", Dictionary.class));
        // AppOpen ads
        signalInfoSet.add(new SignalInfo("app_open_ad_loaded", Dictionary.class));
        signalInfoSet.add(new SignalInfo("app_open_ad_failed_to_load", String.class, String.class));
        signalInfoSet.add(new SignalInfo("app_open_ad_failed_to_show", String.class, String.class));
        signalInfoSet.add(new SignalInfo("app_open_ad_showed", Dictionary.class));
        signalInfoSet.add(new SignalInfo("app_open_ad_clicked", Dictionary.class));
        signalInfoSet.add(new SignalInfo("app_open_ad_dismissed", Dictionary.class));
        signalInfoSet.add(new SignalInfo("app_open_ad_impression", Dictionary.class));

        signalsRegistered = true;
        return signalInfoSet;
    }

    @UsedByGodot
    public void setCasId(String id) {
        casId = id;
    }

    @UsedByGodot
    public void setUseBuiltInConsentManager(boolean enabled) {
        useBuiltInConsentManager = enabled;
    }

    @UsedByGodot
    public void setTestAdMode(boolean enabled) {
        testAdMode = enabled;
    }

    @UsedByGodot
    public void setTenjinKey(String key) {
        tenjinKey = key;
    }

    @UsedByGodot
    public void setFacebookLDU(boolean enabled) {
        fbLDUEnabled = enabled;
    }

    @UsedByGodot
    public void setDebugGeography(int geography) {
        debugGeography = geography;
    }

    @UsedByGodot
    public void setConsentForceTesting(boolean enabled) {
        consentForceTesting = enabled;
    }

    @UsedByGodot
    public void setMutedAdSounds(boolean enabled) {
        CAS.settings.setMutedAdSounds(enabled);
    }

    @UsedByGodot
    public void setTaggedAudience(int audienceType) {
        CAS.settings.setTaggedAudience(audienceType);
    }

    @UsedByGodot
    public void setDebugMode(boolean enabled) {
        CAS.settings.setDebugMode(enabled);
    }

    @UsedByGodot
    public void addTestDeviceId(String id) {
        testDevices.add(id);
        CAS.settings.setTestDeviceIDs(testDevices);
    }

    @UsedByGodot
    public void initializeCAS(String engineVersion) {
        ManagerBuilder builder = CAS.buildManager()
                .withCasId(casId)
                .withConsentFlow(new ConsentFlow(useBuiltInConsentManager))
                .withTestAdMode(testAdMode)
                .withFramework("Godot", engineVersion)
                .withCompletionListener(new InitializationListener() {
                    @Override
                    public void onCASInitialized(@NonNull InitialConfiguration initialConfiguration) {
                        @Nullable String initError = initialConfiguration.getError();
                        // Godot strings can't be null
                        if (initError == null) { initError = ""; }
                        @Nullable String userCountryISO2 = initialConfiguration.getCountryCode();
                        if (userCountryISO2 == null) { userCountryISO2 = ""; }
                        boolean protectionApplied  = initialConfiguration.isConsentRequired();
                        int consentFlowStatus = initialConfiguration.getConsentFlowStatus();
                        emitSignal("initialization_finished", initError, userCountryISO2, protectionApplied, consentFlowStatus);
                    }
                });
        if (tenjinKey != null) {
            builder.withMediationExtras("tenjin_key", tenjinKey);
        }
        if (fbLDUEnabled) {
            builder.withMediationExtras("FB_dp", "LDU");
        }
        else {
            builder.withMediationExtras("FB_dp", "");
        }
        builder.build(activity);
    }

    @UsedByGodot
    public void initializeInterstitial() {
        interstitialAd = new CASInterstitial(casId);
        ScreenAdContentCallback adContentCallback = new ScreenAdContentCallback() {
            @Override
            public void onAdLoaded(@NonNull AdContentInfo adContentInfo) {
                emitSignal("interstitial_loaded", adContentInfoToDictionary(adContentInfo));
            }

            @Override
            public void onAdFailedToLoad(@NonNull AdFormat adFormat, @NonNull AdError adError) {
                emitSignal("interstitial_failed_to_load", adFormat.toString(), adError.toString());
            }

            @Override
            public void onAdFailedToShow(@NonNull AdFormat adFormat, @NonNull AdError adError) {
                emitSignal("interstitial_failed_to_show", adFormat.toString(), adError.toString());
            }

            @Override
            public void onAdShowed(@NonNull AdContentInfo adContentInfo) {
                emitSignal("interstitial_showed", adContentInfoToDictionary(adContentInfo));
            }

            @Override
            public void onAdClicked(@NonNull AdContentInfo adContentInfo) {
                emitSignal("interstitial_clicked", adContentInfoToDictionary(adContentInfo));
            }

            @Override
            public void onAdDismissed(@NonNull AdContentInfo adContentInfo) {
                emitSignal("interstitial_dismissed", adContentInfoToDictionary(adContentInfo));
            }
        };
        interstitialAd.setContentCallback(adContentCallback);
        OnAdImpressionListener impressionListener = new OnAdImpressionListener() {
            @Override
            public void onAdImpression(@NonNull AdContentInfo adContentInfo) {
                emitSignal("interstitial_impression", adContentInfoToDictionary(adContentInfo));
            }
        };
        interstitialAd.setOnImpressionListener(impressionListener);
    }

    @UsedByGodot
    public void loadInterstitial() {
        if (interstitialAd == null) {
            Log.w("Godot CAS Plugin", "Interstitial is not initialized, can't loadInterstitial()");
            return;
        }
        interstitialAd.load(activity);
    }

    @UsedByGodot
    public boolean isInterstitialLoaded() {
        if (interstitialAd == null) {
            Log.w("Godot CAS Plugin", "Interstitial is not initialized, isInterstitialLoaded() defaults to false");
            return false;
        }
        return interstitialAd.isLoaded();
    }

    @UsedByGodot
    public void showInterstitial() {
        if (interstitialAd == null) {
            Log.w("Godot CAS Plugin", "Interstitial is not initialized, can't showInterstitial()");
            return;
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                interstitialAd.show(activity);
            }
        });
    }

    @UsedByGodot
    public void setAutoloadInterstitial(boolean enabled) {
        if (interstitialAd == null) {
            Log.w("Godot CAS Plugin", "Interstitial is not initialized, can't setAutoloadInterstitial()");
            return;
        }
        interstitialAd.setAutoloadEnabled(enabled);
    }

    @UsedByGodot
    public void setMinIntervalInterstitial(int seconds) {
        if (interstitialAd == null) {
            Log.w("Godot CAS Plugin", "Interstitial is not initialized, can't setMinIntervalInterstitial()");
            return;
        }
        interstitialAd.setMinInterval(seconds);
    }

    @UsedByGodot
    public void restartIntervalInterstitial() {
        if (interstitialAd == null) {
            Log.w("Godot CAS Plugin", "Interstitial is not initialized, can't restartIntervalInterstitial()");
            return;
        }
        interstitialAd.restartInterval();
    }

    @UsedByGodot
    public void initializeRewarded() {
        rewardedAd = new CASRewarded(casId);
        ScreenAdContentCallback adContentCallback = new ScreenAdContentCallback() {
            @Override
            public void onAdLoaded(@NonNull AdContentInfo adContentInfo) {
                emitSignal("rewarded_loaded", adContentInfoToDictionary(adContentInfo));
            }

            @Override
            public void onAdFailedToLoad(@NonNull AdFormat adFormat, @NonNull AdError adError) {
                emitSignal("rewarded_failed_to_load", adFormat.toString(), adError.toString());
            }

            @Override
            public void onAdFailedToShow(@NonNull AdFormat adFormat, @NonNull AdError adError) {
                emitSignal("rewarded_failed_to_show", adFormat.toString(), adError.toString());
            }

            @Override
            public void onAdShowed(@NonNull AdContentInfo adContentInfo) {
                emitSignal("rewarded_showed", adContentInfoToDictionary(adContentInfo));
            }

            @Override
            public void onAdClicked(@NonNull AdContentInfo adContentInfo) {
                emitSignal("rewarded_clicked", adContentInfoToDictionary(adContentInfo));
            }

            @Override
            public void onAdDismissed(@NonNull AdContentInfo adContentInfo) {
                emitSignal("rewarded_dismissed", adContentInfoToDictionary(adContentInfo));
            }
        };
        rewardedAd.setContentCallback(adContentCallback);
        OnAdImpressionListener impressionListener = new OnAdImpressionListener() {
            @Override
            public void onAdImpression(@NonNull AdContentInfo adContentInfo) {
                emitSignal("rewarded_impression", adContentInfoToDictionary(adContentInfo));
            }
        };
        rewardedAd.setOnImpressionListener(impressionListener);
    }

    @UsedByGodot
    public void loadRewarded() {
        if (rewardedAd == null) {
            Log.w("Godot CAS Plugin", "Rewarded is not initialized, can't loadRewarded()");
            return;
        }
        rewardedAd.load(activity);
    }

    @UsedByGodot
    public boolean isRewardedLoaded() {
        if (rewardedAd == null) {
            Log.w("Godot CAS Plugin", "Rewarded is not initialized, isRewardedLoaded() defaults to false");
            return false;
        }
        return rewardedAd.isLoaded();
    }

    @UsedByGodot
    public void showRewarded() {
        if (rewardedAd == null) {
            Log.w("Godot CAS Plugin", "Rewarded is not initialized, can't showRewarded()");
            return;
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                rewardedAd.show(activity, new OnRewardEarnedListener() {
                    @Override
                    public void onUserEarnedReward(@NonNull AdContentInfo adContentInfo) {
                        emitSignal("reward_earned", adContentInfoToDictionary(adContentInfo));
                    }
                });
            }
        });
    }

    @UsedByGodot
    public void setAutoloadRewarded(boolean enabled) {
        if (rewardedAd == null) {
            Log.w("Godot CAS Plugin", "Rewarded is not initialized, can't setAutoloadRewarded()");
            return;
        }
        rewardedAd.setAutoloadEnabled(enabled);
    }

    @UsedByGodot
    public void setRewardedExtraFillInterstitial(boolean enabled) {
        if (rewardedAd == null) {
            Log.w("Godot CAS Plugin", "Rewarded is not initialized, can't setRewardedExtraFillInterstitial()");
            return;
        }
        rewardedAd.setExtraFillInterstitialAdEnabled(enabled);
    }

    @UsedByGodot
    public void initializeBanner(int bannerSize) {
        bannerAd = new CASBannerView(activity, casId);
        AdSize adSize;
        switch (bannerSize) {
            case 1:
                adSize = AdSize.LEADERBOARD;
                break;
            case 2:
                adSize = AdSize.MEDIUM_RECTANGLE;
                break;
            case 3:
                adSize = AdSize.getSmartBanner(activity);
                break;
            default:
                adSize = AdSize.BANNER;
        }
        bannerAd.setSize(adSize);
        bannerAd.setAdListener(createBannerCallbacks());
        OnAdImpressionListener impressionListener = new OnAdImpressionListener() {
            @Override
            public void onAdImpression(@NonNull AdContentInfo adContentInfo) {
                emitSignal("banner_impression", adContentInfoToDictionary(adContentInfo));
            }
        };
        bannerAd.setOnImpressionListener(impressionListener);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                layout.removeAllViews();
                layout.addView(bannerAd, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            }
        });
    }

    @UsedByGodot
    public void initializeAdaptiveBanner(int maxWidthDpi) {
        bannerAd = new CASBannerView(activity, casId);
        bannerAd.setSize(AdSize.getAdaptiveBanner(activity, maxWidthDpi));
        bannerAd.setAdListener(createBannerCallbacks());
        OnAdImpressionListener impressionListener = new OnAdImpressionListener() {
            @Override
            public void onAdImpression(@NonNull AdContentInfo adContentInfo) {
                emitSignal("banner_impression", adContentInfoToDictionary(adContentInfo));
            }
        };
        bannerAd.setOnImpressionListener(impressionListener);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                layout.removeAllViews();
                layout.addView(bannerAd, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            }
        });
    }

    @UsedByGodot
    public void setBannerPosition() {}

    @UsedByGodot
    public void loadBanner() {
        if (bannerAd == null) {
            Log.w("Godot CAS Plugin", "Banner is not initialized, can't loadBanner()");
            return;
        }
        bannerAd.load();
    }

    @UsedByGodot
    public boolean isBannerLoaded() {
        if (bannerAd == null) {
            Log.w("Godot CAS Plugin", "Banner is not initialized, isBannerLoaded() defaults to false");
            return false;
        }
        return bannerAd.isLoaded();
    }

    @UsedByGodot
    public void setBannerVisible(boolean visible) {
        if (bannerAd == null) {
            Log.w("Godot CAS Plugin", "Banner is not initialized, can't setBannerVisible()");
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (visible) {
                    bannerAd.setVisibility(View.VISIBLE);
                    return;
                }
                bannerAd.setVisibility(View.GONE);
            }
        });
    }

    @UsedByGodot
    public void setAutoloadBanner(boolean enabled) {
        if (bannerAd == null) {
            Log.w("Godot CAS Plugin", "Banner is not initialized, can't setAutoloadBanner()");
            return;
        }
        bannerAd.setAutoloadEnabled(enabled);
    }

    @UsedByGodot
    public void setBannerRefreshInterval(int seconds) {
        if (bannerAd == null) {
            Log.w("Godot CAS Plugin", "Banner is not initialized, can't setBannerRefreshInterval()");
            return;
        }
        bannerAd.setRefreshInterval(seconds);
    }

    @UsedByGodot
    public void disableBannerAdRefresh() {
        if (bannerAd == null) {
            Log.w("Godot CAS Plugin", "Banner is not initialized, can't disableBannerAdRefresh()");
            return;
        }
        bannerAd.disableAdRefresh();
    }

    @UsedByGodot
    public void setBannerPosition(float x, float y) {
        if (bannerAd == null) {
            Log.w("Godot CAS Plugin", "Banner is not initialized, can't setBannerPosition()");
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bannerAd.setX(x);
                bannerAd.setY(y);
            }
        });
    }

    @UsedByGodot
    public int getBannerWidth() {
        if (bannerAd == null) {
            Log.w("Godot CAS Plugin", "Banner is not initialized, can't getBannerWidth()");
            return 0;
        }
        return bannerAd.getSize().widthPixels(activity);
    }

    @UsedByGodot
    public int getBannerHeight() {
        if (bannerAd == null) {
            Log.w("Godot CAS Plugin", "Banner is not initialized, can't getBannerHeight()");
            return 0;
        }
        return bannerAd.getSize().heightPixels(activity);
    }

    @UsedByGodot
    public void initializeAppOpenAd() {
        appOpenAd = new CASAppOpen(casId);
        ScreenAdContentCallback adContentCallback = new ScreenAdContentCallback() {
            @Override
            public void onAdLoaded(@NonNull AdContentInfo adContentInfo) {
                emitSignal("app_open_ad_loaded", adContentInfoToDictionary(adContentInfo));
            }

            @Override
            public void onAdFailedToLoad(@NonNull AdFormat adFormat, @NonNull AdError adError) {
                emitSignal("app_open_ad_failed_to_load", adFormat.toString(), adError.toString());
            }

            @Override
            public void onAdFailedToShow(@NonNull AdFormat adFormat, @NonNull AdError adError) {
                emitSignal("app_open_ad_failed_to_show", adFormat.toString(), adError.toString());
            }

            @Override
            public void onAdShowed(@NonNull AdContentInfo adContentInfo) {
                emitSignal("app_open_ad_showed", adContentInfoToDictionary(adContentInfo));
            }

            @Override
            public void onAdClicked(@NonNull AdContentInfo adContentInfo) {
                emitSignal("app_open_ad_clicked", adContentInfoToDictionary(adContentInfo));
            }

            @Override
            public void onAdDismissed(@NonNull AdContentInfo adContentInfo) {
                emitSignal("app_open_ad_dismissed", adContentInfoToDictionary(adContentInfo));
            }
        };
        appOpenAd.setContentCallback(adContentCallback);
        OnAdImpressionListener impressionListener = new OnAdImpressionListener() {
            @Override
            public void onAdImpression(@NonNull AdContentInfo adContentInfo) {
                emitSignal("app_open_ad_impression", adContentInfoToDictionary(adContentInfo));
            }
        };
        appOpenAd.setOnImpressionListener(impressionListener);
    }

    @UsedByGodot
    public void loadAppOpenAd() {
        if (appOpenAd == null) {
            Log.w("Godot CAS Plugin", "AppOpen ad is not initialized, can't loadAppOpenAd()");
            return;
        }
        appOpenAd.load(activity);
    }

    @UsedByGodot
    public boolean isAppOpenAdLoaded() {
        if (appOpenAd == null) {
            Log.w("Godot CAS Plugin", "AppOpen ad is not initialized, isAppOpenAdLoaded() defaults to false");
            return false;
        }
        return appOpenAd.isLoaded();
    }

    @UsedByGodot
    public void showAppOpenAd() {
        if (appOpenAd == null) {
            Log.w("Godot CAS Plugin", "AppOpen ad is not initialized, can't showAppOpenAd()");
            return;
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                appOpenAd.show(activity);
            }
        });
    }

    @UsedByGodot
    public void setAutoloadAppOpenAd(boolean enabled) {
        if (appOpenAd == null) {
            Log.w("Godot CAS Plugin", "AppOpen ad is not initialized, can't setAutoloadAppOpenAd()");
            return;
        }
        appOpenAd.setAutoloadEnabled(enabled);
    }

    @UsedByGodot
    public void setAutoshowAppOpenAd(boolean enabled) {
        if (appOpenAd == null) {
            Log.w("Godot CAS Plugin", "AppOpen ad is not initialized, can't setAutoshowAppOpenAd()");
            return;
        }
        appOpenAd.setAutoshowEnabled(enabled);
    }

    @UsedByGodot
    public void showConsentManagerIfRequired() {
        new ConsentFlow()
                .withDebugGeography(debugGeography)
                .withForceTesting(consentForceTesting)
                .withDismissListener(new ConsentFlow.OnDismissListener() {
                    @Override
                    public void onConsentFlowDismissed(int status) {
                        emitSignal("consent_flow_status_changed", status);
                    }
                })
                .showIfRequired();
    }

    @UsedByGodot
    public void showConsentManager() {
        new ConsentFlow()
                .withDebugGeography(debugGeography)
                .withForceTesting(consentForceTesting)
                .withDismissListener(new ConsentFlow.OnDismissListener() {
                    @Override
                    public void onConsentFlowDismissed(int status) {
                        emitSignal("consent_flow_status_changed", status);
                    }
                })
                .show();
    }

    @UsedByGodot
    public void setConsentStatus(int status) {
        CAS.settings.setUserConsent(status);
    }

    @UsedByGodot
    public void setCcpaStatus(int status) {
        CAS.settings.setCcpaStatus(status);
    }

    @UsedByGodot
    public Dictionary getPossibleConsentFlowStatuses() {
        return fieldsToDictionary(ConsentFlow.Status.class.getDeclaredFields());
    }

    @UsedByGodot
    public Dictionary getPossibleDebugGeographies() {
        return fieldsToDictionary(ConsentFlow.DebugGeography.class.getDeclaredFields());
    }

    @UsedByGodot
    public String getSdkVersion() {
        return CAS.getSDKVersion();
    }

    @UsedByGodot
    public void setTrialAdFreeInterval(int seconds) {
        CAS.settings.setTrialAdFreeInterval(seconds);
    }

    @UsedByGodot
    public void setUserID(String userID) {
        CAS.getTargetingOptions().setUserID(userID);
    }

    @UsedByGodot
    public void setUserAge(int age) {
        CAS.getTargetingOptions().setAge(age);
    }

    @UsedByGodot
    public void setUserGender(int gender) {
        TargetingOptions targetingOptions;
        switch (gender) {
            case 0:
                CAS.getTargetingOptions().setGender(TargetingOptions.GENDER_UNKNOWN);
                break;
            case 1:
                CAS.getTargetingOptions().setGender(TargetingOptions.GENDER_MALE);
                break;
            case 2:
                CAS.getTargetingOptions().setGender(TargetingOptions.GENDER_FEMALE);
        }
    }

    @UsedByGodot
    public void setUserLocationAutocollection(boolean enabled) {
        CAS.getTargetingOptions().setLocationCollectionEnabled(enabled);
    }

    @UsedByGodot
    public void setUserAppKeywords(String[] keywords) {
        CAS.getTargetingOptions().setKeywords(Set.of(keywords));
    }

    @UsedByGodot
    public void setAppContentUrl(String contentUrl) {
        CAS.getTargetingOptions().setContentUrl(contentUrl);
    }

    /**
     * I currently have no idea how to create a Location object from inside the engine
     */

    /*
    @UsedByGodot
    public void setUserLocation(String location) {
        CAS.getTargetingOptions().setLocation(new Location(location));
    }
     */

    @NonNull
    private Dictionary fieldsToDictionary(@NonNull Field[] fields) {
        Dictionary dictionary = new Dictionary();
        for (Field f : fields) {
            try {
                if (f.getName().equals("Companion")) { continue; }
                dictionary.put(f.getName(), f.get(null));
            } catch (IllegalAccessException ignored) {}
        }
        return dictionary;
    }

    @NonNull
    private Dictionary adContentInfoToDictionary(@NonNull AdContentInfo contentInfo) {
        Dictionary dictionary = new Dictionary();
        dictionary.put("format", contentInfo.getFormat().toString());
        dictionary.put("sourceName", contentInfo.getSourceName());
        dictionary.put("sourceId", contentInfo.getSourceId());
        dictionary.put("sourceUnitId", contentInfo.getSourceUnitId());
        dictionary.put("creativeId", contentInfo.getCreativeId());
        dictionary.put("revenue", contentInfo.getRevenue());
        dictionary.put("revenuePrecision", contentInfo.getRevenuePrecision());
        dictionary.put("impressionDepth", contentInfo.getImpressionDepth());
        dictionary.put("revenueTotal", contentInfo.getRevenueTotal());
        return dictionary;
    }

    private AdViewListener createBannerCallbacks() {
        return new AdViewListener() {
            @Override
            public void onAdViewLoaded(@NonNull CASBannerView casBannerView) {
                emitSignal("banner_loaded");
            }

            @Override
            public void onAdViewFailed(@NonNull CASBannerView casBannerView, @NonNull AdError adError) {
                emitSignal("banner_failed", adError.toString());
            }

            @Override
            public void onAdViewPresented(@NonNull CASBannerView casBannerView, @NonNull AdStatusHandler adStatusHandler) {
                emitSignal("banner_presented", adStatusHandlerToDictionary(adStatusHandler));
            }

            @Override
            public void onAdViewClicked(@NonNull CASBannerView casBannerView) {
                emitSignal("banner_clicked");
            }
        };
    }

    @NonNull
    private Dictionary adStatusHandlerToDictionary(@NonNull AdStatusHandler adStatusHandler) {
        Dictionary dictionary = new Dictionary();
        dictionary.put("adType", adStatusHandler.getAdType().toString());
        dictionary.put("network", adStatusHandler.getNetwork());
        dictionary.put("cpm", adStatusHandler.getCpm());
        dictionary.put("priceAccuracy", adStatusHandler.getPriceAccuracy());
        dictionary.put("versionInfo", adStatusHandler.getVersionInfo());
        dictionary.put("creativeIdentifier", adStatusHandler.getCreativeIdentifier());
        dictionary.put("identifier", adStatusHandler.getIdentifier());
        dictionary.put("impressionDepth", adStatusHandler.getImpressionDepth());
        dictionary.put("lifetimeRevenue", adStatusHandler.getLifetimeRevenue());
        return dictionary;
    }
}
