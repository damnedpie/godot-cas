package com.onecat.godotcas;

import static com.onecat.godotcas.Utils.adContentInfoToDictionary;
import static com.onecat.godotcas.Utils.adStatusHandlerToDictionary;
import static com.onecat.godotcas.Utils.fieldsToDictionary;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

import com.cleveradssolutions.sdk.AdContentInfo;
import com.cleveradssolutions.sdk.AdFormat;
import com.cleveradssolutions.sdk.OnAdImpressionListener;
import com.cleveradssolutions.sdk.screen.CASAppOpen;
import com.cleveradssolutions.sdk.screen.*;
import com.cleversolutions.ads.*;
import com.cleversolutions.ads.android.CAS;
import com.cleversolutions.ads.android.CAS.ManagerBuilder;
import com.cleversolutions.ads.android.CASBannerView;
import com.tenjin.android.TenjinSDK;

import org.godotengine.godot.Dictionary;
import org.godotengine.godot.Godot;
import org.godotengine.godot.plugin.GodotPlugin;
import org.godotengine.godot.plugin.SignalInfo;
import org.godotengine.godot.plugin.UsedByGodot;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings({"unused"})
public class GodotCas extends GodotPlugin {
    public static final String TAG = "Godot CAS Plugin";
    private FrameLayout godotLayout;

    private String casId;
    private String tenjinKey = null;
    private boolean testAdMode = false;
    private boolean useBuiltInConsentManager = true;
    private boolean consentForceTesting = false;
    private boolean fbLDUEnabled = false;
    private int debugGeography = ConsentFlow.DebugGeography.DISABLED;
    private final Set<String> testDevices = new HashSet<>();

    private final boolean isPluginInitialized;
    private boolean isSdkInitialized = false;

    // Ad handler objects
    private CASInterstitial interstitialAd;
    private CASRewarded rewardedAd;
    private CASBannerView bannerAd;
    private CASAppOpen appOpenAd;

    public GodotCas(Godot godot) {
        super(godot);
        isPluginInitialized = true;
    }

    @Override
    public String getPluginName() {
        return "GodotCas";
    }

    @Override
    public View onMainCreate(Activity activity) {
        godotLayout = new FrameLayout(activity);
        return godotLayout;
    }

    @Override
    public void onMainDestroy() {
        if (interstitialAd != null) { interstitialAd.destroy(); }
        if (rewardedAd != null) { rewardedAd.destroy(); }
        if (bannerAd != null) { bannerAd.destroy(); }
        if (appOpenAd != null) { appOpenAd.destroy(); }
    }

