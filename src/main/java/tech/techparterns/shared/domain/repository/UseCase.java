package tech.techparterns.shared.domain.repository;

import tech.techparterns.shared.domain.Result;

public interface UseCase<T, R> {
    Result<R> execute(T input);
}