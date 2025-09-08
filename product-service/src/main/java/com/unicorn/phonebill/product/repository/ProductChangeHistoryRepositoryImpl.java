package com.unicorn.phonebill.product.repository;

import com.unicorn.phonebill.product.domain.ProductChangeHistory;
import com.unicorn.phonebill.product.domain.ProcessStatus;
import com.unicorn.phonebill.product.repository.entity.ProductChangeHistoryEntity;
import com.unicorn.phonebill.product.repository.jpa.ProductChangeHistoryJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 상품변경 이력 Repository 구현체
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class ProductChangeHistoryRepositoryImpl implements ProductChangeHistoryRepository {

    private final ProductChangeHistoryJpaRepository jpaRepository;

    @Override
    public ProductChangeHistory save(ProductChangeHistory history) {
        log.debug("상품변경 이력 저장: requestId={}", history.getRequestId());
        
        ProductChangeHistoryEntity entity = ProductChangeHistoryEntity.fromDomain(history);
        ProductChangeHistoryEntity savedEntity = jpaRepository.save(entity);
        
        log.info("상품변경 이력 저장 완료: id={}, requestId={}", 
                savedEntity.getId(), savedEntity.getRequestId());
        
        return savedEntity.toDomain();
    }

    @Override
    public Optional<ProductChangeHistory> findByRequestId(String requestId) {
        log.debug("요청 ID로 이력 조회: requestId={}", requestId);
        
        return jpaRepository.findByRequestId(requestId)
                .map(ProductChangeHistoryEntity::toDomain);
    }

    @Override
    public Page<ProductChangeHistory> findByLineNumber(String lineNumber, Pageable pageable) {
        log.debug("회선번호로 이력 조회: lineNumber={}, page={}, size={}", 
                lineNumber, pageable.getPageNumber(), pageable.getPageSize());
        
        return jpaRepository.findByLineNumberOrderByRequestedAtDesc(lineNumber, pageable)
                .map(ProductChangeHistoryEntity::toDomain);
    }

    @Override
    public Page<ProductChangeHistory> findByCustomerId(String customerId, Pageable pageable) {
        log.debug("고객 ID로 이력 조회: customerId={}, page={}, size={}", 
                customerId, pageable.getPageNumber(), pageable.getPageSize());
        
        return jpaRepository.findByCustomerIdOrderByRequestedAtDesc(customerId, pageable)
                .map(ProductChangeHistoryEntity::toDomain);
    }

    @Override
    public Page<ProductChangeHistory> findByProcessStatus(ProcessStatus status, Pageable pageable) {
        log.debug("처리 상태별 이력 조회: status={}, page={}, size={}", 
                status, pageable.getPageNumber(), pageable.getPageSize());
        
        return jpaRepository.findByProcessStatusOrderByRequestedAtDesc(status, pageable)
                .map(ProductChangeHistoryEntity::toDomain);
    }

    @Override
    public Page<ProductChangeHistory> findByPeriod(
            LocalDateTime startDate, 
            LocalDateTime endDate, 
            Pageable pageable) {
        
        log.debug("기간별 이력 조회: startDate={}, endDate={}, page={}, size={}", 
                startDate, endDate, pageable.getPageNumber(), pageable.getPageSize());
        
        return jpaRepository.findByRequestedAtBetweenOrderByRequestedAtDesc(
                startDate, endDate, pageable)
                .map(ProductChangeHistoryEntity::toDomain);
    }

    @Override
    public Page<ProductChangeHistory> findByLineNumberAndPeriod(
            String lineNumber,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {
        
        log.debug("회선번호와 기간으로 이력 조회: lineNumber={}, startDate={}, endDate={}", 
                lineNumber, startDate, endDate);
        
        return jpaRepository.findByLineNumberAndRequestedAtBetweenOrderByRequestedAtDesc(
                lineNumber, startDate, endDate, pageable)
                .map(ProductChangeHistoryEntity::toDomain);
    }

    @Override
    public List<ProductChangeHistory> findProcessingRequestsOlderThan(LocalDateTime timeoutThreshold) {
        log.debug("타임아웃 처리 중인 요청 조회: timeoutThreshold={}", timeoutThreshold);
        
        return jpaRepository.findProcessingRequestsOlderThan(timeoutThreshold)
                .stream()
                .map(ProductChangeHistoryEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ProductChangeHistory> findLatestSuccessfulChangeByLineNumber(String lineNumber) {
        log.debug("최근 성공한 상품변경 이력 조회: lineNumber={}", lineNumber);
        
        Pageable pageable = PageRequest.of(0, 1);
        Page<ProductChangeHistoryEntity> page = jpaRepository
                .findLatestSuccessfulChangeByLineNumber(lineNumber, pageable);
        
        return page.getContent().stream()
                .findFirst()
                .map(ProductChangeHistoryEntity::toDomain);
    }

    @Override
    public List<Object[]> getChangeStatisticsByPeriod(
            LocalDateTime startDate, 
            LocalDateTime endDate) {
        
        log.debug("상품변경 통계 조회: startDate={}, endDate={}", startDate, endDate);
        
        return jpaRepository.getChangeStatisticsByPeriod(startDate, endDate);
    }

    @Override
    public long countSuccessfulChangesByProductCodesSince(
            String currentProductCode,
            String targetProductCode,
            LocalDateTime fromDate) {
        
        log.debug("상품 간 변경 횟수 조회: currentProductCode={}, targetProductCode={}, fromDate={}", 
                currentProductCode, targetProductCode, fromDate);
        
        return jpaRepository.countSuccessfulChangesByProductCodesSince(
                currentProductCode, targetProductCode, fromDate);
    }

    @Override
    public long countInProgressRequestsByLineNumber(String lineNumber) {
        log.debug("회선별 진행 중인 요청 개수 조회: lineNumber={}", lineNumber);
        
        return jpaRepository.countInProgressRequestsByLineNumber(lineNumber);
    }

    @Override
    public boolean existsByRequestId(String requestId) {
        log.debug("요청 ID 존재 여부 확인: requestId={}", requestId);
        
        return jpaRepository.existsByRequestId(requestId);
    }

    @Override
    public void deleteById(Long id) {
        log.info("상품변경 이력 삭제: id={}", id);
        
        jpaRepository.deleteById(id);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }
}