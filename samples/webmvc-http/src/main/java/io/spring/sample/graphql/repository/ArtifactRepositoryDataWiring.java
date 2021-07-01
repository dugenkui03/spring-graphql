package io.spring.sample.graphql.repository;

import graphql.schema.idl.RuntimeWiring;

import org.springframework.graphql.boot.RuntimeWiringBuilderCustomizer;
import org.springframework.graphql.data.QuerydslDataFetcher;
import org.springframework.stereotype.Component;

@Component
public class ArtifactRepositoryDataWiring implements RuntimeWiringBuilderCustomizer {

	private final ArtifactRepositories repositories;

	public ArtifactRepositoryDataWiring(ArtifactRepositories repositories) {
		this.repositories = repositories;
	}

	// 为 Query 下的字段绑定 dataFetcher
	@Override
	public void customize(RuntimeWiring.Builder builder) {
		builder.type("Query", typeWiring -> typeWiring
				// 绑定list字段
				.dataFetcher("artifactRepositories", QuerydslDataFetcher.builder(repositories).many())
				// 绑定单个字段
				.dataFetcher("artifactRepository", QuerydslDataFetcher.builder(repositories).single()));
	}

}
