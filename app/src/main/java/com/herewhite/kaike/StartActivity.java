package com.herewhite.kaike;

import static com.herewhite.kaike.RoomActivity.ROOM_CHANNELID;
import static com.herewhite.kaike.RoomActivity.ROOM_CHANNELTOKEN;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.herewhite.kaike.common.DemoAPI;
import com.herewhite.kaike.common.RtcManager;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;

public class StartActivity extends AppCompatActivity {
    public static final String EXTRA_ROOM_UUID = "com.herewhite.kaike.UUID";
    private static final int PERMISSION_REQ_ID = 22;
    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    DemoAPI demoAPI = DemoAPI.get();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    String getUuid() {
        EditText text = findViewById(R.id.editText);
        return text.getText().toString();
    }

    String getChannelId() {
        EditText text = findViewById(R.id.editText_rtc);
        return text.getText().toString();
    }

    String getChannelToken() {
        EditText text = findViewById(R.id.editText_rtctoken);
        return text.getText().toString();
    }

    void tokenAlert() {
        tokenAlert("token", "请在 https://console.herewhite.com 中注册，并获取 sdk token，再进行使用");
    }

    void tokenAlert(String title, String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(StartActivity.this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setButton(
                AlertDialog.BUTTON_NEUTRAL,
                "OK",
                (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }

    public void joinRoom(View view) {
        if (demoAPI.invalidToken()) {
            tokenAlert();
            return;
        }
        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[2], PERMISSION_REQ_ID)) {
            startRoomAc();
        }
    }

    private boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode);
            return false;
        }
        return true;
    }

    private void startRoomAc() {
        Intent intent = new Intent(this, RoomActivity.class);
        String uuid = getUuid();
        if (uuid.length() > 0) {
            intent.putExtra(EXTRA_ROOM_UUID, uuid);
        }
        String channelId = getChannelId();
        if (channelId.length() > 0) {
            intent.putExtra(ROOM_CHANNELID, channelId);
        }
        String token = getChannelToken();
        if (token.length() > 0) {
            intent.putExtra(ROOM_CHANNELTOKEN, token);
        }
        startActivity(intent);
    }

}
