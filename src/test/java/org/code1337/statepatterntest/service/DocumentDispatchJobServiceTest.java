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
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.13.1-management");

    @DynamicPropertySource
    static void mongoDbProperties(DynamicPropertyRegistry registry) throws InterruptedException {
        mongoDBContainer.start();
        rabbitMQContainer.start();

        //rabbit mq management tool
        logRabbitMqManagementUrl();

        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQContainer::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMQContainer::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQContainer::getAdminPassword);
    }

    private static void logRabbitMqManagementUrl() throws InterruptedException {
        String host = rabbitMQContainer.getHost();
        Integer port = rabbitMQContainer.getMappedPort(15672);
        String managementUrl = "http://" + host + ":" + port;
        log.info("open management url here... {} {}:{}", managementUrl, rabbitMQContainer.getAdminUsername(), rabbitMQContainer.getAdminPassword());
        Thread.sleep(3000);
    }

    @Test
    void fullTest() throws InterruptedException {
        DocumentDispatchJob documentDispatchJob = new DocumentDispatchJob();
        documentDispatchJob.setAddress(prepareAddress());
        documentDispatchJob.setShippingChannel(ShippingChannel.EMAIL);
        DocumentEntity document = prepareDocument();
        document.getAttachments().add(prepareAttachment());
        documentDispatchJob.setDocument(document);

        //simulate new queue item look at DocumentDispatchJobService
        int documentCount = 10;
        for (int i = 0; i < documentCount; i++) {
            rabbitTemplate.convertAndSend("eDocumentDispatchJobRequest", "rDocumentDispatchJobRequest", documentDispatchJob);
            log.info("sent documentDispatchJob {}...", i);
        }

        //wait a few seconds until all stages are processed
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            List<DocumentDispatchJob> allDocumentDispatchJobs = documentDispatchJobService.findAllDocumentDispatchJobs();
            long finishedObjects = allDocumentDispatchJobs.stream().filter(ddj -> ddj.getJobTracking().getFinished() != null && ddj.getJobTracking().getFinished().isOk()).count();
            Assertions.assertEquals(documentCount, finishedObjects);
            log.info("all documents finished");
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