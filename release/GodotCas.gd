extends Node

enum AUDIENCE {
	UNDEFINED = 0
	CHILDRED = 1
	NOT_CHILDREN = 2
}

enum CONSENT_STATUS {
	UNDEFINED = 0
	ACCEPTED = 1
	DENIED = 2
}

enum CCPA_STATUS {
	UNDEFINED = 0
	OPT_OUT_SALE = 1
	OPT_IN_SALE = 2
}

enum BANNER_SIZE {
	BANNER = 0,
	LEADERBOARD = 1,
	MEDIUM_RECTANGLE = 2,
	SMART = 3
}

enum GENDER {
	UNKNOWN = 0,
	MALE = 1,
	FEMALE = 2
}

var possibleDebugGeographies : Dictionary = {}
var possibleConsentFlowStatuses : Dictionary = {}

var _cas : JNISingleton = null

signal initialization_finished(error, country, legalProtected, consentFlowStatus)
signal consent_flow_status_changed(status)

signal interstitial_loaded(adContentInfo)
signal interstitial_failed_to_load(adFormat, adError)
signal interstitial_failed_to_show(adFormat, adError)
signal interstitial_showed(adContentInfo)
signal interstitial_clicked(adContentInfo)
signal interstitial_dismissed(adContentInfo)
signal interstitial_impression(adContentInfo)

signal rewarded_loaded(adContentInfo)
signal rewarded_failed_to_load(adFormat, adError)
signal rewarded_failed_to_show(adFormat, adError)
signal rewarded_showed(adContentInfo)
signal rewarded_clicked(adContentInfo)
signal rewarded_dismissed(adContentInfo)
signal reward_earned(adContentInfo)
signal rewarded_impression(adContentInfo)

signal banner_loaded()
signal banner_failed(adError)
signal banner_clicked()
signal banner_presented(adContentInfo)
signal banner_impression(adContentInfo)

signal app_open_ad_loaded(adContentInfo)
signal app_open_ad_failed_to_load(adFormat, adError)
signal app_open_ad_failed_to_show(adFormat, adError)
signal app_open_ad_showed(adContentInfo)
signal app_open_ad_clicked(adContentInfo)
signal app_open_ad_dismissed(adContentInfo)
signal app_open_ad_impression(adContentInfo)

func output(message) -> void:
	print("%s: %s" % [name, message])

func initialize() -> void:
	if !Engine.has_singleton("GodotCas"):
		output("CAS JNI singleton not present, doing nothing")
		return
	_cas = Engine.get_singleton("GodotCas")
	connectSignals()
	
	# Example of initialization
	setCasId("your.package.name")
	addTestDeviceId("your-test-device-id")
	setTestAdMode(true)
	setDebugMode(true)

	initializeCas()

# Call before initializeCas()
# Mandatory. Sets CAS ID (usually your app package or bundle ID).
# Not setting the ID before initializeCas() will lead to plugin crash.
func setCasId(id:String) -> void:
	_cas.setCasId(id)

# Call before initializeCas()
# CAS built in consent manager is enabled by default, but you can disable it.
# It's still recommended to use their consent manager, so better leave it be.
func setUseBuiltInConsentManager(enabled:bool) -> void:
	_cas.setUseBuiltInConsentManager(enabled)

# Indicates if the application’s audio is muted. Affects initial mute state for all ads.
# Use this method only if your application has its own volume controls.
func setMutedAdSounds(enabled:bool) -> void:
	_cas.setMutedAdSounds(enabled)

# Call before initializeCas()
# If enabled, turns test ads mode on.
func setTestAdMode(enabled:bool) -> void:
	_cas.setTestAdMode(enabled)

# Call before initializeCas()
# See https://github.com/cleveradssolutions/CAS-Android/wiki/Impression-Level-Data#automatic-collect-ad-revenue
# If using this, make sure you have TenjinSDK dependency in GodotCas.gdap configuration.
func setTenjinKey(key:String) -> void:
	_cas.setTenjinKey(key)

# Call before initializeCas()
# See https://github.com/cleveradssolutions/CAS-Android/wiki/Additional-Meta-AudienceNetwork-steps
func setFacebookLDU(enabled:bool) -> void:
	_cas.setFacebookLDU(enabled)

