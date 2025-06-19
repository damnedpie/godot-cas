# Godot CAS 4.1.0
[![Godot](https://img.shields.io/badge/Godot%20Engine-3.6-blue?style=for-the-badge&logo=godotengine&logoSize=auto)](https://godotengine.org/)
[![CAS.AI](https://img.shields.io/badge/CAS.AI_SDK_4.1.0-blue?style=for-the-badge&logoSize=auto)](https://cas.ai/)
[![GitHub License](https://img.shields.io/github/license/damnedpie/godot-cas?style=for-the-badge)](#)
[![GitHub Repo stars](https://img.shields.io/github/stars/damnedpie/godot-cas?style=for-the-badge&logo=github&logoSize=auto&color=%23FFD700)](#)

CAS SDK 4.1.0 Android plugin for Godot. Built on Godot 3.6 AAR.

[**Official Android Wiki**](https://github.com/cleveradssolutions/CAS-Android/wiki)

## Setup

### Project integration

1. Add plugin files (.gd, .gdap, .aar) into your project's `android/plugins`.

2. Add `GodotCas.gd` as a singleton (autoload) to your project.

3. Add your Google AdMob ID to `android/build/AndroidManifest.xml`:
	```xml
	<application android:label="@string/godot_project_name_string" android:allowBackup="false" android:isGame="true" android:hasFragileUserData="false" android:requestLegacyExternalStorage="false" tools:ignore="GoogleAppIndexingWarning" android:icon="@mipmap/icon">
			<meta-data
				android:name="com.google.android.gms.ads.APPLICATION_ID"
				android:value="ca-app-pub-3940256099942544~3347511713" />
				<!-- Other metadata... -->
	```

4. Get your `cas_settings[settings_id].json` file from CAS dashboard and put it into `android/build/res/raw` folder.

5. (Optional) Add `com.google.android.gms.permission.AD_ID` permission to your Android export template if you want to use AD ID (which is usually the case). You can also add `android.permission.ACCESS_COARSE_LOCATION` and `android.permission.READ_PHONE_STATE` permissions if your app has real usecases for those (this can improve monetization).

### Ad network adapters

This plugin's configuration file `GodotCas.gdap` contains all adapters out of the box (excluding beta and cross-promo). Usually it's not desirable because your app may be not making use of all of them and having unnecessary adapters present will increase build size. Feel free to add/remove adapters and their repositories according to your needs.

Make sure to check out [**this list of adapters**](https://github.com/cleveradssolutions/CAS-Android/tree/master/Adapters#casai-mediation-adapters) and also [**this guide page**](https://github.com/cleveradssolutions/CAS-Android/wiki/Manual-setup) from CAS official wiki.

Also check `adapters-list.txt` for precise dependencies and repositories for Optimal and Families presets.

### Other optional dependencies

`com.google.android.gms:play-services-ads-identifier` dependency is required if you want to use AD ID (which is preferrable).

`com.tenjin:android-sdk` dependency would be necessary if you want to enable automatic revenue reporting for Tenjin. [**See this wiki page for more details**](https://github.com/cleveradssolutions/CAS-Android/wiki/Impression-Level-Data#tenjin).


## Initialization

Wrapper script `GodotCas.gd` has `initialize()` method which you are supposed to edit and then call in runtime. There are some methods that tweak initialization options for the SDK, and you have to call them before `initializeCAS()` is executed. Each method in the wrapper is documented by a comment, so make sure to inspect it.

### Initialization settings

All methods below should come before `initializeCAS()` if called.

```gdscript
# Mandatory. Sets CAS ID (usually your app package or bundle ID).
# Not setting the ID before initializeCas() will lead to plugin crash.
func setCasId(id:String) -> void

# CAS built in consent manager is enabled by default, but you can disable it.
# It's still recommended to use their consent manager, so better leave it be.
func setUseBuiltInConsentManager(enabled:bool) -> void

# If enabled, turns test ads mode on.
func setTestAdMode(enabled:bool) -> void

# See https://github.com/cleveradssolutions/CAS-Android/wiki/Impression-Level-Data#automatic-collect-ad-revenue
# If using this, make sure you have TenjinSDK dependency in GodotCas.gdap configuration.
func setTenjinKey(key:String) -> void

# See https://github.com/cleveradssolutions/CAS-Android/wiki/Additional-Meta-AudienceNetwork-steps
func setFacebookLDU(enabled:bool) -> void
```

### Testing and debugging

```gdscript
# Debug Mode displays debug info with log tag "CAS". Disabled by default for performance.
func setDebugMode(enabled:bool) -> void

# Adds testing device ID.
# see https://github.com/cleveradssolutions/CAS-Android/wiki/Enabling-test-ads#enable-test-devices
func addTestDeviceId(id:String) -> void
```

## Working with user data consent

[**Official wiki page**](https://github.com/cleveradssolutions/CAS-Android/wiki/User-Consent-Flow)

### Automatic flow

CAS SDK provides automated consent screen workflow with their own consent screen.

```gdscript
# Shows consent manager if it is required (e.g. based on user's geo).
func showConsentManagerIfRequired() -> void

# Force-shows the consent manager.
func showConsentManager() -> void
```

### Manual flow

In case you use your own consent screen, consent status can be set via these methods:

```gdscript
enum CONSENT_STATUS {
	UNDEFINED = 0
	ACCEPTED = 1
	DENIED = 2
}

# Use this if using your own consent manager. See CONSENT_STATUS enum.
func setConsentStatus(status:int) -> void

enum CCPA_STATUS {
	UNDEFINED = 0
	OPT_OUT_SALE = 1
	OPT_IN_SALE = 2
}

# Use this if using your own consent manager. See CCPA_STATUS enum.
func setCcpaStatus(status:int) -> void
```

### Testing consent flow
You can also test consent screen behavior to make sure everything works as intended.

```gdscript
# Returns possible consent flow statuses as String : int dict for lookup.
func getPossibleConsentFlowStatuses() -> Dictionary

# Returns possible debug geographies as String : int dict for lookup.
func getPossibleDebugGeographies() -> Dictionary

# Sets debug geography for consent manager testing.
# Will have no effect unless force testing is enabled for consent manager.
func setConsentDebugGeography(geography:int) -> void

# Enabled forced testing for consent manager.
func setConsentForceTesting(enabled:bool) -> void
```

## Children-directed treatment
[**Official wiki page**](https://github.com/cleveradssolutions/CAS-Android/wiki/Privacy-Regulations)

Developers who have knowledge of specific individuals as being COPPA-applicable should make use of the API discussed below to inform CAS and all downstream consumers of this information.

```gdscript
enum AUDIENCE {
	UNDEFINED = 0
	CHILDRED = 1
	NOT_CHILDREN = 2
}

# Specify what audience is the user in. See AUDIENCE enum.
func setTaggedAudience(audienceType:int) -> void

```

## Ad types

This plugin supports Interstitial, Rewarded, Banner and App Open ad types. Native ad type is not currently implemented due to having extremely few use cases in games.

Every ad-type has a corresponding initialization function: `initializeInterstitial()`, `initializeRewarded()` and so on. Make sure you call such a function before calling other methods relevant to an ad type.

### Interstitial
[**Official wiki page** ](https://github.com/cleveradssolutions/CAS-Android/wiki/Interstitial-Ads)

```gdscript
# Initializes the CASInterstitial object in the plugin and creates callbacks.
initializeInterstitial() -> void

# Loads the interstitial ad.
loadInterstitial() -> void

# Returns false is ad is not loaded yet or the CASInterstitial object hasn't been initialized.
isInterstitialLoaded() -> bool

# Shows the interstitial ad.
showInterstitial() -> void

# Sets autoloading for interstitials, disabled by default.
setAutoloadInterstitial(enabled:bool) -> void

# Sets an interval between interstitial ad shows prohibiting ads from showing during it.
setMinIntervalInterstitial(seconds:int) -> void

# Restarts the interval specified by setMinIntervalInterstitial().
restartIntervalInterstitial() -> void
```

### Rewarded
[**Official wiki page**](https://github.com/cleveradssolutions/CAS-Android/wiki/Rewarded-Ads)
```gdscript
# Initializes the CASRewarded object in the plugin and creates callbacks.
initializeRewarded() -> void

# Loads the rewarded ad.
loadRewarded() -> void

# Returns false is ad is not loaded yet or the CASRewarded object hasn't been initialized.
isRewardedLoaded() -> bool

# Shows the rewarded ad.
showRewarded() -> void

# Sets autoloading for rewarded, disabled by default.
setAutoloadRewarded(enabled:bool) -> void

# Enabled by default. Sets if an interstitial ad should be displayed in case there is no-fill for rewarded.
setRewardedExtraFillInterstitial(enabled:bool) -> void
```

### Banner
[**Official wiki page**](https://github.com/cleveradssolutions/CAS-Android/wiki/Banner-Ads)
```gdscript
# Initializes CASBannerView. See BANNER_SIZE enum.
# Also see https://github.com/cleveradssolutions/CAS-Android/wiki/Banner-Ads#set-ad-size
initializeBanner(bannerSize:int) -> void

# Initializes CASBannerView with adaptive size.
# See https://github.com/cleveradssolutions/CAS-Android/wiki/Banner-Ads#adaptive-banners
initializeAdaptiveBanner(maxWidthDpi:int) -> void

# Loads the banner ad.
func loadBanner() -> void

# Returns false if ad is not loaded yet or the CASBannerView object hasn't been initialized.
func isBannerLoaded() -> bool

# Sets banner visibility.
func setBannerVisible(isVisible:bool) -> void

# Sets autoloading for banner. Enabled by default.
func setAutoloadBanner(enabled:bool) -> void

# Sets banner refresh interval. Should be longer than 10 seconds, 30 considered optimal (default).
func setBannerRefreshInterval(seconds:int) -> void

# Disables ad refresh for banner.
func disableBannerAdRefresh() -> void

# Sets banner position in device's actual viewport.
func setBannerPosition(posX:float, posY:float) -> void

# Returns banner width in device's actual viewport.
func getBannerWidth() -> int

# Returns banner height in device's actual viewport.
func getBannerHeight() -> int
```

### App Open ads
[**Official wiki page**](https://github.com/cleveradssolutions/CAS-Android/wiki/App-Open-Ads)
```gdscript
# Initializes the CASAppOpen object in the plugin and creates callbacks.
func initializeAppOpenAd() -> void

# Loads the AppOpen ad.
func loadAppOpenAd() -> void

# Returns false if ad is not loaded yet or the CASAppOpen object hasn't been initialized.
func isAppOpenAdLoaded() -> bool

# Shows the AppOpen ad.
func showAppOpenAd() -> void

# Sets autoloading for AppOpen ad, disabled by default.
func setAutoloadAppOpenAd(enabled:bool) -> void

# Sets autoshowing for AppOpen ad, disabled by default.
func setAutoshowAppOpenAd(enabled:bool) -> void
```

### IAP logging with Tenjin

You can log in-app purchase events to Tenjin with this plugin. Just make sure to obtain the necessary `purchase` data from Godot Google Play Billing Library Plugin.

You will probably be calling this from inside `purchases_updated` signal of Billing Plugin, but I strongly advise you call logging with some delay (like 2 seconds). Reason being, TenjinSDK might be suspended in this particular moment and there's a possibility that your logging attempt won't go through.

More info on data required can be found in [Tenjin docs](https://docs.tenjin.com/docs/android-sdk#purchase-events)
```gdscript
# Data for arguments is supposed to be obtained from Google Billing Library plugin for Godot.
# CAS should be initialized with Tenjin key for this to work (might cause crash otherwise).
func logTenjinPurchaseEvent(sku:String, currencyCode:String, quantity:int, price:float, originalJson:String, signature:String) -> void
```

## Targeting options

[**Official wiki page**](https://github.com/cleveradssolutions/CAS-Android/wiki/Targeting-options)

```gdscript
# See https://github.com/cleveradssolutions/CAS-Android/wiki/Targeting-options#user-id
func setUserID(userID:String) -> void

# See https://github.com/cleveradssolutions/CAS-Android/wiki/Targeting-options#user-age
func setUserAge(age:int) -> void

enum GENDER {
	UNKNOWN = 0,
	MALE = 1,
	FEMALE = 2
}

# See GENDER enum for the argument.
# See https://github.com/cleveradssolutions/CAS-Android/wiki/Targeting-options#user-gender
func setUserGender(gender:int) -> void

# See https://github.com/cleveradssolutions/CAS-Android/wiki/Targeting-options#user-location-auto-collection
func setUserLocationAutocollection(enabled:bool) -> void

# See https://github.com/cleveradssolutions/CAS-Android/wiki/Targeting-options#app-keywords
func setUserAppKeywords(keywords:PoolStringArray) -> void

# See https://github.com/cleveradssolutions/CAS-Android/wiki/Targeting-options#app-content-url
func setAppContentUrl(contentUrl:String) -> void
```