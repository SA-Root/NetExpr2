package com.vinewood;

import java.util.ArrayList;

/**
 * For updating routing table
 */
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
        // TODO:
        return null;
    }

    public static ArrayList<RouterExchange> Deserialize(String stream) {
        var ret = new ArrayList<RouterExchange>();
        // TODO:
        return ret;
    }
}
