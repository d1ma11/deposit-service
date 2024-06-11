package ru.mts.depositservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mts.depositservice.entity.CurrentRequestStatus;
import ru.mts.depositservice.entity.CurrentRequestStatusKey;
import ru.mts.depositservice.entity.Request;
import ru.mts.depositservice.entity.RequestStatus;
import ru.mts.depositservice.repository.CurrentRequestStatusRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RequestStatusService {

    private final CurrentRequestStatusRepository currentRequestStatusRepository;

    /**
     * Изменяет текущий статус запроса.
     * <p>
     * Метод используется для обновления статуса запроса на новый, сохраняя историю изменений статуса запроса
     *
     * @param request Запрос, статус которого нужно изменить
     * @param status  Новый статус запроса
     */
    @Transactional
    public void changeCurrentRequestStatus(Request request, RequestStatus status) {
        CurrentRequestStatus currentRequestStatus = new CurrentRequestStatus();
        currentRequestStatus.setRequest(request);
        currentRequestStatus.setStatus(status);
        currentRequestStatus.setUpdateTime(LocalDateTime.now());
        currentRequestStatus.setId(new CurrentRequestStatusKey(request.getId(), status.getId()));
        currentRequestStatusRepository.save(currentRequestStatus);
    }
}