    @Override
    public Set<SignalInfo> getPluginSignals() {
        Set<SignalInfo> signalInfoSet = new HashSet<>();
        // Basic
        signalInfoSet.add(new SignalInfo(Signals.INITIALIZATION_FINISHED, String.class, String.class, Boolean.class, Integer.class));
        signalInfoSet.add(new SignalInfo(Signals.CONSENT_FLOW_STATUS_CHANGED, Integer.class));
        // Interstitial
        signalInfoSet.add(new SignalInfo(Signals.INTERSTITIAL_LOADED, Dictionary.class));
        signalInfoSet.add(new SignalInfo(Signals.INTERSTITIAL_FAILED_TO_LOAD, String.class, String.class));
        signalInfoSet.add(new SignalInfo(Signals.INTERSTITIAL_FAILED_TO_SHOW, String.class, String.class));
        signalInfoSet.add(new SignalInfo(Signals.INTERSTITIAL_SHOWED, Dictionary.class));
        signalInfoSet.add(new SignalInfo(Signals.INTERSTITIAL_CLICKED, Dictionary.class));
        signalInfoSet.add(new SignalInfo(Signals.INTERSTITIAL_DISMISSED, Dictionary.class));
        signalInfoSet.add(new SignalInfo(Signals.INTERSTITIAL_IMPRESSION, Dictionary.class));
        // Rewarded
        signalInfoSet.add(new SignalInfo(Signals.REWARDED_LOADED, Dictionary.class));
        signalInfoSet.add(new SignalInfo(Signals.REWARDED_FAILED_TO_LOAD, String.class, String.class));
        signalInfoSet.add(new SignalInfo(Signals.REWARDED_FAILED_TO_SHOW, String.class, String.class));
        signalInfoSet.add(new SignalInfo(Signals.REWARDED_SHOWED, Dictionary.class));
        signalInfoSet.add(new SignalInfo(Signals.REWARDED_CLICKED, Dictionary.class));
        signalInfoSet.add(new SignalInfo(Signals.REWARDED_DISMISSED, Dictionary.class));
        signalInfoSet.add(new SignalInfo(Signals.REWARD_EARNED, Dictionary.class));
        signalInfoSet.add(new SignalInfo(Signals.REWARDED_IMPRESSION, Dictionary.class));
        // Banner
        signalInfoSet.add(new SignalInfo(Signals.BANNER_LOADED));
        signalInfoSet.add(new SignalInfo(Signals.BANNER_FAILED, String.class));
        signalInfoSet.add(new SignalInfo(Signals.BANNER_CLICKED));
        signalInfoSet.add(new SignalInfo(Signals.BANNER_PRESENTED, Dictionary.class));
        signalInfoSet.add(new SignalInfo(Signals.BANNER_IMPRESSION, Dictionary.class));
        // AppOpen ads
        signalInfoSet.add(new SignalInfo(Signals.APP_OPEN_AD_LOADED, Dictionary.class));
        signalInfoSet.add(new SignalInfo(Signals.APP_OPEN_AD_FAILED_TO_LOAD, String.class, String.class));
        signalInfoSet.add(new SignalInfo(Signals.APP_OPEN_AD_FAILED_TO_SHOW, String.class, String.class));
        signalInfoSet.add(new SignalInfo(Signals.APP_OPEN_AD_SHOWED, Dictionary.class));
        signalInfoSet.add(new SignalInfo(Signals.APP_OPEN_AD_CLICKED, Dictionary.class));
        signalInfoSet.add(new SignalInfo(Signals.APP_OPEN_AD_DISMISSED, Dictionary.class));
        signalInfoSet.add(new SignalInfo(Signals.APP_OPEN_AD_IMPRESSION, Dictionary.class));
        return signalInfoSet;
    }

