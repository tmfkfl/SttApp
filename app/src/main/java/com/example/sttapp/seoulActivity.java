package com.example.sttapp;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;


import net.daum.mf.map.api.CalloutBalloonAdapter;
import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapPolyline;
import net.daum.mf.map.api.MapView;


import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class seoulActivity extends AppCompatActivity implements MapView.CurrentLocationEventListener, MapView.MapViewEventListener, ActivityCompat.OnRequestPermissionsResultCallback {
    private MapView mapView;
    private ViewGroup mapViewContainer;


    private SearchView mSearchView;

    /*현재위치 변수 넣어주는 곳 */
    ArrayList<Double> LatLng = new ArrayList<Double>();
    // 데이터 가져오는 부분 : 데이터는 장소 이름, 위도, 경도로 이루어져 있다.
    TestApiData apiData = new TestApiData();
    ArrayList<TestData> dataArr = apiData.getData();


    /*공영주차장 찾고난 후 생성할 마커 넣어주는 리스트 */
    ArrayList<MapPOIItem> markerArr = new ArrayList<MapPOIItem>();

    /*서울 행정구역 json 파일 불러오는 부분*/
    DataSeoulDistrict seoulDistrict;
    ArrayList<DataSeoulDistrict> districtData = new ArrayList<>();

    /* 길찾기 list */
    List<Map<String, Object>> list_path = new ArrayList<Map<String, Object>>();
    List<Map<String, Object>> info_path = new ArrayList<Map<String, Object>>();

    String urlStr = "";
    double endy = 0.0 ;
    double endx = 0.0;
    Button btn_navi;
    Button btn_navi_close;
    Button btn_move_navi;
    int flag =0; //flag(0 : 길 안찾는중 / 1 : 길찾는중)


    /*Log tag 부분*/
    private static final String LOG_TAG = "LocationDemoActivity";


    final static String TAG = "MapTAG";
    final static String fp_TAG = "findAddressParkingLot TAG";
    final static String onCreate_TAG = "onCreate TAG";

    final static String stt_TAG = "Recognition Listener TAG";

    /*daum setEventListner() 메소드 변수 부분*/
//    private final int MENU_LOCATION = Menu.FIRST;
//    private final int MENU_REVERSE_GEO = Menu.FIRST + 1;

    MapPoint currentMapPoint;
    public double mCurrentLng; //Long = X, Lat = Yㅌ
    public double mCurrentLat;

    /*음성인식 부분*/
    Button sttBtn;

    Intent intent;
    final int PERMISSION = 1;
    /*현재위치 버튼 On/Off 바꾸기 위한 변수*/
    int b = 0;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seoul);


        /*서울 행정구역 불러오는 함수*/
        /*res 에 raw 폴더 생성 후 seoul_district.json 파일 넣어주고
        void parser() 함수와
        Class seoul district 만들어주고
        위에 리스트 선언해주자!
         */
        parser();

        // 서치뷰
        mSearchView = findViewById(R.id.searchView); // SearchView





        //지도를 띄우자
        // java code
        mapView = new MapView(this);

        mapViewContainer = (ViewGroup) findViewById(R.id.map_view);
        mapViewContainer.addView(mapView);


        // 줌 레벨 변경
        mapView.setZoomLevel(4, true);

        /*검색창 부분*/
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                /*생성된 마커(배열에 담긴)를 지우는 함수*/
                mapView.removePOIItems(markerArr.toArray(new MapPOIItem[markerArr.size()]));
                findAddressParkinglot(s);
                return true;
            }@Override
            public boolean onQueryTextChange(String s) {
                // 입력란의 문자열이 바뀔 때 처리
                return false;
            }
        });

        /*현재위치에서 가장 가까운 마커 찾아주는 함수(findParkingLot 작동부분*/
        Button btn_findMarker = (Button) findViewById(R.id.btn_findMarker);
        btn_findMarker.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                /*기존에 생성된 마커 지우는 부분*/
                mapView.removePOIItems(markerArr.toArray(new MapPOIItem[markerArr.size()]));
                findParkingLot();
            }
        });
        /*현재위치 eventListner 선언해서 해당 하위 함수를 통해 현재위치 좌표 받아오고 / 마커 생성*/
        mapView.setCurrentLocationEventListener(this);
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeadingWithoutMapMoving);
        //카메라 추적금 map centerpoint 함수 써서 이동시키자

        /*음성인식 작동 부분*/
        sttBtn = findViewById(R.id.sttStart);

        intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ko-KR");   // 텍스트로 변환시킬 언어 설정

        sttBtn.setOnClickListener(v -> {
            SpeechRecognizer mRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            mRecognizer.setRecognitionListener(listener);
            mRecognizer.startListening(intent);
        });
        /*현재 위치 버튼*/

        Button btn_currentLocation = (Button) findViewById(R.id.btn_currentLocation);
        btn_currentLocation.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(onCreate_TAG, "버튼 클릭 - b : "+b);
                if(b % 2 == 0){
                    Toast.makeText(getApplicationContext(), "현재위치 ON", Toast.LENGTH_SHORT).show();
                    mapView.setMapCenterPoint(currentMapPoint, true);
                    mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithHeading);
                } else{
                    Toast.makeText(getApplicationContext(), "현재위치 OFF", Toast.LENGTH_SHORT).show();
                    mapView.setMapCenterPoint(currentMapPoint, false);
                    mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeadingWithoutMapMoving);
                }
                b++;

            }
        });

        //navi button
        btn_navi = (Button)findViewById(R.id.navi_btn);
        btn_navi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                //초기화
                info_path = new ArrayList<Map<String, Object>>();
                list_path = new ArrayList<Map<String, Object>>();

                Log.d("get_info_starty", Double.toString(LatLng.get(1)));
                Log.d("get_info_startx", Double.toString(LatLng.get(0)));
                Log.d("get_info_endy", Double.toString(endy));
                Log.d("get_info_endx", Double.toString(endx));

                if((endx == 0.0 && endy==0.0)||(flag ==0 )){ // 길찾기 안하면
                    Toast.makeText(getApplicationContext(), "목적지를 설정해주세요", Toast.LENGTH_LONG).show();

                }else{
                    urlStr= "https://apis-navi.kakaomobility.com/v1/directions?priority=RECOMMEND&car_type=1&car_fuel=GASOLINE&origin="+LatLng.get(1)+"%2C"+LatLng.get(0)+"&destination="+endy+"%2C"+endx+"" ;
                    RequestThread thread = new RequestThread();
                    thread.start();
                    try{

                        thread.join();
                        Log.d("check_log", "UI change code start ");

                        //mapView
                        Log.d("mapview change ","before");

                        MapPolyline po = new MapPolyline();
                        mapView.removeAllPolylines();

                        // roads
                        for(Map<String, Object> strMap : info_path){
                            ArrayList<Double> l_info = (ArrayList<Double>) strMap.get("vertexes");
                            for(int pi =0;pi<l_info.size();pi+=2){
                                Log.d("poly_roads", Double.toString(l_info.get(pi)));
                                po.addPoint(MapPoint.mapPointWithGeoCoord(l_info.get(pi+1)  , l_info.get(pi)));
                            }
                        }

                        //guides
//                    for(Map<String, Object> strMap : list_path){
//                        Log.d("poly>> ", String.valueOf(strMap.get("road_index")));
//                        po.addPoint(MapPoint.mapPointWithGeoCoord((Double)strMap.get("y")  , (Double)strMap.get("x")));
//                    }

                        Log.d("mapview change ","1");

                        po.setLineColor(Color.argb(100,0,0,255));


                        mapView.addPolyline(po);
                        mapView.fitMapViewAreaToShowAllPolylines();

                        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithHeading);//https://m.blog.naver.com/bgpoilkj/222826006726
                        //mapView.getMapRotationAngle();


                        Log.d("mapview change ","2");

                        mapViewContainer.addView(mapView);

                        Log.d("mapview change ","3");

                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }catch(Exception e){
                        e.printStackTrace();
                        Log.d("mapview change", e.getMessage());
                    }

                    //길찾기
                    flag =1;
                    if(flag ==1){
                        btn_move_navi.setVisibility(mapView.VISIBLE);
                        btn_navi_close.setVisibility(mapView.VISIBLE);
                        mapView.setMapCenterPoint(currentMapPoint, true);
                        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithHeading);
                        mapView.zoomIn(true);
                        mapView.zoomIn(true);
                        mapView.zoomIn(true);
                        mapView.zoomIn(true);
                    }else{
                        btn_navi_close.setVisibility(mapView.GONE);
                        btn_move_navi.setVisibility(mapView.GONE);
                    }
                }


            }
        });

        //btn_navi_close 작동시 navi 종료
        btn_navi_close = (Button)findViewById(R.id.navi_btn_close);
        btn_navi_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapView.removeAllPolylines();

                mapView.setZoomLevel(2,true);


                flag =0;
                btn_navi_close.setVisibility(mapView.GONE);
                btn_move_navi.setVisibility(mapView.GONE);

                mapView.setMapCenterPoint(currentMapPoint, false);
                mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeadingWithoutMapMoving);
            }
        });

        //btn_move_navi 카카오맵 연동 => manifest파일에서     <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/> 추가,
        // setting에서  maven { url 'https://devrepo.kakao.com/nexus/content/groups/public/' } , url 변경
        btn_move_navi = (Button)findViewById(R.id.move_navi_btn);
        btn_move_navi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("moveApp ","kakao?");
                String move_url ="kakaomap://route?sp="+LatLng.get(0)+","+LatLng.get(1)+"&ep="+endx+","+endy+"&by=CAR";
                Log.d("moveApp ",(String) move_url);

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(move_url));
                startActivity(intent);

                flag =0;
                btn_move_navi.setVisibility(mapView.GONE);
                btn_navi_close.setVisibility(mapView.GONE);
                mapView.removeAllPolylines();
            }
        });






        //구현한 CalloutBalloonAdapter 등록
        mapView.setCalloutBalloonAdapter(new CustomCalloutBalloonAdapter());

