package com.boltpe.insurance.refresh;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ProductRefreshResultTest {
 @Test void fullySuccessfulOnlyWhenDbAndRedisAreBothRefreshed() {
   assertTrue(new ProductRefreshResult(true,true,"SUCCESS",null).fullySuccessful());
   assertFalse(new ProductRefreshResult(true,false,"PARTIAL_SUCCESS","redis down").fullySuccessful());
   assertFalse(new ProductRefreshResult(false,false,"FAILED","timeout").fullySuccessful());
   assertFalse(new ProductRefreshResult(false,true,"FAILED","inconsistent").fullySuccessful());
 }
}
