package org.code1337.statepatterntest.service;

import lombok.extern.slf4j.Slf4j;
import org.code1337.statepatterntest.entity.DocumentAttachment;
import org.code1337.statepatterntest.entity.DocumentDispatchJob;
import org.code1337.statepatterntest.entity.EnrichtedData;
import org.code1337.statepatterntest.entity.state.JobState;
import org.code1337.statepatterntest.entity.state.State;
import org.code1337.statepatterntest.repository.DocumentDispatchJobRepository;
import org.code1337.statepatterntest.service.enrichdata.EnrichDataRequest;
import org.code1337.statepatterntest.service.enrichdata.EnrichDataResponse;
import org.code1337.statepatterntest.service.exception.NotFoundException;
import org.code1337.statepatterntest.service.renderdocument.RenderDocumentRequest;
import org.code1337.statepatterntest.service.renderdocument.RenderDocumentResponse;
import org.code1337.statepatterntest.service.sendoutput.SendOutputRequest;
import org.code1337.statepatterntest.service.sendoutput.SendOutputResponse;
import org.code1337.statepatterntest.service.switchstate.SwitchStateRequest;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
@Slf4j
public class DocumentDispatchJobService {
    private static final String NEW_MESSAGE_RECEIVED = "new message received: {}";
    private static final boolean SIMULATE_WAIT_TIME = true;
    private final DocumentDispatchJobRepository dispatchJobRepository;
    private final RabbitTemplate rabbitTemplate;
    private final DocumentDispatchJobRepository documentDispatchJobRepository;

    @Autowired
    public DocumentDispatchJobService(DocumentDispatchJobRepository dispatchJobRepository, RabbitTemplate rabbitTemplate, DocumentDispatchJobRepository documentDispatchJobRepository) {
        this.dispatchJobRepository = dispatchJobRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.documentDispatchJobRepository = documentDispatchJobRepository;
    }

    /**
     * Entry Point
     * <p>
     * <p>
     * OPEN (First State),
     * ENRICH_DATA,
     * PROCESS_RENDER_JOBS,
     * SEND_OUTPUT,
     * FINISHED
     *
     * @param documentDispatchJob
     */
    @RabbitListener(queues = "qDocumentDispatchJobRequest")
    public void receiveDocumentDispatchJobRequest(DocumentDispatchJob documentDispatchJob) {
        try {
            log.info(NEW_MESSAGE_RECEIVED, documentDispatchJob);
            //do stuff
            validate(documentDispatchJob);
            //open ok
            documentDispatchJob.getJobTracking().setOpen(ok());
        } catch (Exception e) {
            log.error(e.getMessage(), e);

            throw new RuntimeException(e);
        } finally {
            dispatchJobRepository.save(documentDispatchJob);
            //Next State
            sendSwitchState(documentDispatchJob.getId(), State.ENRICH_DATA);
        }
    }

    @RabbitListener(queues = "qSwitchStateRequest")
    public void switchState(SwitchStateRequest switchStateRequest) {
        try {
            log.info(NEW_MESSAGE_RECEIVED, switchStateRequest);

            switch (switchStateRequest.getNextState()) {
                case ENRICH_DATA:
                    stateEnrichData(switchStateRequest.getDocumentDispatchJobId());
                    break;
                case PROCESS_RENDER_JOBS:
                    stateRenderJobs(switchStateRequest.getDocumentDispatchJobId());
                    break;
                case SEND_OUTPUT:
                    stateSendOutput(switchStateRequest.getDocumentDispatchJobId());
                    break;
                case FINISHED:
                    stateFinished(switchStateRequest.getDocumentDispatchJobId());
                    break;
                default:
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);

            throw new RuntimeException(e);
        }
    }

    public void stateEnrichData(String documentDispatchJobId) {
        try {
            log.info("stageEnrichData: {}", documentDispatchJobId);
            DocumentDispatchJob documentDispatchJob = findDocumentDispatchJobByIdThrowException(documentDispatchJobId);
            documentDispatchJob.setState(State.ENRICH_DATA);

            //send enrichDataRequest
            EnrichDataRequest enrichDataRequest = new EnrichDataRequest(documentDispatchJobId, documentDispatchJob.getDocument().getId());
            rabbitTemplate.convertAndSend("eEnrichDataRequest", "rEnrichDataRequest", enrichDataRequest);
            documentDispatchJob.getJobTracking().setEnrichData(sent());
            dispatchJobRepository.save(documentDispatchJob);
        } catch (NotFoundException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            handleStateError(State.ENRICH_DATA, documentDispatchJobId, e);
        }
    }

