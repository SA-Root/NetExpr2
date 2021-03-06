package com.vinewood;

import java.util.ArrayList;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * For updating routing table
 */
public class RouterExchange {
    public String SrcNode;
    public String DestNode;
    public float Distance;

    public RouterExchange()
    {

    }
    public RouterExchange(String src, String dest, float dist) {
        SrcNode = src;
        DestNode = dest;
        Distance = dist;
    }

    public static String Serialize(ArrayList<RouterExchange> re) {
        var OM = new ObjectMapper();
        String st;
        
        try {
            st = OM.writeValueAsString(re.toArray());
            return st;
        } catch (Exception e) {

        }
        return null;

    }

    public static RouterExchange[] Deserialize(String stream) {
        // var ret = new ArrayList<RouterExchange>();
        var OM = new ObjectMapper();
        try {
            RouterExchange[] re = OM.readValue(stream, RouterExchange[].class);
            //System.out.println(re.length);
            //System.out.println(re[1].SrcNode);
            //System.out.println(re[1].Distance);
            //System.out.println(re[1].DestNode);
            return re;
        } catch (Exception e) {
            
            //System.out.println(e);
        }
        return null;
    }
}
