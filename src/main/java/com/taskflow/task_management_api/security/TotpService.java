package com.taskflow.task_management_api.security;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;

/** Codes à usage unique (TOTP, RFC 6238) compatibles Google Authenticator, Authy, Microsoft Authenticator… */
@Service
public class TotpService {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final long STEP_SECONDS = 30;
    private final SecureRandom random = new SecureRandom();

    public String newSecret() {
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        return base32Encode(bytes);
    }

    public String otpauthUri(String issuer, String account, String secret) {
        return "otpauth://totp/" + enc(issuer) + ":" + enc(account)
                + "?secret=" + secret + "&issuer=" + enc(issuer)
                + "&algorithm=SHA1&digits=6&period=30";
    }

    /** Accepte le code courant et ceux des 30 s voisines (décalage d'horloge). */
    public boolean verify(String secret, String code) {
        if (secret == null || code == null || !code.matches("\\d{6}")) return false;
        long step = System.currentTimeMillis() / 1000 / STEP_SECONDS;
        boolean ok = false;
        for (long i = -1; i <= 1; i++) {
            ok |= MessageDigest.isEqual(code(secret, step + i).getBytes(StandardCharsets.UTF_8),
                    code.getBytes(StandardCharsets.UTF_8));
        }
        return ok;
    }

    public String code(String secret, long step) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(base32Decode(secret), "HmacSHA1"));
            byte[] h = mac.doFinal(ByteBuffer.allocate(8).putLong(step).array());
            int o = h[h.length - 1] & 0x0f;
            int bin = ((h[o] & 0x7f) << 24) | ((h[o + 1] & 0xff) << 16) | ((h[o + 2] & 0xff) << 8) | (h[o + 3] & 0xff);
            return String.format("%06d", bin % 1_000_000);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0, bits = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xff);
            bits += 8;
            while (bits >= 5) {
                sb.append(ALPHABET.charAt((buffer >> (bits - 5)) & 31));
                bits -= 5;
                buffer &= (1 << bits) - 1;
            }
        }
        if (bits > 0) sb.append(ALPHABET.charAt((buffer << (5 - bits)) & 31));
        return sb.toString();
    }

    private static byte[] base32Decode(String s) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int buffer = 0, bits = 0;
        for (char c : s.toUpperCase().toCharArray()) {
            int v = ALPHABET.indexOf(c);
            if (v < 0) continue;
            buffer = (buffer << 5) | v;
            bits += 5;
            if (bits >= 8) {
                out.write((buffer >> (bits - 8)) & 0xff);
                bits -= 8;
                buffer &= (1 << bits) - 1;
            }
        }
        return out.toByteArray();
    }
}
