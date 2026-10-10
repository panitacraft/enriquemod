package com.panita.enriquecraft.auth.service;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * Turns the address of a connection into the text the auth module compares: the host only, never the port,
 * which changes with every connection.
 */
public final class Addresses {

    private Addresses() {
    }

    public static String of(SocketAddress address) {
        if (address instanceof InetSocketAddress inet && inet.getAddress() != null) {
            return inet.getAddress().getHostAddress();
        }
        return String.valueOf(address);
    }
}
