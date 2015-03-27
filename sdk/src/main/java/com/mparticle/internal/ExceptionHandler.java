package com.mparticle.internal;

import android.util.Log;

import com.mparticle.MParticle;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.ref.WeakReference;

/**
 * Class used to capture uncaught exceptions. Maintains a WeakReference to the original exception handler
 * so that we can support at least 2 exception handlers at a time.
 */
/* package-private */public class ExceptionHandler implements UncaughtExceptionHandler {

    private static final String TAG = Constants.LOG_TAG;
    private UncaughtExceptionHandler mOriginalUncaughtExceptionHandler = null;

    public ExceptionHandler(UncaughtExceptionHandler originalUncaughtExceptionHandler) {
        if (originalUncaughtExceptionHandler != null && !(originalUncaughtExceptionHandler instanceof ExceptionHandler)) {
            mOriginalUncaughtExceptionHandler = originalUncaughtExceptionHandler;
        }
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        try {
            MParticle.getInstance().internal().logUnhandledError(ex);

            if (mOriginalUncaughtExceptionHandler != null) {
                mOriginalUncaughtExceptionHandler.uncaughtException(thread, ex);
            }
        } catch (Exception t) {
            Log.e(TAG, "Failed to log error event for uncaught exception", t);
            // we tried. don't make things worse.
        }
    }

    public UncaughtExceptionHandler getOriginalExceptionHandler() {
        return mOriginalUncaughtExceptionHandler;
    }

}
