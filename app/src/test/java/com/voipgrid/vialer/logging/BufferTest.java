package com.voipgrid.vialer.logging;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BufferTest {

    @Test
    public void it_only_stores_up_to_the_defined_buffer_size() {
        Buffer buffer = new Buffer(10);
        addStringsToBuffer(buffer, 10);
        assertEquals(10, buffer.get().size());

        buffer = new Buffer(10);
        addStringsToBuffer(buffer, 5);
        assertEquals(5, buffer.get().size());

        buffer = new Buffer(10);
        addStringsToBuffer(buffer, 20);
        assertEquals(10, buffer.get().size());
    }

    @Test
    public void it_returns_the_buffer_in_the_order_they_were_inserted() {
        String firstLog = "first log", secondLog = "second log", thirdLog = "third log";

        Buffer buffer = new Buffer(5);
        buffer.add(firstLog);
        buffer.add(secondLog);
        buffer.add(thirdLog);

        assertEquals(firstLog, buffer.get().get(0));
    }

    @Test
    public void it_removes_excess_logs_starting_with_the_oldest() {
        String firstLog = "first log", secondLog = "second log", thirdLog = "third log";

        Buffer buffer = new Buffer(2);
        buffer.add(firstLog);
        buffer.add(secondLog);
        buffer.add(thirdLog);

        assertEquals(secondLog, buffer.get().get(0));
    }

    @Test
    public void it_is_possible_to_clear_the_buffer() {
        Buffer buffer = new Buffer();
        buffer.add("first log");
        buffer.add("second log");
        assertEquals(2, buffer.get().size());
        buffer.clear();
        assertEquals(0, buffer.get().size());
    }

    private void addStringsToBuffer(Buffer buffer, int amount) {
        for (int i = 0; i < amount; i++) {
            buffer.add("A test string");
        }
    }
}
