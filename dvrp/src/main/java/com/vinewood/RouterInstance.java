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
import java.util.concurrent.ConcurrentLinkedQueue;

import com.fasterxml.jackson.databind.ObjectMapper;

//import jdk.internal.joptsimple.util.InetAddressConverter;

public class RouterInstance {
    private HashMap<String, Integer> NeighbourMap;
    private HashMap<String, RoutingInfo> RoutingTable;

    private HashMap<String,Boolean> NeighbourAlive;

    ArrayList<String> checkNode;
    
    private Thread TUpdateRoutingTable;
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
        while (!TUpdateRoutingTable.isInterrupted()) {

            try {
                Thread.sleep(Config.Frequency);
                
            } catch (InterruptedException e) {
                TUpdateRoutingTable.interrupt();
            }
            int getSize=0;
            if(!QueueExchangeReceived.isEmpty()){
                getSize=QueueExchangeReceived.size();
            }

            while (getSize-->0) {
                if(!isRunning){
                    QueueExchangeReceived.poll();
                    continue;
                }
                RouterExchange[] exchangesNow=QueueExchangeReceived.poll();
                
                //HashMap<String,RoutingInfo> rtNow=new HashMap<String,RoutingInfo>(RoutingTable);
                for(RoutingInfo it:RoutingTable.values()){
                    if(!it.DestNode.equals(it.Neighbour))
                        it.Distance=Config.Unreachable;
                }
                

                for(RouterExchange exchangeNow:exchangesNow){
                    if(RoutingTable.get(exchangeNow.DestNode).Distance>RoutingTable.get(exchangeNow.SrcNode).Distance+exchangeNow.Distance){
                        RoutingTable.get(exchangeNow.DestNode).Neighbour=exchangeNow.SrcNode;
                        RoutingTable.get(exchangeNow.DestNode).Distance=exchangeNow.Distance;
                    }
                }

        }
        if(isRunning){
            var exchanges=new ArrayList<RouterExchange>();
                for(RoutingInfo it:RoutingTable.values()){
                    exchanges.add(new RouterExchange(LocalID, it.DestNode, it.Distance));
                }
                byte[] sendMsg=RouterExchange.Serialize(exchanges).getBytes();
                
                for(String itNow: NeighbourMap.keySet()){
                    Integer it=NeighbourMap.get(itNow);
                    if(!NeighbourAlive.get(itNow)){
                        continue;
                    }
                    DatagramPacket outPack=null;
                    try {
                        outPack= new DatagramPacket(sendMsg, sendMsg.length,InetAddress.getLocalHost(),it);
                    } catch (Exception e) {
                         
                    }
                    
                    try {

                        UDPSocket.send(outPack);
                        SentSeqNumber++;
                    } catch (Exception e) {
                        System.out.println("[ERROR]Could not send RoutingTable.");
                    }
                    
                }
                PrintRoutingInfo();
        }
        }
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
        configPath=ifpath;
        checkNode=new ArrayList<String>();
        NeighbourAlive=new HashMap<String,Boolean>();
        isRunning = true; 
        startTime=0;
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
        InitializeNode();
        
    }

    /**
     * Listen to UDP port and push to queue
     */
    private void UDPListenerThread() {
       startTime=System.currentTimeMillis();
        while (!TUDPListener.isInterrupted()) {
            synchronized (SyncIsRunning) {
                // receive and discard
                if (!isRunning) {
                    var buffer = new byte[4096];
                    var ReceivedPacket = new DatagramPacket(buffer, buffer.length);
                    try {
                        UDPSocket.receive(ReceivedPacket);
                    } catch (Exception e) {

                    }
                } else {
                   
                    var buffer = new byte[4096];
                    var ReceivedPacket = new DatagramPacket(buffer, buffer.length);
                    try {
                        UDPSocket.receive(ReceivedPacket);
                        
                    } catch (Exception e) {

                    }
                    RouterExchange[] exchanges=RouterExchange.Deserialize(new String(buffer));
                    QueueExchangeReceived.add(exchanges);
                    ReceivedSeqNumber++;
                    checkNode.add(exchanges[0].SrcNode);
                    NeighbourAlive.put(exchanges[0].SrcNode,true);
                }
                if(System.currentTimeMillis()-startTime>Config.MaxValidTime){
                    for(String it:RoutingTable.keySet()){
                        int flag=0;
                        for(String itn:checkNode){
                            if(itn.equals(it)){
                                flag=1;
                                break;
                            }
                        }
                        if(flag==0){
                            NeighbourAlive.put(it,false);
                        }
                    }
                    startTime=System.currentTimeMillis();
                    checkNode.clear();
                }
            }
            // try {
            //     Thread.sleep(250);
            // } catch (InterruptedException e) {
            //     TUDPListener.interrupt();
            // }
        }
    }

    /**
     * Activate the router and response to keyboard input
     */
    public void Launch() {
        TUpdateRoutingTable = new Thread(new Runnable() {
            @Override
            public void run() {
                sendMsgThread();
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
        
    }

    /**
     * Resume the router
     */
    public void Resume() {
        synchronized (SyncIsRunning) {
            isRunning = true;
        }
        InitializeNode();
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
    private void PrintRoutingInfo() {
        
        System.out.println("##Sent. Source Node = "+LocalID+"Sequence Number = "+SentSeqNumber);
        for(RoutingInfo it:RoutingTable.values()){
            var printMsg=new String();
            printMsg+="DestNode = ";
            printMsg+=it.DestNode;
            printMsg+="; Distance = ";
            printMsg+=it.Distance;  
            printMsg+="; Neighbor = ";
            printMsg+=it.Neighbour;
            System.out.println(printMsg);
        }             
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
        LoadConfig(configPath);

    }
}
