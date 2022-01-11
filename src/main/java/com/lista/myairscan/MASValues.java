package com.lista.myairscan;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MASValues {
    private class PMSValuesPM {
        public  int pm10=0, pm25=0, pm100=0;
        public String[] getStringArr(){
            return new String[]{Integer.toString(pm10), Integer.toString(pm25), Integer.toString(pm100)};
        }
    }
    private class PMSValuesNum {
        public int um3 = 0, um5 = 0, um10 = 0, um25 = 0, um50 = 0, um100 = 0;
        public String[] getStringArr() {
            return new String[]{Integer.toString(um3), Integer.toString(um5), Integer.toString(um10), Integer.toString(um25), Integer.toString(um50), Integer.toString(um100)};
        }
    }
    private class DHTValues {
        public float temp=0, hum=0;
        public String[] getStringArr() {
            return new String[]{Float.toString(temp), Float.toString(hum)};
        }
    }


    public PMSValuesPM sm, ae;
    public PMSValuesNum num;
    public DHTValues general;
    private byte[] bytes;
    private float esp_temp=0;
    private int type=-1;
    public static final int PM_MODE_SM=1;
    public static final int PM_MODE_AE=2;
    private int modePM=PM_MODE_SM;
    private final String[]nameAirQualityIndex={"Znakomita", "Dobra", "Umiarkowana", "Dostateczna", "Zła", "Bardzo zła"};



    public MASValues(int type){
        this.type=type;
        sm=new PMSValuesPM();
        ae=new PMSValuesPM();
        num=new PMSValuesNum();
        general=new DHTValues();
        esp_temp=0;
    }
    
    private int decodeFirst12bitsPM(byte highByte, byte midByte){
        return ((((int)highByte)<<4) &0xFF0) | (((int)midByte>>4) &0x00F);
    }
    
    private int decodeLast12bitsPM(byte midByte, byte lowByte){
        return (((int)(midByte&0x0F))<<8) | ((int)(lowByte&0xFF));
    }
    
    private int decode2uint8ToInt(byte highByte, byte lowByte){
        return ((((int)highByte)<<8) &0xFF00) | (((int)lowByte) &0x00FF);
    }

    public boolean setFromPacketBytes(byte[] bytes){
        if(bytes==null ||
                (bytes.length<31) ||
                (bytes[0]!=0x02) ||
                (bytes[1]!=0x01) ||
                (bytes[2]!=0x06) ||
                (bytes[3]!=0x01B) ||
                (bytes[4]!=0x06) ||
                (bytes[5])!=0x06){
           return false;
        }

        if(type!=(bytes[6]&0x03))
            return false;

        this.bytes=bytes;
        int offset=9;
        sm.pm10= decodeFirst12bitsPM(bytes[offset+0], bytes[offset+1]);
        sm.pm25= decodeLast12bitsPM(bytes[offset+1], bytes[offset+2]);

        sm.pm100= decodeFirst12bitsPM(bytes[offset+3], bytes[offset+4]);
        ae.pm10= decodeLast12bitsPM(bytes[offset+4], bytes[offset+5]);

        ae.pm25= decodeFirst12bitsPM(bytes[offset+6], bytes[offset+7]);
        ae.pm100= decodeLast12bitsPM(bytes[offset+7], bytes[offset+8]);


        num.um3=decode2uint8ToInt(bytes[offset+10], bytes[offset+9]);
        num.um5=decode2uint8ToInt(bytes[offset+12], bytes[offset+11]);
        num.um10=decode2uint8ToInt(bytes[offset+14], bytes[offset+13]);
        num.um25=decode2uint8ToInt(bytes[offset+16], bytes[offset+15]);
        num.um50=decode2uint8ToInt(bytes[offset+18], bytes[offset+17]);
        num.um100=decode2uint8ToInt(bytes[offset+20], bytes[offset+19]);

        offset=6;
        general.temp = ((float)(((((int)bytes[offset+0]&0x00FC)>>2)) | (((int)(bytes[offset+1]) &0x001F)<<6)))/10.0f;
        if((bytes[offset+1]&0x20) == 0x20) general.temp*=-1.0;

        general.hum = ((float)(((((int)(bytes[offset+1]&0x00C0))>>6)) | (((int)(bytes[offset+2] &0x00FF)<<2))))/10.0f;

       //setFromBytes(bytes, 6);
        //pms.setFromBytes(bytes, 9);

        esp_temp= (((float)(bytes[30]&0xFF))-32.0f)/1.8f;

        return true;
    }

    public List<String> getListString(){
        List<String> val =  new ArrayList<>();
        val.addAll(Arrays.asList(general.getStringArr()));
        val.add(getAirQualityIndexName());
        val.addAll(Arrays.asList(ae.getStringArr()));
        val.addAll(Arrays.asList(sm.getStringArr()));
        val.addAll(Arrays.asList(num.getStringArr()));

        return val;
    }

    public String getAirQualityIndexName(){
        if(ae.pm25<=13 && ae.pm100<=20)
            return nameAirQualityIndex[0];
        else if(ae.pm25<=35 && ae.pm100<=50)
            return nameAirQualityIndex[1];
        else if(ae.pm25<=55 && ae.pm100<=80)
            return nameAirQualityIndex[2];
        else if(ae.pm25<=75 && ae.pm100<=110)
            return nameAirQualityIndex[3];
        else if(ae.pm25<=110 && ae.pm100<=150)
            return nameAirQualityIndex[4];
        else
            return nameAirQualityIndex[5];
    }

    public String getBytesString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ");
        int i=0, n=-1;
        for (byte b : bytes) {
            sb.append(String.format("0x%02X ", b));
            if(++i%4==0){
                sb.append("\n"+n+": ");
                n+=4;
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public void setModePM(int mode){
        modePM=mode;
    }
}
