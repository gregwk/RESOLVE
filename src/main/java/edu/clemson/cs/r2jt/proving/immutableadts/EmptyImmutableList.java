package edu.clemson.cs.r2jt.proving.immutableadts;

import java.util.Iterator;

import edu.clemson.cs.r2jt.proving.DummyIterator;

public class EmptyImmutableList<E> extends AbstractImmutableList<E> {

    private final Iterator<E> TYPESAFE_ITERATOR = (Iterator<E>) null;

    @Override
    public E get(int index) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public SimpleImmutableList<E> head(int length) {

        if (length != 0) {
            throw new IndexOutOfBoundsException();
        }

        return this;
    }

    @Override
    public Iterator<E> iterator() {
        return DummyIterator.getInstance(TYPESAFE_ITERATOR);
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public SimpleImmutableList<E> tail(int startIndex) {

        if (startIndex != 0) {
            throw new IndexOutOfBoundsException();
        }

        return this;
    }

}
