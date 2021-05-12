package com.vinewood;

public class RouterExchange {
    public String SrcNode;
    public String DestNode;
    public float Distance;

    public RouterExchange(String src, String dest, float dist) {
        SrcNode = src;
        DestNode = dest;
        Distance = dist;
    }

    public static String Serialize(RouterExchange re) {
        return null;
    }

    public static RouterExchange Deserialize(String stream) {
        return null;
    }
}
