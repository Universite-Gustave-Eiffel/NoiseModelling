package org.noise_planet.noisemodelling.pathfinder.profilebuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Read-only List wrapper which logs a warning when mutation is attempted and
 * then throws {@link UnsupportedOperationException} to prevent changes.
 *
 * <p>Use this for service "get" accessors when you want to discourage callers
 * from mutating internal collections but still provide fast read access.</p>
 */
public class LoggingUnmodifiableList<E> implements List<E> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingUnmodifiableList.class);
    private final List<E> delegate;
    private final String ownerDescription;

    public LoggingUnmodifiableList(List<E> delegate) {
        this(delegate, null);
    }

    public LoggingUnmodifiableList(List<E> delegate, String ownerDescription) {
        this.delegate = Objects.requireNonNull(delegate);
        this.ownerDescription = ownerDescription != null ? ownerDescription : "list";
    }

    private void warnAndThrow(String op) {
        LOGGER.warn("Attempt to {} on {} returned by service; modification not allowed", op, ownerDescription);
        throw new UnsupportedOperationException("Modification not allowed on service-provided collection");
    }

    // --- read-only delegations ---
    @Override public int size() { return delegate.size(); }
    @Override public boolean isEmpty() { return delegate.isEmpty(); }
    @Override public boolean contains(Object o) { return delegate.contains(o); }
    @Override public Object[] toArray() { return delegate.toArray(); }
    @Override public <T> T[] toArray(T[] a) { return delegate.toArray(a); }
    @Override public boolean containsAll(Collection<?> c) { return delegate.containsAll(c); }
    @Override public E get(int index) { return delegate.get(index); }
    @Override public int indexOf(Object o) { return delegate.indexOf(o); }
    @Override public int lastIndexOf(Object o) { return delegate.lastIndexOf(o); }
    @Override public boolean equals(Object o) { return delegate.equals(o); }
    @Override public int hashCode() { return delegate.hashCode(); }

    @Override
    public Iterator<E> iterator() {
        final Iterator<E> it = delegate.iterator();
        return new Iterator<>() {
            @Override public boolean hasNext() { return it.hasNext(); }
            @Override public E next() { return it.next(); }
            @Override public void remove() { warnAndThrow("iterator.remove()"); }
        };
    }

    @Override
    public ListIterator<E> listIterator() { return listIterator(0); }

    @Override
    public ListIterator<E> listIterator(final int index) {
        final ListIterator<E> it = delegate.listIterator(index);
        return new ListIterator<>() {
            @Override public boolean hasNext() { return it.hasNext(); }
            @Override public E next() { return it.next(); }
            @Override public boolean hasPrevious() { return it.hasPrevious(); }
            @Override public E previous() { return it.previous(); }
            @Override public int nextIndex() { return it.nextIndex(); }
            @Override public int previousIndex() { return it.previousIndex(); }
            @Override public void remove() { warnAndThrow("listIterator.remove()"); }
            @Override public void set(E e) { warnAndThrow("listIterator.set()"); }
            @Override public void add(E e) { warnAndThrow("listIterator.add()"); }
        };
    }

    @Override public List<E> subList(int fromIndex, int toIndex) { return new LoggingUnmodifiableList<>(delegate.subList(fromIndex, toIndex), ownerDescription + "[subList]"); }

    // --- mutation operations that will log + throw ---
    @Override public boolean add(E e) { warnAndThrow("add"); return false; }
    @Override public void add(int index, E element) { warnAndThrow("add(index,element)"); }
    @Override public boolean addAll(Collection<? extends E> c) { warnAndThrow("addAll"); return false; }
    @Override public boolean addAll(int index, Collection<? extends E> c) { warnAndThrow("addAll(index,coll)"); return false; }
    @Override public boolean remove(Object o) { warnAndThrow("remove(Object)"); return false; }
    @Override public E remove(int index) { warnAndThrow("remove(index)"); return null; }
    @Override public boolean removeAll(Collection<?> c) { warnAndThrow("removeAll"); return false; }
    @Override public boolean retainAll(Collection<?> c) { warnAndThrow("retainAll"); return false; }
    @Override public void clear() { warnAndThrow("clear"); }
    @Override public E set(int index, E element) { warnAndThrow("set"); return null; }
}
