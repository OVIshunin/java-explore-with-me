package ru.practicum.ewm.util;

import lombok.Getter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.Serializable;

@Getter
public class OffsetPageRequest implements Pageable, Serializable {

    private final int offset;
    private final int size;
    private final Sort sort;

    public OffsetPageRequest(int offset, int size) {
        this(offset, size, Sort.unsorted());
    }

    public OffsetPageRequest(int offset, int size, Sort sort) {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must not be less than zero");
        }
        if (size < 1) {
            throw new IllegalArgumentException("Size must not be less than one");
        }
        this.offset = offset;
        this.size = size;
        this.sort = sort != null ? sort : Sort.unsorted();
    }

    @Override
    public int getPageNumber() {
        return offset / size;
    }

    @Override
    public int getPageSize() {
        return size;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public Pageable next() {
        return new OffsetPageRequest(offset + size, size, sort);
    }

    @Override
    public Pageable previousOrFirst() {
        return hasPrevious()
                ? new OffsetPageRequest(offset - size, size, sort)
                : first();
    }

    @Override
    public Pageable first() {
        return new OffsetPageRequest(0, size, sort);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        return new OffsetPageRequest(pageNumber * size, size, sort);
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof OffsetPageRequest that)) {
            return false;
        }
        return this.offset == that.offset
                && this.size == that.size
                && this.sort.equals(that.sort);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + offset;
        result = 31 * result + size;
        result = 31 * result + sort.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format("Offset page request [offset: %d, size: %d, sort: %s]", offset, size, sort);
    }
}