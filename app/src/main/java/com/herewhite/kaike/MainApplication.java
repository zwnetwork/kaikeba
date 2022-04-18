package com.herewhite.kaike;

import android.app.Application;

import com.herewhite.kaike.common.DemoAPI;
import com.herewhite.kaike.common.RtcManager;
import com.herewhite.sdk.WhiteboardView;

import io.agora.rtc.RtcEngine;

public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        DemoAPI.get().init(getApplicationContext());
        WhiteboardView.setWebContentsDebuggingEnabled(true);
    }
}
