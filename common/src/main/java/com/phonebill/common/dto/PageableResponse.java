package com.phonebill.common.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 페이징 응답 DTO
 * 목록 조회 결과의 페이징 정보를 포함하는 공통 응답 구조
 */
@Getter
@Setter
@NoArgsConstructor
public class PageableResponse<T> {
    
    /**
     * 실제 데이터 목록
     */
    private List<T> content;
    
    /**
     * 현재 페이지 번호 (0부터 시작)
     */
    private int page;
    
    /**
     * 페이지 크기
     */
    private int size;
    
    /**
     * 전체 요소 개수
     */
    private long totalElements;
    
    /**
     * 전체 페이지 수
     */
    private int totalPages;
    
    /**
     * 첫 번째 페이지 여부
     */
    private boolean first;
    
    /**
     * 마지막 페이지 여부
     */
    private boolean last;
    
    /**
     * 정렬 기준
     */
    private String sort;
    
    public PageableResponse(List<T> content, int page, int size, long totalElements, String sort) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = (int) Math.ceil((double) totalElements / size);
        this.first = page == 0;
        this.last = page >= totalPages - 1;
        this.sort = sort;
    }
    
    /**
     * 페이징 응답 생성
     */
    public static <T> PageableResponse<T> of(List<T> content, PageableRequest request, long totalElements) {
        return new PageableResponse<>(content, request.getPage(), request.getSize(), totalElements, request.getSort());
    }
}