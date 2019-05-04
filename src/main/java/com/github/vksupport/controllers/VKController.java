package com.github.vksupport.controllers;

import com.github.vksupport.services.IVkService;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.UserAuthResponse;
import com.vk.api.sdk.queries.oauth.OAuthUserAuthorizationCodeFlowQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Objects;

@Controller

public class VKController {

    private TransportClient transportClient = HttpTransportClient.getInstance();
    private VkApiClient vk = new VkApiClient(transportClient);

    @Autowired
    private IVkService service;
    private UserActor actor;

    @RequestMapping(value = "vkauth", method = RequestMethod.GET)
    public String auth(@RequestParam(required = false) String code) {

        actor = new UserActor(297050968, "a89b543db696e28796582680308a9e014fc39f82b6b02f78e4dccf7cd4e40e3132b616660ddf3aa487295");
        if (Objects.nonNull(code)) {
            OAuthUserAuthorizationCodeFlowQuery oAuthUserAuthorizationCodeFlowQuery = vk.oauth().userAuthorizationCodeFlow(6966593, "7hqyr2R1JHwVMJr7Ol2z", "http://localhost:8080/vkauth", code);
            try {
                UserAuthResponse authResponse = oAuthUserAuthorizationCodeFlowQuery.execute();
                actor = new UserActor(authResponse.getUserId(), authResponse.getAccessToken());
                service.getOnline(actor);
                int a = 2;
            } catch (ApiException | ClientException e) {
                return "";
            }
        }
        service.getOnline(actor);
        return "";

    }

    @RequestMapping(value = "vkauth", method = RequestMethod.POST)
    public void token(@RequestBody String token) {
        int a = 2;
    }

    @RequestMapping(value = "scrapper", method = RequestMethod.GET)
    public String scrappingNews() {
        service.scrapperNews(actor);
        return "";
    }

    @RequestMapping(value = "deletePosts", method = RequestMethod.GET)
    public String deletePosts() {
        service.deleteAllPosts(actor);
        return "";
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
