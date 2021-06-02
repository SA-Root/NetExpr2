package com.vinewood;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.fasterxml.jackson.databind.ObjectMapper;


//import jdk.internal.joptsimple.util.InetAddressConverter;

public class RouterInstance {
    private Runnable SendMsgRunnable;
    private HashMap<String, Integer> NeighbourMap;
    private HashMap<String, RoutingInfo> RoutingTable;

    private HashMap<String, Boolean> NeighbourAlive;

    private HashMap<String,RoutingInfo> RoutingNeighbor;
    private HashSet<String> checkNode;

    private Thread TSendMsg;
    private ConcurrentLinkedQueue<RouterExchange[]> QueueExchangeReceived;
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
    private long startTime;
    private String configPath;

    /**
     * Using DV algo to update routing table
     */

    private void sendMsgThread() {
        while (!TSendMsg.isInterrupted()) {
            try {
                Thread.sleep(Config.Frequency);

            } catch (InterruptedException e) {
                TSendMsg.interrupt();
                continue;
            }
            if (!QueueExchangeReceived.isEmpty()) {
                var getSize = QueueExchangeReceived.size();
                for (RoutingInfo it : RoutingTable.values()) {
                    if (!it.DestNode.equals(it.Neighbour))
                        it.Distance = Config.Unreachable;
                }
                while (getSize-- > 0) {
                    RouterExchange[] exchangesNow = QueueExchangeReceived.poll();

                    // HashMap<String,RoutingInfo> rtNow=new
                    // HashMap<String,RoutingInfo>(RoutingTable);
                        for (RouterExchange exchangeNow : exchangesNow) {
                            var destInfo = RoutingTable.get(exchangeNow.DestNode);
                            if(destInfo==null){
                                RoutingTable.put(exchangeNow.DestNode, new RoutingInfo(exchangeNow.DestNode, exchangeNow.Distance, exchangeNow.SrcNode));
                                continue;
                            }
                            
                            if (destInfo.Distance > RoutingTable.get(exchangeNow.SrcNode).Distance
                                    + exchangeNow.Distance) {
                                destInfo.Neighbour = exchangeNow.SrcNode;
                                destInfo.Distance = exchangeNow.Distance+ RoutingTable.get(exchangeNow.SrcNode).Distance;
                            }
                            if(RoutingNeighbor.get(exchangeNow.DestNode)!=null){
                                if(destInfo.Distance>RoutingNeighbor.get(exchangeNow.DestNode).Distance){
                                    destInfo.Distance=RoutingNeighbor.get(exchangeNow.DestNode).Distance;
                                    destInfo.Neighbour=exchangeNow.DestNode;
                                }
                            }
                            if(NeighbourAlive.get(exchangeNow.DestNode)==null||!NeighbourAlive.get(exchangeNow.DestNode)){
                                destInfo.Distance=Config.Unreachable;
                                destInfo.Neighbour=exchangeNow.DestNode;
                            }
                        }
                        RoutingTable.put(LocalID, new RoutingInfo(LocalID, 0, LocalID));
                        
                    }
                   sendMsgOnce();
                
            }

            else{
                sendMsgOnce();
            }
        }
        QueueExchangeReceived.clear();
    }

    public void sendMsgOnce() {
        var exchanges = new ArrayList<RouterExchange>();
        for (RoutingInfo it : RoutingTable.values()) {
            exchanges.add(new RouterExchange(LocalID, it.DestNode, it.Distance));
        }
        byte[] sendMsg = RouterExchange.Serialize(exchanges).getBytes();

        for (String itNow : NeighbourMap.keySet()) {
            Integer it = NeighbourMap.get(itNow);
            if (!NeighbourAlive.get(itNow)) {
                continue;
            }
            DatagramPacket outPack = null;
            try {
                outPack = new DatagramPacket(sendMsg, sendMsg.length, InetAddress.getLocalHost(), it);
            } catch (Exception e) {

            }

            try {

                UDPSocket.send(outPack);
                
            } catch (Exception e) {
                System.out.println("[ERROR]Could not send RoutingTable.");
            }

        }
        PrintRoutingInfo();
        SentSeqNumber++;
    }

