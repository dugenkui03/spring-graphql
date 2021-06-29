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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import org.springframework.util.Assert;
import org.springframework.web.client.ExtractingResponseErrorHandler;

/**
 * {@link DataFetcherExceptionHandler} that invokes {@link DataFetcherExceptionResolver}'s
 * in a sequence until one returns a non-null list of {@link GraphQLError}'s.
 *
 * @author Rossen Stoyanchev
 */
class ExceptionResolversExceptionHandler implements DataFetcherExceptionHandler {

	private static final Log logger = LogFactory.getLog(ExtractingResponseErrorHandler.class);

	// kp 多实例组合，很常用
	private final List<DataFetcherExceptionResolver> resolvers;

	/**
	 * kp 真正使用的异常处理器。
	 *
	 * Create an instance.
	 * @param resolvers the resolvers to use
	 */
	ExceptionResolversExceptionHandler(List<DataFetcherExceptionResolver> resolvers) {
		Assert.notNull(resolvers, "'resolvers' is required");
		this.resolvers = new ArrayList<>(resolvers);
	}

	@Override
	public DataFetcherExceptionHandlerResult onException(DataFetcherExceptionHandlerParameters parameters) {
		Throwable exception = parameters.getException();
		exception = ((exception instanceof CompletionException) ? exception.getCause() : exception);
		// 使用 resolvers 处理错误
		return invokeChain(exception, parameters.getDataFetchingEnvironment());
	}

	// @formatter:off

	DataFetcherExceptionHandlerResult invokeChain(Throwable ex, DataFetchingEnvironment env) {
		// For now we have to block:
		// https://github.com/graphql-java/graphql-java/issues/2356
		try {
			return Flux.fromIterable(this.resolvers)
					.flatMap((resolver) -> resolveErrors(ex, env, resolver))
					.next()
					.map((errors) -> DataFetcherExceptionHandlerResult.newResult().errors(errors).build())
					.switchIfEmpty(Mono.fromCallable(() -> applyDefaultHandling(ex, env)))
					.contextWrite((context) -> { // todo 现在 context是什么数据
						/**
						 * 获取执行上下文，详情见 {@link ExecutionGraphQlService}
						 */
						ContextView contextView = ContextManager.getReactorContext(env);
						return (contextView.isEmpty() ? context : context.putAll(contextView));
					})
					.toFuture()
					.get();
		}
		catch (Exception ex2) {
			if (logger.isWarnEnabled()) {
				logger.warn("Failed to handle " + ex.getMessage(), ex2);
			}
			// 抛出异常后怼错误进行简单处理返回
			return applyDefaultHandling(ex, env);
		}
	}

	private Mono<List<GraphQLError>> resolveErrors(
			Throwable ex, DataFetchingEnvironment environment, DataFetcherExceptionResolver resolver) {

		ContextView contextView = ContextManager.getReactorContext(environment);
		try {
			ContextManager.restoreThreadLocalValues(contextView);
			return resolver.resolveException(ex, environment);
		}
		finally {
			ContextManager.resetThreadLocalValues(contextView);
		}
	}

	private DataFetcherExceptionHandlerResult applyDefaultHandling(Throwable ex, DataFetchingEnvironment env) {
		GraphQLError error = GraphqlErrorBuilder.newError(env)
				// 异常信息
				.message(ex.getMessage())
				.errorType(ErrorType.INTERNAL_ERROR)
				.build();
		return DataFetcherExceptionHandlerResult.newResult(error).build();
	}

	// @formatter:on

}
