package com.voipgrid.vialer.logging;

import java.util.ArrayList;
import java.util.List;

class Buffer {

    /**
     * The default size of the buffer, if no buffer size is declared then this will be used.
     *
     */
    private static final int DEFAULT_BUFFER_SIZE = 100;

    /**
     * The size of the buffer, this amount of records will be stored in the buffer before old
     * records are removed from the stack.
     *
     */
    private int mSize;

    private List<String> buffer = new ArrayList<>();

    Buffer(int size) {
        mSize = size;
    }

    Buffer() {
        mSize = DEFAULT_BUFFER_SIZE;
    }

    /**
     * Get the contents of the buffer.
     *
     * @return A List of strings that have been added to the buffer.
     */
    public List<String> get() {
        return buffer;
    }

    /**
     * Add a new string to the buffer.
     *
     * @param log The string that will be added to the buffer
     */
    public void add(String log) {
        if (buffer.size() >= mSize) {
            buffer.remove(0);
        }

        buffer.add(log);
    }

    /**
     * Remove all logs from the buffer.
     *
     */
    public void clear() {
        buffer = new ArrayList<>();
    }
}
