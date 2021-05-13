package com.vinewood;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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

    private void UpdateRoutingTableThread() {
        while (!TUpdateRoutingTable.isInterrupted()) {
            while (!QueueExchangeReceived.isEmpty()) {

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
        File cur = new File(".");
        try {
            pwd = cur.getCanonicalPath();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Date now = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd hh-mm-ss a");
        File logPath = new File(pwd + "/logs");
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

    private void UDPListenerThread() {
        while (!TUDPListener.isInterrupted()) {

        }
    }

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
        // Scanner Input = new Scanner(System.in);
        while (true) {
            Byte command = ' ';
            try {
                command = (byte) System.in.read();
            } catch (Exception e) {
                e.printStackTrace();
            }
            // quit
            if (command == 'K' || command == 'k') {
                // Input.close();
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

    public void Pause() {

    }

    public void Resume() {

    }

    public void Terminate() {
        TUpdateRoutingTable.interrupt();
        TUDPListener.interrupt();
    }

    private void PrintRoutingInfo(Boolean isSent, List<RouterExchange> lre) {

    }

    private void LoadConfig(String path) {
        try {
            File cfg = new File(pwd + "/configs/syscfg.json");
            ObjectMapper mapper = new ObjectMapper();
            Config = mapper.readValue(cfg, SysCfg.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        File init = new File(pwd + "/" + path);
        try {
            BufferedReader br = new BufferedReader(new FileReader(init));
            while (true) {
                String line = br.readLine();
                if (line == null)
                    break;
                String[] res = line.split(" ");
                String neighbour = res[0];
                float dist = Float.parseFloat(res[1]);
                int port = Integer.parseInt(res[2]);
                RoutingTable.put(neighbour, new RoutingInfo(neighbour, dist, neighbour));
                NeighbourMap.put(neighbour, port);
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void InitializeNode() {

    }
}
