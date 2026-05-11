package com.walking.backend.audit.annotation;

import com.walking.backend.domain.model.ActivityType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TrackActivity {
    ActivityType type();
    String description();
}