    public RouterInstance(String id, int udpport, String ifpath) {
        RoutingTable = new HashMap<String, RoutingInfo>();
        NeighbourMap = new HashMap<String, Integer>();
        QueueExchangeReceived = new ConcurrentLinkedQueue<RouterExchange[]>();
        LocalID = id;
        LocalPort = udpport;
        SentSeqNumber = 1;
        ReceivedSeqNumber = 1;
        SyncIsRunning = new Object();
        configPath = ifpath;
        checkNode = new HashSet<String>();
        NeighbourAlive = new HashMap<String, Boolean>();
        RoutingNeighbor=new HashMap<String,RoutingInfo>();
        SendMsgRunnable = new Runnable() {
            @Override
            public void run() {
                sendMsgThread();
            }
        };
        isRunning = true;
        startTime = 0;
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


    }

    /**
     * Listen to UDP port and push to queue
     */
    private void UDPListenerThread() {
        startTime = System.currentTimeMillis();
        while (!TUDPListener.isInterrupted()) {
            var buffer = new byte[4096];
            var ReceivedPacket = new DatagramPacket(buffer, buffer.length);
            try {
                UDPSocket.receive(ReceivedPacket);
            } catch (Exception e) {
                continue;
            }
            if (isRunning) {
               // System.out.println(new String(buffer));
                for(int it=0;it<buffer.length;it++){
                    if(buffer[it]==']'){
                        buffer[it+1]=0;
                        break;
                    }
                }
                RouterExchange[] exchanges = RouterExchange.Deserialize(new String(buffer));
                QueueExchangeReceived.add(exchanges);
                ReceivedSeqNumber++;

                checkNode.add(exchanges[0].SrcNode);
                NeighbourAlive.put(exchanges[0].SrcNode, true);
                if (System.currentTimeMillis() - startTime > Config.MaxValidTime) {
                    for (String it : RoutingTable.keySet()) {
                        int flag = 0;
                        for (String itn : checkNode) {
                            if (itn.equals(it)) {
                                flag = 1;
                                break;
                            }
                        }
                        if (flag == 0) {
                            NeighbourAlive.put(it, false);
                        }
                    }
                    startTime = System.currentTimeMillis();
                    checkNode.clear();
                }
            }
        }
    }

    /**
     * Activate the router and response to keyboard input
     */
    public void Launch() {
        System.out.println("[INFO]Router is running.");
        InitializeNode();
        TUDPListener = new Thread(new Runnable() {
            @Override
            public void run() {
                UDPListenerThread();
            }
        });
        TUDPListener.start();

        
        TSendMsg = new Thread(SendMsgRunnable);
        TSendMsg.start();
        
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
        TSendMsg.interrupt();
        System.out.println("[INFO]Router is paused.");
    }

    /**
     * Resume the router
     */
    public void Resume() {
        synchronized (SyncIsRunning) {
            isRunning = true;
        }
        InitializeNode();
        TSendMsg = new Thread(SendMsgRunnable);
        TSendMsg.start();
        System.out.println("[INFO]Router has resumed.");
    }

    /**
     * Shutdown the router
     */
    public void Terminate() {
        UDPSocket.close();
        TSendMsg.interrupt();
        TUDPListener.interrupt();
        System.out.println("[INFO]Router is terminated.");
    }

    /**
     * Print after sending exchg info
     * 
     * @param isSent
     * @param lre
     */
    private void PrintRoutingInfo() {

        System.out.println("##Sent. Source Node = " + LocalID + " Sequence Number = " + SentSeqNumber);
        for (RoutingInfo it : RoutingTable.values()) {
            var printMsg = new String();
            printMsg += "DestNode = ";
            printMsg += it.DestNode;
            printMsg += "; Distance = ";
            printMsg += it.Distance;
            printMsg += "; Neighbor = ";
            printMsg += it.Neighbour;
            System.out.println(printMsg);
        }
        System.out.println("");
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
                NeighbourAlive.put(neighbour, true);
                RoutingNeighbor.put(neighbour,new RoutingInfo(neighbour, dist, neighbour));
            }
            RoutingTable.put(LocalID, new RoutingInfo(LocalID, 0, LocalID));
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * First flooding
     */
    private void InitializeNode() {
        RoutingTable.clear();
        NeighbourAlive.clear();
        NeighbourMap.clear();
        LoadConfig(configPath);
        sendMsgOnce();
    }
}
