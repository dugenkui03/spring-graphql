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

import java.util.LinkedHashMap;
import java.util.Map;

import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.schema.DataFetchingEnvironment;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import org.springframework.lang.Nullable;

/**
 * Package private utility class for propagating(传播) a Reactor {@link ContextView}
 * through the {@link ExecutionInput} and the {@link DataFetchingEnvironment} of a request.
 *
 * @author Rossen Stoyanchev
 * @since 1.0.0
 */
public abstract class ContextManager {

	private static final String CONTEXT_VIEW_KEY = ContextManager.class.getName() + ".CONTEXT_VIEW";

	private static final String THREAD_ID = ContextManager.class.getName() + ".THREAD_ID";

	private static final String THREAD_LOCAL_VALUES_KEY = ContextManager.class.getName() + ".THREAD_VALUES_ACCESSOR";

	private static final String THREAD_LOCAL_ACCESSOR_KEY = ContextManager.class.getName() + ".THREAD_LOCAL_ACCESSOR";

	/**
	 * kp 将 ContextView对象 放到请求上下文中，key为 CONTEXT_VIEW_KEY，后续可以通过DataFetchingEnvironment获取
	 *
	 * Save the given Reactor {@link ContextView} in the an {@link ExecutionInput} for
	 * later access through the {@link DataFetchingEnvironment}.
	 * @param contextView the reactor context view
	 * @param input the GraphQL query input
	 */
	static void setReactorContext(ContextView contextView, ExecutionInput input) {
		((GraphQLContext) input.getContext()).put(CONTEXT_VIEW_KEY, contextView);
	}

	/**
	 * Return the Reactor {@link ContextView} saved in the given DataFetchingEnvironment.
	 * @param environment the DataFetchingEnvironment
	 * @return the reactor {@link ContextView}
	 */
	static ContextView getReactorContext(DataFetchingEnvironment environment) {
		GraphQLContext graphQlContext = environment.getContext();
		return graphQlContext.getOrDefault(CONTEXT_VIEW_KEY, Context.empty());
	}

	/**
	 * Use the given accessor to extract ThreadLocal values and save them in a
	 * sub-map in the given {@link Context}, so those can be restored later
	 * around the execution of data fetchers and exception resolvers. The accessor
	 * instance is also saved in the Reactor Context so it can be used to
	 * actually restore and reset ThreadLocal values.
	 * @param accessor the accessor to use
	 * @param context the context to write to if there are ThreadLocal values
	 * @return a new Reactor {@link ContextView} or the {@code Context} instance
	 * that was passed in, if there were no ThreadLocal values to extract.
	 */
	public static Context extractThreadLocalValues(ThreadLocalAccessor accessor, Context context) {
		Map<String, Object> valuesMap = new LinkedHashMap<>();
		accessor.extractValues(valuesMap);
		if (valuesMap.isEmpty()) {
			return context;
		}
		return context.putAll((ContextView) Context.of(
				THREAD_LOCAL_VALUES_KEY, valuesMap,
				THREAD_LOCAL_ACCESSOR_KEY, accessor,
				THREAD_ID, Thread.currentThread().getId()));
	}

	/**
	 * Look up saved ThreadLocal values and restore them if any are found.
	 * This is a no-op if invoked on the thread that values were extracted on.
	 * @param contextView the reactor {@link ContextView}
	 */
	static void restoreThreadLocalValues(ContextView contextView) {
		ThreadLocalAccessor accessor = getThreadLocalAccessor(contextView);
		if (accessor != null) {
			accessor.restoreValues(contextView.get(THREAD_LOCAL_VALUES_KEY));
		}
	}

	/**
	 * Look up saved ThreadLocal values and remove the ThreadLocal values.
	 * This is a no-op if invoked on the thread that values were extracted on.
	 * @param contextView the reactor {@link ContextView}
	 */
	static void resetThreadLocalValues(ContextView contextView) {
		ThreadLocalAccessor accessor = getThreadLocalAccessor(contextView);
		if (accessor != null) {
			accessor.resetValues(contextView.get(THREAD_LOCAL_VALUES_KEY));
		}
	}

	@Nullable
	private static ThreadLocalAccessor getThreadLocalAccessor(ContextView view) {
		Long id = view.getOrDefault(THREAD_ID, null);
		return (id != null && id != Thread.currentThread().getId() ? view.get(THREAD_LOCAL_ACCESSOR_KEY) : null);
	}

}
