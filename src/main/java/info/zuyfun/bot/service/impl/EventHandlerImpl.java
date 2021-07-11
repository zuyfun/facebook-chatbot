package info.zuyfun.bot.service.impl;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import info.zuyfun.bot.model.Attachment;
import info.zuyfun.bot.model.Button;
import info.zuyfun.bot.model.Element;
import info.zuyfun.bot.model.Event;
import info.zuyfun.bot.model.Message;
import info.zuyfun.bot.model.Payload;
import info.zuyfun.bot.model.Request;
import info.zuyfun.bot.model.RequestMessage;
import info.zuyfun.bot.model.RequestRecipient;
import info.zuyfun.bot.model.SimsimiResponse;
import info.zuyfun.bot.service.EventHandler;

@Service
public class EventHandlerImpl implements EventHandler {
	private static final Logger logger = LoggerFactory.getLogger(EventHandlerImpl.class);
	@Value("${fb_access_token}")
	private String FB_ACCESS_TOKEN;
	@Value("${simsimi_url}")
	private String SIMSIMI_URL;

	private String fbURLSender = "https://graph.facebook.com/v2.6/me/messages?access_token=";

	private WebClient clientPool;
	private ObjectMapper mapper;

	@PostConstruct
	public void eventHandleConstruct() {
		fbURLSender += FB_ACCESS_TOKEN;
		mapper = new ObjectMapper();
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
			logger.info("ObjMessage text: " + objMessage);
			if (objMessage.getText() != null) {
				// Object Message
				SimsimiResponse simsimi = mapper.readValue(callSimsimi(objMessage.getText()), SimsimiResponse.class);
				objRequestMessage.setText(simsimi.getSuccess());
			} else if (objMessage.getAttachments() != null || objMessage.getAttachments().isEmpty()) {
				logger.info("Payload: object" + objMessage.getAttachments());
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
			}
			callSendAPI(objRequest);
		} catch (Exception e) {
			logger.error("handleMessage - Exception: {}", e);
		}
	}

	@Override
	public void handlePostback(Event event) {

		logger.info("handlePostback received_postback: ");

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
			break;
		}

		callSendAPI(objRequest);

	}

	@Override
	public void callSendAPI(Request objRequest) {
		// Construct the message body
		// Send the HTTP request to the Messenger Platform
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.add("Accept", MediaType.APPLICATION_JSON_UTF8_VALUE);
			headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
			HttpEntity<Request> requestBody = new HttpEntity<>(objRequest, headers);
			logger.info("Request Object {}", requestBody.getBody());
			restTemplate.postForEntity(fbURLSender, requestBody, String.class);
		} catch (Exception e) {
			logger.error("*** callSendAPI System Error: " + e);
		}

	}

	@Override
	public String callSimsimi(String messageText) {
		logger.debug("***Call Simsimi");
		String result = "";
		try {
			clientPool = WebClient.create(SIMSIMI_URL);
			clientPool.query("text", messageText).query("lang", "vi_VN");
			Response response = clientPool.get();
			result = response.readEntity(String.class);
			logger.info("Simsimi Data response {}", result);

		} catch (Exception ex) {
			logger.error("callSimsimi - Exception: {}", ex);
		}
		return result;
	}

}
