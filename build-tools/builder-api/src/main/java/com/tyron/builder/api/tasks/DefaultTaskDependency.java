package com.tyron.builder.api.tasks;

import static com.google.common.collect.Iterables.toArray;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.tyron.builder.api.Task;
import com.tyron.builder.api.internal.tasks.AbstractTaskDependency;
import com.tyron.builder.api.internal.tasks.TaskDependencyContainer;
import com.tyron.builder.api.internal.tasks.TaskDependencyResolveContext;
import com.tyron.builder.api.internal.typeconversion.UnsupportedNotationException;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.RandomAccess;
import java.util.Set;
import java.util.concurrent.Callable;

public class DefaultTaskDependency extends AbstractTaskDependency {

    private final ImmutableSet<Object> immutableValues;
    private Set<Object> mutableValues;
    private final TaskResolver resolver;

    public DefaultTaskDependency() {
        this(null);
    }

    public DefaultTaskDependency(TaskResolver resolver) {
        this(resolver, ImmutableSet.of());
    }

    public DefaultTaskDependency(TaskResolver resolver, ImmutableSet<Object> immutableValues) {
        this.resolver = resolver;
        this.immutableValues = immutableValues;
    }

    @Override
    public void visitDependencies(TaskDependencyResolveContext context) {
        Set<Object> mutableValues = getMutableValues();
        if (mutableValues.isEmpty() && immutableValues.isEmpty()) {
            return;
        }
        final Deque<Object> queue = new ArrayDeque<>(mutableValues.size() + immutableValues.size());
        queue.addAll(immutableValues);
        queue.addAll(mutableValues);
        while (!queue.isEmpty()) {
            Object dependency = queue.removeFirst();
//            if (dependency instanceof Buildable) {
//                context.add(dependency);
//            } else
            if (dependency instanceof Task) {
                context.add(dependency);
            } else if (dependency instanceof TaskDependency) {
                context.add(dependency);
//            } else if (dependency instanceof ProviderInternal) {
//                // When a Provider is used as a task dependency (rather than as a task input), need to unpack the value
//                ProviderInternal<?> provider = (ProviderInternal<?>) dependency;
//                ValueSupplier.ValueProducer producer = provider.getProducer();
//                if (producer.isKnown()) {
//                    producer.visitProducerTasks(context);
//                } else {
//                    // The provider does not know how to produce the value, so use the value instead
//                    queue.addFirst(provider.get());
//                }
            } else if (dependency instanceof TaskDependencyContainer) {
                ((TaskDependencyContainer) dependency).visitDependencies(context);
//            } else if (dependency instanceof Closure) {
//                Closure closure = (Closure) dependency;
//                Object closureResult = closure.call(context.getTask());
//                if (closureResult != null) {
//                    queue.addFirst(closureResult);
//                }
            } else if (dependency instanceof List) {
                List<?> list = (List) dependency;
                if (list instanceof RandomAccess) {
                    for (int i = list.size() - 1; i >= 0; i--) {
                        queue.addFirst(list.get(i));
                    }
                } else {
                    ListIterator<?> iterator = list.listIterator(list.size());
                    while (iterator.hasPrevious()) {
                        Object item = iterator.previous();
                        queue.addFirst(item);
                    }
                }
            } else if (dependency instanceof Iterable && !(dependency instanceof Path)) {
                // Path is Iterable, but we don't want to unpack it
                Iterable<?> iterable = (Iterable<?>) dependency;
                addAllFirst(queue, toArray(iterable, Object.class));
            } else if (dependency instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) dependency;
                addAllFirst(queue, map.values().toArray());
            } else if (dependency instanceof Object[]) {
                Object[] array = (Object[]) dependency;
                addAllFirst(queue, array);
            } else if (dependency instanceof Callable) {
                Callable<?> callable = (Callable<?>) dependency;
                Object callableResult = null;
                try {
                    callableResult = callable.call();
                } catch (Exception e) {
                    callable = null;
                }
                if (callableResult != null) {
                    queue.addFirst(callableResult);
                }
            } else if (resolver != null && dependency instanceof CharSequence) {
                context.add(resolver.resolveTask(dependency.toString()));
            } else {
                List<String> formats = new ArrayList<String>();
                if (resolver != null) {
                    formats.add("A String or CharSequence task name or path");
                }
                formats.add("A Task instance");
                formats.add("A TaskReference instance");
                formats.add("A Buildable instance");
                formats.add("A TaskDependency instance");
                formats.add("A Provider that represents a task output");
                formats.add("A Provider instance that returns any of these types");
                formats.add("A Closure instance that returns any of these types");
                formats.add("A Callable instance that returns any of these types");
                formats.add("An Iterable, Collection, Map or array instance that contains any of these types");
                throw new UnsupportedNotationException(dependency, String.format("Cannot convert %s to a task.", dependency), null, formats);
            }
        }
    }

    private static void addAllFirst(Deque<Object> queue, Object[] items) {
        for (int i = items.length - 1; i >= 0; i--) {
            queue.addFirst(items[i]);
        }
    }

    public Set<Object> getMutableValues() {
        if (mutableValues == null) {
            mutableValues = new TaskDependencySet();
        }
        return mutableValues;
    }

    public void setValues(Iterable<?> values) {
        getMutableValues().clear();
        for (Object value : values) {
            addValue(value);
        }
    }

    public DefaultTaskDependency add(Object... values) {
        for (Object value : values) {
            addValue(value);
        }
        return this;
    }

    private void addValue(Object dependency) {
        if (dependency == null) {
            throw new RuntimeException("A dependency must not be empty");
        }
        getMutableValues().add(dependency);
    }

    private static class TaskDependencySet implements Set<Object> {
        private final Set<Object> delegate = Sets.newHashSet();
        private final static String REMOVE_ERROR = "Removing a task dependency from a task instance is not supported.";

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return delegate.contains(o);
        }

        @Override
        public Iterator<Object> iterator() {
            return delegate.iterator();
        }

        @Override
        public Object[] toArray() {
            return delegate.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return delegate.toArray(a);
        }

        @Override
        public boolean add(Object o) {
            return delegate.add(o);
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException(REMOVE_ERROR);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return delegate.containsAll(c);
        }

        @Override
        public boolean addAll(Collection<?> c) {
            return delegate.addAll(c);
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException(REMOVE_ERROR);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException(REMOVE_ERROR);
        }

        @Override
        public void clear() {
            delegate.clear();
        }

        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        @Override
        public boolean equals(Object o) {
            return delegate.equals(o);
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }
}