# Specify what audience is the user in. See AUDIENCE enum.
func setTaggedAudience(audienceType:int) -> void:
	_cas.setTaggedAudience(audienceType)

# Initializes CAS (don't forget to specify pre-init parameters before calling).
func initializeCas() -> void:
	var versionInfo : Dictionary = Engine.get_version_info()
	var versionString = "%s.%s.%s" % [versionInfo["major"], versionInfo["minor"], versionInfo["patch"]]
	_cas.initializeCAS(versionString)

# Initializes the CASInterstitial object in the plugin and creates callbacks.
func initializeInterstitial() -> void:
	_cas.initializeInterstitial()

# Loads the interstitial ad.
func loadInterstitial() -> void:
	_cas.loadInterstitial()

# Returns false is ad is not loaded yet or the CASInterstitial object hasn't been initialized.
func isInterstitialLoaded() -> bool:
	return _cas.isInterstitialLoaded()

# Shows the interstitial ad.
func showInterstitial() -> void:
	_cas.showInterstitial()

# Destroys the interstitial ad instance.
func destroyInterstitial() -> void:
	_cas.destroyInterstitial()

# Sets autoloading for interstitials, disabled by default.
func setAutoloadInterstitial(enabled:bool) -> void:
	_cas.setAutoloadInterstitial(enabled)

# Sets an interval between interstitial ad shows prohibiting ads from showing during it.
func setMinIntervalInterstitial(seconds:int) -> void:
	_cas.setMinIntervalInterstitial(seconds)

# Restarts the interval specified by setMinIntervalInterstitial().
func restartIntervalInterstitial() -> void:
	_cas.restartIntervalInterstitial()

# Initializes the CASRewarded object in the plugin and creates callbacks.
func initializeRewarded() -> void:
	_cas.initializeRewarded()

# Loads the rewarded ad.
func loadRewarded() -> void:
	_cas.loadRewarded()

# Returns false is ad is not loaded yet or the CASRewarded object hasn't been initialized.
func isRewardedLoaded() -> bool:
	return _cas.isRewardedLoaded()

# Shows the rewarded ad.
func showRewarded() -> void:
	_cas.showRewarded()

# Destroys the rewarded ad instance.
func destroyRewarded() -> void:
	_cas.destroyRewarded()

# Sets autoloading for rewarded, disabled by default.
func setAutoloadRewarded(enabled:bool) -> void:
	_cas.setAutoloadRewarded(enabled)

# Enabled by default. Sets if an interstitial ad should be displayed in case there is no-fill for rewarded.
func setRewardedExtraFillInterstitial(enabled:bool) -> void:
	_cas.setRewardedExtraFillInterstitial(enabled)

# Initializes CASBannerView. See BANNER_SIZE enum.
# Also see https://github.com/cleveradssolutions/CAS-Android/wiki/Banner-Ads#set-ad-size
func initializeBanner(bannerSize:int) -> void:
	_cas.initializeBanner(bannerSize)

# Initializes CASBannerView with adaptive size.
# See https://github.com/cleveradssolutions/CAS-Android/wiki/Banner-Ads#adaptive-banners
func initializeAdaptiveBanner(maxWidthDpi:int) -> void:
	_cas.initializeAdaptiveBanner(maxWidthDpi)

# Loads the banner ad.
func loadBanner() -> void:
	_cas.loadBanner()

# Returns false if ad is not loaded yet or the CASBannerView object hasn't been initialized.
func isBannerLoaded() -> bool:
	return _cas.isBannerLoaded()

# Sets banner visibility.
func setBannerVisible(isVisible:bool) -> void:
	_cas.setBannerVisible(isVisible)

# Destroys the banner ad instance.
func destroyBanner() -> void:
	_cas.destroyBanner()

# Sets autoloading for banner. Enabled by default.
func setAutoloadBanner(enabled:bool) -> void:
	_cas.setAutoloadBanner(enabled)

# Sets banner refresh interval. Should be longer than 10 seconds, 30 considered optimal (default).
func setBannerRefreshInterval(seconds:int) -> void:
	_cas.setBannerRefreshInterval(seconds)

