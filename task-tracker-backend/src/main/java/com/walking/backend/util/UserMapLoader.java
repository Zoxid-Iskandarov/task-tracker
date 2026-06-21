package com.walking.backend.util;

import com.walking.backend.domain.dto.user.UserShortResponse;
import com.walking.backend.domain.model.User;
import com.walking.backend.service.UserService;
import lombok.experimental.UtilityClass;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@UtilityClass
public class UserMapLoader {

    public static <T> Map<Long, UserShortResponse> loadUserMap(
            Collection<T> items, 
            Function<T, User> userExtractor, 
            UserService userService) {
        Set<Long> userIds = items.stream()
                .map(userExtractor)
                .filter(Objects::nonNull)
                .map(User::getId)
                .collect(Collectors.toSet());

        if (userIds.isEmpty()) return Map.of();

        return userService.getUserShortsByIds(userIds)
                .stream()
                .collect(Collectors.toMap(UserShortResponse::id, u -> u));
    }
}
