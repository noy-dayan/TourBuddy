package com.tourbuddy.tourbuddy.utils;

import android.app.Application;
import android.util.LruCache;
import android.util.Log;

/**
 * Singleton class for caching data throughout the application.
 */
public class DataCache extends Application {

    // Singleton instance of DataCache
    static DataCache instance;

    // LruCache for storing key-value pairs
    final LruCache<String, Object> cache;

    // Flag to track whether the app is terminating
    boolean isAppTerminating = false;

    /**
     * Constructor for initializing the LruCache.
     * Use 1/8th of the available memory for this memory cache.
     */
    public DataCache() {
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;
        cache = new LruCache<>(cacheSize);
    }

    /**
     * Method to get the singleton instance of DataCache.
     *
     * @return The singleton instance of DataCache.
     */
    public static synchronized DataCache getInstance() {
        if (instance == null) {
            instance = new DataCache();
        }
        return instance;
    }

    /**
     * Method to put a key-value pair into the cache.
     *
     * @param key   The key to be associated with the value.
     * @param value The value to be stored in the cache.
     */
    public void put(String key, Object value) {
        cache.put(key, value);
    }

    /**
     * Method to retrieve a value from the cache using a key.
     *
     * @param key The key associated with the value in the cache.
     * @return The value associated with the key, or null if the key is not found.
     */
    public Object get(String key) {
        return cache.get(key);
    }

    /**
     * Method to clear the entire cache.
     */
    public void clearCache() {
        cache.evictAll();
    }

    /**
     * Overridden method that is called when the application is running low on memory.
     * Clears the cache and sets the appTerminating flag if the app is in the background or UI is hidden.
     *
     * @param level The memory trim level.
     */
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

    /**
     * Method to check if the app is terminating.
     *
     * @return True if the app is terminating, false otherwise.
     */
    public boolean isAppTerminating() {
        return isAppTerminating;
    }
}
