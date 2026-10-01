package com.boltpe.insurance.cache;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.boltpe.insurance.entity.InsuranceVendorApiConfig;
import com.boltpe.insurance.enums.InsuranceType;
import com.boltpe.insurance.repository.InsuranceVendorApiConfigRepository;
import com.boltpe.insurance.refresh.VendorApiRecord;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.json.JsonMapper;

class InsuranceApiCacheServiceTest {
    @SuppressWarnings("unchecked")
    @Test void cacheHitReturnsRedisWithoutDb() {
        var redis=mock(StringRedisTemplate.class); var ops=mock(ValueOperations.class); var repo=mock(InsuranceVendorApiConfigRepository.class);
        when(redis.opsForValue()).thenReturn(ops); when(ops.get("insurance:apis:HEALTH")).thenReturn("[{\"apiType\":\"/x\",\"companyId\":\"1\",\"planId\":\"2\"}]");
        var service=new InsuranceApiCacheService(redis,repo,JsonMapper.builder().build(),30);
        var result=service.getOrLoad(InsuranceType.HEALTH);
        assertEquals(1,result.size()); assertEquals("/x",result.getFirst().apiType()); verifyNoInteractions(repo);
    }

    @SuppressWarnings("unchecked")
    @Test void cacheMissLoadsDbAndRefillsRedis() {
        var redis=mock(StringRedisTemplate.class); var ops=mock(ValueOperations.class); var repo=mock(InsuranceVendorApiConfigRepository.class);
        when(redis.opsForValue()).thenReturn(ops); when(ops.get(anyString())).thenReturn(null);
        var row=new InsuranceVendorApiConfig(); row.setApiType("/pa"); row.setCompanyId("1003"); row.setPlanId("227");
        when(repo.findAllByInsuranceTypeOrderByIdAsc("PERSONAL_ACCIDENT")).thenReturn(List.of(row));
        var service=new InsuranceApiCacheService(redis,repo,JsonMapper.builder().build(),30);
        var result=service.getOrLoad(InsuranceType.PERSONAL_ACCIDENT);
        assertEquals("/pa",result.getFirst().apiType()); verify(ops).set(eq("insurance:apis:PERSONAL_ACCIDENT"),anyString(),eq(Duration.ofHours(30)));
    }

    @SuppressWarnings("unchecked")
    @Test void redisFailureStillFallsBackToDbAndEmptyPutRejected() {
        var redis=mock(StringRedisTemplate.class); var ops=mock(ValueOperations.class); var repo=mock(InsuranceVendorApiConfigRepository.class);
        when(redis.opsForValue()).thenReturn(ops); when(ops.get(anyString())).thenThrow(new RuntimeException("redis down"));
        when(repo.findAllByInsuranceTypeOrderByIdAsc("HOSPITAL_CASH")).thenReturn(List.of());
        var service=new InsuranceApiCacheService(redis,repo,JsonMapper.builder().build(),30);
        assertTrue(service.getOrLoad(InsuranceType.HOSPITAL_CASH).isEmpty());
        assertThrows(IllegalArgumentException.class,()->service.put(InsuranceType.HEALTH,List.of()));
    }
}
