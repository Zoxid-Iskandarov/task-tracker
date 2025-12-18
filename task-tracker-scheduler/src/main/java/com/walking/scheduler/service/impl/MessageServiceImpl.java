package com.walking.scheduler.service.impl;

import com.walking.scheduler.domain.dto.MessageDto;
import com.walking.scheduler.domain.model.Task;
import com.walking.scheduler.domain.projection.UserProjection;
import com.walking.scheduler.service.KafkaProducerService;
import com.walking.scheduler.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {
    private final KafkaProducerService kafkaProducerService;

    @Override
    public void buildAndSendMessage(UserProjection user, List<Task> uncompleted, List<Task> completedToday) {
        MessageDto messageDto;

        if (uncompleted.isEmpty()) {
            messageDto = buildCompletedTasksMessage(user, completedToday);
        } else if (completedToday.isEmpty()) {
            messageDto = buildPendingTasksMessage(user, uncompleted);
        } else {
            messageDto = buildCombinedReportMessage(user, uncompleted, completedToday);
        }

        kafkaProducerService.sendMessage(user.id().toString(), messageDto);
    }

    private MessageDto buildCompletedTasksMessage(UserProjection user, List<Task> completedToday) {
        String title = "Task Tracker: Congratulations on completing your tasks!";
        String message = """
                Hello, %s!
                
                This is your daily update from Task Tracker.
                
                Great news! You've completed %d tasks in the last 24 hours. Well done!
                
                You finished:
                %s
                Celebrate your progress! Acknowledging completed tasks is a great motivator.
                
                Keep up the fantastic work!
                
                Best regards,
                Your Task Tracker Team
                """.formatted(user.username(), completedToday.size(), formatTasksAsNumberedList(completedToday));

        return new MessageDto(user.email(), title, message);
    }

    private MessageDto buildPendingTasksMessage(UserProjection user, List<Task> uncompleted) {
        String title = "Task Tracker: You have pending tasks";
        String message = """
                Hello, %s!
                
                This is your daily update from Task Tracker.
                
                You have %d pending tasks waiting for your attention. Don't let them slip away!
                
                Here are the tasks on your list:
                %s
                Take a moment to tackle one today. Consistency is the key to productivity!
                
                Best regards,
                Your Task Tracker Team
                """.formatted(user.username(), uncompleted.size(), formatTasksAsNumberedList(uncompleted));

        return new MessageDto(user.email(), title, message);
    }

    private MessageDto buildCombinedReportMessage(UserProjection user,
                                                  List<Task> uncompleted,
                                                  List<Task> completedToday) {
        String title = "Task Tracker: Your Daily Task Summary";
        String message = """
                Hello, %s!
                
                Here is your daily activity summary from Task Tracker.
                
                You successfully completed %d tasks today! That's an achievement.
                Completed:
                %s
                You still have %d pending tasks to focus on.
                Pending:
                %s
                You're on the right track! Keep the momentum going by reviewing your pending list.
                
                Best regards,
                Your Task Tracker Team
                """.formatted(user.username(), completedToday.size(), formatTasksAsNumberedList(completedToday),
                uncompleted.size(), formatTasksAsNumberedList(uncompleted));

        return new MessageDto(user.email(), title, message);
    }

    private String formatTasksAsNumberedList(List<Task> tasks) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < tasks.size(); i++) {
            sb.append("%d. %s\n".formatted(i + 1, tasks.get(i).getTitle()));

            if (i == 4) break;
        }

        return sb.toString();
    }
}
