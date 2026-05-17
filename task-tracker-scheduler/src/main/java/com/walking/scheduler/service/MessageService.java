package com.walking.scheduler.service;

import com.walking.scheduler.domain.model.ActivityType;
import com.walking.scheduler.domain.model.UserActivity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MessageService {

    public String generateMessage(String username, List<UserActivity> activities) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hello, %s!\n\nHere is a summary of your activity for today across all boards.\n\n"
                .formatted(username));

        sb.append("Today's Activity\n");
        sb.append(countActivitiesByType(activities));

        List<Map.Entry<String, List<UserActivity>>> activitiesByBoard = activities.stream()
                .collect(Collectors.groupingBy(UserActivity::getBoardName))
                .entrySet()
                .stream()
                .sorted((b1, b2) ->
                        Integer.compare(b2.getValue().size(), b1.getValue().size()))
                .toList();

        List<Map.Entry<String, List<UserActivity>>> topBoards = activitiesByBoard.stream()
                .limit(3)
                .toList();

        for (Map.Entry<String, List<UserActivity>> topBoard : topBoards) {
            String boardName = topBoard.getKey();
            List<UserActivity> boardActivities = topBoard.getValue();

            sb.append("Board: %s\n".formatted(boardName));

            List<UserActivity> completedTasks = boardActivities.stream()
                    .filter(board -> board.getActivityType() == ActivityType.TASK_COMPLETED)
                    .toList();

            if (!completedTasks.isEmpty()) {
                sb.append("  Completed tasks:\n");

                completedTasks.stream()
                        .limit(5)
                        .forEach(task ->
                                sb.append("    - %s\n".formatted(extractTaskTitle(task.getDescription()))));

                if (completedTasks.size() > 5) {
                    sb.append("    (and %d more completed tasks...)\n".formatted(completedTasks.size() - 5));
                }
            }

            sb.append("  Activity summary:\n");
            sb.append(countActivitiesByType(boardActivities));
        }

        if (activitiesByBoard.size() > 3) {
            int extraBoardsCount = activitiesByBoard.size() - 3;
            sb.append("(and %d more boards with activity today. You can check them out in the app!)\n".formatted(extraBoardsCount));
        }

        sb.append("\nKeep up the great work!\nBest regards,\nYour Task Tracker Team");
        return sb.toString();
    }

    private String countActivitiesByType(List<UserActivity> activities) {
        StringBuilder sb = new StringBuilder();

        Map<ActivityType, Long> activitiesByType = activities.stream()
                .collect(Collectors.groupingBy(UserActivity::getActivityType, Collectors.counting()));

        activitiesByType.forEach((type, count) -> {
            String readableType = switch (type) {
                case TASK_CREATED -> "Tasks created";
                case TASK_UPDATED -> "Tasks updated";
                case TASK_DELETED -> "Tasks deleted";
                case TASK_MOVED -> "Tasks moved";
                case TASK_COMPLETED -> "Tasks completed";
                case TASK_REOPENED -> "Tasks reopened";
                case TASK_LABEL_ADDED -> "Labels added to tasks";
                case TASK_LABEL_DELETED -> "Labels deleted from tasks";
                case SECTION_CREATED -> "Sections created";
                case SECTION_UPDATED -> "Sections updated";
                case SECTION_DELETED -> "Sections deleted";
                case LABEL_CREATED -> "Labels created";
                case LABEL_UPDATED -> "Labels updated";
                case LABEL_DELETED -> "Labels deleted";
                case BOARD_CREATED -> "Boards created";
                case BOARD_UPDATED -> "Boards updated";
                case BOARD_DELETED -> "Boards deleted";
                case MEMBER_ADDED -> "Members added";
                case MEMBER_REMOVED -> "Members removed";
                case MEMBER_ROLE_CHANGED -> "Members role changed";
                case MEMBER_LEAVED -> "Leaved board";
            };
            sb.append("   - %s: %d\n".formatted(readableType, count));
        });

        return sb.toString();
    }

    private String extractTaskTitle(String description) {
        if (description == null || description.length() < 15) {
            return description;
        }

        return description.substring(15);
    }
}
