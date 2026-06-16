package com.servicematch.util;

public class CpfUtils {

    private CpfUtils() {}

    public static boolean isValido(String cpf) {
        if (cpf == null) return false;
        String nums = cpf.replaceAll("[^0-9]", "");
        if (nums.length() != 11) return false;
        
        if (nums.chars().distinct().count() == 1) return false;

        int sum = 0;
        for (int i = 0; i < 9; i++) sum += (nums.charAt(i) - '0') * (10 - i);
        int r1 = 11 - (sum % 11);
        if (r1 >= 10) r1 = 0;
        if (r1 != (nums.charAt(9) - '0')) return false;

        sum = 0;
        for (int i = 0; i < 10; i++) sum += (nums.charAt(i) - '0') * (11 - i);
        int r2 = 11 - (sum % 11);
        if (r2 >= 10) r2 = 0;
        return r2 == (nums.charAt(10) - '0');
    }

    public static String formatar(String cpf) {
        if (cpf == null) return null;
        String n = cpf.replaceAll("[^0-9]", "");
        if (n.length() != 11) return cpf;
        return n.substring(0, 3) + "." + n.substring(3, 6) + "." + n.substring(6, 9) + "-" + n.substring(9, 11);
    }
}
