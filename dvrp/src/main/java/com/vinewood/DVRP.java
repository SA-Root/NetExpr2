package com.vinewood;

/**
 * Launch Program
 *
 */
public class DVRP {
    public static void main(String[] args) {
        RouterInstance ri = new RouterInstance(args[0], Integer.parseInt(args[1]), args[2]);
        ri.Launch();
    }
}