    /**
     * simulate extern system receive request and send response back
     *
     * @param enrichDataRequest
     */
    @RabbitListener(queues = "qEnrichDataRequest")
    public void receivedEnrichDataRequest(EnrichDataRequest enrichDataRequest) {
        try {
            log.info(NEW_MESSAGE_RECEIVED, enrichDataRequest);
            if (SIMULATE_WAIT_TIME) {
                Thread.sleep(randomBetween(500, 1000));
            }
            EnrichDataResponse response = new EnrichDataResponse(enrichDataRequest.getDocumentDispatchJobId(), enrichDataRequest.getDocumentId(), "data1", "data2", "data3");
            rabbitTemplate.convertAndSend("eEnrichDataResponse", "rEnrichDataResponse", response);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "qEnrichDataResponse")
    public void receivedEnrichDataResponse(EnrichDataResponse enrichDataResponse) {
        try {
            log.info(NEW_MESSAGE_RECEIVED, enrichDataResponse);
            DocumentDispatchJob documentDispatchJob = findDocumentDispatchJobByIdThrowException(enrichDataResponse.getDocumentDispatchJobId());
            documentDispatchJob.setEnrichtedData(new EnrichtedData(enrichDataResponse.getData1(), enrichDataResponse.getData2(), enrichDataResponse.getData3()));
            documentDispatchJob.getJobTracking().setEnrichData(ok());
            save(documentDispatchJob);
            //next state
            sendSwitchState(documentDispatchJob.getId(), State.PROCESS_RENDER_JOBS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            handleStateError(State.ENRICH_DATA, enrichDataResponse.getDocumentDispatchJobId(), e);
            throw new RuntimeException(e);
        }
    }

    public void stateRenderJobs(String documentDispatchJobId) {
        try {
            log.info("stateRenderJobs: {}", documentDispatchJobId);
            DocumentDispatchJob documentDispatchJob = findDocumentDispatchJobByIdThrowException(documentDispatchJobId);
            documentDispatchJob.setState(State.PROCESS_RENDER_JOBS);

            //render attachments
            for (DocumentAttachment documentAttachment : documentDispatchJob.getDocument().getAttachments()) {
                RenderDocumentRequest request = new RenderDocumentRequest(true, documentDispatchJobId, documentAttachment.getId(), documentAttachment.getTemplateName(), documentAttachment.getFormat());
                rabbitTemplate.convertAndSend("eRenderJobsRequest", "rRenderJobsRequest", request);
                documentAttachment.setRendered(sent());
                dispatchJobRepository.save(documentDispatchJob);
            }

            //render main document
            RenderDocumentRequest request = new RenderDocumentRequest(false, documentDispatchJobId, documentDispatchJob.getDocument().getId(), documentDispatchJob.getDocument().getTemplateName(), documentDispatchJob.getDocument().getFormat());
            rabbitTemplate.convertAndSend("eRenderJobsRequest", "rRenderJobsRequest", request);
            documentDispatchJob.getDocument().setRendered(sent());

            //all render jobs sent
            documentDispatchJob.getJobTracking().setProcessRenderJobs(sent());

            dispatchJobRepository.save(documentDispatchJob);
        } catch (NotFoundException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            handleStateError(State.PROCESS_RENDER_JOBS, documentDispatchJobId, e);
        }
    }

    /**
     * simulate extern system
     *
     * @param renderDocumentRequest
     */
    @RabbitListener(queues = "qRenderJobsRequest")
    public void receivedRenderDocumentRequest(RenderDocumentRequest renderDocumentRequest) {
        try {
            log.info(NEW_MESSAGE_RECEIVED, renderDocumentRequest);
            if (SIMULATE_WAIT_TIME) {
                Thread.sleep(randomBetween(500, 1000));
            }
            RenderDocumentResponse response = new RenderDocumentResponse(renderDocumentRequest.isAttachment(), renderDocumentRequest.getDocumentDispatchJobId(), renderDocumentRequest.getDocumentId(), String.format("http://document.x/", UUID.randomUUID()));
            rabbitTemplate.convertAndSend("eRenderJobsResponse", "rRenderJobsResponse", response);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "qRenderJobsResponse")
    public void receivedRenderDocumentResponse(RenderDocumentResponse renderDocumentResponse) {
        try {
            log.info(NEW_MESSAGE_RECEIVED, renderDocumentResponse);
            DocumentDispatchJob documentDispatchJob = findDocumentDispatchJobByIdThrowException(renderDocumentResponse.getDocumentDispatchJobId());
            if (renderDocumentResponse.isAttachment()) {
                for (DocumentAttachment attachment : documentDispatchJob.getDocument().getAttachments()) {
                    if (attachment.getId().equals(renderDocumentResponse.getDocumentId())) {
                        attachment.setDocumentUrl(renderDocumentResponse.getDocumentUrl());
                        attachment.setRendered(ok());
                    }
                }
            } else if (documentDispatchJob.getDocument().getId().equals(renderDocumentResponse.getDocumentId())) {
                documentDispatchJob.getDocument().setDocumentUrl(renderDocumentResponse.getDocumentUrl());
                documentDispatchJob.getDocument().setRendered(ok());
            } else {
                log.error("Document dispatch job id {} not match any document", renderDocumentResponse.getDocumentId());
            }

            //check if all done
            boolean allDone = areAllDocumentsRendered(documentDispatchJob);
            if (allDone) {
                documentDispatchJob.getJobTracking().setProcessRenderJobs(ok());
            }
            save(documentDispatchJob);
            if (allDone) {
                //next state
                sendSwitchState(documentDispatchJob.getId(), State.SEND_OUTPUT);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            handleStateError(State.PROCESS_RENDER_JOBS, renderDocumentResponse.getDocumentDispatchJobId(), e);
            throw new RuntimeException(e);
        }
    }

    public void stateSendOutput(String documentDispatchJobId) {
        try {
            log.info("stateSendOutput: {}", documentDispatchJobId);
            DocumentDispatchJob documentDispatchJob = findDocumentDispatchJobByIdThrowException(documentDispatchJobId);
            documentDispatchJob.setState(State.SEND_OUTPUT);

            SendOutputRequest sendOutputRequest = new SendOutputRequest();
            sendOutputRequest.setDocumentDispatchJobId(documentDispatchJobId);
            sendOutputRequest.setDocumentId(documentDispatchJob.getDocument().getId());
            sendOutputRequest.setAddress(documentDispatchJob.getAddress());
            sendOutputRequest.setShippingChannel(documentDispatchJob.getShippingChannel());
            sendOutputRequest.setDocument(documentDispatchJob.getDocument());

            rabbitTemplate.convertAndSend("eSendOutputRequest", "rSendOutputRequest", sendOutputRequest);
            documentDispatchJob.getJobTracking().setSendOutput(sent());
            dispatchJobRepository.save(documentDispatchJob);
            //Next Step look Queue Receiver
        } catch (NotFoundException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            handleStateError(State.SEND_OUTPUT, documentDispatchJobId, e);
        }
    }

    /**
     * simulate extern system
     *
     * @param sendOutputRequest
     */
    @RabbitListener(queues = "qSendOutputRequest")
    public void receivedSendOutputRequest(SendOutputRequest sendOutputRequest) {
        try {
            log.info(NEW_MESSAGE_RECEIVED, sendOutputRequest);
            SendOutputResponse sendOutputResponse = new SendOutputResponse(sendOutputRequest.getDocumentDispatchJobId(), sendOutputRequest.getDocumentId(), false, null);
            if (SIMULATE_WAIT_TIME) {
                Thread.sleep(randomBetween(500, 1000));
            }
            rabbitTemplate.convertAndSend("eSendOutputResponse", "rSendOutputResponse", sendOutputResponse);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }


    @RabbitListener(queues = "qSendOutputResponse")
    public void receivedSendOutputResponse(SendOutputResponse sendOutputResponse) {
        try {
            log.info(NEW_MESSAGE_RECEIVED, sendOutputResponse);
            DocumentDispatchJob documentDispatchJob = findDocumentDispatchJobByIdThrowException(sendOutputResponse.getDocumentDispatchJobId());
            if (sendOutputResponse.isError()) {
                documentDispatchJob.getJobTracking().setSendOutput(error());
            } else {
                //stuff
                documentDispatchJob.getJobTracking().setSendOutput(ok());
            }
            save(documentDispatchJob);
            sendSwitchState(sendOutputResponse.getDocumentDispatchJobId(), State.FINISHED);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            handleStateError(State.PROCESS_RENDER_JOBS, sendOutputResponse.getDocumentDispatchJobId(), e);
            throw new RuntimeException(e);
        }
    }

    public void stateFinished(String documentDispatchJobId) {
        try {
            log.info("stateFinished: {}", documentDispatchJobId);
            DocumentDispatchJob documentDispatchJob = findDocumentDispatchJobByIdThrowException(documentDispatchJobId);
            documentDispatchJob.setState(State.FINISHED);
            documentDispatchJob.getJobTracking().setFinished(ok());
            dispatchJobRepository.save(documentDispatchJob);
            log.info("Document dispatch job finished! {}", documentDispatchJobId);
        } catch (NotFoundException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            handleStateError(State.SEND_OUTPUT, documentDispatchJobId, e);
        }
    }

    private int randomBetween(int min, int max) {
        return (int) (Math.random() * (max - min) + min);
    }

    private void handleStateError(State state, String documentDispatchJobId, Exception e) {
        try {
            log.error("error in state {} occured {}", state.name(), e.getMessage(), e);
            DocumentDispatchJob documentDispatchJob = documentDispatchJobRepository.findById(documentDispatchJobId).orElseThrow(() -> new NotFoundException(String.format("document dispatch job not found, can not handle error id=%s", documentDispatchJobId)));
            switch (state) {
                case ENRICH_DATA:
                    documentDispatchJob.getJobTracking().getEnrichData().setError(true);
                    documentDispatchJob.getJobTracking().getEnrichData().setErrorMessage(e.getMessage());
                    break;
                case PROCESS_RENDER_JOBS:
                    documentDispatchJob.getJobTracking().getProcessRenderJobs().setError(true);
                    documentDispatchJob.getJobTracking().getProcessRenderJobs().setErrorMessage(e.getMessage());
                    break;
                case SEND_OUTPUT:
                    documentDispatchJob.getJobTracking().getSendOutput().setError(true);
                    documentDispatchJob.getJobTracking().getSendOutput().setErrorMessage(e.getMessage());
                    break;
                default:
            }
            save(documentDispatchJob);
        } catch (NotFoundException ex) {
            log.error("can not handle error", ex);
        }
    }


    private void sendSwitchState(String id, State state) {
        log.info("send switch state to {} for {}...", state, id);
        rabbitTemplate.convertAndSend("eSwitchStateRequest", "rSwitchStateRequest", new SwitchStateRequest(id, state));
    }


    public DocumentDispatchJob save(DocumentDispatchJob documentDispatchJob) {
        return dispatchJobRepository.save(documentDispatchJob);
    }

    private DocumentDispatchJob findDocumentDispatchJobByIdThrowException(String documentDispatchJobId) throws NotFoundException {
        return documentDispatchJobRepository.findById(documentDispatchJobId).orElseThrow(() -> new NotFoundException(String.format("document dispatch job not found %s", documentDispatchJobId)));
    }

    private boolean areAllDocumentsRendered(DocumentDispatchJob documentDispatchJob) {
        if (!documentDispatchJob.getDocument().getRendered().isOk()) {
            return false;
        }
        for (DocumentAttachment attachment : documentDispatchJob.getDocument().getAttachments()) {
            if (!attachment.getRendered().isOk()) {
                return false;
            }
        }
        return true;
    }


    private static JobState sent() {
        JobState open = new JobState();
        open.setSent(true);
        return open;
    }

    private static JobState ok() {
        JobState open = new JobState();
        open.setSent(true);
        open.setOk(true);
        return open;
    }

    private static JobState error() {
        JobState open = new JobState();
        open.setSent(true);
        open.setError(true);
        return open;
    }


    private void validate(DocumentDispatchJob documentDispatchJob) {
        Assert.notNull(documentDispatchJob.getAddress(), "Address is required");
        Assert.notNull(documentDispatchJob.getDocument(), "Document is required");
        Assert.notNull(documentDispatchJob.getShippingChannel(), "Shipping channel is required");
    }

    public Optional<DocumentDispatchJob> findDocumentDispatchJobById(String id) {
        return dispatchJobRepository.findById(id);
    }

    public List<DocumentDispatchJob> findAllDocumentDispatchJobs() {
        return dispatchJobRepository.findAll();
    }

    public void deleteDocumentDispatchJobById(String id) {
        dispatchJobRepository.deleteById(id);
    }
}