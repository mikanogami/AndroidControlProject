package com.example.androidcontrol.utils;

public abstract class MyConstants {
    public static final String AUTH_TOKEN = "WAK4k5SthTsWrAxp49U4yfybjpjZ7XRu";
    public static final String FOL_CLIENT_KEY = "ad_fol";
    public static final String EXP_CLIENT_KEY = "ad_exp";
    public static final String PEER_CONNECTED = "@@@";
    public static final String PEER_DISCONNECTED = "ßßß";
    public static final String PEER_UNAVAILABLE = "***Peer unavailable";
    //public static final String SERVER_URL = "ws://10.0.0.77:8001/";
    public static final String SERVER_URL = "wss://mirthus.herokuapp.com";

    // Video track properties
    public static final String VIDEO_TRACK_ID = "ARDAMSv0";

    // These are set using WindowManager from MainActivity
    public static int VIDEO_PIXELS_WIDTH;
    public static int VIDEO_PIXELS_HEIGHT;
    public static int DISPLAY_SURFACE_WIDTH;
    public static int DISPLAY_SURFACE_HEIGHT;
    public static final int FPS = 30;

    public static final String NOTIF_CHANNEL_ID = "ForegroundServiceChannel";
    public static final String M_PROJ_INTENT = "mediaProjectionIntent";
    public static final float FLOAT_NULL = -1;
    public static final String DATA_CHANNEL_NAME = "phoneControl";

}