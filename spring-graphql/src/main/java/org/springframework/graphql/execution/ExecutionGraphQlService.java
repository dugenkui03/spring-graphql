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

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import reactor.core.publisher.Mono;

import org.springframework.graphql.GraphQlService;

/**
 * Implementation of {@link GraphQlService} that performs GraphQL request execution
 * through {@link GraphQL#executeAsync(ExecutionInput)}.
 *
 * @author Rossen Stoyanchev
 * @since 1.0.0
 */
public class ExecutionGraphQlService implements GraphQlService {

	//kp 包括 GraphQL 和 GraphQLSchema
	private final GraphQlSource graphQlSource;

	public ExecutionGraphQlService(GraphQlSource graphQlSource) {
		this.graphQlSource = graphQlSource;
	}

	@Override
	public Mono<ExecutionResult> execute(ExecutionInput input) {
		GraphQL graphQl = this.graphQlSource.graphQl();
		// kp Function<ContextView, ? extends Mono<? extends T>>
		//	  相比于 defer(Supplier)、deferContextual(Function)可以携带上下文参数
		return Mono.deferContextual(
				// ContextView
				(contextView) -> {
					// kp 将 ContextView对象 放到请求上下文中
					ContextManager.setReactorContext(contextView, input);

					// 通过 CompletableFuture 对象创建Mono
					// 注意：该逻辑是包装在 deferContextual 中的
					// 		所以在解析deferContextual结果的时候才会执行这些逻辑
					return Mono.fromFuture(graphQl.executeAsync(input));
				}
		);
	}

}
