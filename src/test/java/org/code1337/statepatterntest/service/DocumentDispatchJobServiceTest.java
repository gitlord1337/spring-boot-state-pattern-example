package org.code1337.statepatterntest.service;

import lombok.extern.slf4j.Slf4j;
import org.code1337.statepatterntest.entity.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@SpringBootTest
@Slf4j
class DocumentDispatchJobServiceTest {
    @Autowired
    private DocumentDispatchJobService documentDispatchJobService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0").withExposedPorts(27017);

    @Container
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.13");

    @DynamicPropertySource
    static void mongoDbProperties(DynamicPropertyRegistry registry) {
        mongoDBContainer.start();
        rabbitMQContainer.start();

        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQContainer::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMQContainer::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQContainer::getAdminPassword);
    }

    @Test
    void fullTest() {
        DocumentDispatchJob documentDispatchJob = new DocumentDispatchJob();
        documentDispatchJob.setAddress(prepareAddress());
        documentDispatchJob.setShippingChannel(ShippingChannel.EMAIL);
        DocumentEntity document = prepareDocument();
        document.getAttachments().add(prepareAttachment());
        documentDispatchJob.setDocument(document);

        //simulate new queue item look at RabbitMQReceiverService
        rabbitTemplate.convertAndSend("eDocumentDispatchJobRequest", "rDocumentDispatchJobRequest", documentDispatchJob);
        log.info("sent 1 documentDispatchJob...");

        //wait a few seconds until all stages are processed
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            List<DocumentDispatchJob> allDocumentDispatchJobs = documentDispatchJobService.findAllDocumentDispatchJobs();
            Assertions.assertTrue(allDocumentDispatchJobs.get(0).getJobTracking().getFinished() != null && allDocumentDispatchJobs.get(0).getJobTracking().getFinished().isOk());
            log.info(allDocumentDispatchJobs.get(0).getJobTracking().toString());
        });
    }

    private static @NotNull DocumentAttachment prepareAttachment() {
        DocumentAttachment documentAttachment = new DocumentAttachment();
        documentAttachment.setId(UUID.randomUUID().toString());
        documentAttachment.setTemplateName("bill_of_gates");
        documentAttachment.setFormat("pdf");
        return documentAttachment;
    }

    private static @NotNull DocumentEntity prepareDocument() {
        DocumentEntity document = new DocumentEntity();
        document.setId(UUID.randomUUID().toString());
        document.setFormat("html");
        document.setTemplateName("template_html_hello");
        return document;
    }

    private static @NotNull Address prepareAddress() {
        Address address = new Address();
        address.setFirstName("Hans");
        address.setLastName("Peter");
        address.setStreet("affe 1337");
        address.setEmail("hans.peter@junit.test");
        address.setZip("1337");
        return address;
    }
}