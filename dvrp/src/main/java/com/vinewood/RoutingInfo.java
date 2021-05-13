package com.vinewood;

public class RoutingInfo {
    public String DestNode;
    public float Distance;
    public String Neighbour;

    public RoutingInfo(String dest, float dist, String neighbour) {
        DestNode = dest;
        Distance = dist;
        Neighbour = neighbour;
    }
}
