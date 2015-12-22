package com.voipgrid.vialer.api.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Basic VoipGridResponse to use with the Retrofit requests
 */
public class VoipGridResponse<T> {

    private Meta meta;

    private List<T> objects;

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    public List<T> getObjects() {
        return objects;
    }

    public void setObjects(List<T> objects) {
        this.objects = objects;
    }

    public class Meta {

        private int limit;

        private String next;

        private int offset;

        private String previous;

        @SerializedName("total_count")
        private int totalCount;

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public String getNext() {
            return next;
        }

        public void setNext(String next) {
            this.next = next;
        }

        public int getOffset() {
            return offset;
        }

        public void setOffset(int offset) {
            this.offset = offset;
        }

        public String getPrevious() {
            return previous;
        }

        public void setPrevious(String previous) {
            this.previous = previous;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(int totalCount) {
            this.totalCount = totalCount;
        }
    }
}
