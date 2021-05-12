package com.vinewood;

public class RouterInfo {
    public String DestNode;
    public float Distance;
    public String Neighbour;

    public RouterInfo(String dest, float dist, String neighbour) {
        DestNode = dest;
        Distance = dist;
        Neighbour = neighbour;
    }
}
