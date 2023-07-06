package com.example.androidcontrol.utils;

public abstract class MyConstants {
    public static final String AUTH_TOKEN = "WAK4k5SthTsWrAxp49U4yfybjpjZ7XRu";
    public static final String FOL_CLIENT_KEY = "ad_fol";
    public static final String EXP_CLIENT_KEY = "ad_exp";
    public static final String PEER_CONNECTED = "@@@";
    public static final String PEER_DISCONNECTED = "###";
    public static final String PEER_UNAVAILABLE = "***Peer unavailable";
    //public static final String SERVER_URL = "ws://10.0.0.77:8001/";         // Rogers-5G-FWA
    //public static final String SERVER_URL = "ws://128.189.204.182:8001/";   // ubcsecure
    public static final String SERVER_URL = "wss://mirthus.herokuapp.com";

    // Video track properties
    public static final String VIDEO_TRACK_ID = "ARDAMSv0";

    // These are set using WindowManager from MainActivity
    public static int APP_SCREEN_PIXELS_HEIGHT;
    public static int APP_SCREEN_PIXELS_WIDTH;
    public static int FULL_SCREEN_PIXELS_HEIGHT;
    public static int PROJECTED_PIXELS_WIDTH;
    public static int PROJECTED_PIXELS_HEIGHT;
    public static int BUBBLE_ICON_RADIUS;
    public static int TRASH_ICON_SIDE_LEN;
    public static int DISPLAY_SURFACE_WIDTH;
    public static int DISPLAY_SURFACE_HEIGHT;
    public static final int FPS = 15;

    public static final String NOTIF_CHANNEL_ID = "ForegroundServiceChannel";
    public static final String M_PROJ_INTENT = "mediaProjectionIntent";
    public static final String DC_CONTROL_LABEL = "phoneControl";
    public static final String DC_ORIENTATION_LABEL = "phoneOrientation";

}
