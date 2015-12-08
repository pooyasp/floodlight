package org.sdnplatform.sync.internal.util;

import java.util.NoSuchElementException;

import org.sdnplatform.sync.IClosableIterator;


public class EmptyClosableIterator<T> implements IClosableIterator<T> {
    
    @Override
	public boolean hasNext() {
        return false;
    }

    @Override
	public T next() {
        throw new NoSuchElementException();
    }

    @Override
	public void remove() {
        throw new NoSuchElementException();
    }

    @Override
    public void close() {
        // no-op
    }
}
