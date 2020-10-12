package com.sse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class NewsController {

	List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

	Map<String, SseEmitter> emittersUserSpecific = new ConcurrentHashMap<>();

	// Method for client subscription
	@CrossOrigin
	@RequestMapping(value = "/subscribe", consumes = MediaType.ALL_VALUE)
	public SseEmitter subscribe(@RequestParam String userId) {
		SseEmitter sseEmitter = new SseEmitter(Long.MAX_VALUE);

		try {
			sseEmitter.send(SseEmitter.event().name("INIT"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		sseEmitter.onCompletion(() -> emitters.remove(sseEmitter));

		emitters.add(sseEmitter);
		return sseEmitter;
	}

	// Method for client subscription for specific users.
	@CrossOrigin
	@RequestMapping(value = "/subscribeUser", consumes = MediaType.ALL_VALUE)
	public SseEmitter subscribeForUser(@RequestParam String userId) {
		SseEmitter sseEmitter = new SseEmitter(Long.MAX_VALUE);

		try {
			sseEmitter.send(SseEmitter.event().name("INIT"));
			emittersUserSpecific.put(userId, sseEmitter);
		} catch (IOException e) {
			e.printStackTrace();
		}

		sseEmitter.onCompletion(() -> emitters.remove(sseEmitter));
		sseEmitter.onTimeout(() -> emitters.remove(sseEmitter));
		sseEmitter.onError((e) -> emitters.remove(sseEmitter));

		return sseEmitter;
	}

	// Method for dispatching events to all clients
	@PostMapping(value = "/dispatchEvent")
	public void dispatchEventToClients(@RequestParam String title, @RequestParam String text) {

		for (SseEmitter emitter : emitters) {
			try {

				String news = new JSONObject().put("title", title).put("text", text).toString();

				emitter.send(SseEmitter.event().name("latestNews").data(news));

			} catch (IOException e) {
//				When we are trying to send an event but client closes the connection
				emitters.remove(emitter);
			}
		}
	}

	// Method for dispatching events to Specific user
	@PostMapping(value = "/dispatchEventForUser")
	public void dispatchEventToSpecificUser(@RequestParam String title, @RequestParam String text,
			@RequestParam String userId) {

		String news = new JSONObject().put("title", title).put("text", text).toString();

		SseEmitter emitter = emittersUserSpecific.get(userId);
		if (emitter != null) {
			try {
				emitter.send(SseEmitter.event().name("latestNews").data(news));

			} catch (IOException e) {
//				When we are trying to send an event but client closes the connection
				emitters.remove(emitter);
			}

		}
	}

}
