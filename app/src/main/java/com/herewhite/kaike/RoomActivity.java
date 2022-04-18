
package com.herewhite.kaike;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.gson.Gson;
import com.herewhite.kaike.common.DemoAPI;
import com.herewhite.kaike.common.RtcManager;
import com.herewhite.sdk.CommonCallback;
import com.herewhite.sdk.Room;
import com.herewhite.sdk.RoomListener;
import com.herewhite.sdk.RoomParams;
import com.herewhite.sdk.WhiteSdk;
import com.herewhite.sdk.WhiteSdkConfiguration;
import com.herewhite.sdk.WhiteboardView;
import com.herewhite.sdk.domain.Promise;
import com.herewhite.sdk.domain.RoomPhase;
import com.herewhite.sdk.domain.RoomState;
import com.herewhite.sdk.domain.SDKError;

import org.json.JSONObject;

import java.util.Date;

public class RoomActivity extends BaseActivity {
    static final String TAG = RoomActivity.class.getSimpleName();
    /**
     * 和 iOS 名字一致
     */
    private static final String EVENT_NAME = "WhiteCommandCustomEvent";
    public static final String ROOM_CHANNELID = "Room_CHANNELID";
    public static final String ROOM_CHANNELTOKEN = "Room_TOKEN";
    final Gson gson = new Gson();
    final DemoAPI demoAPI = DemoAPI.get();
    final RtcManager rtcManager = RtcManager.get();
    // Room Params
    private String uuid;
    private String token;
    private String channelId;
    private String channelToken;

