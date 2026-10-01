package com.boltpe.insurance.refresh;

public record ProductRefreshResult(boolean dbRefreshed, boolean redisRefreshed, String status, String errorMessage) {
    public boolean fullySuccessful() { return dbRefreshed && redisRefreshed; }
}
