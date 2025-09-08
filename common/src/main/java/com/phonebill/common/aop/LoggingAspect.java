package com.phonebill.common.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 로깅 AOP
 * 메소드 실행 시간과 파라미터를 로깅합니다.
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {
    
    @Around("execution(* com.phonebill..service..*(..))")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        log.info("[SERVICE] {}.{}() called with args: {}", className, methodName, args);
        
        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("[SERVICE] {}.{}() completed in {}ms", className, methodName, executionTime);
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("[SERVICE] {}.{}() failed in {}ms with error: {}", className, methodName, executionTime, e.getMessage());
            throw e;
        }
    }
    
    @Around("execution(* com.phonebill..controller..*(..))")
    public Object logControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        log.info("[CONTROLLER] {}.{}() called with args: {}", className, methodName, args);
        
        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("[CONTROLLER] {}.{}() completed in {}ms", className, methodName, executionTime);
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("[CONTROLLER] {}.{}() failed in {}ms with error: {}", className, methodName, executionTime, e.getMessage());
            throw e;
        }
    }
}
