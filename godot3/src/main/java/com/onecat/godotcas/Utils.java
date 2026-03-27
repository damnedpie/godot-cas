package com.onecat.godotcas;

import com.cleveradssolutions.sdk.AdContentInfo;
import com.cleversolutions.ads.AdStatusHandler;

import org.godotengine.godot.Dictionary;

import java.lang.reflect.Field;

public class Utils {

    static Dictionary fieldsToDictionary(Field[] fields) {
        Dictionary dictionary = new Dictionary();
        for (Field f : fields) {
            try {
                if (f.getName().equals("Companion")) { continue; }
                dictionary.put(f.getName(), f.get(null));
            } catch (IllegalAccessException ignored) {}
        }
        return dictionary;
    }

    static Dictionary adContentInfoToDictionary(AdContentInfo contentInfo) {
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

    static Dictionary adStatusHandlerToDictionary(AdStatusHandler adStatusHandler) {
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
