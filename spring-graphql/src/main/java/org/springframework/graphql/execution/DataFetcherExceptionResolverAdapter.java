/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.graphql.execution;

import java.util.Collections;
import java.util.List;

import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import org.springframework.lang.Nullable;

/**
 * Adapter for {@link DataFetcherExceptionResolver} that pre-implements the
 * asynchronous contract and exposes the following synchronous methods:
 * <ul>
 * <li>{@link #resolveToSingleError}
 * <li>{@link #resolveToMultipleErrors}
 * </ul>
 *
 * <p>Implementations can also express interest in ThreadLocal context
 * propagation, from the underlying transport thread, via
 * {@link #setThreadLocalContextAware(boolean)}.
 *
 * @author Rossen Stoyanchev
 */
public class DataFetcherExceptionResolverAdapter implements DataFetcherExceptionResolver {

	private boolean threadLocalContextAware;


	/**
	 * Sub-classes can set this to indicate that ThreadLocal context from the
	 * transport handler (e.g. HTTP handler) should be restored when resolving
	 * exceptions.
	 * <p><strong>Note:</strong> This property is applicable only if transports
	 * use ThreadLocal's' (e.g. Spring MVC) and if a {@link ThreadLocalAccessor}
	 * is registered to extract ThreadLocal values of interest. There is no
	 * impact from setting this property otherwise.
	 * <p>By default this is set to "false" in which case there is no attempt
	 * to propagate ThreadLocal context.
	 * @param threadLocalContextAware whether this resolver needs access to
	 * ThreadLocal context or not.
	 */
	public void setThreadLocalContextAware(boolean threadLocalContextAware) {
		this.threadLocalContextAware = threadLocalContextAware;
	}

	/**
	 * Whether ThreadLocal context needs to be restored for this resolver.
	 */
	public boolean isThreadLocalContextAware() {
		return this.threadLocalContextAware;
	}

	@Override
	public final Mono<List<GraphQLError>> resolveException(Throwable ex, DataFetchingEnvironment env) {
		return Mono.defer(() -> Mono.justOrEmpty(resolveInternal(ex, env)));
	}

	@Nullable
	private List<GraphQLError> resolveInternal(Throwable ex, DataFetchingEnvironment env) {
		if (!this.threadLocalContextAware) {
			return resolveToMultipleErrors(ex, env);
		}
		ContextView contextView = ReactorContextManager.getReactorContext(env);
		try {
			ReactorContextManager.restoreThreadLocalValues(contextView);
			return resolveToMultipleErrors(ex, env);
		}
		finally {
			ReactorContextManager.resetThreadLocalValues(contextView);
		}
	}

	/**
	 * Override this method to resolve an Exception to multiple GraphQL errors.
	 * @param ex the exception to resolve
	 * @param env the environment for the invoked {@code DataFetcher}
	 * @return the resolved errors or {@code null} if unresolved
	 */
	@Nullable
	protected List<GraphQLError> resolveToMultipleErrors(Throwable ex, DataFetchingEnvironment env) {
		GraphQLError error = resolveToSingleError(ex, env);
		return (error != null ? Collections.singletonList(error) : null);
	}

	/**
	 * Override this method to resolve an Exception to a single GraphQL error.
	 * @param ex the exception to resolve
	 * @param env the environment for the invoked {@code DataFetcher}
	 * @return the resolved error or {@code null} if unresolved
	 */
	@Nullable
	protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
		return null;
	}

}
