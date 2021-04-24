/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite;

import org.openrewrite.internal.lang.Nullable;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class InMemoryExecutionContext implements ExecutionContext {
    private final Map<String, Object> messages = new ConcurrentHashMap<>();
    private final Consumer<Throwable> onError;
    private final Function<Integer, Duration> runTimeout;

    public InMemoryExecutionContext() {
        this(
                t -> {
                }
        );
    }

    public InMemoryExecutionContext(Consumer<Throwable> onError) {
        this(onError, n -> Duration.ofHours(2));
    }

    public InMemoryExecutionContext(Consumer<Throwable> onError,
                                    Function<Integer, Duration> runTimeout) {
        this.onError = onError;
        this.runTimeout = runTimeout;
    }

    @Override
    public void putMessage(String key, Object value) {
        messages.put(key, value);
    }

    @Override
    @Nullable
    public <T> T getMessage(String key) {
        //noinspection unchecked
        return (T) messages.get(key);
    }

    @Override
    @Nullable
    public <T> T pollMessage(String key) {
        //noinspection unchecked
        return (T) messages.remove(key);
    }

    public Consumer<Throwable> getOnError() {
        return onError;
    }

    @Override
    public Duration getRunTimeout(int inputs) {
        return runTimeout.apply(inputs);
    }
}
