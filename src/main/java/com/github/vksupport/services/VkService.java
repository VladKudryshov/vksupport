package com.github.vksupport.services;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.groups.Group;
import com.vk.api.sdk.objects.groups.GroupFull;
import com.vk.api.sdk.objects.groups.responses.GetExtendedResponse;
import com.vk.api.sdk.objects.groups.responses.GetMembersResponse;
import com.vk.api.sdk.objects.users.User;
import com.vk.api.sdk.objects.users.UserMin;
import com.vk.api.sdk.objects.users.UserXtrCounters;
import com.vk.api.sdk.objects.wall.WallPost;
import com.vk.api.sdk.objects.wall.WallPostFull;
import com.vk.api.sdk.objects.wall.WallpostAttachment;
import com.vk.api.sdk.objects.wall.WallpostAttachmentType;
import com.vk.api.sdk.queries.users.UserField;
import com.vk.api.sdk.queries.wall.WallGetFilter;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class VkService implements IVkService {
    private TransportClient transportClient = HttpTransportClient.getInstance();
    private VkApiClient vk = new VkApiClient(transportClient);
    private AtomicReference<Set<Integer>> storage = new AtomicReference<>(Sets.newHashSet());

    @Override
    public Map<Integer, String> getGroups(UserActor actor) {
        Map<Integer, String> storage = Maps.newConcurrentMap();
        AtomicInteger delay = new AtomicInteger();
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
        this.storage.get().forEach(id -> executor.schedule(
                () -> {
                    try {
                        GetExtendedResponse execute = vk.groups()
                                .getExtended(actor)
                                .userId(id)
                                .execute();
                        storage.putAll(execute.getItems()
                                .stream()
                                .filter(this::checkAudioGroup)
                                .collect(Collectors.toMap(Group::getId, Group::getName)));
                    } catch (Exception e) {
                        System.out.println(String.format("Can't get groups by user %d", id));
                    }

                },
                delay.getAndAdd(1),
                TimeUnit.SECONDS));


        while (((ThreadPoolExecutor)executor).getActiveCount() > 0  ) {
            try {
                TimeUnit.MILLISECONDS.sleep(5000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        return storage;
    }

    private boolean checkAudioGroup(GroupFull f) {
        String name = f.getName();
        return name.contains("музыка")
                || name.contains("Музыка")
                || name.contains("music")
                || name.contains("Music");
    }

    @Override
    public void checkOnline(UserActor actor) {

        int count = 0;
        int offset = 0;
        Set<Integer> temp = Sets.newHashSet();
        try {
            Boolean morePages = Boolean.TRUE;
            while (morePages) {
                GetMembersResponse execute = vk.groups()
                        .getMembers(actor)
                        .groupId(String.valueOf(98658058))
                        .offset(offset)
                        .execute();

                List<String> items = execute.getItems()
                        .stream()
                        .map(String::valueOf)
                        .collect(Collectors.toList());

                List<UserXtrCounters> execute1 = vk.users()
                        .get(actor)
                        .userIds(items)
                        .fields(UserField.ONLINE)
                        .execute();

                Set<Integer> ids = execute1
                        .stream()
                        .filter(User::isOnline)
                        .map(UserMin::getId)
                        .collect(Collectors.toSet());

                temp.addAll(ids);

                count += ids.size();

                offset += items.size();
                if (items.size() < 1000) {
                    morePages = Boolean.FALSE;
                }
            }

        } catch (ApiException | ClientException e) {
            e.printStackTrace();
        }

        Set<Integer> online = Sets.difference(temp, storage.get()).immutableCopy();
        Set<Integer> offline = Sets.difference(storage.get(), temp).immutableCopy();
        storage.get().addAll(temp);
        temp.clear();
        SimpleDateFormat format = new SimpleDateFormat("HH-mm-ss");
        String dateString = format.format(new Date());

        System.out.println(String.format("Time %s Online %d [ New online %d Offline %s ] All Today %d", dateString, count, online.size(), offline.size(), storage.get().size()));

    }

    @Override
    public void scrapperNews(UserActor actor, String startPlannedDate) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yy-HH-mm-ss");
        Date parse = format.parse(startPlannedDate);
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(parse);
        Set<List<WallpostAttachment>> storage = Sets.newHashSet();
        try {

            storage.addAll(vk.wall()
                    .get(actor)
                    .ownerId(-98658058)
                    .execute()
                    .getItems()
                    .stream()
                    .map(WallPost::getAttachments)
                    .collect(Collectors.toSet()));


            List<WallPostFull> collect1 = vk.wall()
                    .get(actor)
                    .ownerId(-161118119)
                    .execute()
                    .getItems()
                    .stream()
                    .filter(f -> Objects.nonNull(f.getAttachments()))
                    .filter(f -> f.getAttachments().stream().allMatch(w -> {
                        WallpostAttachmentType type = w.getType();
                        return type.equals(WallpostAttachmentType.AUDIO)
                                || type.equals(WallpostAttachmentType.PHOTO);
                    }))
                    .filter(f -> !storage.contains(f.getAttachments()))
                    .collect(Collectors.toList());


            if (CollectionUtils.isNotEmpty(collect1)) {
                for (WallPostFull post : collect1) {
                    List<WallpostAttachment> attachments = post.getAttachments();
                    storage.add(attachments);
                    mutatePost(actor, gregorianCalendar, post);
                }
            } else {
                String dateString = format.format(new Date());
                System.out.println("Can't find new items " + dateString);
            }

        } catch (ApiException | ClientException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            String dateString = format.format(new Date());
            System.out.println("Incorrect item " + dateString);
            System.out.println(e.getMessage());
        }
    }

    private void mutatePost(UserActor actor, GregorianCalendar gregorianCalendar, WallPostFull post) throws ApiException, ClientException, InterruptedException {
        List<String> collect = post.getAttachments()
                .stream()
                .map(this::getStringAttachment)
                .collect(Collectors.toList());

        try {
            vk.wall()
                    .post(actor)
                    .ownerId(-98658058)
                    .publishDate((int) (gregorianCalendar.getTimeInMillis() / 1000))
                    .attachments(collect)
                    .execute();
            SimpleDateFormat format = new SimpleDateFormat("HH-mm-ss");
            String dateString = format.format(new Date());
            System.out.println(String.format("Planned in %s new post %s", format.format(gregorianCalendar.getTime()), dateString));
            gregorianCalendar.add(Calendar.MINUTE, 30);
            Thread.sleep(2000);
        } catch (ApiException | ClientException | InterruptedException e) {
            System.out.println("Can't planned post " + e.getMessage());
        }
    }

    @Override
    public void deleteAllPosts(UserActor actor, WallGetFilter filter) {
        final AtomicInteger delay = new AtomicInteger();

        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);
        scheduledExecutorService.scheduleWithFixedDelay(() -> {

                    try {
                        vk.wall()
                                .get(actor)
                                .ownerId(-98658058)
                                .filter(filter)
                                .execute()
                                .getItems()
                                .stream()
                                .map(WallPost::getId)
                                .forEach(f -> scheduledExecutorService.schedule(() -> {
                                            try {
                                                vk.wall()
                                                        .delete(actor)
                                                        .ownerId(-98658058)
                                                        .postId(f)
                                                        .execute();
                                                System.out.println("deleted post");
                                            } catch (ApiException | ClientException e) {
                                                e.printStackTrace();
                                            }
                                        },
                                        delay.addAndGet(3), TimeUnit.SECONDS));
                    } catch (ApiException | ClientException e) {
                        e.printStackTrace();
                    }


                },
                0, 1, TimeUnit.MINUTES);
    }

    private String getStringAttachment(WallpostAttachment attachment) {
        WallpostAttachmentType type = attachment.getType();
        switch (type) {
            case AUDIO:
                return String.format("%s%d_%d", type.getValue().toLowerCase(), attachment.getAudio().getOwnerId(), attachment.getAudio().getId());
            case PHOTO:
                return String.format("%s%d_%d", type.getValue().toLowerCase(), attachment.getPhoto().getOwnerId(), attachment.getPhoto().getId());
            case VIDEO:
                throw new RuntimeException();
        }

        return null;
    }
}
