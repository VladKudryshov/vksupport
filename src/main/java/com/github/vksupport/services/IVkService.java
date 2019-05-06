package com.github.vksupport.services;

import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.queries.wall.WallGetFilter;

import java.text.ParseException;
import java.util.Map;

public interface IVkService {
    void checkOnline(UserActor actor);

    void scrapperNews(UserActor actor, String startPlannedDate) throws ParseException;

    void deleteAllPosts(UserActor actor, WallGetFilter filter);

    Map<Integer, String> getGroups(UserActor actor);
}
