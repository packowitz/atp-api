package io.pacworx.atp.survey;

import io.pacworx.atp.user.User;

public interface SurveyRepositoryCustom {

    Survey findAnswerable(User user);

    Survey findAnswerableSecurity(User user);

    Survey findAnswerablePermanent(User user);
}