/*구별 주차장 찾기 버튼*/
        Button btn_gangnamgu = (Button)findViewById(R.id.btn_Gangnamgu);

        btn_gangnamgu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mapView.removePOIItems(markerArr.toArray(new MapPOIItem[markerArr.size()]));
                findGuParkingLot("강남구");
                mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(37.486228, 127.080194), true);
                mapView.setZoomLevel(6, true);

            }
        });


        Button btn_Gangdonggu = (Button)findViewById(R.id.btn_Gangdonggu);

        btn_Gangdonggu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                    mapView.removePOIItems(markerArr.toArray(new MapPOIItem[markerArr.size()]));
                    findGuParkingLot("강동구");
                    mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(37.549465, 127.148993), true);
                    mapView.setZoomLevel(6, true);

            }
        });

        Button btn_Gangbukgu = (Button)findViewById(R.id.btn_Gangbukgu);

        btn_Gangbukgu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                mapView.removePOIItems(markerArr.toArray(new MapPOIItem[markerArr.size()]));
                Toast.makeText(getApplicationContext(),  "주차장이 없는 지역입니다.", Toast.LENGTH_SHORT).show();

            }
        });

        Button btn_Gangseogu = (Button)findViewById(R.id.btn_Gangseogu);

        btn_Gangseogu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                mapView.removePOIItems(markerArr.toArray(new MapPOIItem[markerArr.size()]));
                findGuParkingLot("강서구");
                mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(37.564934, 126.826928), true);
                mapView.setZoomLevel(6, true);

            }
        });

        Button btn_Gwanakgu = (Button)findViewById(R.id.btn_Gwanakgu);

        btn_Gwanakgu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                mapView.removePOIItems(markerArr.toArray(new MapPOIItem[markerArr.size()]));
                findGuParkingLot("관악구");
                mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(37.488999, 126.910819), true);
                mapView.setZoomLevel(6, true);

            }
        });

        Button btn_Gwangjingu = (Button)findViewById(R.id.btn_Gwangjingu);

        btn_Gwangjingu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                mapView.removePOIItems(markerArr.toArray(new MapPOIItem[markerArr.size()]));
                Toast.makeText(getApplicationContext(),  "주차장이 없는 지역입니다.", Toast.LENGTH_SHORT).show();

            }
        });

        Button btn_Gurogu = (Button)findViewById(R.id.btn_Gurogu);

        btn_Gurogu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                mapView.removePOIItems(markerArr.toArray(new MapPOIItem[markerArr.size()]));
                findGuParkingLot("구로구");
                mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(37.494361, 126.895112), true);
                mapView.setZoomLevel(6, true);

            }
        });

        Button btn_Geumcheongu = (Button)findViewById(R.id.btn_Geumcheongu);

        btn_Geumcheongu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                mapView.removePOIItems(markerArr.toArray(new MapPOIItem[markerArr.size()]));
                Toast.makeText(getApplicationContext(),  "주차장이 없는 지역입니다.", Toast.LENGTH_SHORT).show();

            }
        });

        Button btn_Nowongu = (Button)findViewById(R.id.btn_Nowongu);

        btn_Nowongu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                mapView.removePOIItems(markerArr.toArray(new MapPOIItem[markerArr.size()]));
                findGuParkingLot("노원구");
                mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(37.615004, 127.064555), true);
                mapView.setZoomLevel(6, true);

            }
        });

        Button btn_Dobonggu = (Button)findViewById(R.id.btn_Dobonggu);

        btn_Dobonggu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                mapView.removePOIItems(markerArr.toArray(new MapPOIItem[markerArr.size()]));
                findGuParkingLot("도봉구");
                mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(37.679142, 127.045871), true);
                mapView.setZoomLevel(6, true);

            }
        });

        Button btn_Dongdaemungu = (Button)findViewById(R.id.btn_Dongdaemungu);

        btn_Dongdaemungu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                mapView.removePOIItems(markerArr.toArray(new MapPOIItem[markerArr.size()]));
                findGuParkingLot("동대문구");
                mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(37.581423, 127.049262), true);
                mapView.setZoomLevel(6, true);

            }
        });

        Button btn_Dongjakgu = (Button)findViewById(R.id.btn_Dongjakgu);

        btn_Dongjakgu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                mapView.removePOIItems(markerArr.toArray(new MapPOIItem[markerArr.size()]));
                Toast.makeText(getApplicationContext(),  "주차장이 없는 지역입니다.", Toast.LENGTH_SHORT).show();
            }
        });

        Button btn_Mapogu = (Button)findViewById(R.id.btn_Mapogu);

        btn_Mapogu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                mapView.removePOIItems(markerArr.toArray(new MapPOIItem[markerArr.size()]));
                findGuParkingLot("마포구");
                mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(37.549935, 126.913536), true);
                mapView.setZoomLevel(6, true);

            }
        });

        Button btn_Seodaemungu = (Button)findViewById(R.id.btn_Seodaemungu);

        btn_Seodaemungu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                mapView.removePOIItems(markerArr.toArray(new MapPOIItem[markerArr.size()]));
                findGuParkingLot("서대문구");

            }
        });

        Button btn_Seochogu = (Button)findViewById(R.id.btn_Seochogu);

        btn_Seochogu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                mapView.removePOIItems(markerArr.toArray(new MapPOIItem[markerArr.size()]));
                findGuParkingLot("서초구");
                mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(37.493347, 127.014314), true);
                mapView.setZoomLevel(6, true);

            }
        });

        Button btn_Sungdongguu = (Button)findViewById(R.id.btn_Sungdongguu);

        btn_Sungdongguu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                mapView.removePOIItems(markerArr.toArray(new MapPOIItem[markerArr.size()]));
                Toast.makeText(getApplicationContext(),  "주차장이 없는 지역입니다.", Toast.LENGTH_SHORT).show();

            }
        });

        Button btn_Sungbukgu = (Button)findViewById(R.id.btn_Sungbukgu);

        btn_Sungbukgu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                mapView.removePOIItems(markerArr.toArray(new MapPOIItem[markerArr.size()]));
                Toast.makeText(getApplicationContext(),  "주차장이 없는 지역입니다.", Toast.LENGTH_SHORT).show();

            }
        });

        Button btn_Songpagu = (Button)findViewById(R.id.btn_Songpagu);

        btn_Songpagu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                mapView.removePOIItems(markerArr.toArray(new MapPOIItem[markerArr.size()]));
                findGuParkingLot("송파구");
                mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(37.499357, 127.111668), true);
                mapView.setZoomLevel(6, true);

            }
        });

        Button btn_Yangcheongu = (Button)findViewById(R.id.btn_Yangcheongu);

        btn_Yangcheongu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                mapView.removePOIItems(markerArr.toArray(new MapPOIItem[markerArr.size()]));
                findGuParkingLot("양천구");
                mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(37.525930, 126.864544), true);
                mapView.setZoomLevel(6, true);

            }
        });


        Button btn_Yeongdeungpogu = (Button)findViewById(R.id.btn_Yeongdeungpogu);

        btn_Yeongdeungpogu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                mapView.removePOIItems(markerArr.toArray(new MapPOIItem[markerArr.size()]));
                findGuParkingLot("영등포구");
                mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(37.525252, 126.896682), true);
                mapView.setZoomLevel(6, true);

            }
        });

        Button btn_Yongsangu = (Button)findViewById(R.id.btn_Yongsangu);

        btn_Yongsangu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                mapView.removePOIItems(markerArr.toArray(new MapPOIItem[markerArr.size()]));
                findGuParkingLot("용산구");
                mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(37.532556, 126.990655), true);
                mapView.setZoomLevel(6, true);

            }
        });

        Button btn_Eunpyeonggu = (Button)findViewById(R.id.btn_Eunpyeonggu);

        btn_Eunpyeonggu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                mapView.removePOIItems(markerArr.toArray(new MapPOIItem[markerArr.size()]));
                findGuParkingLot("은평구");
                mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(37.602916, 126.928905), true);
                mapView.setZoomLevel(6, true);

            }
        });

        Button btn_Jongrogu = (Button)findViewById(R.id.btn_Jongrogu);

        btn_Jongrogu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                mapView.removePOIItems(markerArr.toArray(new MapPOIItem[markerArr.size()]));
                findGuParkingLot("종로구");
                mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(37.573731, 126.978648), true);
                mapView.setZoomLevel(6, true);

            }
        });

        Button btn_Jeunggu = (Button)findViewById(R.id.btn_Jeunggu);

        btn_Jeunggu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                mapView.removePOIItems(markerArr.toArray(new MapPOIItem[markerArr.size()]));
                findGuParkingLot("중구");
                mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(37.566799, 126.997383), true);
                mapView.setZoomLevel(6, true);

            }
        });

        Button btn_Joongranggu = (Button)findViewById(R.id.btn_Joongranggu);

        btn_Joongranggu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                mapView.removePOIItems(markerArr.toArray(new MapPOIItem[markerArr.size()]));
                findGuParkingLot("중랑구");
                mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(37.606153, 127.092799), true);
                mapView.setZoomLevel(6, true);

            }
        });


    /*구별 버튼찾기 끝*/

    }
    /*>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>oncreate 끝*/


    // 권한 체크 이후로직
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grandResults) {
        // READ_PHONE_STATE의 권한 체크 결과를 불러온다
        super.onRequestPermissionsResult(requestCode, permissions, grandResults);
        if (requestCode == 1000) {
            boolean check_result = true;

            // 모든 퍼미션을 허용했는지 체크
            for (int result : grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }

            // 권한 체크에 동의를 하지 않으면 안드로이드 종료
            if (check_result == false) {
                finish();
            }
        }
    }


    /*카카오맵 현제위치 관련한 setcurrentlocationeventlistner 관련 함수 부분*/
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOff);
        mapView.setShowCurrentLocationMarker(false);
    }

    @Override
    public void onCurrentLocationUpdate(MapView mapView, MapPoint mapPoint, float accuracyInMeters) {
        MapPoint.GeoCoordinate mapPointGeo = mapPoint.getMapPointGeoCoord();
        Log.i(LOG_TAG, String.format("MapView onCurrentLocationUpdate (%f,%f) accuracy (%f)", mapPointGeo.latitude, mapPointGeo.longitude, accuracyInMeters));
        currentMapPoint = MapPoint.mapPointWithGeoCoord(mapPointGeo.latitude, mapPointGeo.longitude);
        //이 좌표로 지도 중심 이동
//        mapView.setMapCenterPoint(currentMapPoint, true);
        //전역변수로 현재 좌표 저장
        mCurrentLat = mapPointGeo.latitude;
        mCurrentLng = mapPointGeo.longitude;
        if(LatLng.size() < 2){
            LatLng.add(mCurrentLat);
            LatLng.add(mCurrentLng);
            Log.d(TAG, "현재위치 => " + LatLng.get(LatLng.size()-2) + "  " + LatLng.get(LatLng.size()-1));
            mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(mCurrentLat, mCurrentLng), true);
        } else {
            LatLng.add(mCurrentLat);
            LatLng.add(mCurrentLng);
            Log.d(TAG, "현재위치 => " + LatLng.get(LatLng.size()-2) + "  " + LatLng.get(LatLng.size()-1));
        }
        Log.d(TAG, "리스트 크기 확인" + LatLng.size());
    }

    @Override
    public void onCurrentLocationDeviceHeadingUpdate(MapView mapView, float v) {

    }

    @Override
    public void onCurrentLocationUpdateFailed(MapView mapView) {
        Log.i(TAG, "onCurrentLocationUpdateFailed");
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading);
    }

    @Override
    public void onCurrentLocationUpdateCancelled(MapView mapView) {
        Log.i(TAG, "onCurrentLocationUpdateCancelled");
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading);
    }

    @Override
    public void onMapViewInitialized(MapView mapView) {

    }

    @Override
    public void onMapViewCenterPointMoved(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewZoomLevelChanged(MapView mapView, int i) {

    }

    @Override
    public void onMapViewSingleTapped(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewDoubleTapped(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewLongPressed(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewDragStarted(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewDragEnded(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewMoveFinished(MapView mapView, MapPoint mapPoint) {

    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        super.onCreateOptionsMenu(menu);
//        menu.add(0, MENU_LOCATION, Menu.NONE, "Location");
//        menu.add(0, MENU_REVERSE_GEO, Menu.NONE, "Reverse Geo-Coding");
//
//        return true;
//    }


    /*검색창에 값을 입력해서 가까운 공영 노외 주차장을 찾아주는 함수*/
    private void findAddressParkinglot(String address) {

        Log.d(fp_TAG,"검색 마커 찾기 함수 작동");
        Location locationA = new Location("pointA");
        locationA.setLatitude(LatLng.get( LatLng.size() - 2 ));
        locationA.setLongitude(LatLng.get( LatLng.size() - 1 ));

        float min = 99999;
        int index = -1;
        for (int i = 0; i < dataArr.size(); i++) {

            if (dataArr.get(i).name.contains(address)) {
                Location locationB = new Location("pointB");
                locationB.setLatitude(dataArr.get(i).latitude);
                locationB.setLongitude(dataArr.get(i).longitude);
                float d = locationA.distanceTo(locationB);

                if (d < min) {
                    min = d;
                    index = i;
                }
            }
        }
        /*검색한 장소에 공영주차장이 없을 경우 index = -1 을 그대로 반환한다
        이럴 경우 else 구문을 활용 검색 지역의 위도 경도를 다시 검색해서
        검색한 곳 기준으로 가장 가까운 공영주차장을 반환하는 함수
         */
        if (index != -1) {
            markerArr.clear();
//        ArrayList<MapPOIItem> markerArr = new ArrayList<MapPOIItem>(); 선언은 앞쪽에 / clear 함수와 겹치지 않기 위해서
            MapPOIItem marker = new MapPOIItem();
            marker.setMapPoint(MapPoint.mapPointWithGeoCoord(dataArr.get(index).latitude, dataArr.get(index).longitude));
            marker.setItemName(dataArr.get(index).pname);
            HashMap<String, String> userObject = new HashMap<>();
            marker.setMarkerType(MapPOIItem.MarkerType.CustomImage); // 마커타입을 커스텀 마커로 지정.
            marker.setCustomImageResourceId(R.drawable.custom_poi_marker); // 마커 이미지.
            marker.setCustomImageAutoscale(false); // hdpi, xhdpi 등 안드로이드 플랫폼의 스케일을 사용할 경우 지도 라이브러리의 스케일 기능을 꺼줌.
            marker.setCustomImageAnchor(0.5f, 1.0f); // 마커 이미지중 기준이 되는 위치(앵커포인트) 지정 - 마커 이미지 좌측 상단 기준 x(0.0f ~ 1.0f), y(0.0f ~ 1.0f) 값.
            userObject.put("desc" , "주소                " + dataArr.get(index).name);
            userObject.put("desc2", "주차장 유형           " + dataArr.get(index).building);
            userObject.put("desc3", "총 주자 가능대수      " + dataArr.get(index).capacity);
            userObject.put("desc4", "현재 주차 대수        " + dataArr.get(index).left);
            marker.setUserObject(userObject);
            markerArr.add(marker);
            mapView.addPOIItems(markerArr.toArray(new MapPOIItem[markerArr.size()]));

            mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(dataArr.get(index).latitude, dataArr.get(index).longitude), true);
            mapView.setZoomLevel(4, true);
            mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeadingWithoutMapMoving);


        } else {
            Toast.makeText(getApplicationContext(), "주차장이 없습니다 : " + address + "\n근처 주차장으로 안내", Toast.LENGTH_LONG).show();

            Double lat = 0.0;
            Double lng = 0.0;
            for (int i = 0; i < districtData.size(); i++) {
                if (districtData.get(i).dong.contains(address)) {
                    lat = districtData.get(i).lat;
                    lng = districtData.get(i).lng;
                }
            }
            locationA.setLatitude(lat);
            locationA.setLongitude(lng);

            for (int j = 0; j < dataArr.size(); j++) {
                Location locationB = new Location("pointB");
                locationB.setLatitude(dataArr.get(j).latitude);
                locationB.setLongitude(dataArr.get(j).longitude);
                float d = locationA.distanceTo(locationB);

                if (d < min) {
                    min = d;
                    index = j;
                }
            }

            if(index != -1){
                markerArr.clear();
//        ArrayList<MapPOIItem> markerArr = new ArrayList<MapPOIItem>(); 선언은 앞쪽에 / clear 함수와 겹치지 않기 위해서
                MapPOIItem marker = new MapPOIItem();
                marker.setMapPoint(MapPoint.mapPointWithGeoCoord(dataArr.get(index).latitude, dataArr.get(index).longitude));
                marker.setItemName(dataArr.get(index).pname);
                HashMap<String, String> userObject = new HashMap<>();
                marker.setMarkerType(MapPOIItem.MarkerType.CustomImage); // 마커타입을 커스텀 마커로 지정.
                marker.setCustomImageResourceId(R.drawable.custom_poi_marker); // 마커 이미지.
                marker.setCustomImageAutoscale(false); // hdpi, xhdpi 등 안드로이드 플랫폼의 스케일을 사용할 경우 지도 라이브러리의 스케일 기능을 꺼줌.
                marker.setCustomImageAnchor(0.5f, 1.0f); // 마커 이미지중 기준이 되는 위치(앵커포인트) 지정 - 마커 이미지 좌측 상단 기준 x(0.0f ~ 1.0f), y(0.0f ~ 1.0f) 값.
                userObject.put("desc" , "주소                " + dataArr.get(index).name);
                userObject.put("desc2", "주차장 유형           " + dataArr.get(index).building);
                userObject.put("desc3", "총 주자 가능대수      " + dataArr.get(index).capacity);
                userObject.put("desc4", "현재 주차 대수        " + dataArr.get(index).left);
                marker.setUserObject(userObject);
                markerArr.add(marker);
                mapView.addPOIItems(markerArr.toArray(new MapPOIItem[markerArr.size()]));

                mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(dataArr.get(index).latitude, dataArr.get(index).longitude), true);
                mapView.setZoomLevel(4, true);
                mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeadingWithoutMapMoving);
            } else{
                Toast.makeText(getApplicationContext(), "서울의 법정동이 아닙니다 : " + address, Toast.LENGTH_LONG).show();
            }


        }

        endx = dataArr.get(index).latitude;
        endy = dataArr.get(index).longitude;
        flag=1;
    }

    /*현재 위치에서 가장 가까운 공영 주차장을 찾아주는 함수*/
    private void findParkingLot() {
        Toast.makeText(getApplicationContext(),"현재 위치에서 가장 가까운 주차장",Toast.LENGTH_LONG).show();
        Location locationA = new Location("pointA");
        locationA.setLatitude(LatLng.get( LatLng.size() - 2 ));
        locationA.setLongitude(LatLng.get( LatLng.size() - 1 ));
        float min = 99999;
        int index = 0;

        for (int i = 0; i < dataArr.size(); i++) {
            Location locationB = new Location("pointB");
            locationB.setLatitude(dataArr.get(i).latitude);
            locationB.setLongitude(dataArr.get(i).longitude);
            float d = locationA.distanceTo(locationB);

            if (d < min) {
                min = d;
                index = i;
            }

        }
        markerArr.clear();
//        ArrayList<MapPOIItem> markerArr = new ArrayList<MapPOIItem>(); 선언은 앞쪽에 / clear 함수와 겹치지 않기 위해서
        MapPOIItem marker = new MapPOIItem();
        marker.setMapPoint(MapPoint.mapPointWithGeoCoord(dataArr.get(index).latitude, dataArr.get(index).longitude));
        marker.setItemName(dataArr.get(index).pname);
        HashMap<String, String> userObject = new HashMap<>();
        marker.setMarkerType(MapPOIItem.MarkerType.CustomImage); // 마커타입을 커스텀 마커로 지정.
        marker.setCustomImageResourceId(R.drawable.custom_poi_marker); // 마커 이미지.
        marker.setCustomImageAutoscale(false); // hdpi, xhdpi 등 안드로이드 플랫폼의 스케일을 사용할 경우 지도 라이브러리의 스케일 기능을 꺼줌.
        marker.setCustomImageAnchor(0.5f, 1.0f); // 마커 이미지중 기준이 되는 위치(앵커포인트) 지정 - 마커 이미지 좌측 상단 기준 x(0.0f ~ 1.0f), y(0.0f ~ 1.0f) 값.
        userObject.put("desc" , "주소                " + dataArr.get(index).name);
        userObject.put("desc2", "주차장 유형           " + dataArr.get(index).building);
        userObject.put("desc3", "총 주자 가능대수      " + dataArr.get(index).capacity);
        userObject.put("desc4", "현재 주차 대수        " + dataArr.get(index).left);
        marker.setUserObject(userObject);

        markerArr.add(marker);
        System.out.println("가장가까운 마커 확인");
        System.out.print(markerArr.size());
        mapView.addPOIItems(markerArr.toArray(new MapPOIItem[markerArr.size()]));
        /*생성된 마커로 카메라 이동*/
        mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(dataArr.get(index).latitude, dataArr.get(index).longitude), true);
        mapView.setZoomLevel(4, true);
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeadingWithoutMapMoving);
        endx = dataArr.get(index).latitude;
        endy = dataArr.get(index).longitude;
        flag =1;

    }

    private void findGuParkingLot(String Gu){
        markerArr.clear();
        for(int index = 0; index < dataArr.size(); index++){
            if(dataArr.get(index).name.contains(Gu)){
                MapPOIItem marker = new MapPOIItem();
                marker.setMapPoint(MapPoint.mapPointWithGeoCoord(dataArr.get(index).latitude, dataArr.get(index).longitude));
                marker.setItemName(dataArr.get(index).pname);
                HashMap<String, String> userObject = new HashMap<>();
                marker.setMarkerType(MapPOIItem.MarkerType.CustomImage); // 마커타입을 커스텀 마커로 지정.
                marker.setCustomImageResourceId(R.drawable.custom_poi_marker); // 마커 이미지.
                marker.setCustomImageAutoscale(false); // hdpi, xhdpi 등 안드로이드 플랫폼의 스케일을 사용할 경우 지도 라이브러리의 스케일 기능을 꺼줌.
                marker.setCustomImageAnchor(0.5f, 1.0f); // 마커 이미지중 기준이 되는 위치(앵커포인트) 지정 - 마커 이미지 좌측 상단 기준 x(0.0f ~ 1.0f), y(0.0f ~ 1.0f) 값.
                userObject.put("desc" , "주소                " + dataArr.get(index).name);
                userObject.put("desc2", "주차장 유형           " + dataArr.get(index).building);
                userObject.put("desc3", "총 주자 가능대수      " + dataArr.get(index).capacity);
                userObject.put("desc4", "현재 주차 대수        " + dataArr.get(index).left);
                marker.setUserObject(userObject);
                markerArr.add(marker);
            }
        }
        mapView.addPOIItems(markerArr.toArray(new MapPOIItem[markerArr.size()]));
        Toast.makeText(getApplicationContext(),  Gu + "의 주차장 목록입니다.", Toast.LENGTH_SHORT).show();
    }


    /*서울 행정구역 데이터 받아오는 부분*/
    private void parser() {
        InputStream inputStream = getResources().openRawResource(R.raw.seoul_district);
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        StringBuffer stringBuffer = new StringBuffer();
        String line = null;

        try {
            while ((line = bufferedReader.readLine()) != null) {
                stringBuffer.append(line);
            }
            Log.v("TAG", "StringBuffer : " + stringBuffer.toString());

            JSONObject jsonObject = new JSONObject(stringBuffer.toString());
            JSONArray jsonArray = new JSONArray(jsonObject.getString("seoul_district"));

            for (int i = 0; i < jsonArray.length(); i++) {

                JSONObject jsonObject1 = (JSONObject) jsonArray.get(i);

                String gu = jsonObject1.getString("gu");
                String dong = jsonObject1.getString("dong");
                Double lat = jsonObject1.getDouble("lat");
                Double lng = jsonObject1.getDouble("lng");

                seoulDistrict = new DataSeoulDistrict(gu, dong, lat, lng);
                districtData.add(seoulDistrict);

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (inputStreamReader != null) inputStreamReader.close();
                if (bufferedReader != null) bufferedReader.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /*음성인식 작동 함수 부분*/
    private RecognitionListener listener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {
            Toast.makeText(getApplicationContext(),"음성인식을 시작합니다. 예시) 서울 동작 찾아줘. 음성인식 가이드 참조.",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onBeginningOfSpeech() {}

        @Override
        public void onRmsChanged(float rmsdB) {}

        @Override
        public void onBufferReceived(byte[] buffer) {}

        @Override
        public void onEndOfSpeech() {}

        @Override
        public void onError(int error) {
            String message;

            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO:
                    message = "오디오 에러";
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    message = "클라이언트 에러";
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    message = "퍼미션 없음";
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    message = "네트워크 에러";
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    message = "네트웍 타임아웃";
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    message = "찾을 수 없음";
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    message = "RECOGNIZER가 바쁨";
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    message = "서버가 이상함";
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    message = "말하는 시간초과";
                    break;
                default:
                    message = "알 수 없는 오류임";
                    break;
            }

            Toast.makeText(getApplicationContext(), "에러가 발생하였습니다. : " + message,Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onResults(Bundle results) {
            try {
                /*생성된 마커(배열에 담긴)를 지우는 함수*/
                mapView.removePOIItems(markerArr.toArray(new MapPOIItem[markerArr.size()]));
                // 말을 하면 ArrayList에 단어를 넣고 textView에 단어를 이어준다.
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String word = matches.toString();
                Log.d(stt_TAG, "word  : "+word.substring(1, word.length()-1));
//                Toast.makeText(getApplicationContext(),"입력받은 메시지 ",Toast.LENGTH_SHORT).show();
                //String []array  = word.split(" ");
                String array[] = word.split(" ");

                Log.d(stt_TAG, "word spolit 배열 크기 : "+array.length);
                for(int i = 0; i < array.length; i++){
                    Log.d(stt_TAG, "word spolit 배열 : "+array[i]);
                }
                /*음성인식 처리 같은 동까지 구분해서 시작 부분 */
                if( word.contains("관악구 삼성동") || word.contains("강남구 삼성동") || word.contains("강남구 신사동") || word.contains("은평구 신사동") || word.contains("마포구 신정동") || word.contains("양천구 신정동")){
                    Log.d(stt_TAG, "예외처리 : "+array[0].substring( 1, array[0].length() ) + array[1].substring( 0 , array[0].length()-1 ));
                    //예외 처리 몇개 없어서 하드코딩으로 해결
                    if( word.contains("양천구 신정동")){
                        Toast.makeText(getApplicationContext(), "검색: " +array[0].substring( 1, array[0].length() ) + array[1].substring( 0 , array[0].length()-1 ), Toast.LENGTH_SHORT).show();
                        findAddressParkinglot("양천구 신정동");
                    } else if ( word.contains("관악구 삼성동") ) {
                        Toast.makeText(getApplicationContext(), "검색: " +array[0].substring( 1, array[0].length() ) + array[1].substring( 0 , array[0].length()-1 ), Toast.LENGTH_SHORT).show();
                        findAddressParkinglot("신림동");
                    } else if ( word.contains("강남구 삼성동")){
                        Toast.makeText(getApplicationContext(), "검색: " +array[0].substring( 1, array[0].length() ) + array[1].substring( 0 , array[0].length()-1 ), Toast.LENGTH_SHORT).show();
                        findAddressParkinglot("삼성동");
                    } else if ( word.contains("강남구 신사동")){
                        Toast.makeText(getApplicationContext(), "검색: " +array[0].substring( 1, array[0].length() ) + array[1].substring( 0 , array[0].length()-1 ), Toast.LENGTH_SHORT).show();
                        findAddressParkinglot("신사동");
                    } else if ( word.contains("은평구 신사동")) {
                        Toast.makeText(getApplicationContext(), "검색: " +array[0].substring( 1, array[0].length() ) + array[1].substring( 0 , array[0].length()-1 ), Toast.LENGTH_SHORT).show();
                        findAddressParkinglot("망원동");
                    } else if ( word.contains("마포구 신정동")) {
                        Toast.makeText(getApplicationContext(), "검색: " +array[0].substring( 1, array[0].length() ) + array[1].substring( 0 , array[0].length()-1 ), Toast.LENGTH_SHORT).show();
                        findAddressParkinglot("마포동");
                    }
                } else{
                    /*음성인식 처리 시작 부분*/
                    if(array[0].contains("가까운") || array[0].contains("근처") || array[0].contains("주변") || array[0].contains("주차장")){
                        Toast.makeText(getApplicationContext(), "검색: " + word.substring(1, word.length()-1), Toast.LENGTH_SHORT).show();
                        Log.d(stt_TAG, "함수 정상 작동");
                        Log.d(stt_TAG, "근처 주차장 찾기");
                        Log.d(stt_TAG, "생성된 음석인식 문자열 : " + array[0]);
                        findParkingLot();
                    } else{
                        //문자열 가짓수를 모두 비교해서 단어 찾기
                        if(array.length == 1 ){
                            Log.d(stt_TAG, "함수 정상 작동");
                            Toast.makeText(getApplicationContext(), "검색: " + word.substring(1, word.length()-1), Toast.LENGTH_SHORT).show();
                            //음성입력 : 노량진동
                            findAddressParkinglot(array[0].substring(1, array[0].length()-1));
                            Log.d(stt_TAG, "생성된 음성인식 문자열 :" + array[0].substring(1, array[0].length()-1));

                        } else if(array.length == 2){

                            if(array[1].contains("주차장")){
                                Log.d(stt_TAG, "함수 정상 작동");
                                //음성입력 : 노량진동 주차장
                                Toast.makeText(getApplicationContext(), "검색: " + word.substring(1, word.length()-1), Toast.LENGTH_SHORT).show();
                                findAddressParkinglot(array[0].substring(1, array[0].length()-1));
                                Log.d(stt_TAG, "생성된 음성인식 문자열 :" + array[0].substring(1, array[0].length()));
                            } else {
                                if(array[1].contains("찾아")){
                                    Log.d(stt_TAG, "함수 정상 작동");
                                    //음성입력 : 노량진동 찾아
                                    Toast.makeText(getApplicationContext(), "검색: " + word.substring(1, word.length()-1), Toast.LENGTH_SHORT).show();
                                    findAddressParkinglot(array[0].substring(1, array[0].length()));
                                    Log.d(stt_TAG, "생성된 음성인식 문자열 :" + array[0].substring(1, array[0].length()));
                                } else {
                                    //음성입력(1): 강남구 도곡동
                                    //음성입력(2): 서울 도곡동
                                    Log.d(stt_TAG, "함수 정상 작동");
                                    Toast.makeText(getApplicationContext(), "검색: " + word.substring(1, word.length()-1), Toast.LENGTH_SHORT).show();
                                    findAddressParkinglot(array[1].substring(0, array[1].length()-1));
                                    Log.d(stt_TAG, "생성된 음성인식 문자열 :" + array[1].substring(0, array[1].length()-1));
                                }

                            }
                        } else if(array.length == 3) {
                            //음성입력 : 노량진동 찾아 줘(찾아줘- 띄어서 인식됨)  /  동작구 노량진동 찾아
                            if(array[2].contains("줘")){
                                Log.d(stt_TAG, "함수 정상 작동");
                                //예)노량진동 찾아 줘
                                Toast.makeText(getApplicationContext(), "검색: " + word.substring(1, word.length()-1), Toast.LENGTH_SHORT).show();
                                findAddressParkinglot(array[0].substring(1, array[0].length()));
                                Log.d(stt_TAG, "생성된 음성인식 문자열 :" + array[0].substring(1, array[0].length()));
                            }
                            else{
                                if(array[1].contains("주차장")){
                                    Log.d(stt_TAG, "함수 정상 작동");
                                    //노량진동 주차장 찾아
                                    Toast.makeText(getApplicationContext(), "검색: " + word.substring(1, word.length()-1), Toast.LENGTH_SHORT).show();
                                    findAddressParkinglot(array[0].substring(1, array[0].length()));
                                    Log.d(stt_TAG, "생성된 음성인식 문자열 :" + array[0].substring(1, array[0].length()));
                                }else {
                                    if(array[2].contains("찾아")){
                                        Log.d(stt_TAG, "함수 정상 작동");
                                        //예)동작구 노량진동 찾아/ 서울 노량진동 찾아
                                        Toast.makeText(getApplicationContext(), "검색: " + word.substring(1, word.length()-1), Toast.LENGTH_SHORT).show();
                                        findAddressParkinglot(array[1]);
                                        Log.d(stt_TAG, "생성된 음성인식 문자열 :" + array[1]);
                                    }else{
                                        //서울 강남구 도곡동
                                        Log.d(stt_TAG, "함수 정상 작동");
                                        Toast.makeText(getApplicationContext(), "검색: " + word.substring(1, word.length()-1), Toast.LENGTH_SHORT).show();
                                        findAddressParkinglot(array[2].substring(0, array[2].length()-1));
                                        Log.d(stt_TAG, "생성된 음성인식 문자열 :" + array[2].substring(0, array[2].length()-1));
                                    }
                                }
                            }
                        } else if(array.length ==4 ) {
                            //음성입력 : 서울 노량진동 찾아 줘 / 서울 동작구 노량진동 찾아
                            if(array[3].contains("줘")){
                                if(array[1].contains("주차장")){
                                    Log.d(stt_TAG, "함수 정상 작동");
                                    //예) 노량진동 주차장 찾아 줘
                                    Toast.makeText(getApplicationContext(), "검색: " + word.substring(1, word.length()-1), Toast.LENGTH_SHORT).show();
                                    findAddressParkinglot(array[0].substring(1, array[0].length()));
                                    Log.d(stt_TAG, "생성된 음성인식 문자열 :" + array[0].substring(1, array[0].length()));
                                }else {
                                    Log.d(stt_TAG, "함수 정상 작동");
                                    //예)서울 노량진동 찾아 줘 / 동작구 노량진동 찾아 줘
                                    Toast.makeText(getApplicationContext(), "검색: " + word.substring(1, word.length()-1), Toast.LENGTH_SHORT).show();
                                    findAddressParkinglot(array[1]);
                                    Log.d(stt_TAG, "생성된 음성인식 문자열 :" + array[1]);
                                }
                            }
                            else{
                                if(array[2].contains("주차장")){
                                    Log.d(stt_TAG, "함수 정상 작동");
                                    //예) 서울 노량진동 주차장 찾아 / 동작구 노량진동 주차장 찾아
                                    Toast.makeText(getApplicationContext(), "검색: " + word.substring(1, word.length()-1), Toast.LENGTH_SHORT).show();
                                    findAddressParkinglot(array[1]);
                                    Log.d(stt_TAG, "생성된 음성인식 문자열 :" + array[1]);
                                } else {
                                    Log.d(stt_TAG, "함수 정상 작동");
                                    //예)서울 동작구 노량진동 찾아
                                    Toast.makeText(getApplicationContext(), "검색: " + word.substring(1, word.length()-1), Toast.LENGTH_SHORT).show();
                                    findAddressParkinglot(array[2]);
                                    Log.d(stt_TAG, "생성된 음성인식 문자열 :" + array[2]);
                                }

                            }
                        } else if(array.length == 5) {

                            if(array[3].contains("줘")){
                                Log.d(stt_TAG, "함수 정상 작동");
                                //음성입력: 서울 동작구 노량진동 찾아 줘
                                Toast.makeText(getApplicationContext(), "검색: " + word.substring(1, word.length()-1), Toast.LENGTH_SHORT).show();
                                findAddressParkinglot(array[2]);
                                Log.d(stt_TAG, "생성된 음성인식 문자열 :" + array[2]);
                            }else {
                                Log.d(stt_TAG, "함수 정상 작동");
                                //음성입력 : 서울 동작구 노량진동 주차장 찾아
                                Toast.makeText(getApplicationContext(), "검색: " + word.substring(1, word.length()-1), Toast.LENGTH_SHORT).show();
                                findAddressParkinglot(array[2]);
                                Log.d(stt_TAG, "생성된 음성인식 문자열 :" + array[2]);

                            }
                        } else if (array.length == 6) {
                            Log.d(stt_TAG, "함수 정상 작동");
                            //예 서울 동작구 노량진동 주차장 찾아 줘
                            Toast.makeText(getApplicationContext(), "검색: " + word.substring(1, word.length()-1), Toast.LENGTH_SHORT).show();
                            findAddressParkinglot(array[2]);
                            Log.d(stt_TAG, "생성된 음성인식 문자열 :" + array[2]);

                        }
                    }
                    /*음성인식 처리 끝 부분 */

                }
                /*음성인식 처리 같은 동까지 구분해서 끝 부분 */

            }catch (IndexOutOfBoundsException e){
                System.out.println(e);
            }
        }
        @Override
        public void onPartialResults(Bundle partialResults) {}

        @Override
        public void onEvent(int eventType, Bundle params) {}
    };

    // 길찾기 코드 Thread
    class RequestThread extends Thread {
        @Override
        public void run() { // 이 쓰레드에서 실행 될 메인 코드
            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Authorization", "KakaoAK eddec5d9f48ae855841daa18d7d81e47");


                if(conn != null){
                    conn.setConnectTimeout(10000);
                    conn.setRequestMethod("GET");

                    int resCode = conn.getResponseCode();
                    if(resCode == HttpURLConnection.HTTP_OK){
                        Log.d("check_log", "connect ok");

                        InputStream responseBody = conn.getInputStream();
                        InputStreamReader responseBodyReader = new InputStreamReader(responseBody, "UTF-8");

                        Gson gson = new Gson(); //https://hong00.tistory.com/45

                        JsonReader jsonReader = new JsonReader(responseBodyReader);
                        JsonObject jsonObject = gson.fromJson(jsonReader, JsonObject.class);

                        //https://like-tomato.tistory.com/83
                        //json 파싱

                        JsonArray json_routes = jsonObject.getAsJsonArray("routes");
                        JsonObject inner_json = (JsonObject) json_routes.get(0);
                        JsonArray json_sections = inner_json.getAsJsonArray("sections");
                        JsonObject inner2_json = (JsonObject) json_sections.get(0);
                        JsonArray json_guides = inner2_json.getAsJsonArray("guides");
//                        Log.d("check_log","jsonobject1");


                        for(int i=0; i<json_guides.size();i++){
                            JsonObject item = (JsonObject) json_guides.get(i);
                            Map<String, Object> map = new HashMap<String, Object>();

                            Log.d("check_log_path", Integer.toString(item.get("road_index").getAsInt()));
                            Log.d("check_log_path", item.get("guidance").getAsString());

                            map.put("y",  item.get("y").getAsDouble());
                            map.put("x",  item.get("x").getAsDouble());
                            map.put("road_index",  item.get("road_index").getAsInt());
                            map.put("guidance",  item.get("guidance").getAsString());
                            list_path.add(map);

                        }
                        //roads
                        JsonArray json_roads = inner2_json.getAsJsonArray("roads");
                        for(int i=0; i<json_roads.size();i++){
                            JsonObject item_roads = (JsonObject) json_roads.get(i);
                            Map<String, Object> roads_info = new HashMap<String, Object>();
                            ArrayList<Double> map_info_detail = new ArrayList<Double>();

                            Log.d("check_log_roads", item_roads.get("name").getAsString());
                            roads_info.put("name",  item_roads.get("name").getAsString());
                            roads_info.put("distance",  item_roads.get("distance").getAsInt());
                            roads_info.put("duration",  item_roads.get("duration").getAsInt());
                            roads_info.put("traffic_speed",  item_roads.get("traffic_speed").getAsInt());
                            roads_info.put("traffic_state",  item_roads.get("traffic_state").getAsInt());

                            JsonArray json_road_detail = item_roads.get("vertexes").getAsJsonArray();
                            Log.d("check_log_roads_start", item_roads.get("name").getAsString());
                            if(json_road_detail != null){
                                for (int j=0;j<json_road_detail.size();j++){
//                                    Log.d("check_log_roads_inside>>", json_road_detail.get(j).getAsString());
                                    map_info_detail.add(json_road_detail.get(j).getAsDouble());
                                }
                            }
                            Log.d("check_log_roads_type", Integer.toString(map_info_detail.size()));
                            Log.d("check_log_roads_end", item_roads.get("name").getAsString());
                            roads_info.put("vertexes",  map_info_detail);
                            roads_info.put("vertexes_size",  map_info_detail.size());
//                            System.out.println("infocheck***************"+roads_info);
                            info_path.add(roads_info);
                        }

                        Log.d("check_log", "map input");

                        jsonReader.close();
                    }
                    conn.disconnect();
                }
            } catch (Exception e) {
                Log.d("check_log", "error");
                Log.e("check_log", e.getMessage());

                e.printStackTrace();
            }
        }
    }



    /*인포 윈도우 생성 함수*/
    class CustomCalloutBalloonAdapter implements CalloutBalloonAdapter {
        private final View mCalloutBalloon;

        public CustomCalloutBalloonAdapter() {
            mCalloutBalloon = getLayoutInflater().inflate(R.layout.custom_callout_balloon, null);
        }

        @Override
        public View getCalloutBalloon(MapPOIItem poiItem) {
            ((ImageView) mCalloutBalloon.findViewById(R.id.badge)).setImageResource(R.drawable.parking_place_transportation_road_area_icon_228898);
            ((TextView) mCalloutBalloon.findViewById(R.id.title)).setText(poiItem.getItemName());
            HashMap<String, String> userObject = (HashMap<String, String>) poiItem.getUserObject();
            String desc = userObject.get("desc");
            ((TextView) mCalloutBalloon.findViewById(R.id.desc)).setText(desc);
            String desc2 = userObject.get("desc2");
            ((TextView) mCalloutBalloon.findViewById(R.id.desc2)).setText(desc2);
            String desc3 = userObject.get("desc3");
            ((TextView) mCalloutBalloon.findViewById(R.id.desc3)).setText(desc3);
            String desc4 = userObject.get("desc4");
            ((TextView) mCalloutBalloon.findViewById(R.id.desc4)).setText(desc4);
            return mCalloutBalloon;
        }

        @Override
        public View getPressedCalloutBalloon(MapPOIItem poiItem) {
            return null;
        }
    }

    /*생성된 마커 관련 이벤트 리스너*/
    private MapView.POIItemEventListener poiItemEventListener = new MapView.POIItemEventListener() {
        @Override
        public void onPOIItemSelected(MapView mapView, MapPOIItem mapPOIItem) {
            //sample code 없음
            Log.i("111111111111111","진입");
        }

        @Override
        public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem) {
            Toast.makeText(seoulActivity.this, "Clicked" + mapPOIItem.getItemName() + " Callout Balloon", Toast.LENGTH_LONG).show();
            Log.i("2222222222222222","진입");
        }

        @Override
        public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem, MapPOIItem.CalloutBalloonButtonType calloutBalloonButtonType) {
            //sample code 없음
            Log.i("333333333333","진입");
        }

        @Override
        public void onDraggablePOIItemMoved(MapView mapView, MapPOIItem mapPOIItem, MapPoint mapPoint) {
            //sample code 없음
            Log.i("444444444444444","진입");
        }
    };

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(seoulActivity.this, MainActivity.class); //지금 액티비티에서 다른 액티비티로 이동하는 인텐트 설정
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);    //인텐트 플래그 설정
        startActivity(intent);  //인텐트 이동
        finish();   //현재 액티비티 종료
    }
}

