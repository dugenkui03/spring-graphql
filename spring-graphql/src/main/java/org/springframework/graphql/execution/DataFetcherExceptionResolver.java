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

import java.util.List;

import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import reactor.core.publisher.Mono;

/**
 * Contract to resolve exceptions from {@link graphql.schema.DataFetcher}s.
 * Implementations are typically declared as beans in Spring configuration and
 * are invoked sequentially until one emits a List of {@link GraphQLError}s.
 *
 * <p>Most resolver implementations can extend
 * {@link DataFetcherExceptionResolverAdapter} and override one of its
 * {@link DataFetcherExceptionResolverAdapter#resolveToSingleError resolveToSingleError} or
 * {@link DataFetcherExceptionResolverAdapter#resolveToMultipleErrors resolveToMultipleErrors}
 * methods that resolve the exception synchronously.
 *
 * <p>Resolver implementations can use {@link ErrorType} to classify errors
 * using one of several common categories.
 *
 * @author Rossen Stoyanchev
 * @since 1.0.0
 * @see ErrorType
 * @see DataFetcherExceptionResolverAdapter
 * @see ExceptionResolversExceptionHandler
 */
public interface DataFetcherExceptionResolver {

	/**
	 * kp 异常处理器：将 dataFetcher 返回的异常结果包装为 List<GraphQLError>。
	 *
	 * Resolve the given exception and return the error(s) to add to the response.
	 * <p>Implementations can use
	 * {@link graphql.GraphqlErrorBuilder#newError(DataFetchingEnvironment)} to
	 * create an error with the coordinates of the target field, and use
	 * {@link ErrorType} to specify a category for the error.
	 * @param exception the exception to resolve
	 * @param environment the environment for the invoked {@code DataFetcher}
	 * @return a {@code Mono} with errors to add to the GraphQL response;
	 * if the {@code Mono} completes with an empty List, the exception is resolved
	 * without any errors added to the response; if the {@code Mono} completes
	 * empty, without emitting a List, the exception remains unresolved and gives
	 * other resolvers a chance.
	 */
	Mono<List<GraphQLError>> resolveException(Throwable exception, DataFetchingEnvironment environment);

}
