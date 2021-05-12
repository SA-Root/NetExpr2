package com.vinewood;

import java.util.HashMap;

public class RouterInstance {
    private HashMap<String, RouterInfo> RoutingTable;
    private Thread TListener;

    private void UDPListenerThread() {

    }

    public RouterInstance(String id, int udpport, String ifpath) {
        RoutingTable = new HashMap<String, RouterInfo>();

    }

    public void Launch() {
        TListener = new Thread(new Runnable() {
            @Override
            public void run() {
                UDPListenerThread();
            }
        });
        TListener.start();
    }

    public void Pause() {

    }

    public void Resume() {

    }

    public void Terminate() {

    }

    private void PrintRoutingInfoSent() {

    }

    private void LoadConfig(String path) {

    }

    private void InitializeNode() {

    }
}
