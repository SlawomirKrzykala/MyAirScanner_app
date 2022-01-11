package com.lista.myairscan;

import androidx.annotation.Nullable;

public abstract class ModelCallback {
    public static final int NOT_HAVE_PERM = -13;
    public static final int NOT_EN_FUNC = -12;
    public static final int ERROR = -10;
    public static final int NO_FIND_PACKETS = -1;
    public static final int NO_ERROR = 0;

    public void onRefreshDataEnd(int endCode){
    }

    public void onRefreshDataFailed(int errorCode, @Nullable String[] val ){
    }

}
