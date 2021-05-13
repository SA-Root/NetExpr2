package com.vinewood;

import java.util.ArrayList;

public class RouterExchange {
    public String SrcNode;
    public String DestNode;
    public float Distance;

    public RouterExchange(String src, String dest, float dist) {
        SrcNode = src;
        DestNode = dest;
        Distance = dist;
    }

    public static String Serialize(ArrayList<RouterExchange> re) {
        return null;
    }

    public static ArrayList<RouterExchange> Deserialize(String stream) {
        ArrayList<RouterExchange> ret = new ArrayList<RouterExchange>();
        return ret;
    }
}
