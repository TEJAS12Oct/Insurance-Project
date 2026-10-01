package com.boltpe.insurance.cache;

import com.boltpe.insurance.entity.InsuranceVendorApiConfig;
import com.boltpe.insurance.enums.InsuranceType;
import com.boltpe.insurance.repository.InsuranceVendorApiConfigRepository;
import com.boltpe.insurance.refresh.VendorApiRecord;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Service
public class InsuranceApiCacheService {
    private static final Logger LOGGER = LoggerFactory.getLogger(InsuranceApiCacheService.class);
    private static final String PREFIX = "insurance:apis:";
    private final StringRedisTemplate redisTemplate;
    private final InsuranceVendorApiConfigRepository repository;
    private final JsonMapper jsonMapper;
    private final Duration ttl;

    public InsuranceApiCacheService(StringRedisTemplate redisTemplate,
                                    InsuranceVendorApiConfigRepository repository,
                                    JsonMapper jsonMapper,
                                    @Value("${insurance.cache.ttl-hours}") long ttlHours) {
        this.redisTemplate = redisTemplate;
        this.repository = repository;
        this.jsonMapper = jsonMapper;
        this.ttl = Duration.ofHours(ttlHours);
    }

    public List<VendorApiRecord> getOrLoad(InsuranceType type) {
        String key = key(type);
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null && !json.isBlank()) {
                List<VendorApiRecord> cached = jsonMapper.readValue(json, new TypeReference<List<VendorApiRecord>>() {});
                if (!cached.isEmpty()) return cached;
            }
        } catch (RuntimeException ex) {
            LOGGER.warn("Redis lookup failed for insuranceType={}; falling back to database", type, ex);
        }

        List<VendorApiRecord> fromDb = repository.findAllByInsuranceTypeOrderByIdAsc(type.name()).stream()
                .map(this::toRecord).toList();
        if (!fromDb.isEmpty()) {
            try { put(type, fromDb); }
            catch (RuntimeException ex) { LOGGER.warn("Unable to repopulate Redis for insuranceType={}", type, ex); }
        }
        return fromDb;
    }

    public void put(InsuranceType type, List<VendorApiRecord> records) {
        if (records == null || records.isEmpty()) throw new IllegalArgumentException("Cannot cache an empty vendor API list");
        String json = jsonMapper.writeValueAsString(records);
        redisTemplate.opsForValue().set(key(type), json, ttl);
    }

    private VendorApiRecord toRecord(InsuranceVendorApiConfig row) {
        return new VendorApiRecord(row.getApiType(), row.getCompanyId(), row.getPlanId());
    }
    private static String key(InsuranceType type) { return PREFIX + type.name(); }
}
