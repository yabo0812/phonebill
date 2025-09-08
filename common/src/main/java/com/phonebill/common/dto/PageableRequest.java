package com.phonebill.common.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 페이징 요청 DTO
 * 목록 조회시 페이징 처리를 위한 공통 요청 구조
 */
@Getter
@Setter
@NoArgsConstructor
public class PageableRequest {
    
    /**
     * 페이지 번호 (0부터 시작)
     */
    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
    private int page = 0;
    
    /**
     * 페이지 크기
     */
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    private int size = 20;
    
    /**
     * 정렬 기준 (예: "id,desc" 또는 "name,asc")
     */
    private String sort;
    
    public PageableRequest(int page, int size) {
        this.page = page;
        this.size = size;
    }
    
    public PageableRequest(int page, int size, String sort) {
        this.page = page;
        this.size = size;
        this.sort = sort;
    }
}