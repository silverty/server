package io.moquette.persistence;

import io.moquette.spi.impl.security.AES;
import java.io.*;
import java.util.Base64;

public class License {
    private String ip;
    private String port;
    private String mac;
    private int userCount;
    private long expiredTime;

    public static License decodeLicense(File licFile) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(licFile)));
        String sign = reader.readLine();
        byte[] bytes = Base64.getDecoder().decode(sign);

        bytes = AES.AESDecrypt(bytes, DatabaseStore.getData(), false);
        String[] str = new String(bytes).split(new StringBuilder().append((char)9).toString());
        License lic = new License();
        lic.mac = str[0];
        lic.ip = str[1];
        lic.port = str[2];
        lic.expiredTime = Long.parseLong(str[3]);
        lic.userCount = Integer.parseInt(str[4]);

        if (!lic.mac.equals(MemoryMessagesStore.getMacAddress())) {
            return null;
        }
        return lic;
    }

    public String getIp() {
        return ip;
    }

    public String getPort() {
        return port;
    }

    public long getExpiredTime() {
        return expiredTime;
    }

    public int getUserCount() {
        return userCount;
    }
}