// 주차장 정보를 담고있는TestData class 생성
class TestData {
    String name;
    String pname;
    String building;
    Double latitude;
    Double longitude;

    //전체 면적
    String capacity;
    //가능 주차대수
    String left;




    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setPname(String pname) {
        this.pname = pname;
    }

    public String getPname() {
        return pname;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public String getBuilding() {
        return building;
    }


    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setCapacity(String capacity) {
        this.capacity = capacity;
    }

    public String getCapacity() {
        return capacity;
    }
    public void setLeft(String left) {
        this.left = left;
    }

    public String getLeft() {
        return left;
    }


    @Override
    public String toString() {
        return "TestData{" +
                "name='" + name + '\'' +
                ", pname=" + pname + '\'' +
                ", building=" + building + '\'' +
                ", capacity=" + capacity + '\'' +
                ", left=" + left + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }
}

/*서울 공공데이터 포털에서 데이터를 불러들이는 부분 */
class TestApiData {
    String apiUrl = "http://openapi.seoul.go.kr:8088/";
    String apiKey = "495a664f59676864393363796b586b";
    public ArrayList<TestData> getData() {
        //return과 관련된 부분
        ArrayList<TestData> dataArr = new ArrayList<TestData>();

        //네트워킹 작업은 메인스레드에서 처리하면 안된다. 따로 스레드를 만들어 처리하자

        Thread t = new Thread() {
            @Override
            public void run() {

                try {
                    //url과 관련된 부분
                    String fullurl = apiUrl + apiKey + "/xml" + "/GetParkingInfo/1/1000";
                    URL url = new URL(fullurl);
                    InputStream is = url.openStream();

                    //xmlParser 생성
                    XmlPullParserFactory xmlFactory = XmlPullParserFactory.newInstance();
                    XmlPullParser parser = xmlFactory.newPullParser();
                    parser.setInput(is,"utf-8");

                    //xml과 관련된 변수들
                    boolean bName = false, bLat = false, bLong = false, bPname = false, bBuilding = false, bCapacity = false, bLeft = false;
                    String name = "", latitude = "", longitude = "", pname = "", building = "", capacity = "", left = "";

                    //본격적으로 파싱
                    while(parser.getEventType() != XmlPullParser.END_DOCUMENT) {
                        int type = parser.getEventType();
                        TestData data = new TestData();

                        //태그 확인
                        if(type == XmlPullParser.START_TAG) { // 시작 태그부터 xml 모든 데이터 파싱
                            if (parser.getName().equals("ADDR")) {// 위치명 태그
                                bName = true; // true 값 변수 지정
                            } else if (parser.getName().equals("LAT")) {// 위도 태그
                                bLat = true;
                            }else if (parser.getName().equals("LNG")) {// 경도 태그
                                bLong = true;
                            }else if (parser.getName().equals("PARKING_NAME")){
                                bPname = true;
                            }else if (parser.getName().equals("PARKING_TYPE_NM")){
                                bBuilding = true;
                            }else if (parser.getName().equals("CAPACITY")){
                                bCapacity = true;
                            }else if (parser.getName().equals("CUR_PARKING")){
                                bLeft = true;
                            }
                        }
                        //내용(텍스트) 확인
                        else if(type == XmlPullParser.TEXT) {
                            if (bName) {
                                name = parser.getText();
                                bName = false;
                            } else if (bLat) {
                                latitude = parser.getText();
                                bLat = false;
                            } else if (bLong) {
                                longitude = parser.getText();
                                bLong = false;
                            } else if (bPname) {
                                pname = parser.getText();
                                bPname = false;
                            } else if (bBuilding){
                                building = parser.getText();
                                bBuilding = false;
                            } else if(bCapacity) {
                                capacity = parser.getText();
                                bCapacity = false;
                            } else if(bLeft) {
                                left = parser.getText();
                                bLeft = false;
                            }

                        }
                        //내용 다 읽었으면 데이터 추가
                        else if (type == XmlPullParser.END_TAG && parser.getName().equals("row")) { // 엔드 태그의 이름 일치 확인

                            data.setName(name); // 데이터 네임 확인
                            data.setLatitude(Double.valueOf(latitude));
                            data.setLongitude(Double.valueOf(longitude));
                            data.setPname(pname);
                            data.setBuilding(building);
                            data.setCapacity(capacity);
                            data.setLeft(left);
                            if(building.equals("노외 주차장")){
                                dataArr.add(data);
                            }else {
                                //공백으로 pass
                            }
                        }

                        type = parser.next();
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        };
        try {
            t.start();
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return dataArr;
    }

}

/*서울 행정구역 데이터 저장 class*/
class DataSeoulDistrict {
    String gu;
    String dong;
    Double lat;
    Double lng;


    public DataSeoulDistrict(String gu, String dong, Double lat, Double lng) {
        this.gu = gu;
        this.dong = dong;
        this.lat = lat;
        this.lng = lng;
    }

    public String getGu() {
        return gu;
    }

    public void setGu(String gu) {
        this.gu = gu;
    }

    public String getDong() {
        return dong;
    }

    public void setDong(String dong) {
        this.dong = dong;
    }


    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }
}




