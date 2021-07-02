package io.spring.sample.graphql.repository;

import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.CrudRepository;

public interface ArtifactRepositories
		// 接口多继承
		extends CrudRepository<ArtifactRepository, String>,
				QuerydslPredicateExecutor<ArtifactRepository> {

}