# Disables ad refresh for banner.
func disableBannerAdRefresh() -> void:
	_cas.disableBannerAdRefresh()

# Sets banner position in device's actual viewport.
func setBannerPosition(posX:float, posY:float) -> void:
	_cas.setBannerPosition(posX, posY)

# Returns banner width in device's actual viewport.
func getBannerWidth() -> int:
	return _cas.getBannerWidth()

# Returns banner height in device's actual viewport.
func getBannerHeight() -> int:
	return _cas.getBannerHeight()

# Initializes the CASAppOpen object in the plugin and creates callbacks.
func initializeAppOpenAd() -> void:
	_cas.initializeAppOpenAd()

# Loads the AppOpen ad.
func loadAppOpenAd() -> void:
	_cas.loadAppOpenAd()

# Returns false if ad is not loaded yet or the CASAppOpen object hasn't been initialized.
func isAppOpenAdLoaded() -> bool:
	return _cas.isAppOpenAdLoaded()

# Shows the AppOpen ad.
func showAppOpenAd() -> void:
	_cas.showAppOpenAd()

# Destroys the AppOpen ad instance.
func destroyAppOpenAd() -> void:
	_cas.destroyAppOpenAd()

# Sets autoloading for AppOpen ad, disabled by default.
func setAutoloadAppOpenAd(enabled:bool) -> void:
	_cas.setAutoloadAppOpenAd(enabled)

# Sets autoshowing for AppOpen ad, disabled by default.
func setAutoshowAppOpenAd(enabled:bool) -> void:
	_cas.setAutoshowAppOpenAd(enabled)

# Programmatically retrieves the SDK version number at runtime.
func getSdkVersion() -> String:
	return _cas.getSdkVersion()

# Shows consent manager if it is required (e.g. based on user's geo).
func showConsentManagerIfRequired() -> void:
	_cas.showConsentManagerIfRequired()

# Force-shows the consent manager.
func showConsentManager() -> void:
	_cas.showConsentManager()

# Sets debug geography for consent manager testing.
# Will have no effect unless force testing is enabled for consent manager.
func setConsentDebugGeography(geography:int) -> void:
	_cas.setDebugGeography(geography)

# Enabled forced testing for consent manager.
func setConsentForceTesting(enabled:bool) -> void:
	_cas.setConsentForceTesting(enabled)

# Use this if using your own consent manager. See CONSENT_STATUS enum.
func setConsentStatus(status:int) -> void:
	_cas.setConsentStatus(status)

# Use this if using your own consent manager. See CCPA_STATUS enum.
func setCcpaStatus(status:int) -> void:
	_cas.setCcpaStatus(status)

# Returns possible consent flow statuses as String : int dict for lookup.
func getPossibleConsentFlowStatuses() -> Dictionary:
	if possibleConsentFlowStatuses.empty():
		possibleConsentFlowStatuses = _cas.getPossibleConsentFlowStatuses()
	return possibleConsentFlowStatuses

func humanizeConsentFlowStatus(status:int) -> String:
	return getPossibleConsentFlowStatuses().find_key(status)

# Returns possible debug geographies as String : int dict for lookup.
func getPossibleDebugGeographies() -> Dictionary:
	if possibleDebugGeographies.empty():
		possibleDebugGeographies = _cas.getPossibleDebugGeographies()
	return possibleDebugGeographies

func humanizeDebugGeography(geography:int) -> String:
	return getPossibleDebugGeographies().find_key(geography)

# Debug Mode displays debug info with log tag "CAS". Disabled by default for performance.
func setDebugMode(enabled:bool) -> void:
	_cas.setDebugMode(enabled)

# Adds testing device ID.
# see https://github.com/cleveradssolutions/CAS-Android/wiki/Enabling-test-ads#enable-test-devices
func addTestDeviceId(id:String) -> void:
	_cas.addTestDeviceId(id)

# Set the time interval during which users can enjoy an ad-free experience while retaining access to Rewarded Ads and App Open Ads formats.
# This interval is defined from the moment of the initial app installation, in seconds.
func setTrialAdFreeInterval(seconds:int) -> void:
	_cas.setTrialAdFreeInterval(seconds)

