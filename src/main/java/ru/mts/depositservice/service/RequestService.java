package ru.mts.depositservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mts.depositservice.client.CustomerClient;
import ru.mts.depositservice.entity.Customer;
import ru.mts.depositservice.entity.Request;
import ru.mts.depositservice.entity.RequestStatus;
import ru.mts.depositservice.enums.RequestStatusEnum;
import ru.mts.depositservice.model.OpenDepositRequest;
import ru.mts.depositservice.repository.RequestRepository;
import ru.mts.depositservice.repository.RequestStatusRepository;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestService {

    private final DepositService depositService;

    private final CustomerClient customerClient;
    private final RequestRepository requestRepository;
    private final RequestStatusService requestStatusService;
    private final RequestStatusRepository requestStatusRepository;
    private final SmsConfirmationServiceImpl smsConfirmationService;

    /**
     * Открывает заявку на новый депозит
     * <p>
     * Проверяет минимальную сумму для открытия вклада, генерирует код подтверждения, сохраняет заявку в базе данных
     * и устанавливает статус "НА ПОДТВЕРЖДЕНИИ".
     *
     * @param openDepositRequest Объект запроса на открытие депозита с указанной суммой
     */
    @Transactional
    public void openRequest(OpenDepositRequest openDepositRequest) {
        // Проверяем минимальную сумму для открытия вклада
        depositService.validateMinimumDepositAmount(openDepositRequest.getDepositAmount());

        // Генерируем код подтверждения для открытия вклада
        String openConfirmationCode = smsConfirmationService.generateVerificationCode();
        smsConfirmationService.setOpenConfirmationCode(openConfirmationCode);
        log.info("Код {} для операции открытия вклада", openConfirmationCode);

        RequestStatus confirmingStatus = requestStatusRepository.findRequestStatusByStatusName(RequestStatusEnum.CONFIRMING);
        Customer customer = customerClient.findCustomer(openDepositRequest.getCustomerId());

        // Создаем новую заявку для открытия вклада
        Request request = new Request();
        request.setCustomer(customer);
        request.setAmount(openDepositRequest.getDepositAmount());
        request.setRequestDate(new Date());

        request = requestRepository.save(request);                                      // сохраняем заявку в базе данных

        requestStatusService.changeCurrentRequestStatus(request, confirmingStatus);     // устанавливаем статус "НА ПОДТВЕРЖДЕНИИ"
    }

    /**
     * Генерирует код подтверждения для операции пополнения вклада
     */
    public void refillRequest() {
        String refillConfirmationCode = smsConfirmationService.generateVerificationCode();
        smsConfirmationService.setRefillConfirmationCode(refillConfirmationCode);
        log.info("Код {} для операции пополнения вклада", refillConfirmationCode);
    }

    /**
     * Генерирует код подтверждения для операции закрытия вклада
     */
    public void closeRequest() {
        String closeConfirmationCode = smsConfirmationService.generateVerificationCode();
        smsConfirmationService.setCloseConfirmationCode(closeConfirmationCode);
        log.info("Код {} для операции закрытия вклада", closeConfirmationCode);
    }

    /**
     * Находит все отклоненные заявки для указанного клиента
     *
     * @param customerId Идентификатор клиента
     * @return Список отклоненных заявок
     */
    public List<Request> findRejectedRequests(Integer customerId) {
        return requestRepository.findRequestsByCustomerId(customerId);
    }
}
