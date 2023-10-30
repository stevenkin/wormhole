package com.github.wormhole.common.utils;

import org.apache.commons.lang3.RandomStringUtils;

public class IDUtil {
    public static String genRequestId() {
        return System.currentTimeMillis() + RandomStringUtils.randomAlphabetic(8);
    }
}