# See https://github.com/cleveradssolutions/CAS-Android/wiki/Targeting-options#user-id
func setUserID(userID:String) -> void:
	_cas.setUserID(userID)

# See https://github.com/cleveradssolutions/CAS-Android/wiki/Targeting-options#user-age
func setUserAge(age:int) -> void:
	_cas.setUserAge(age)

# See GENDER enum for the argument.
# See https://github.com/cleveradssolutions/CAS-Android/wiki/Targeting-options#user-gender
func setUserGender(gender:int) -> void:
	_cas.setUserGender(gender)

# See https://github.com/cleveradssolutions/CAS-Android/wiki/Targeting-options#user-location-auto-collection
func setUserLocationAutocollection(enabled:bool) -> void:
	_cas.setUserLocationAutocollection(enabled)

# See https://github.com/cleveradssolutions/CAS-Android/wiki/Targeting-options#app-keywords
func setUserAppKeywords(keywords:PoolStringArray) -> void:
	_cas.setUserAppKeywords(keywords)

# See https://github.com/cleveradssolutions/CAS-Android/wiki/Targeting-options#app-content-url
func setAppContentUrl(contentUrl:String) -> void:
	_cas.setAppContentUrl(contentUrl)

# Data for arguments is supposed to be obtained from Google Billing Library plugin for Godot.
# CAS should be initialized with Tenjin key for this to work (might cause crash otherwise).
func logTenjinPurchaseEvent(sku:String, currencyCode:String, quantity:int, price:float, originalJson:String, signature:String) -> void:
	_cas.logTenjinPurchaseEvent(sku, currencyCode, quantity, price, originalJson, signature)

func connectSignals() -> void:
	_cas.connect("initialization_finished", self, "_onInitializationFinished")
	_cas.connect("consent_flow_status_changed", self, "_onConsentFlowStatusChanged")

	_cas.connect("interstitial_loaded", self, "_onInterstitialLoaded")
	_cas.connect("interstitial_failed_to_load", self, "_onInterstitialFailedToLoad")
	_cas.connect("interstitial_failed_to_show", self, "_onInterstitialFailedToShow")
	_cas.connect("interstitial_showed", self, "_onInterstitialShowed")
	_cas.connect("interstitial_clicked", self, "_onInterstitialClicked")
	_cas.connect("interstitial_dismissed", self, "_onInterstitialDismissed")
	_cas.connect("interstitial_impression", self, "_onInterstitialImpression")

	_cas.connect("rewarded_loaded", self, "_onRewardedLoaded")
	_cas.connect("rewarded_failed_to_load", self, "_onRewardedFailedToLoad")
	_cas.connect("rewarded_failed_to_show", self, "_onRewardedFailedToShow")
	_cas.connect("rewarded_showed", self, "_onRewardedShowed")
	_cas.connect("rewarded_clicked", self, "_onRewardedClicked")
	_cas.connect("rewarded_dismissed", self, "_onRewardedDismissed")
	_cas.connect("reward_earned", self, "_onRewardEarned")
	_cas.connect("rewarded_impression", self, "_onRewardedImpression")

	_cas.connect("banner_loaded", self, "_onBannerLoaded")
	_cas.connect("banner_failed", self, "_onBannerFailed")
	_cas.connect("banner_clicked", self, "_onBannerClicked")
	_cas.connect("banner_presented", self, "_onBannerPresented")
	_cas.connect("banner_impression", self, "_onBannerImpression")

	_cas.connect("app_open_ad_loaded", self, "_onAppOpenAdLoaded")
	_cas.connect("app_open_ad_failed_to_load", self, "_onAppOpenAdFailedToLoad")
	_cas.connect("app_open_ad_failed_to_show", self, "_onAppOpenAdFailedToShow")
	_cas.connect("app_open_ad_showed", self, "_onAppOpenAdShowed")
	_cas.connect("app_open_ad_clicked", self, "_onAppOpenAdClicked")
	_cas.connect("app_open_ad_dismissed", self, "_onAppOpenAdDismissed")
	_cas.connect("app_open_ad_impression", self, "_onAppOpenAdImpression")

