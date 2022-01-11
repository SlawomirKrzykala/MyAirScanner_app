package com.lista.myairscan;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Handler;
import android.provider.Settings;

import androidx.core.content.ContextCompat;
import java.util.List;



public class Model {
    private static final String[] REQUIRED_PERMISSION= new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
    private static final long SCAN_MAX_PERIOD = 12000;

    private Context context;
    private ModelCallback callback;

    private BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothLeScanner btLeScanner = btAdapter.getBluetoothLeScanner();
    private Handler btScanHandler = new Handler();
    private boolean activeScanning=false;
    private int recivedFlag=0;

    private MASValues masValues = new MASValues(0);
    private MASValues masValuesAvg = new MASValues(1);

    public Model(Context context, ModelCallback callback) {
        this.context=context;
        this.callback=callback;
    }

    public List<String> getListActualValues(){
        return masValues.getListString();
    }

    public List<String> getListAvgValues(){
        return masValuesAvg.getListString();
    }

    public void refreshData() {
        scanBlePacket();
    }

    private void scanBlePacket() {

        if(checkPermission(REQUIRED_PERMISSION) == false)
        {
            //Toast.makeText(context,"Wymagane są uprawnienia.",Toast.LENGTH_LONG).show();
            callback.onRefreshDataFailed(ModelCallback.NOT_HAVE_PERM, REQUIRED_PERMISSION);
            return;
        }

        if(((LocationManager)context.getSystemService(Context.LOCATION_SERVICE)).isProviderEnabled(LocationManager.GPS_PROVIDER) == false)
        {
            //Toast.makeText(context,"Wymagane jest właczenie lokalizacji.",Toast.LENGTH_LONG).show();
            callback.onRefreshDataFailed(ModelCallback.NOT_EN_FUNC, new String[]{Settings.ACTION_LOCATION_SOURCE_SETTINGS});
            return;
        }

        if (btAdapter.isEnabled() == false)
        {
            //Toast.makeText(context, "Wymagane jest włączenie bluetooth.", Toast.LENGTH_LONG).show();
            callback.onRefreshDataFailed(ModelCallback.NOT_EN_FUNC, new String[]{BluetoothAdapter.ACTION_REQUEST_ENABLE});
            return;
        }

        if(btAdapter == null) btAdapter = BluetoothAdapter.getDefaultAdapter();
        if(btLeScanner == null) btLeScanner = btAdapter.getBluetoothLeScanner();

        if (!activeScanning)
        {
            // Stops scanning after a predefined scan period.
            btScanHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(activeScanning==true) {
                        activeScanning = false;
                        btLeScanner.stopScan(bleScanCallback);
                        callback.onRefreshDataEnd(ModelCallback.NO_FIND_PACKETS);
                    }
                }
            }, SCAN_MAX_PERIOD);

            activeScanning = true;
            btLeScanner.startScan(bleScanCallback);
        }
        else
        {
            activeScanning = false;
            btLeScanner.stopScan(bleScanCallback);
        }
    }

    // Device scan callback.
    private ScanCallback bleScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    byte bytes[]=result.getScanRecord().getBytes();

                    if(masValues.setFromPacketBytes(bytes)){
                        recivedFlag=recivedFlag | 0x01;
                    }
                    else if(masValuesAvg.setFromPacketBytes(bytes))
                    {
                        recivedFlag=recivedFlag | 0x02;
                    }

                    if(recivedFlag==0x03)
                    {
                        btLeScanner.stopScan(bleScanCallback);
                        activeScanning=false;
                        recivedFlag=0;
                        callback.onRefreshDataEnd(ModelCallback.NO_ERROR);
                    }
                };

                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    super.onBatchScanResults(results);
                    //callback.onRefreshDataEnd(ModelCallback.NO_FIND_PACKETS);
                }

                @Override
                public void onScanFailed(int errorCode) {
                    super.onScanFailed(errorCode);
                    callback.onRefreshDataEnd(ModelCallback.ERROR);
                }
            };


    private boolean checkPermission(String[] permission) {
        for(String perm : permission)
            if (ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_DENIED)
            {
                return false;
            }
        return true;
    }






}