    WhiteboardView mWhiteboardView;
    WhiteSdk mWhiteSdk;
    Room mRoom;
    private boolean isVertical = true;
    private FrameLayout fr_container, fr_float;
    private int mUid = 0;
    private LinearLayout ll_parent, ll_content;
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);
        mWhiteboardView = findViewById(R.id.white);
        ll_parent = findViewById(R.id.ll_parent);
        ll_content = findViewById(R.id.ll_content);
        fr_container = findViewById(R.id.fr_container);
        fr_float = findViewById(R.id.fr_float);
        webView = findViewById(R.id.webview);
        channelId = getIntent().getStringExtra(ROOM_CHANNELID);
        channelToken = getIntent().getStringExtra(ROOM_CHANNELTOKEN);
        rtcinit(getString(R.string.agora_app_id));
        setupRoom();
        webView.loadUrl("https://docs.agora.io/cn/Voice/API%20Reference/cpp/v3.6.2/index.html#a31617bf007d960611c12d9b8fd1c0244");
    }

    private void rtcinit(String appId) {
        rtcManager.init(getBaseContext(), appId);
    }

    private void setupRoom() {
        String uuid = getIntent().getStringExtra(StartActivity.EXTRA_ROOM_UUID);
        DemoAPI.Result result = new DemoAPI.Result() {
            @Override
            public void success(String uuid, String token) {
                joinRoom(uuid, token);
            }

            @Override
            public void fail(String message) {
                alert("创建房间失败", message);
            }
        };

        if (uuid != null) {
            demoAPI.getRoomToken(uuid, result);
        } else {
            demoAPI.getNewRoom(result);
        }
        rtcManager.joinChannel(channelId, null, channelToken, new RtcManager.OnChannelListener() {
            @Override
            public void onError(int code, String message) {

            }

            @Override
            public void onJoinSuccess(int uid) {

            }

            @Override
            public void onUserJoined(int uid) {
                mUid = uid;
                runOnUiThread(() -> {
                    if (isVertical) {
                        fr_float.setVisibility(View.VISIBLE);
                        fr_container.setVisibility(View.GONE);
                        rtcManager.renderRemoteVideo(fr_float, uid, false);
                    } else {
                        fr_float.setVisibility(View.GONE);
                        fr_container.setVisibility(View.VISIBLE);
                        rtcManager.renderRemoteVideo(fr_container, uid, false);
                    }
                });

            }

            @Override
            public void onUserOffline(int uid) {
                runOnUiThread(() -> {
                    mUid = 0;
                    fr_float.setVisibility(View.GONE);
                    fr_container.setVisibility(View.GONE);
                    if (isVertical) {
                        rtcManager.renderRemoteVideo(fr_float, uid, true);
                    } else {
                        rtcManager.renderRemoteVideo(fr_container, uid, true);
                    }
                });
            }

            @Override
            public void onFirstRemoteVideoDecoded(int uid) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        if (mRoom != null) {
            mRoom.disconnect();
        }
        if (rtcManager != null) {
            rtcManager.release();
        }
        super.onDestroy();
    }

    //region room
    private void joinRoom(String uuid, String token) {
        logRoomInfo("room uuid: " + uuid + "\nroom token: " + token);
        //存档一下，方便重连
        this.uuid = uuid;
        this.token = token;
        WhiteSdkConfiguration configuration = new WhiteSdkConfiguration(demoAPI.getAppId(), true);
        /*显示用户头像*/
        configuration.setUserCursor(true);
        mWhiteSdk = new WhiteSdk(mWhiteboardView, this, configuration);
        //图片替换 API，需要在 whiteSDKConfig 中先行调用 setHasUrlInterrupterAPI，进行设置，否则不会被回调。
        mWhiteSdk.setCommonCallbacks(new CommonCallback() {
            @Override
            public String urlInterrupter(String sourceUrl) {
                return sourceUrl;
            }

            @Override
            public void onMessage(JSONObject message) {
                Log.d(TAG, message.toString());
            }

            @Override
            public void sdkSetupFail(SDKError error) {
                Log.e(TAG, "sdkSetupFail " + error.toString());
            }

            @Override
            public void throwError(Object args) {
                Log.e(TAG, "throwError " + args);
            }

            @Override
            public void onPPTMediaPlay() {
                logAction();
            }

            @Override
            public void onPPTMediaPause() {
                logAction();
            }

            @Override
            public void onLogger(JSONObject object) {
                logAction(object.toString());
            }
        });

        //如需支持用户头像，请在设置 WhiteSdkConfiguration 后，再调用 setUserPayload 方法，传入符合用户信息
        RoomParams roomParams = new RoomParams(uuid, token, DemoAPI.DEFAULT_UID);
        roomParams.setDisableNewPencil(false);
        roomParams.setWritable(false);

        final Date joinDate = new Date();
        logRoomInfo("native join " + joinDate);
        mWhiteSdk.joinRoom(roomParams, new RoomListener() {
            @Override
            public void onCanUndoStepsUpdate(long canUndoSteps) {
                logRoomInfo("canUndoSteps: " + canUndoSteps);
            }

            @Override
            public void onCanRedoStepsUpdate(long canRedoSteps) {
                logRoomInfo("onCanRedoStepsUpdate: " + canRedoSteps);
            }

            @Override
            public void onCatchErrorWhenAppendFrame(long userId, Exception error) {
                logRoomInfo("onCatchErrorWhenAppendFrame: " + userId + " error " + error.getMessage());
            }

            @Override
            public void onPhaseChanged(RoomPhase phase) {
                //在此处可以处理断连后的重连逻辑
                logRoomInfo("onPhaseChanged: " + phase.name());
                showToast(phase.name());
            }

            @Override
            public void onDisconnectWithError(Exception e) {
                logRoomInfo("onDisconnectWithError: " + e.getMessage());
            }

            @Override
            public void onKickedWithReason(String reason) {
                logRoomInfo("onKickedWithReason: " + reason);
            }

            @Override
            public void onRoomStateChanged(RoomState modifyState) {
                logRoomInfo("onRoomStateChanged:" + gson.toJson(modifyState));
            }
        }, new Promise<Room>() {
            @Override
            public void then(Room room) {
                //记录加入房间消耗的时长
                logRoomInfo("native join in room duration: " + (System.currentTimeMillis() - joinDate.getTime()) / 1000f + "s");
                mRoom = room;
                addCustomEventListener();
            }

            @Override
            public void catchEx(SDKError t) {
                logRoomInfo("native join fail: " + t.getMessage());
                showToast(t.getMessage());
            }
        });
    }
    //endregion

    //region private
    private void alert(final String title, final String detail) {
        runOnUiThread(() -> {
            AlertDialog alertDialog = new AlertDialog.Builder(RoomActivity.this).create();
            alertDialog.setTitle(title);
            alertDialog.setMessage(detail);
            alertDialog.setButton(
                    AlertDialog.BUTTON_NEUTRAL,
                    "OK",
                    (dialog, which) -> {
                        dialog.dismiss();
                        finish();
                    });
            alertDialog.show();
        });
    }

    private void addCustomEventListener() {
        mRoom.addMagixEventListener(EVENT_NAME, event -> {
            logRoomInfo("customEvent payload: " + event.getPayload().toString());
            showToast(gson.toJson(event.getPayload()));
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (rtcManager == null) return;
        if (isVertical) {
            if (mUid != 0) {
                rtcManager.renderRemoteVideo(fr_float, mUid, true);
                fr_float.setVisibility(View.GONE);
                fr_container.setVisibility(View.VISIBLE);
                rtcManager.renderRemoteVideo(fr_container, mUid, false);
            }

            ll_parent.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mWhiteboardView.getLayoutParams();
            params.width = 0;
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            params.weight = 0.8f;
            mWhiteboardView.setLayoutParams(params);

            LinearLayout.LayoutParams params2 = (LinearLayout.LayoutParams) ll_content.getLayoutParams();
            params2.width = 0;
            params2.height = ViewGroup.LayoutParams.MATCH_PARENT;
            params2.weight = 0.2f;
            ll_content.setLayoutParams(params2);
        } else {
            if (mUid != 0) {
                rtcManager.renderRemoteVideo(fr_container, mUid, true);
                fr_container.setVisibility(View.GONE);
                fr_float.setVisibility(View.VISIBLE);
                rtcManager.renderRemoteVideo(fr_float, mUid, false);
            }

            ll_parent.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mWhiteboardView.getLayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = 0;
            params.weight = 0.5f;

            mWhiteboardView.setLayoutParams(params);
            LinearLayout.LayoutParams params2 = (LinearLayout.LayoutParams) ll_content.getLayoutParams();
            params2.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params2.height = 0;
            params2.weight = 0.5f;
            ll_content.setLayoutParams(params2);
        }
        isVertical = !isVertical;
        // Note：sdk内部已经实现size变更。
        // 特别情况下出现页面异常状况，调用WhiteboardView.setAutoResize(false)禁用内部处理; 外部调用在合适时机调用Room.refreshViewSize()
        // logRoomInfo("width:" + mWhiteboardView.getWidth() / getResources().getDisplayMetrics().density + " height: " + mWhiteboardView.getHeight() / getResources().getDisplayMetrics().density);
        // onConfigurationChanged 调用时，横竖屏切换并没有完成，需要延迟调用
        // new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
        //     @Override
        //     public void run() {
        //         mRoom.refreshViewSize();
        //         logRoomInfo("width:" + mWhiteboardView.getWidth() / getResources().getDisplayMetrics().density + " height: " + mWhiteboardView.getHeight() / getResources().getDisplayMetrics().density);
        //     }
        // }, 1000);
    }

    //region menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.room_command, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return true;
    }

    public void reconnect(MenuItem item) {
        mRoom.disconnect(new Promise<Object>() {
            @Override
            public void then(Object b) {
                joinRoom(RoomActivity.this.uuid, RoomActivity.this.token);
            }

            @Override
            public void catchEx(SDKError t) {

            }
        });
    }

    public void setWritableFalse(MenuItem item) {
        mRoom.setWritable(false, new Promise<Boolean>() {
            @Override
            public void then(Boolean aBoolean) {
                logRoomInfo("room writable: " + aBoolean);
            }

            @Override
            public void catchEx(SDKError t) {

            }
        });
    }

    public void setWritableTrue(MenuItem item) {
        mRoom.setWritable(true, new Promise<Boolean>() {
            @Override
            public void then(Boolean aBoolean) {
                logRoomInfo("room writable: " + aBoolean);
            }

            @Override
            public void catchEx(SDKError t) {

            }
        });
    }

    @SuppressLint("SourceLockedOrientationActivity")
    public void orientation(MenuItem item) {
        if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            RoomActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            RoomActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    public void getRoomPhase(MenuItem item) {
        logAction();
        logRoomInfo("RoomPhase: " + gson.toJson(mRoom.getRoomPhase()));
    }

    public void getRoomState(MenuItem item) {
        logAction();
        //获取房间状态，包含很多信息
        logRoomInfo("roomState: " + gson.toJson(mRoom.getRoomState()));
    }

    /**
     * 清除所有内容
     * @param item
     */
    public void cleanScene(MenuItem item) {
        mRoom.cleanScene(false);
    }

    public void disconnect(MenuItem item) {

        //如果需要房间断开连接后回调
        mRoom.disconnect(new Promise<Object>() {
            @Override
            public void then(Object o) {
                logAction("disconnect success");
            }

            @Override
            public void catchEx(SDKError t) {

            }
        });

        //如果不需要回调，则直接断开连接即可
        //room.disconnect();
    }
}
