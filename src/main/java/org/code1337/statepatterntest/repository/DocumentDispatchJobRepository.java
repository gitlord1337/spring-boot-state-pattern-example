package org.code1337.statepatterntest.repository;

import org.code1337.statepatterntest.entity.DocumentDispatchJob;
import org.code1337.statepatterntest.entity.ShippingChannel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentDispatchJobRepository extends MongoRepository<DocumentDispatchJob, String> {

    List<DocumentDispatchJob> findByShippingChannel(ShippingChannel shippingChannel);

    List<DocumentDispatchJob> findByAddressZip(String zip);

    // Weitere benutzerdefinierte Abfragen hier definieren
}