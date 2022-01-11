package com.lista.myairscan;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private final static long TIME_TO_REFRESH=360000;
    private CountDownTimer timerToRefresh;
    private List<TextView> labelActualValues, labelAvgValues;
    private Model model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        model = new Model(getApplicationContext(), modelCallback);

        labelActualValues=(getListTextView((LinearLayout)findViewById(R.id.genVal)));
        labelActualValues.addAll(getListTextView((LinearLayout)findViewById(R.id.pmVal)));
        labelActualValues.addAll(getListTextView((LinearLayout)findViewById(R.id.umVal)));

        labelAvgValues=(getListTextView((LinearLayout)findViewById(R.id.genAvgVal)));
        labelAvgValues.addAll(getListTextView((LinearLayout)findViewById(R.id.pmAvgVal)));
        labelAvgValues.addAll(getListTextView((LinearLayout)findViewById(R.id.umAvgVal)));

        refreshData();
    }

    public List<TextView> getListTextView(LinearLayout ll){
        List<TextView> list = new ArrayList<>();
        int count=ll.getChildCount();
        for(int i=0; i<count; i++)
        {
            list.add((TextView)ll.getChildAt(i));
        }
        return list;
    }

    public void btnScanPressed(View view) {
        refreshData();
    }

    public void refreshData(){
        Toast.makeText(getApplicationContext(),"Odświeżam dane.",Toast.LENGTH_SHORT).show();
        model.refreshData();
    }


    private void resetTimeRenew() {
        if(timerToRefresh !=null) timerToRefresh.cancel();
        timerToRefresh =new CountDownTimer(TIME_TO_REFRESH, 100) {
            public void onTick(long millisUntilFinished) {
                ((TextView)findViewById(R.id.renewLab)).setText("Autoodświeżenie za " + millisUntilFinished / 1000+"s");
                ((ProgressBar)findViewById(R.id.renewProgressBar)).setProgress((int)((float)millisUntilFinished/36.0f), true);
            }
            public void onFinish() {
                refreshData();
            }
        }.start();
    }

    private void setValuesToTextViews(List<String> val, List<TextView> txt){
        int count = Math.min(val.size(), txt.size());
        for(int i=0; i<count; i++)
        {
            txt.get(i).setText(val.get(i));
        }
    }

    private void setViewValues() {
        setValuesToTextViews(model.getListActualValues(), labelActualValues);
        setValuesToTextViews(model.getListAvgValues(), labelAvgValues);
    }


    //==== CALLBACK FROM MODEL ===
    private ModelCallback modelCallback = new ModelCallback() {
        @Override
        public void onRefreshDataEnd(int endCode){
            if(endCode == NO_ERROR)
            {
                setViewValues();
                Toast.makeText(getApplicationContext(),"Dane odświeżone",Toast.LENGTH_LONG).show();
            }
            else if(endCode==NO_FIND_PACKETS){
                Toast.makeText(getApplicationContext(),"Nie odnaleziono urządzenia MyAirScan",Toast.LENGTH_LONG).show();
            }
            else
            {
                Toast.makeText(getApplicationContext(),"Nieoczekiwany błąd podczas odświeżania.",Toast.LENGTH_LONG).show();
            }
            resetTimeRenew();
        }

        @Override
        public void onRefreshDataFailed(int errorCode, String[] val){
            if(errorCode == NOT_HAVE_PERM)
            {
                requestGetPermission(val);
            }
            else if(errorCode == NOT_EN_FUNC)
            {
                requestEnableFunction(val[0]);
            }
            else
            {
                Toast.makeText(getApplicationContext(),"Nieoczekiwany błąd podczas próby rozpoczęcia odświeżania.",Toast.LENGTH_LONG).show();
            }
            resetTimeRenew();
        }
    };


    //==== REQUEST ENABLE FUNCTION ====
    private void requestEnableFunction(String function) {
        Toast.makeText(getApplicationContext(),"Wymagane jest włączenie funkcjonalności.",Toast.LENGTH_LONG).show();
        Intent enableIntent = new Intent(function);
        startActivityForResult(enableIntent, 6);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode!= Activity.RESULT_OK)
        {
            Toast.makeText(getApplicationContext(),"Bluetooth nie został włączony.",Toast.LENGTH_LONG).show();
        }
        else
        {
            refreshData();
        }
    }


    //==== REQUEST PERMISSION ====
    private void requestGetPermission(String[] permission){
        Toast.makeText(getApplicationContext(),"Wymagane jest nadanie uprawnień.",Toast.LENGTH_LONG).show();
        ActivityCompat.requestPermissions(MainActivity.this, permission, 6);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,  String[] permissions,  int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(MainActivity.this, "Uprawnienia "+ permissions.toString() +" nie zostały przyznane.", Toast.LENGTH_SHORT) .show();
        }
        else {
            refreshData();
        }
    }
}