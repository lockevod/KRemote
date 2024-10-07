package com.enderthor.kremote.utils;

import android.view.KeyEvent;

/**
 * Represents a Karoo hardware or virtual key and the corresponding KeyEvent keycode.
 * from valterc ki2
 */
public enum KarooKey {

    /**
     * Right top key.
     */
    RIGHT(KeyEvent.KEYCODE_NAVIGATE_NEXT),

    /**
     * Left bottom key.
     */
    BACK(KeyEvent.KEYCODE_BACK),


    /**
     * Virtual key to switch the ride activity to the map page.
     */
    VIRTUAL_SWITCH_TO_MAP_PAGE(10_000 + 1);


    private final int keyCode;

    KarooKey(int keyCode) {
        this.keyCode = keyCode;
    }

}