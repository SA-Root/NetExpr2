package com.vinewood;

import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RouterInstance {
    private HashMap<String, RouterInfo> RoutingTable;
    private Thread TUpdateRoutingTable;
    private ConcurrentLinkedQueue<RouterExchange> QueueExchangeReceived;
    private Thread TUDPListener;

    private void UpdateRoutingTableThread() {
        while (!Thread.currentThread().isInterrupted()) {
            while (!QueueExchangeReceived.isEmpty()) {

            }
            try {
                TUpdateRoutingTable.wait(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public RouterInstance(String id, int udpport, String ifpath) {
        RoutingTable = new HashMap<String, RouterInfo>();
        QueueExchangeReceived = new ConcurrentLinkedQueue<RouterExchange>();
        LoadConfig(ifpath);
    }

    private void UDPListenerThread() {
        while (!Thread.currentThread().isInterrupted()) {

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
        Scanner Input = new Scanner(System.in);
        while (true) {
            Byte command = Input.nextByte();
            // quit
            if (command == 'K' || command == 'k') {
                Input.close();
                Terminate();
                break;
            }
            // pause
            else if (command == 'P' || command == 'p') {

            }
            // resume
            else if (command == 'S' || command == 's') {

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

    private void PrintRoutingInfoSent() {

    }

    private void LoadConfig(String path) {

    }

    private void InitializeNode() {

    }
}