# Can fire multiple times in case initialization failed last time
func _onInitializationFinished(initErrorOrNull:String, userCountryISO2orNull:String, protectionApplied:bool, consentFlowStatus:int) -> void:
	emit_signal("initialization_finished", initErrorOrNull, userCountryISO2orNull, protectionApplied, consentFlowStatus)

# Fires when consent manager was invoked manually and consent status is obtained.
func _onConsentFlowStatusChanged(status:int) -> void:
	emit_signal("consent_flow_status_changed", status)

func _onInterstitialLoaded(adContentInfo:Dictionary) -> void:
	emit_signal("interstitial_loaded", adContentInfo)

func _onInterstitialFailedToLoad(adFormat:String, adError:String) -> void:
	emit_signal("interstitial_failed_to_load", adFormat, adError)

func _onInterstitialFailedToShow(adFormat:String, adError:String) -> void:
	emit_signal("interstitial_failed_to_show", adFormat, adError)

func _onInterstitialShowed(adContentInfo:Dictionary) -> void:
	emit_signal("interstitial_showed", adContentInfo)

func _onInterstitialClicked(adContentInfo:Dictionary) -> void:
	emit_signal("interstitial_clicked", adContentInfo)

func _onInterstitialDismissed(adContentInfo:Dictionary) -> void:
	emit_signal("interstitial_dismissed", adContentInfo)

func _onInterstitialImpression(adContentInfo:Dictionary) -> void:
	emit_signal("interstitial_impression", adContentInfo)

func _onRewardedLoaded(adContentInfo:Dictionary) -> void:
	emit_signal("rewarded_loaded", adContentInfo)

func _onRewardedFailedToLoad(adFormat:String, adError:String) -> void:
	emit_signal("rewarded_failed_to_load", adFormat, adError)

func _onRewardedFailedToShow(adFormat:String, adError:String) -> void:
	emit_signal("rewarded_failed_to_show", adFormat, adError)

func _onRewardedShowed(adContentInfo:Dictionary) -> void:
	emit_signal("rewarded_showed", adContentInfo)

func _onRewardedClicked(adContentInfo:Dictionary) -> void:
	emit_signal("rewarded_clicked", adContentInfo)

func _onRewardedDismissed(adContentInfo:Dictionary) -> void:
	emit_signal("rewarded_dismissed", adContentInfo)

func _onRewardEarned(adContentInfo:Dictionary) -> void:
	emit_signal("reward_earned", adContentInfo)

func _onRewardedImpression(adContentInfo:Dictionary) -> void:
	emit_signal("rewarded_impression", adContentInfo)

func _onBannerLoaded() -> void:
	emit_signal("banner_loaded")

func _onBannerFailed(adError:String) -> void:
	emit_signal("banner_failed", adError)

func _onBannerClicked() -> void:
	emit_signal("banner_clicked")

func _onBannerPresented(adContentInfo:Dictionary) -> void:
	emit_signal("banner_presented", adContentInfo)

func _onBannerImpression(adContentInfo:Dictionary) -> void:
	emit_signal("banner_impression", adContentInfo)

func _onAppOpenAdLoaded(adContentInfo:Dictionary) -> void:
	emit_signal("app_open_ad_loaded", adContentInfo)

func _onAppOpenAdFailedToLoad(adFormat:String, adError:String) -> void:
	emit_signal("app_open_ad_failed_to_load", adFormat, adError)

func _onAppOpenAdFailedToShow(adFormat:String, adError:String) -> void:
	emit_signal("app_open_ad_failed_to_show", adFormat, adError)

func _onAppOpenAdShowed(adContentInfo:Dictionary) -> void:
	emit_signal("app_open_ad_showed", adContentInfo)

func _onAppOpenAdClicked(adContentInfo:Dictionary) -> void:
	emit_signal("app_open_ad_clicked", adContentInfo)

func _onAppOpenAdDismissed(adContentInfo:Dictionary) -> void:
	emit_signal("app_open_ad_dismissed", adContentInfo)

func _onAppOpenAdImpression(adContentInfo:Dictionary) -> void:
	emit_signal("app_open_ad_impression", adContentInfo)
