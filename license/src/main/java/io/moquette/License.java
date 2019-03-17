package io.moquette;

import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Base64;
import java.util.Calendar;

public class License {
    public static String getMacAddress() throws UnknownHostException,
        SocketException {
        InetAddress ipAddress = InetAddress.getLocalHost();
        NetworkInterface networkInterface = NetworkInterface
            .getByInetAddress(ipAddress);
        byte[] macAddressBytes = networkInterface.getHardwareAddress();
        StringBuilder macAddressBuilder = new StringBuilder();

        for (int macAddressByteIndex = 0; macAddressByteIndex < macAddressBytes.length; macAddressByteIndex++) {
            String macAddressHexByte = String.format("%02X",
                macAddressBytes[macAddressByteIndex]);
            macAddressBuilder.append(macAddressHexByte);

            if (macAddressByteIndex != macAddressBytes.length - 1)
            {
                macAddressBuilder.append(":");
            }
        }

        return macAddressBuilder.toString();
    }


    public static void generateLicense(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.println("Usage: machinecode years usercount");
            System.exit(-1);
        }


        String machineCode = args[0];

        machineCode = new String(Base64.getDecoder().decode(machineCode));

        String[] array = machineCode.split("\\|");
        if (array.length != 3) {
            System.out.println("无效的机器码");
            System.exit(-1);
        }

        String mac = array[0];
        String ip = array[1];
        String port = array[2];

        int year = Integer.parseInt(args[1]);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, year);

        StringBuilder sb1 = new StringBuilder();
        char ch = 9;
        sb1.append(mac);
        sb1.append(ch);
        sb1.append(ip).append(ch).append(port).append(ch).append(calendar.getTimeInMillis()).append(ch).append(args[2]);

        byte[] bytes = sb1.toString().getBytes();
        StringBuilder sb = new StringBuilder();
        sb.append((char)5).
            append((char)9).
            append((char)14).
            append((char)29).
            append((char)88).
            append((char)106).
            append((char)99).
            append((char)253).
            append((char)231).
            append((char)15).
            append((char)77).
            append((char)106).
            append((char)99).
            append((char)253).
            append((char)231).
            append((char)15);
        bytes = DES.AESEncrypt(bytes, sb.toString());
        String sign = new String(Base64.getEncoder().encode(bytes));

        Writer writer = new OutputStreamWriter(new FileOutputStream("wildfirechat.license"));
        writer.write(sign);
        writer.flush();
        writer.close();
    }
}
