package com.phonebill.common.exception;

/**
 * 리소스를 찾을 수 없는 경우 발생하는 예외
 * 사용자, 요금제, 청구서 등의 데이터가 존재하지 않을 때 사용
 */
public class ResourceNotFoundException extends BusinessException {
    
    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND", 404);
    }
    
    public ResourceNotFoundException(String resourceType, Object id) {
        super(String.format("%s를 찾을 수 없습니다. ID: %s", resourceType, id), "RESOURCE_NOT_FOUND", 404);
    }
    
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, "RESOURCE_NOT_FOUND", 404, cause);
    }
}