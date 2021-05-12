package com.vinewood;

/**
 * Launch Program
 *
 */
public class DVRP {
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("[ERROR]Invalid parameter count.");
        } else {
            int port = 0;
            try {
                port = Integer.parseInt(args[1]);
            } catch (Exception e) {
                System.out.println("[ERROR]Invalid UDPPort.");
                return;
            }
            if (port <= 0 || port >= 65535) {
                System.out.println("[ERROR]Invalid UDPPort.");
            } else {
                RouterInstance ri = new RouterInstance(args[0], port, args[2]);
                ri.Launch();
            }
        }
    }
}
