package org.code1337.statepatterntest;

import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.code1337.statepatterntest.entity.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.UUID;

@Slf4j
@SpringBootApplication
public class StatePatternTestApplication implements CommandLineRunner {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    public static void main(String[] args) {
        SpringApplication.run(StatePatternTestApplication.class, args);
    }

    @Override
    public void run(String... args) {
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
