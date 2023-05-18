package com.example.sttapp;


import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
public class seoulActivity extends AppCompatActivity implements MapView.CurrentLocationEventListener, MapView.MapViewEventListener {
    private MapView mapView;
    private ViewGroup mapViewContainer;


    private SearchView mSearchView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seoul);

        // 서치뷰
        mSearchView = findViewById(R.id.searchView); // SearchView
        // 권한ID를 가져옵니다
        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.INTERNET);

        int permission2 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        int permission3 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);

        // 권한이 열려있는지 확인
        if (permission == PackageManager.PERMISSION_DENIED || permission2 == PackageManager.PERMISSION_DENIED || permission3 == PackageManager.PERMISSION_DENIED) {
            // 마쉬멜로우 이상버전부터 권한을 물어본다
            if (VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // 권한 체크(READ_PHONE_STATE의 requestCode를 1000으로 세팅
                requestPermissions(
                        new String[]{Manifest.permission.INTERNET, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        1000);
            }
            return;
        }

        //지도를 띄우자
        // java code
        mapView = new MapView(this);

        mapViewContainer = (ViewGroup) findViewById(R.id.map_view);
        mapViewContainer.addView(mapView);


        // 줌 레벨 변경
        mapView.setZoomLevel(4, true);

        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading);

        // 데이터 가져오는 부분 : 데이터는 장소 이름, 위도, 경도로 이루어져 있다.
        TestApiData apiData = new TestApiData();
        ArrayList<TestData> dataArr = apiData.getData();

        ArrayList<MapPOIItem> markerArr = new ArrayList<MapPOIItem>();
        for (TestData data : dataArr) {
            MapPOIItem marker = new MapPOIItem();
            marker.setMapPoint(MapPoint.mapPointWithGeoCoord(data.getLatitude(), data.getLongitude()));
            marker.setItemName(data.getPname() + '\n' + data.getName());
            markerArr.add(marker);
        }
        mapView.addPOIItems(markerArr.toArray(new MapPOIItem[markerArr.size()]));

        //서치뷰 함수 부분 );까지
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String s) {
                for (TestData data : dataArr) {
                    if (data.pname.contains(s)) {
                        //System.out.println("data11111111111111111111111111111111111111111111111111111111111111" + data);
                        mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(data.latitude, data.longitude), true);

                    }
                }
                return true;
            }
            @Override
            public boolean onQueryTextChange(String s) {
                // 입력란의 문자열이 바뀔 때 처리
                return false;
            }
        });
    }

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


    @Override
    public void onCurrentLocationUpdate(MapView mapView, MapPoint mapPoint, float v) {

    }

    @Override
    public void onCurrentLocationDeviceHeadingUpdate(MapView mapView, float v) {

    }

    @Override
    public void onCurrentLocationUpdateFailed(MapView mapView) {

    }

    @Override
    public void onCurrentLocationUpdateCancelled(MapView mapView) {

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

}
// TestData class 생성
class TestData {
    String name;

    String pname;
    Double latitude;
    Double longitude;

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

    @Override
    public String toString() {
        return "TestData{" +
                "name='" + name + '\'' +
                ", pname='" + pname + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }
}

class TestApiData {
    String apiUrl = "http://openapi.seoul.go.kr:8088/";
    String apiKey = "786f4e517663686137376e6b70456e";

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
                    boolean bName = false, bLat = false, bLong = false, pName= false ;
                    String name = "", latitude = "", longitude = "", pname= "";

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
                            }else if (parser.getName().equals("PARKING_NAME")) {// 위도 태그
                                pName = true;
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
                            }else if (pName) {
                                pname = parser.getText();
                                pName = false;
                            }
                        }
                        //내용 다 읽었으면 데이터 추가
                        else if (type == XmlPullParser.END_TAG && parser.getName().equals("row")) { // 엔드 태그의 이름 일치 확인
                            data.setName(name); // 데이터 네임 확인
                            data.setLatitude(Double.valueOf(latitude));
                            data.setLongitude(Double.valueOf(longitude));
                            data.setPname(pname);
                            dataArr.add(data);
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