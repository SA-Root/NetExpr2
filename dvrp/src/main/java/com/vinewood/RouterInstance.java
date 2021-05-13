package com.vinewood;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.fasterxml.jackson.databind.ObjectMapper;

public class RouterInstance {
    private HashMap<String, Integer> NeighbourMap;
    private HashMap<String, RoutingInfo> RoutingTable;
    private Thread TUpdateRoutingTable;
    private ConcurrentLinkedQueue<RouterExchange> QueueExchangeReceived;
    private Thread TUDPListener;
    private int LocalPort;
    private String LocalID;
    private SysCfg Config;
    private File LogFile;
    private String pwd;
    private int SentSeqNumber;
    private int ReceivedSeqNumber;
    private DatagramSocket UDPSocket;
    private boolean isRunning;
    private Object SyncIsRunning;

    /**
     * Using DV algo to update routing table
     */
    private void UpdateRoutingTableThread() {
        while (!TUpdateRoutingTable.isInterrupted()) {
            while (!QueueExchangeReceived.isEmpty()) {
                // TODO: impl
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                TUpdateRoutingTable.interrupt();
            }
        }
    }

    public RouterInstance(String id, int udpport, String ifpath) {
        RoutingTable = new HashMap<String, RoutingInfo>();
        NeighbourMap = new HashMap<String, Integer>();
        QueueExchangeReceived = new ConcurrentLinkedQueue<RouterExchange>();
        LocalID = id;
        LocalPort = udpport;
        SentSeqNumber = 1;
        ReceivedSeqNumber = 1;
        SyncIsRunning = new Object();
        isRunning = true;
        try {
            UDPSocket = new DatagramSocket(LocalPort);
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }
        var cur = new File(".");
        try {
            pwd = cur.getCanonicalPath();
        } catch (Exception e) {
            e.printStackTrace();
        }
        var now = new Date();
        var ft = new SimpleDateFormat("yyyy-MM-dd hh-mm-ss a");
        var logPath = new File(pwd + "/logs");
        if (!logPath.exists()) {
            logPath.mkdir();
        }
        LogFile = new File(pwd + "/logs/" + ft.format(now) + ".log");
        try {
            LogFile.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
        LoadConfig(ifpath);
    }

    /**
     * Listen to UDP port and push to queue
     */
    private void UDPListenerThread() {
        try {
            UDPSocket.setSoTimeout(1000);
        } catch (SocketException e) {
            System.out.println("[ERROR]Could not set socket timeout.");
            return;
        }
        while (!TUDPListener.isInterrupted()) {
            synchronized (SyncIsRunning) {
                // receive and discard
                if (!isRunning) {
                    var buffer = new byte[100];
                    var ReceivedPacket = new DatagramPacket(buffer, buffer.length);
                    try {
                        UDPSocket.receive(ReceivedPacket);
                    } catch (Exception e) {

                    }
                } else {
                    // TODO: impl
                }
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                TUDPListener.interrupt();
            }
        }
    }

    /**
     * Activate the router and response to keyboard input
     */
    public void Launch() {
        TUpdateRoutingTable = new Thread(new Runnable() {
            @Override
            public void run() {
                UpdateRoutingTableThread();
            }
        });
        TUpdateRoutingTable.start();
        TUDPListener = new Thread(new Runnable() {
            @Override
            public void run() {
                UDPListenerThread();
            }
        });
        TUDPListener.start();
        InitializeNode();
        while (true) {
            var command = ' ';
            try {
                command = (char) System.in.read();
            } catch (Exception e) {
                e.printStackTrace();
            }
            // quit
            if (command == 'K' || command == 'k') {
                Terminate();
                break;
            }
            // pause
            else if (command == 'P' || command == 'p') {
                Pause();
            }
            // resume
            else if (command == 'S' || command == 's') {
                Resume();
            }
        }
    }

    /**
     * Pause the router
     */
    public void Pause() {
        synchronized (SyncIsRunning) {
            isRunning = false;
        }
        // TODO: impl
    }

    /**
     * Resume the router
     */
    public void Resume() {
        synchronized (SyncIsRunning) {
            isRunning = true;
        }
        // TODO: impl
    }

    /**
     * Shutdown the router
     */
    public void Terminate() {
        UDPSocket.close();
        TUpdateRoutingTable.interrupt();
        TUDPListener.interrupt();
    }

    /**
     * Print after sending exchg info
     * 
     * @param isSent
     * @param lre
     */
    private void PrintRoutingInfo(Boolean isSent, List<RouterExchange> lre) {
        // TODO: impl
    }

    /**
     * Load syscfg and init cfg
     * 
     * @param path
     */
    private void LoadConfig(String path) {
        try {
            var cfg = new File(pwd + "/configs/syscfg.json");
            var mapper = new ObjectMapper();
            Config = mapper.readValue(cfg, SysCfg.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        var init = new File(pwd + "/" + path);
        try {
            var br = new BufferedReader(new FileReader(init));
            while (true) {
                var line = br.readLine();
                if (line == null)
                    break;
                var res = line.split(" ");
                var neighbour = res[0];
                var dist = Float.parseFloat(res[1]);
                var port = Integer.parseInt(res[2]);
                RoutingTable.put(neighbour, new RoutingInfo(neighbour, dist, neighbour));
                NeighbourMap.put(neighbour, port);
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * First flooding
     */
    private void InitializeNode() {
        // TODO: impl
    }
}
