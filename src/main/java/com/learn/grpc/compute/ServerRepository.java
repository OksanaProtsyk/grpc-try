package com.learn.grpc.compute;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServerRepository extends PagingAndSortingRepository<ServerEntity, Long> {
}
