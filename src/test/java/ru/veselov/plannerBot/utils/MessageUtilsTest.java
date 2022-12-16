package ru.veselov.plannerBot.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MessageUtilsTest {

    @Test
    void shortenString() {


        Assertions.assertEquals("0123456...6543210",MessageUtils.shortenString(
                "01234567asdfasdfasdfasdfasdfasdfasdfasdfasdfasdf76543210"));
    }
}