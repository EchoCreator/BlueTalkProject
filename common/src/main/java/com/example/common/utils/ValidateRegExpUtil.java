package com.example.common.utils;

import com.example.common.constant.RegExp;

import java.util.regex.Pattern;

public class ValidateRegExpUtil {
    public static boolean isValidPhoneNumber(String phoneNumber) {
        String pattern = RegExp.PHONE_NUMBER_REGEXP;
        return Pattern.matches(pattern, phoneNumber);
    }
}
