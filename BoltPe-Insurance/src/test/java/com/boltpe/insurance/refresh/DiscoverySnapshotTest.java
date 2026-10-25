package com.boltpe.insurance.refresh;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiscoverySnapshotTest {
 @Test void nullRecordsBecomeEmptyList() {
   var snapshot=new DiscoverySnapshot(null,"{}");
   assertNotNull(snapshot.records()); assertTrue(snapshot.records().isEmpty()); assertEquals("{}",snapshot.responseJson());
 }
 @Test void recordsAreDefensivelyCopiedAndImmutable() {
   var source=new ArrayList<>(List.of(new VendorApiRecord("/h","1","2")));
   var snapshot=new DiscoverySnapshot(source,"{raw}");
   source.add(new VendorApiRecord("/x","3","4"));
   assertEquals(1,snapshot.records().size()); assertEquals("/h",snapshot.records().getFirst().apiType());
   assertThrows(UnsupportedOperationException.class,()->snapshot.records().add(new VendorApiRecord("/y","5","6")));
 }
}
