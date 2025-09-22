package it.glucotrack.util;

import java.nio.charset.StandardCharsets;

public class PasswordUtils {

    private static int SHIFT = 3; // SHIFT NUMBER


    //==========================
    //==== CRYPT OPERATIONS ====
    //==========================

    // Encryption with shift and xor
    public static String encryptPassword(String password, String email) {
        String shifted = shiftPassword(password);
        return xorPassword(shifted, email);
    }

    public static String decryptPassword(String encryptedPassword, String email) {
        // Inverted XOR
        String unxored = unXorPassword(encryptedPassword, email);
        // Unshift
        return unShiftPassword(unxored);
    }



    //==========================
    //==== SHIFT OPERATIONS ====
    //==========================

    public static String unShiftPassword(String shiftedPassword) {
        StringBuilder unshifted = new StringBuilder();
        for (char c : shiftedPassword.toCharArray()) {
            unshifted.append((char) (c - SHIFT)); // Shift back
        }
        return unshifted.toString();
    }

    public static String shiftPassword(String password) {
        StringBuilder shifted = new StringBuilder();
        for (char c : password.toCharArray()) {
            shifted.append((char) (c + SHIFT)); // Shift forward
        }
        return shifted.toString();
    }

    //========================
    //==== XOR OPERATIONS ====
    //========================


    public static String xorPassword(String password, String email) {

        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
        byte[] emailBytes = email.toLowerCase().getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[passwordBytes.length];

        // XOR, if email is shorter than password, it repeats
        for (int i = 0; i < passwordBytes.length; i++) {
            result[i] = (byte) (passwordBytes[i] ^ emailBytes[i % emailBytes.length]);
        }

        // Convert in hex
        StringBuilder sb = new StringBuilder();
        for (byte b : result) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static String unXorPassword(String xoredPassword, String email) {
        byte[] xoredBytes = new byte[xoredPassword.length() / 2];
        for (int i = 0; i < xoredBytes.length; i++) {
            xoredBytes[i] = (byte) Integer.parseInt(xoredPassword.substring(i * 2, i * 2 + 2), 16);
        }

        byte[] emailBytes = email.toLowerCase().getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[xoredBytes.length];

        // XOR, if email is shorter than password, it repeats
        for (int i = 0; i < xoredBytes.length; i++) {
            result[i] = (byte) (xoredBytes[i] ^ emailBytes[i % emailBytes.length]);
        }

        return new String(result, StandardCharsets.UTF_8);
    }


    //===============================
    //==== ADDITIONAL OPERATIONS ====
    //===============================

    public static boolean ValidPassword(String encryptedPassword, String email, String password) {
        String encrypted = encryptPassword(password, email);
        return encrypted.equals(encryptedPassword);

    }
}