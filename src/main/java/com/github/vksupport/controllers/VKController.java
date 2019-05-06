package com.github.vksupport.controllers;

import com.github.vksupport.services.IVkService;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.queries.wall.WallGetFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.*;

@RestController
public class VKController {

    ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(10);

    @Autowired
    private IVkService service;
    private UserActor actor;

    @RequestMapping(value = "vkauth", method = RequestMethod.GET)
    public String auth(@RequestParam(required = false) String code) {
        actor = new UserActor(297050968, "a89b543db696e28796582680308a9e014fc39f82b6b02f78e4dccf7cd4e40e3132b616660ddf3aa487295");
        return "";
    }

    @RequestMapping(value = "checkOnline", method = RequestMethod.GET)
    public String checkOnline(@RequestParam(required = false) Integer delay) {
        scheduledExecutorService.scheduleWithFixedDelay(
                () -> service.checkOnline(actor),
                0,
                delay,
                TimeUnit.SECONDS
        );
        return "";
    }

    @RequestMapping(value = "scrapper", method = RequestMethod.GET)
    public String scrappingNews(@RequestParam String startPlannedDate) {
        scheduledExecutorService.schedule(
                () -> {
                    try {
                        service.scrapperNews(actor, startPlannedDate);
                    } catch (ParseException ignored) {
                        System.out.println("coco");
                    }
                },
                0,
                TimeUnit.SECONDS
        );
        return "";
    }

    @RequestMapping(value = "deletePosts", method = RequestMethod.GET)
    public String deletePosts(@RequestParam WallGetFilter filter) {
        scheduledExecutorService.schedule(
                () -> service.deleteAllPosts(actor, filter),
                0,
                TimeUnit.SECONDS
        );
        return "";
    }

    @RequestMapping(value = "musicGroups", method = RequestMethod.GET)
    public Map<Integer, String> getMusicGroup() throws ExecutionException, InterruptedException, TimeoutException {
        ScheduledFuture<Map<Integer, String>> schedule = scheduledExecutorService.schedule(
                () -> service.getGroups(actor),
                0,
                TimeUnit.SECONDS
        );
        return schedule.get(1000, TimeUnit.SECONDS);
    }


    @RequestMapping(value = "callback", method = RequestMethod.POST)
    public ResponseEntity callback() {
        return new ResponseEntity<>("c9af8389", HttpStatus.OK);
    }

    @RequestMapping(value = "callback", method = RequestMethod.GET)
    public ResponseEntity callback1() {
        return new ResponseEntity<>("c9af8389", HttpStatus.OK);
    }

}
