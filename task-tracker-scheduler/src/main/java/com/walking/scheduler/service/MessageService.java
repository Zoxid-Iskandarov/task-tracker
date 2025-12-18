package com.walking.scheduler.service;

import com.walking.scheduler.domain.model.Task;
import com.walking.scheduler.domain.projection.UserProjection;

import java.util.List;

public interface MessageService {

    void buildAndSendMessage(UserProjection user, List<Task> uncompleted, List<Task> completedToday);
}
