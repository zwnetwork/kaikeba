package com.herewhite.kaike.common;

import static io.agora.rtc.video.VideoCanvas.RENDER_MODE_HIDDEN;
import static io.agora.rtc.video.VideoEncoderConfiguration.FRAME_RATE;
import static io.agora.rtc.video.VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_1;
import static io.agora.rtc.video.VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_10;
import static io.agora.rtc.video.VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15;
import static io.agora.rtc.video.VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_24;
import static io.agora.rtc.video.VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_30;
import static io.agora.rtc.video.VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_7;
import static io.agora.rtc.video.VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT;
import static io.agora.rtc.video.VideoEncoderConfiguration.STANDARD_BITRATE;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_120x120;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_1280x720;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_160x120;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_180x180;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_240x180;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_240x240;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_320x180;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_320x240;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_360x360;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_424x240;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_480x360;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_480x480;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_640x360;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_640x480;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_840x480;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_960x720;

import android.content.Context;
import android.graphics.PixelFormat;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.mediaio.AgoraSurfaceView;
import io.agora.rtc.mediaio.AgoraTextureView;
import io.agora.rtc.mediaio.IVideoSink;
import io.agora.rtc.mediaio.MediaIO;
import io.agora.rtc.models.ChannelMediaOptions;
import io.agora.rtc.models.ClientRoleOptions;
import io.agora.rtc.video.CameraCapturerConfiguration;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;

public class RtcManager {
    private static final String TAG = "RtcManager";
    private static final int LOCAL_RTC_UID = 0;
    private static RtcManager instance;
    private RtcEngine engine;
    private OnChannelListener publishChannelListener;
    private Map<Integer, ViewGroup> remoteViews = new ConcurrentHashMap<Integer, ViewGroup>();
    public static final VideoEncoderConfiguration encoderConfiguration = new VideoEncoderConfiguration(
            VD_640x360,
            FRAME_RATE.FRAME_RATE_FPS_15,
            VideoEncoderConfiguration.STANDARD_BITRATE,
            VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT
    );

    public synchronized static RtcManager get() {
        if (instance == null) {
            instance = new RtcManager();
        }
        return instance;
    }

    public void init(Context context, String appId) {
        try {
            engine = RtcEngine.create(context.getApplicationContext(), appId, mRtcEventHandler);
        } catch (Exception e) {
            Log.e(TAG, "RtcEngine create exception : " + e.toString());
        }
    }

    public void joinChannel(String channelId, String uid, String token, OnChannelListener listener) {
        if (engine == null) {
            return;
        }
        int _uid = LOCAL_RTC_UID;
        if (!TextUtils.isEmpty(uid)) {
            try {
                _uid = Integer.parseInt(uid);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        ChannelMediaOptions options = new ChannelMediaOptions();
        options.publishLocalAudio = false;
        options.publishLocalVideo = false;
        engine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
        engine.setClientRole(Constants.CLIENT_ROLE_AUDIENCE);
        engine.enableVideo();
        engine.setVideoEncoderConfiguration(encoderConfiguration);
        publishChannelListener = new OnChannelListener() {
            @Override
            public void onError(int code, String message) {
                if (listener != null) {
                    listener.onError(code, message);
                }
            }

            @Override
            public void onJoinSuccess(int uid) {
                if (listener != null) {
                    listener.onJoinSuccess(uid);
                }
            }

            @Override
            public void onUserJoined(int uid) {
                if (listener != null) {
                    listener.onUserJoined(uid);
                }
            }

            @Override
            public void onUserOffline(int uid) {
                if (listener != null) {
                    listener.onUserOffline(uid);
                }
            }

            @Override
            public void onFirstRemoteVideoDecoded(int uid) {
                if (listener != null) {
                    listener.onFirstRemoteVideoDecoded(uid);
                }
            }

        };

        int ret = engine.joinChannel(token, channelId, null, _uid, options);
        Log.i(TAG, String.format("joinChannel channel %s ret %d", channelId, ret));
    }

    public synchronized void renderRemoteVideo(FrameLayout container, int uid, boolean isRemove) {
        if (engine == null) {
            return;
        }
        if (!isRemove) {
            if (remoteViews.containsKey(uid)) {
                return;
            } else {
                Log.e(TAG, String.format("renderRemoteVideo uid  %d", uid));
                if (container.getChildCount() > 0) {
                    container.removeAllViews();
                }
                SurfaceView surfaceView = RtcEngine.CreateRendererView(container.getContext());
                surfaceView.setZOrderMediaOverlay(true);
                remoteViews.put(uid, container);
                // Add to the remote container
                container.addView(surfaceView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                // Setup remote video to render
                engine.setupRemoteVideo(new VideoCanvas(surfaceView, RENDER_MODE_HIDDEN, uid));
            }
        } else {
            engine.setupRemoteVideo(new VideoCanvas(null, RENDER_MODE_HIDDEN, uid));
            if (remoteViews.get(uid) != null) {
                remoteViews.get(uid).removeAllViews();
                remoteViews.remove(uid);
            }
        }
    }

    public void release() {
        publishChannelListener = null;
        if (engine != null) {
            engine.leaveChannel();
            RtcEngine.destroy();
            engine = null;
        }
        if (remoteViews != null) {
            remoteViews.clear();
        }
        instance = null;
    }

    // rtc 回调
    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onWarning(int warn) {
            super.onWarning(warn);
            Log.w(TAG, String.format("onWarning code %d message %s", warn, RtcEngine.getErrorDescription(warn)));
        }

        @Override
        public void onError(int err) {
            super.onError(err);
            Log.e(TAG, String.format("onError code %d", err));
            if (err == ErrorCode.ERR_OK) {
                if (publishChannelListener != null) {
                    publishChannelListener.onError(err, "");
                }
            }
        }

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            super.onJoinChannelSuccess(channel, uid, elapsed);
            if (publishChannelListener != null) {
                publishChannelListener.onJoinSuccess(uid);
            }
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            super.onUserJoined(uid, elapsed);
            Log.d(TAG, "onUserJoined uid=" + uid + ",elapsed=" + elapsed);
            if (publishChannelListener != null) {
                publishChannelListener.onUserJoined(uid);
            }
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            super.onUserOffline(uid, reason);
            Log.d(TAG, "onUserOffline uid=" + uid + ",reason=" + reason);
            if (publishChannelListener != null) {
                publishChannelListener.onUserOffline(uid);
            }
        }

        @Override
        public void onRtcStats(RtcStats stats) {
            super.onRtcStats(stats);
        }

        @Override
        public void onStreamMessage(int uid, int streamId, byte[] data) {
            super.onStreamMessage(uid, streamId, data);
            Log.d(TAG, "onStreamMessage uid=" + uid + ",streamId=" + streamId + ",data=" + new String(data));
        }

        @Override
        public void onStreamMessageError(int uid, int streamId, int error, int missed,
                                         int cached) {
            super.onStreamMessageError(uid, streamId, error, missed, cached);
            Log.d(TAG, "onStreamMessageError uid=" + uid + ",streamId=" + streamId + ",error=" + error + ",missed=" + missed + ",cached=" + cached);
        }
    };

    public interface OnChannelListener {
        void onError(int code, String message);

        void onJoinSuccess(int uid);

        void onUserJoined(int uid);

        void onUserOffline(int uid);

        void onFirstRemoteVideoDecoded(int uid);
    }
}
