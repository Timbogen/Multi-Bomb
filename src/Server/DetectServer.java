package Server;

import Server.Messages.REST.ServerInfo;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.HashMap;

public class DetectServer {

    /**
     * Scan local network for running servers
     */
    public void search(HashMap<String, ServerInfo> serverList) {
        try {
            // Open socket
            DatagramSocket c = new DatagramSocket();
            c.setBroadcast(true);
            c.setSoTimeout(1000);

            byte[] request = DiscoveryThread.UDP_REQUEST.getBytes();

            // Try the 255.255.255.255 first
            try {
                DatagramPacket sendPacket = new DatagramPacket(
                        request,
                        request.length,
                        InetAddress.getByName("255.255.255.255"),
                        DiscoveryThread.PORT
                );
                c.send(sendPacket);
            } catch (Exception e) {
                System.out.println("255.255.255.255 did not work!");
            }

            // Broadcast message over all network interfaces
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                // Check if it is loopback interface
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                // Iterate through the addresses
                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress broadcast = interfaceAddress.getBroadcast();
                    if (broadcast == null) {
                        continue;
                    }

                    // Send the package
                    try {
                        DatagramPacket sendPacket = new DatagramPacket(request, request.length, broadcast, DiscoveryThread.PORT);
                        c.send(sendPacket);
                    } catch (Exception e) {
                        System.out.println("Did not send broadcast package!");
                    }
                }
            }


            while (true) {
                // Wait for a response
                byte[] responseBuffer = new byte[15000];
                DatagramPacket receivePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
                try {
                    c.receive(receivePacket);
                } catch (SocketTimeoutException e) {
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String serverAddress = receivePacket.getAddress().getHostAddress();

                // Start thread to get server info
                new Thread(new ScanServerThread(serverAddress, serverList, "Local")).start();

            }

            // Close the port
            c.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

