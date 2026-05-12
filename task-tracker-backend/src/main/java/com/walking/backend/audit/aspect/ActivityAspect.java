package com.walking.backend.audit.aspect;

import com.walking.backend.audit.annotation.TrackActivity;
import com.walking.backend.audit.service.BoardLookupService;
import com.walking.backend.domain.dto.activity.UserActivityInternalEvent;
import com.walking.backend.domain.dto.board.BoardResponse;
import com.walking.backend.domain.dto.label.LabelResponse;
import com.walking.backend.domain.dto.section.SectionResponse;
import com.walking.backend.domain.dto.task.TaskFullResponse;
import com.walking.backend.domain.projection.BoardInfo;
import com.walking.backend.security.principal.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class ActivityAspect {
    private final BoardLookupService boardLookupService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @AfterReturning(
            pointcut = "@annotation(trackActivity) && @within(org.springframework.stereotype.Service)",
            returning = "result")
    public void track(JoinPoint joinPoint, TrackActivity trackActivity, Object result) {
        CustomUserDetails userDetails = getCurrentUser();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                joinPoint.getTarget(), signature.getMethod(), joinPoint.getArgs(), parameterNameDiscoverer);

        context.setVariable("result", result);
        context.setVariable("userDetails", userDetails);

        String description = expressionParser.parseExpression(trackActivity.description())
                .getValue(context, String.class);

        BoardInfo boardInfo = resolveBoardInfo(result, context);

        applicationEventPublisher.publishEvent(new UserActivityInternalEvent(
                userDetails.id(),
                userDetails.username(),
                userDetails.email(),
                boardInfo.id(),
                boardInfo.name(),
                trackActivity.type(),
                description));
    }

    private BoardInfo resolveBoardInfo(Object result, EvaluationContext context) {
        if (result instanceof BoardResponse res) {
            return new BoardInfo(res.id(), res.name());
        } else if (result instanceof SectionResponse res) {
            return boardLookupService.getBoardInfoById(res.boardId());
        } else if (result instanceof LabelResponse res) {
            return boardLookupService.getBoardInfoById(res.boardId());
        } else if (result instanceof TaskFullResponse res) {
            return boardLookupService.getBoardInfoBySectionId(res.sectionId());
        }

        try {
            Long boardId = expressionParser.parseExpression("#boardId").getValue(context, Long.class);
            if (boardId != null) {
                return boardLookupService.getBoardInfoById(boardId);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return new BoardInfo(null, "Unknown Board");
    }

    private CustomUserDetails getCurrentUser() {
        return (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
