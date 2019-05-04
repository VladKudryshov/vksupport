package com.github.vksupport.services;

import com.vk.api.sdk.client.actors.UserActor;

public interface IVkService {
    Integer getOnline(UserActor actor);
    void scrapperNews(UserActor actor);
    void deleteAllPosts(UserActor actor);
}
