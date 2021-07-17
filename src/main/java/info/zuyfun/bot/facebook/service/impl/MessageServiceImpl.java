package info.zuyfun.bot.facebook.service.impl;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import info.zuyfun.bot.constants.FacebookAPIUrl;
import info.zuyfun.bot.constants.MessageConstants;
import info.zuyfun.bot.constants.PayloadConstants;
import info.zuyfun.bot.facebook.model.Action;
import info.zuyfun.bot.facebook.model.Attachment;
import info.zuyfun.bot.facebook.model.Message;
import info.zuyfun.bot.facebook.model.Profile;
import info.zuyfun.bot.facebook.model.Request;
import info.zuyfun.bot.facebook.model.Simsimi;
import info.zuyfun.bot.facebook.service.MessageService;
import info.zuyfun.bot.facebook.template.MessageTemplate;
import info.zuyfun.bot.service.UserService;
import info.zuyfun.bot.utils.UserAction;

@Service
public class MessageServiceImpl implements MessageService {

	private static final Logger logger = LoggerFactory.getLogger(MessageServiceImpl.class);

	@Value("${fb_access_token}")
	private String FB_ACCESS_TOKEN;
	@Autowired
	UserAction userAction;
	@Autowired
	protected RestTemplate restTemplate;

	@Autowired
	MessageTemplate messageTemplate;

	@Autowired
	UserService userService;
	@Autowired
	ObjectMapper mapper;

	public void callSendAPI(Object objRequest) {
		logger.info("***API Send Attachment***");
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<Object> requestBody = new HttpEntity<>(objRequest, headers);
			logger.info("***Request Object: {}", requestBody.getBody());
			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(FacebookAPIUrl.SEND_MESSAGE)
					.queryParam("access_token", FB_ACCESS_TOKEN);
			String uriBuilder = builder.build().encode().toUriString();
			restTemplate.exchange(uriBuilder, HttpMethod.POST, requestBody, String.class);
		} catch (Exception e) {
			logger.error("***callSendAPI Exception: {}", e);
		}

	}

	public Profile callGetUserAPI(BigDecimal senderID) {
		logger.info("***API GET PROFILE***");
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<Object> requestBody = new HttpEntity<>(headers);
			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(FacebookAPIUrl.GET_PROFILE + senderID)
					.queryParam("fields", "first_name,last_name,profile_pic")
					.queryParam("access_token", FB_ACCESS_TOKEN);
			String uriBuilder = builder.build().encode().toUriString();
			String bodyResponse = restTemplate.exchange(uriBuilder, HttpMethod.GET, requestBody, String.class)
					.getBody();
			Profile objProfile = mapper.readValue(bodyResponse, Profile.class);
			logger.info("***callGetUserAPI : {}", objProfile);
			return objProfile;
		} catch (Exception e) {
			logger.error("***callGetUserAPI Exception: {}", e);
		}
		return null;
	}

	@Override
	public void handleMessage(BigDecimal senderID, Message objMessage) {
		try {
			if (objMessage == null)
				return;
			Request objRequest = null;
			if (objMessage.getText() != null) {
				logger.info("***Message object: {}", objMessage);
				String messageText = objMessage.getText().toLowerCase();
				if (userAction.isCommand(messageText)) {

				} else if (userService.isChatWithBot(senderID)) {
					// Call simsimi here
				} else {
					objRequest = messageTemplate.sendText(senderID, MessageConstants.MESSAGE_ERROR);
				}
			} else if (objMessage.getAttachments() != null || objMessage.getAttachments().isEmpty()) {
				logger.info("***Payload object: {}", objMessage.getAttachments());
				String attachmentUrl = objMessage.getAttachments().get(0).getPayload().getUrl();
				Attachment objAttachment = messageTemplate.testPayload(attachmentUrl);
				objRequest = messageTemplate.sendAttachment(senderID, objAttachment);
			} else {
				return;
			}
			if (objRequest == null)
				return;
			callSendAPI(objRequest);
		} catch (Exception e) {
			logger.error("***handleMessage - Exception: {}", e);
		}
	}

	@Override
	public void handlePostback(BigDecimal senderID, String payload) {
		logger.info("***handlePostback***");
		switch (payload) {
		case "yes":
			payloadGetStarted(senderID);
			break;
		case PayloadConstants.GET_STARTED:
			payloadGetStarted(senderID);
			break;
		default:
			break;
		}
	}

	@Override
	public void typingAction(BigDecimal senderID, String action) {
		Action objAction = messageTemplate.typingAction(senderID, action);
		callSendAPI(objAction);
	}

	public Simsimi callSimsimi(String messageText) {
		logger.info("***Call Simsimi***");
		Simsimi result = null;
		return result;
	}

	public void payloadGetStarted(BigDecimal senderID) {
		Profile userProfile = callGetUserAPI(senderID);
		String fullName = userProfile.getLastName() + "" + userProfile.getFirstName();
		String messageText = MessageConstants.PAYLOAD_GET_STARTED.replace("username", fullName);
		Request objRequest = messageTemplate.sendText(senderID, messageText);
		callSendAPI(objRequest);
	}

}
