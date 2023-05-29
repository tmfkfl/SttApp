package com.example.sttapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.jetbrains.annotations.NotNull;


public class MainActivity extends AppCompatActivity {

    // 클래스를 선언
    private PermissionSupport permission;
    private String TAG ="VideoActivity";
    private VideoView mVideoview;

    private long backButtonPressedTime = 0;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        showCustomDialog();

        mVideoview = (VideoView) findViewById(R.id.video_view);

        mVideoview.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.main_video));
        mVideoview.requestFocus();
        mVideoview.start();

        mVideoview.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setLooping(true);
            }
        });

        findViewById(R.id.btnSuccess).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCustomDialog();
            }
        });


        setTitle(R.string.app_name);


        findViewById(R.id.SeoulParkingLot).setOnClickListener(view -> {
            // 서울 주차장 현황
            Intent intent = new Intent(this, seoulActivity.class);
            startActivity(intent);
        });


        permissionCheck();

    }


    private void showCustomDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this
                , R.style.AlertDialogTheme);

        View view = LayoutInflater.from(MainActivity.this).inflate(
                R.layout.layout_green_dialog,
                (LinearLayout)findViewById(R.id.layoutDialog));
        builder.setView(view);
        ((TextView)view.findViewById(R.id.textTitle)).setText("안내문");
        ((TextView)view.findViewById(R.id.textMessage)).setText("*초기 설치시  지도 및 마이크 권한 획득 을 반드시 해주시기 바랍니다.*\n"+"\n"+"1. 서울 시내에 위치한 주차장을 검색해줍니다.\n" +
                "   (음성 및 텍스트)\n" +
                "\n" +
                "2. 목적지 검색 시 동이름으로 검색해주십시오.\n" +
                "   Ex) 흑석동, 흑석동 찾아줘,  관악구 삼성동\n" +
                "\n" +
                "3. 길안내, 주차장 잔여석 안내 기능을 제공합니다.");
        ((Button)view.findViewById(R.id.btnOk)).setText("OK");

        AlertDialog alertDialog = builder.create();

        view.findViewById(R.id.btnOk).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                alertDialog.dismiss();
            }
        });
        if(alertDialog.getWindow() != null){
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
        alertDialog.show();


    }

    private long time= 0;

    @Override

    public void onBackPressed(){

        if(System.currentTimeMillis() - time >= 2000){
            time=System.currentTimeMillis();
            Toast.makeText(getApplicationContext(),"한번더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show();
        }

        else if(System.currentTimeMillis() - time < 2000 ){

            finish();
        }
    }
    private void permissionCheck(){
        // sdk 23버전 이하 버전에서는 permission이 필요하지 않음
        if(Build.VERSION.SDK_INT >= 23){

            // 클래스 객체 생성
            permission =  new PermissionSupport(this, this);

            // 권한 체크한 후에 리턴이 false일 경우 권한 요청을 해준다.
            if(!permission.checkPermission()){
                permission.requestPermission();
            }
        }
    }

    // Request Permission에 대한 결과 값을 받는다.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions, @NonNull @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // 리턴이 false일 경우 다시 권한 요청
        if (!permission.permissionResult(requestCode, permissions, grantResults)){
            permission.requestPermission();
        }
    }

}