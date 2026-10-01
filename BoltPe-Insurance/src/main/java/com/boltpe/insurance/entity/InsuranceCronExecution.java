package com.boltpe.insurance.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "insurance_cron_execution")
public class InsuranceCronExecution {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="execution_id", nullable=false, unique=true, length=36) private String executionId;
    @Column(name="job_name", nullable=false, length=100) private String jobName;
    @Column(name="started_at", nullable=false) private Instant startedAt;
    @Column(name="completed_at") private Instant completedAt;
    @Column(name="status", nullable=false, length=30) private String status;
    @Column(name="total_products", nullable=false) private int totalProducts;
    @Column(name="successful_products", nullable=false) private int successfulProducts;
    @Column(name="failed_products", nullable=false) private int failedProducts;
    @Column(name="error_message", columnDefinition="TEXT") private String errorMessage;
    @Column(name="created_at", nullable=false) private Instant createdAt;

    public Long getId(){return id;} public String getExecutionId(){return executionId;} public void setExecutionId(String v){executionId=v;}
    public String getJobName(){return jobName;} public void setJobName(String v){jobName=v;} public Instant getStartedAt(){return startedAt;} public void setStartedAt(Instant v){startedAt=v;}
    public Instant getCompletedAt(){return completedAt;} public void setCompletedAt(Instant v){completedAt=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public int getTotalProducts(){return totalProducts;} public void setTotalProducts(int v){totalProducts=v;} public int getSuccessfulProducts(){return successfulProducts;} public void setSuccessfulProducts(int v){successfulProducts=v;}
    public int getFailedProducts(){return failedProducts;} public void setFailedProducts(int v){failedProducts=v;} public String getErrorMessage(){return errorMessage;} public void setErrorMessage(String v){errorMessage=v;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
}
