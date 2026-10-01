package com.boltpe.insurance.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="insurance_cron_product_execution",
       indexes=@Index(name="idx_cron_product_execution_id", columnList="execution_id"))
public class InsuranceCronProductExecution {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="execution_id", nullable=false, length=36) private String executionId;
    @Column(name="insurance_type", nullable=false, length=50) private String insuranceType;
    @Column(name="started_at", nullable=false) private Instant startedAt;
    @Column(name="completed_at") private Instant completedAt;
    @Column(name="status", nullable=false, length=30) private String status;
    @Column(name="records_received", nullable=false) private int recordsReceived;
    @Column(name="records_inserted", nullable=false) private int recordsInserted;
    @Column(name="db_refreshed", nullable=false) private boolean dbRefreshed;
    @Column(name="redis_refreshed", nullable=false) private boolean redisRefreshed;
    @Column(name="response_json", columnDefinition="TEXT") private String responseJson;
    @Column(name="error_message", columnDefinition="TEXT") private String errorMessage;
    @Column(name="created_at", nullable=false) private Instant createdAt;

    public Long getId(){return id;} public String getExecutionId(){return executionId;} public void setExecutionId(String v){executionId=v;}
    public String getInsuranceType(){return insuranceType;} public void setInsuranceType(String v){insuranceType=v;} public Instant getStartedAt(){return startedAt;} public void setStartedAt(Instant v){startedAt=v;}
    public Instant getCompletedAt(){return completedAt;} public void setCompletedAt(Instant v){completedAt=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public int getRecordsReceived(){return recordsReceived;} public void setRecordsReceived(int v){recordsReceived=v;} public int getRecordsInserted(){return recordsInserted;} public void setRecordsInserted(int v){recordsInserted=v;}
    public boolean isDbRefreshed(){return dbRefreshed;} public void setDbRefreshed(boolean v){dbRefreshed=v;} public boolean isRedisRefreshed(){return redisRefreshed;} public void setRedisRefreshed(boolean v){redisRefreshed=v;}
    public String getResponseJson(){return responseJson;} public void setResponseJson(String v){responseJson=v;} public String getErrorMessage(){return errorMessage;} public void setErrorMessage(String v){errorMessage=v;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
}
