package info.zuyfun.bot.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import info.zuyfun.bot.constants.FacebookAPI;
import info.zuyfun.bot.model.Attachment;
import info.zuyfun.bot.model.Button;
import info.zuyfun.bot.model.Element;
import info.zuyfun.bot.model.Event;
import info.zuyfun.bot.model.Message;
import info.zuyfun.bot.model.Payload;
import info.zuyfun.bot.model.Request;
import info.zuyfun.bot.model.RequestMessage;
import info.zuyfun.bot.model.RequestRecipient;

import info.zuyfun.bot.model.Simsimi;
import info.zuyfun.bot.service.EventHandler;

@Service
public class EventHandlerImpl implements EventHandler {
	private static final Logger logger = LoggerFactory.getLogger(EventHandlerImpl.class);
	@Value("${fb_access_token}")
	private String FB_ACCESS_TOKEN;
	@Value("${simsimi_url}")
	private String SIMSIMI_URL;
	Map<String, String> params;

	@PostConstruct
	public void eventHandleConstruct() {
		params = new HashMap<String, String>();
		params.put("access_token=", FB_ACCESS_TOKEN);
	}

	@Autowired
	protected RestTemplate restTemplate;

	@Override
	public void handleMessage(Event event) {
		try {
			Message objMessage = event.getMessage();
			if (objMessage == null)
				return;
			Request objRequest = new Request();
			RequestRecipient objRequestRecipient = new RequestRecipient();
			objRequestRecipient.setId(event.getSender().getId());
			objRequest.setRequestRecipient(objRequestRecipient);
			RequestMessage objRequestMessage = new RequestMessage();
			objRequest.setRequestMessage(objRequestMessage);

			if (objMessage.getText() != null) {
				logger.info("***Message object: {}", objMessage);
				String messageText = objMessage.getText();
				// Object Message
				if (messageText.contains("/ssm ")) {
					Simsimi simsimi = callSimsimi(messageText.replace("/ssm ", ""));
					if (simsimi == null)
						return;
					objRequestMessage.setText(simsimi.getSuccess());
				} else {
					objRequestMessage.setText(messageText);
				}
			} else if (objMessage.getAttachments() != null || objMessage.getAttachments().isEmpty()) {
				logger.info("***Payload object: {}", objMessage.getAttachments());
				String attachmentUrl = objMessage.getAttachments().get(0).getPayload().getUrl();
				Attachment objAttachment = new Attachment();
				Payload objPayload = new Payload();
				List<Element> objElements = new ArrayList<Element>();
				List<Button> objButtons = new ArrayList<Button>();
				// Object message
				objRequestMessage.setAttachment(objAttachment);
				// Type
				objAttachment.setType("template");
				objAttachment.setPayload(objPayload);
				// Payload
				objPayload.setTemplateType("generic");
				objPayload.setElements(objElements);
				// Element
				objElements.add(new Element());
				objElements.get(0).setTitle("Is this the right picture?");
				objElements.get(0).setSubtitle("Tap a button to answer.");
				objElements.get(0).setImageUrl(attachmentUrl);
				objElements.get(0).setButtons(objButtons);
				objButtons.add(new Button());
				objButtons.add(new Button());
				objButtons.get(0).setType("postback");
				objButtons.get(0).setTitle("Yes!");
				objButtons.get(0).setPayload("yes");
				objButtons.get(1).setType("postback");
				objButtons.get(1).setTitle("No!");
				objButtons.get(1).setPayload("no");
			} else {
				return;
			}
			callSendAPI(objRequest);
		} catch (Exception e) {
			logger.error("***handleMessage - Exception: {}", e);
		}
	}

	@Override
	public void handlePostback(Event event) {
		logger.info("***handlePostback***");
		// Get the payload for the postback
		String payload = event.getPostback().getPayload();
		// Set the response based on the postback payload
		Request objRequest = new Request();
		RequestRecipient objRequestRecipient = new RequestRecipient();
		objRequestRecipient.setId(event.getSender().getId());
		objRequest.setRequestRecipient(objRequestRecipient);
		RequestMessage objRequestMessage = new RequestMessage();
		objRequest.setRequestMessage(objRequestMessage);
		switch (payload) {
		case "yes":
			objRequestMessage.setText("Cảm ơn!");
			break;
		case "no":
			objRequestMessage.setText("Oops, Hãy thử lại tấm ảnh khác!");
			break;
		default:
			objRequestMessage.setText("Oops!!!");
			break;
		}
		callSendAPI(objRequest);

	}

	public void callSendAPI(Object objRequest) {
		// Construct the message body
		// Send the HTTP request to the Messenger Platform
		logger.info("***API Send Message***");
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
			HttpEntity<Object> requestBody = new HttpEntity<>(objRequest, headers);
			logger.info("***Request Object: {}", requestBody.getBody());
			restTemplate.postForObject(FacebookAPI.SEND_MESSAGE, requestBody, String.class, params);
		} catch (Exception e) {
			logger.error("***callSendAPI Exception: {}", e);
		}

	}

	public Simsimi callSimsimi(String messageText) {
		logger.info("***Call Simsimi***");

		Simsimi result = null;
//		webClient = WebClient.create(SIMSIMI_URL);
//		try {
//			Flux<Simsimi> flux = webClient.get().uri("?text=" + messageText + "&lang=vi_VN").retrieve()
//					.bodyToFlux(Simsimi.class);
//			result = flux.blockFirst() == null ? null : flux.blockFirst();
//			logger.info("***Simsimi Response {}", result);
//
//		} catch (Exception ex) {
//			logger.error("***callSimsimi Exception: {}", ex);
//		}
		return result;

	}

}
