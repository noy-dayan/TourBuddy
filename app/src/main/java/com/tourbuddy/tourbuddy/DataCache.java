package com.tourbuddy.tourbuddy;

import android.app.Application;
import android.os.Build;
import android.util.LruCache;
import android.util.Log;

public class DataCache extends Application {
    private static DataCache instance;
    private final LruCache<String, Object> cache;

    private boolean isAppTerminating = false;

    public DataCache() {
        // Use 1/8th of the available memory for this memory cache
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;
        cache = new LruCache<>(cacheSize);
    }

    public static synchronized DataCache getInstance() {
        if (instance == null) {
            instance = new DataCache();
        }
        return instance;
    }

    public void put(String key, Object value) {
        cache.put(key, value);
    }

    public Object get(String key) {
        return cache.get(key);
    }

    public void clearCache() {
        // Clear the cache
        cache.evictAll();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        if (level >= TRIM_MEMORY_BACKGROUND || level == TRIM_MEMORY_UI_HIDDEN) {
            // The app is in the background or UI is hidden (app is not visible)
            isAppTerminating = true;
            clearCache();
            instance = null;
            Log.d("DataCache", "Cache cleared due to low memory");
        }
    }

    public boolean isAppTerminating() {
        return isAppTerminating;
    }
}
