package com.frenzi.wifip2p;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class SsdpDiscoveryTask implements Runnable {

    private static final String TAG = "SsdpDiscoveryTask";
    private static final int SSDP_PORT = 1900;
    private static final String SSDP_ADDRESS = "239.255.255.250";
    private static final int TIMEOUT_MS = 10000;

    private final SsdpDiscoveryCallback callback;

    public SsdpDiscoveryTask(SsdpDiscoveryCallback callback) {
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            // Create the SSDP discovery message
            String searchTarget = "urn:dial-multiscreen-org:service:dial:1";
//            String searchTarget = "ssdp:all";
            String ssdpDiscoveryMessage = "M-SEARCH * HTTP/1.1\r\n" +
                    "HOST: " + SSDP_ADDRESS + ":" + SSDP_PORT + "\r\n" +
                    "MAN: \"ssdp:discover\"\r\n" +
                    "MX: 1\r\n" +
                    "ST: " + searchTarget + "\r\n" +
                    "\r\n";

            // Send the discovery message
            DatagramSocket socket = new DatagramSocket();
            InetAddress address = InetAddress.getByName(SSDP_ADDRESS);
            DatagramPacket discoveryPacket = new DatagramPacket(
                    ssdpDiscoveryMessage.getBytes(),
                    ssdpDiscoveryMessage.length(),
                    address,
                    SSDP_PORT
            );
            socket.send(discoveryPacket);
            // Listen for responses
            byte[] buffer = new byte[64000];
            // Loop to receive multiple responses
            while (true) {
                DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
                socket.setSoTimeout(TIMEOUT_MS);
                try {
                    socket.receive(responsePacket);
                    String responseMessage = new String(responsePacket.getData(), 0, responsePacket.getLength());
                    Log.d(TAG, "Received response: " + responseMessage);
                    DeviceModel deviceModel = extractDeviceNameFromSSDPResponse(responseMessage);
                    if (deviceModel != null) {
                        callback.onDeviceNameReceived(deviceModel);
                    }
                } catch (SocketTimeoutException e) {
                    // Timeout reached, exit the loop
                    break;
                }
            }

            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private DeviceModel extractDeviceNameFromSSDPResponse(String response) {
        String locationHeader = extractHeaderValue(response);
        if (locationHeader != null && !isWakeupFound(response)) {
            try {
                URL deviceDescriptionUrl = new URL(locationHeader);
                HttpURLConnection connection = (HttpURLConnection) deviceDescriptionUrl.openConnection();
                connection.setRequestMethod("GET");

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line;
                    StringBuilder xmlResponse = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        xmlResponse.append(line);
                    }
                    reader.close();

                    // Parse the XML response to extract the device name
                    String device_des = xmlResponse.toString();
                    String device_name = parseDeviceNameFromXML(device_des);
                    try {
                        return new DeviceModel(locationHeader, device_name);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                } else {
                    // Handle error (Failed to retrieve the device description)
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null; // Device name not found
    }

    /*device name from xml response*/
    private static String parseDeviceNameFromXML(String xmlResponse) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlResponse)));

            NodeList deviceNodes = doc.getElementsByTagName("device");
            if (deviceNodes.getLength() > 0) {
                Element deviceElement = (Element) deviceNodes.item(0);
                NodeList friendlyNameNodes = deviceElement.getElementsByTagName("friendlyName");
                if (friendlyNameNodes.getLength() > 0) {
                    Node friendlyNameNode = friendlyNameNodes.item(0);
                    return friendlyNameNode.getTextContent();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /*extract location value*/
    private static String extractHeaderValue(String response) {
        String[] lines = response.split("\r\n");
        for (String line : lines) {
            if (line.startsWith("LOCATION" + ":")) {
                return line.substring("LOCATION".length() + 1).trim();
            }
        }
        return null;
    }

    /*ignore the wakeup devices*/
    private static boolean isWakeupFound(String response) {
        String[] lines = response.split("\r\n");
        for (String line : lines) {
            if (line.startsWith("WAKEUP" + ":")) {
                return true;
            }
        }
        return false;
    }

    public interface SsdpDiscoveryCallback {
        void onDeviceNameReceived(DeviceModel deviceModel);
    }
}