    //region Initialization

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
        switch (gender) {
            case 0:
                CAS.getTargetingOptions().setGender(TargetingOptions.GENDER_UNKNOWN);
                break;
            case 1:
                CAS.getTargetingOptions().setGender(TargetingOptions.GENDER_MALE);
                break;
            case 2:
                CAS.getTargetingOptions().setGender(TargetingOptions.GENDER_FEMALE);
                break;
        }
    }

    @UsedByGodot
    public void initializeCAS(String engineVersion) {
        ManagerBuilder builder = CAS.buildManager()
                .withCasId(getCasId())
                .withConsentFlow(new ConsentFlow(useBuiltInConsentManager))
                .withTestAdMode(testAdMode)
                .withFramework("Godot", engineVersion)
                .withCompletionListener(initialConfiguration -> {
					// Godot strings can't be null
					String initError = initialConfiguration.getError();
					if (initError == null) {
                        initError = "";
                        isSdkInitialized = true;
                    }
					String userCountryISO2 = initialConfiguration.getCountryCode();
					if (userCountryISO2 == null) { userCountryISO2 = ""; }
					boolean protectionApplied  = initialConfiguration.isConsentRequired();
					int consentFlowStatus = initialConfiguration.getConsentFlowStatus();
					emitSignal(Signals.INITIALIZATION_FINISHED, initError, userCountryISO2, protectionApplied, consentFlowStatus);
				});
        if (tenjinKey != null) {
            builder.withMediationExtras("tenjin_key", tenjinKey);
        }
        builder.withMediationExtras("FB_dp", fbLDUEnabled ? "LDU" : "");
        builder.build(getActivity());
    }

	@UsedByGodot
	public boolean isInitialized() {
		return isSdkInitialized && isPluginInitialized;
	}

    @UsedByGodot
    public void validateIntegration() {
        CAS.validateIntegration(getActivity());
    }

    //endregion
    //region Interstitial

    @UsedByGodot
    public void initializeInterstitial() {
        interstitialAd = new CASInterstitial(getCasId());
        ScreenAdContentCallback adContentCallback = new ScreenAdContentCallback() {
            @Override
            public void onAdLoaded( AdContentInfo adContentInfo) {
                emitSignal(Signals.INTERSTITIAL_LOADED, adContentInfoToDictionary(adContentInfo));
            }

            @Override
            public void onAdFailedToLoad(AdFormat adFormat, AdError adError) {
                emitSignal(Signals.INTERSTITIAL_FAILED_TO_LOAD, adFormat.toString(), adError.toString());
            }

            @Override
            public void onAdFailedToShow( AdFormat adFormat,  AdError adError) {
                emitSignal(Signals.INTERSTITIAL_FAILED_TO_SHOW, adFormat.toString(), adError.toString());
            }

            @Override
            public void onAdShowed( AdContentInfo adContentInfo) {
                emitSignal(Signals.INTERSTITIAL_SHOWED, adContentInfoToDictionary(adContentInfo));
            }

            @Override
            public void onAdClicked( AdContentInfo adContentInfo) {
                emitSignal(Signals.INTERSTITIAL_CLICKED, adContentInfoToDictionary(adContentInfo));
            }

            @Override
            public void onAdDismissed( AdContentInfo adContentInfo) {
                emitSignal(Signals.INTERSTITIAL_DISMISSED, adContentInfoToDictionary(adContentInfo));
            }
        };
        interstitialAd.setContentCallback(adContentCallback);
        OnAdImpressionListener impressionListener = adContentInfo -> emitSignal(Signals.INTERSTITIAL_IMPRESSION, adContentInfoToDictionary(adContentInfo));
        interstitialAd.setOnImpressionListener(impressionListener);
    }

    @UsedByGodot
    public void loadInterstitial() {
        if (interstitialAd == null) {
            Log.w(TAG, "Interstitial is not initialized, can't loadInterstitial()");
            return;
        }
        interstitialAd.load(getActivity());
    }

    @UsedByGodot
    public boolean isInterstitialLoaded() {
        if (interstitialAd == null) {
            Log.w(TAG, "Interstitial is not initialized, isInterstitialLoaded() defaults to false");
            return false;
        }
        return interstitialAd.isLoaded();
    }

    @UsedByGodot
    public void showInterstitial() {
        if (interstitialAd == null) {
            Log.w(TAG, "Interstitial is not initialized, can't showInterstitial()");
            return;
        }
		interstitialAd.show(getActivity());
    }

    @UsedByGodot
    public void destroyInterstitial() {
        if (interstitialAd == null) {
            Log.w(TAG, "Interstitial is not initialized, can't destroyInterstitial()");
            return;
        }
        interstitialAd.destroy();
    }

    @UsedByGodot
    public void setAutoloadInterstitial(boolean enabled) {
        if (interstitialAd == null) {
            Log.w(TAG, "Interstitial is not initialized, can't setAutoloadInterstitial()");
            return;
        }
        interstitialAd.setAutoloadEnabled(enabled);
    }

    @UsedByGodot
    public void setMinIntervalInterstitial(int seconds) {
        if (interstitialAd == null) {
            Log.w(TAG, "Interstitial is not initialized, can't setMinIntervalInterstitial()");
            return;
        }
        interstitialAd.setMinInterval(seconds);
    }

    @UsedByGodot
    public void restartIntervalInterstitial() {
        if (interstitialAd == null) {
            Log.w(TAG, "Interstitial is not initialized, can't restartIntervalInterstitial()");
            return;
        }
        interstitialAd.restartInterval();
    }

    //endregion
    //region Rewarded

    @UsedByGodot
    public void initializeRewarded() {
        rewardedAd = new CASRewarded(casId);
        ScreenAdContentCallback adContentCallback = new ScreenAdContentCallback() {
            @Override
            public void onAdLoaded(AdContentInfo adContentInfo) {
                emitSignal(Signals.REWARDED_LOADED, adContentInfoToDictionary(adContentInfo));
            }

            @Override
            public void onAdFailedToLoad(AdFormat adFormat, AdError adError) {
                emitSignal(Signals.REWARDED_FAILED_TO_LOAD, adFormat.toString(), adError.toString());
            }

            @Override
            public void onAdFailedToShow(AdFormat adFormat, AdError adError) {
                emitSignal(Signals.REWARDED_FAILED_TO_SHOW, adFormat.toString(), adError.toString());
            }

            @Override
            public void onAdShowed(AdContentInfo adContentInfo) {
                emitSignal(Signals.REWARDED_SHOWED, adContentInfoToDictionary(adContentInfo));
            }

            @Override
            public void onAdClicked(AdContentInfo adContentInfo) {
                emitSignal(Signals.REWARDED_CLICKED, adContentInfoToDictionary(adContentInfo));
            }

            @Override
            public void onAdDismissed(AdContentInfo adContentInfo) {
                emitSignal(Signals.REWARDED_DISMISSED, adContentInfoToDictionary(adContentInfo));
            }
        };
        rewardedAd.setContentCallback(adContentCallback);
        OnAdImpressionListener impressionListener = new OnAdImpressionListener() {
            @Override
            public void onAdImpression(AdContentInfo adContentInfo) {
                emitSignal(Signals.REWARDED_IMPRESSION, adContentInfoToDictionary(adContentInfo));
            }
        };
        rewardedAd.setOnImpressionListener(impressionListener);
    }

    @UsedByGodot
    public void loadRewarded() {
        if (rewardedAd == null) {
            Log.w(TAG, "Rewarded is not initialized, can't loadRewarded()");
            return;
        }
        rewardedAd.load(getActivity());
    }

    @UsedByGodot
    public boolean isRewardedLoaded() {
        if (rewardedAd == null) {
            Log.w(TAG, "Rewarded is not initialized, isRewardedLoaded() defaults to false");
            return false;
        }
        return rewardedAd.isLoaded();
    }

    @UsedByGodot
    public void showRewarded() {
        if (rewardedAd == null) {
            Log.w(TAG, "Rewarded is not initialized, can't showRewarded()");
            return;
        }
		rewardedAd.show(getActivity(), new OnRewardEarnedListener() {
			@Override
			public void onUserEarnedReward(AdContentInfo adContentInfo) {
				emitSignal(Signals.REWARD_EARNED, adContentInfoToDictionary(adContentInfo));
			}
		});
    }

    @UsedByGodot
    public void destroyRewarded() {
        if (rewardedAd == null) {
            Log.w(TAG, "Rewarded is not initialized, can't destroyRewarded()");
            return;
        }
        rewardedAd.destroy();
    }

    @UsedByGodot
    public void setAutoloadRewarded(boolean enabled) {
        if (rewardedAd == null) {
            Log.w(TAG, "Rewarded is not initialized, can't setAutoloadRewarded()");
            return;
        }
        rewardedAd.setAutoloadEnabled(enabled);
    }

    @UsedByGodot
    public void setRewardedExtraFillInterstitial(boolean enabled) {
        if (rewardedAd == null) {
            Log.w(TAG, "Rewarded is not initialized, can't setRewardedExtraFillInterstitial()");
            return;
        }
        rewardedAd.setExtraFillInterstitialAdEnabled(enabled);
    }

	//endregion
	//region Banner

    @UsedByGodot
    public void initializeBanner(int bannerSize) {
        bannerAd = new CASBannerView(getActivity(), getCasId());
        AdSize adSize = switch (bannerSize) {
			case 1 -> AdSize.LEADERBOARD;
			case 2 -> AdSize.MEDIUM_RECTANGLE;
			case 3 -> AdSize.getSmartBanner(getActivity());
			default -> AdSize.BANNER;
		};
		bannerAd.setSize(adSize);
        bannerAd.setAdListener(createBannerCallbacks());
        OnAdImpressionListener impressionListener = new OnAdImpressionListener() {
            @Override
            public void onAdImpression(AdContentInfo adContentInfo) {
                emitSignal(Signals.BANNER_IMPRESSION, adContentInfoToDictionary(adContentInfo));
            }
        };
        bannerAd.setOnImpressionListener(impressionListener);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getGodotLayout().removeAllViews();
                getGodotLayout().addView(bannerAd, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            }
        });
    }

    @UsedByGodot
    public void initializeAdaptiveBanner(int maxWidthDpi) {
        bannerAd = new CASBannerView(getActivity(), getCasId());
        bannerAd.setSize(AdSize.getAdaptiveBanner(getActivity(), maxWidthDpi));
        bannerAd.setAdListener(createBannerCallbacks());
        OnAdImpressionListener impressionListener = new OnAdImpressionListener() {
            @Override
            public void onAdImpression(AdContentInfo adContentInfo) {
                emitSignal(Signals.BANNER_IMPRESSION, adContentInfoToDictionary(adContentInfo));
            }
        };
        bannerAd.setOnImpressionListener(impressionListener);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getGodotLayout().removeAllViews();
                getGodotLayout().addView(bannerAd, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            }
        });
    }

    @UsedByGodot
    public void setBannerPosition() { }

    @UsedByGodot
    public void loadBanner() {
        if (bannerAd == null) {
            Log.w(TAG, "Banner is not initialized, can't loadBanner()");
            return;
        }
        bannerAd.load();
    }

    @UsedByGodot
    public boolean isBannerLoaded() {
        if (bannerAd == null) {
            Log.w(TAG, "Banner is not initialized, isBannerLoaded() defaults to false");
            return false;
        }
        return bannerAd.isLoaded();
    }

    @UsedByGodot
    public void setBannerVisible(boolean visible) {
        if (bannerAd == null) {
            Log.w(TAG, "Banner is not initialized, can't setBannerVisible()");
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
    public void destroyBanner() {
        if (bannerAd == null) {
            Log.w(TAG, "Banner is not initialized, can't destroyBanner()");
            return;
        }
        bannerAd.destroy();
    }

    @UsedByGodot
    public void setAutoloadBanner(boolean enabled) {
        if (bannerAd == null) {
            Log.w(TAG, "Banner is not initialized, can't setAutoloadBanner()");
            return;
        }
        bannerAd.setAutoloadEnabled(enabled);
    }

    @UsedByGodot
    public void setBannerRefreshInterval(int seconds) {
        if (bannerAd == null) {
            Log.w(TAG, "Banner is not initialized, can't setBannerRefreshInterval()");
            return;
        }
        bannerAd.setRefreshInterval(seconds);
    }

    @UsedByGodot
    public void disableBannerAdRefresh() {
        if (bannerAd == null) {
            Log.w(TAG, "Banner is not initialized, can't disableBannerAdRefresh()");
            return;
        }
        bannerAd.disableAdRefresh();
    }

    @UsedByGodot
    public void setBannerPosition(float x, float y) {
        if (bannerAd == null) {
            Log.w(TAG, "Banner is not initialized, can't setBannerPosition()");
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
            Log.w(TAG, "Banner is not initialized, can't getBannerWidth()");
            return 0;
        }
        return bannerAd.getSize().widthPixels(getActivity());
    }

    @UsedByGodot
    public int getBannerHeight() {
        if (bannerAd == null) {
            Log.w(TAG, "Banner is not initialized, can't getBannerHeight()");
            return 0;
        }
        return bannerAd.getSize().heightPixels(getActivity());
    }

	//endregion
	//region AppOpen

    @UsedByGodot
    public void initializeAppOpenAd() {
        appOpenAd = new CASAppOpen(casId);
        ScreenAdContentCallback adContentCallback = new ScreenAdContentCallback() {
            @Override
            public void onAdLoaded(AdContentInfo adContentInfo) {
                emitSignal(Signals.APP_OPEN_AD_LOADED, adContentInfoToDictionary(adContentInfo));
            }

            @Override
            public void onAdFailedToLoad(AdFormat adFormat, AdError adError) {
                emitSignal(Signals.APP_OPEN_AD_FAILED_TO_LOAD, adFormat.toString(), adError.toString());
            }

            @Override
            public void onAdFailedToShow(AdFormat adFormat, AdError adError) {
                emitSignal(Signals.APP_OPEN_AD_FAILED_TO_SHOW, adFormat.toString(), adError.toString());
            }

            @Override
            public void onAdShowed(AdContentInfo adContentInfo) {
                emitSignal(Signals.APP_OPEN_AD_SHOWED, adContentInfoToDictionary(adContentInfo));
            }

            @Override
            public void onAdClicked(AdContentInfo adContentInfo) {
                emitSignal(Signals.APP_OPEN_AD_CLICKED, adContentInfoToDictionary(adContentInfo));
            }

            @Override
            public void onAdDismissed(AdContentInfo adContentInfo) {
                emitSignal(Signals.APP_OPEN_AD_DISMISSED, adContentInfoToDictionary(adContentInfo));
            }
        };
        appOpenAd.setContentCallback(adContentCallback);
        OnAdImpressionListener impressionListener = new OnAdImpressionListener() {
            @Override
            public void onAdImpression(AdContentInfo adContentInfo) {
                emitSignal(Signals.APP_OPEN_AD_IMPRESSION, adContentInfoToDictionary(adContentInfo));
            }
        };
        appOpenAd.setOnImpressionListener(impressionListener);
    }

    @UsedByGodot
    public void loadAppOpenAd() {
        if (appOpenAd == null) {
            Log.w(TAG, "AppOpen ad is not initialized, can't loadAppOpenAd()");
            return;
        }
        appOpenAd.load(getActivity());
    }

    @UsedByGodot
    public boolean isAppOpenAdLoaded() {
        if (appOpenAd == null) {
            Log.w(TAG, "AppOpen ad is not initialized, isAppOpenAdLoaded() defaults to false");
            return false;
        }
        return appOpenAd.isLoaded();
    }

    @UsedByGodot
    public void showAppOpenAd() {
        if (appOpenAd == null) {
            Log.w(TAG, "AppOpen ad is not initialized, can't showAppOpenAd()");
            return;
        }
		appOpenAd.show(getActivity());
    }

    @UsedByGodot
    public void destroyAppOpenAd() {
        if (appOpenAd == null) {
            Log.w(TAG, "AppOpen ad is not initialized, can't destroyAppOpenAd()");
            return;
        }
        appOpenAd.destroy();
    }

    @UsedByGodot
    public void setAutoloadAppOpenAd(boolean enabled) {
        if (appOpenAd == null) {
            Log.w(TAG, "AppOpen ad is not initialized, can't setAutoloadAppOpenAd()");
            return;
        }
        appOpenAd.setAutoloadEnabled(enabled);
    }

    @UsedByGodot
    public void setAutoshowAppOpenAd(boolean enabled) {
        if (appOpenAd == null) {
            Log.w(TAG, "AppOpen ad is not initialized, can't setAutoshowAppOpenAd()");
            return;
        }
        appOpenAd.setAutoshowEnabled(enabled);
    }

	//endregion

    @UsedByGodot
    public void showConsentManagerIfRequired() {
        new ConsentFlow()
                .withDebugGeography(debugGeography)
                .withForceTesting(consentForceTesting)
                .withDismissListener(status -> emitSignal(Signals.CONSENT_FLOW_STATUS_CHANGED, status))
                .showIfRequired();
    }

    @UsedByGodot
    public void showConsentManager() {
        new ConsentFlow()
                .withDebugGeography(debugGeography)
                .withForceTesting(consentForceTesting)
                .withDismissListener(status -> emitSignal(Signals.CONSENT_FLOW_STATUS_CHANGED, status))
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
    public void logTenjinPurchaseEvent(String sku, String currencyCode, int quantity, float price, String originalJson, String signature) {
        TenjinSDK tjInstance = TenjinSDK.getInstance(getActivity(), tenjinKey);
        if (tjInstance == null) {
            Log.w(TAG, "Tenjin instance is null, logTenjinPurchaseEvent() did nothing");
            return;
        }
        tjInstance.transaction(sku, currencyCode, quantity, price, originalJson, signature);
    }

    @UsedByGodot
    public boolean isWifiOrMobileInternetEnabled() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager != null ? connectivityManager.getActiveNetworkInfo() : null;
        //noinspection deprecation
        return networkInfo != null && networkInfo.isConnected();
    }

    private AdViewListener createBannerCallbacks() {
        return new AdViewListener() {
            @Override
            public void onAdViewLoaded( CASBannerView casBannerView) {
                emitSignal(Signals.BANNER_LOADED);
            }

            @Override
            public void onAdViewFailed( CASBannerView casBannerView,  AdError adError) {
                emitSignal(Signals.BANNER_FAILED, adError.toString());
            }

            @Override
            public void onAdViewPresented( CASBannerView casBannerView,  AdStatusHandler adStatusHandler) {
                emitSignal(Signals.BANNER_PRESENTED, adStatusHandlerToDictionary(adStatusHandler));
            }

            @Override
            public void onAdViewClicked( CASBannerView casBannerView) {
                emitSignal(Signals.BANNER_CLICKED);
            }
        };
    }

    private String getCasId() {
        return casId;
    }

    private String getTenjinKey() {
        return tenjinKey;
    }

    private FrameLayout getGodotLayout() {
        return godotLayout;
    }

}
