package ru.mts.depositservice.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.mts.depositservice.entity.Deposit;
import ru.mts.depositservice.entity.Request;
import ru.mts.depositservice.model.*;
import ru.mts.depositservice.service.DepositService;
import ru.mts.depositservice.service.RequestConfirmationService;
import ru.mts.depositservice.service.RequestService;

import java.math.BigDecimal;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/deposit")
public class DepositController {

    private final DepositService depositService;
    private final RequestService requestService;
    private final RequestConfirmationService confirmationService;

    /**
     * Обрабатывает запрос пользователя для вычисления процентной ставки
     * на основе условий открытия вклада
     *
     * @param request Объект запроса с данными для расчета процентной ставки
     * @return Процентная ставка в виде {@code BigDecimal}
     */
    @GetMapping("/check")
    public ResponseEntity<BigDecimal> checkDepositTerms(@RequestBody OpenDepositRequest request) {
        BigDecimal rate = depositService.calculateInterestRate(request);

        return ResponseEntity.ok(rate);
    }

    /**
     * Обрабатывает запрос пользователя по созданию заявки для открытия вклада:
     * <div>
     *   <ul>
     *     <li>высылается СМС сообщение с кодом подтверждения (оно выводится в консоль)</li>
     *     <li>проверяется необходимая сумма</li>
     *     <li>создается заявка</li>
     *   </ul>
     * </div>
     * Также высылается СМС сообщение с кодом подтверждения (оно выводится в консоль)
     *
     * @param openDepositRequest Объект запроса с данными для открытия вклада
     */
    @PostMapping("/open")
    @ResponseStatus(HttpStatus.OK)
    public void requestForOpeningDeposit(@RequestBody OpenDepositRequest openDepositRequest) {
        requestService.openRequest(openDepositRequest);
    }

    /**
     * Обрабатывает запрос пользователя для подтверждения заявки на открытие вклада:
     * <div>
     *   <ul>
     *     <li>подтверждение заявки кодом из СМС сообщения</li>
     *     <li>открытие вклада</li>
     *     <li>перевод денежной суммы со счета на открытий вклад (при достаточном количестве денежной суммы)</li>
     *   </ul>
     * </div>
     *
     * @param openDepositRequest Объект запроса с данными для подтверждения открытия вклада
     * @return Ответ на подтверждение заявки
     */
    @PostMapping("/open/confirm")
    public ResponseEntity<RequestResponse> confirmRequestOpenDeposit(@RequestBody OpenDepositRequest openDepositRequest) {
        RequestResponse response = confirmationService.confirmOpenRequest(openDepositRequest);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Обрабатывает запрос пользователя, который отображает список его открытых вкладов
     * и отклоненных заявок
     *
     * @param customerId Идентификатор клиента
     * @return Информация о депозитах и запросах
     */
    @GetMapping("/{customerId}")
    public ResponseEntity<DepositAccountResponse> showDepositsAndRejectedRequests(@PathVariable Integer customerId) {
        DepositAccountResponse depositAccountResponse = new DepositAccountResponse();

        List<Request> requests = requestService.findRejectedRequests(customerId);
        List<Deposit> deposits = depositService.findOpenedDeposits(customerId);

        depositAccountResponse.setRejectedRequests(requests);
        depositAccountResponse.setDepositList(deposits);

        return new ResponseEntity<>(depositAccountResponse, HttpStatus.FOUND);
    }

    /**
     * Обрабатывает запрос пользователя на пополнение вклада:
     * <div>
     *   <ul>
     *     <li>высылается СМС сообщение с кодом подтверждения (оно выводится в консоль)</li>
     *   </ul>
     * </div>
     */
    @GetMapping("/refill")
    @ResponseStatus(HttpStatus.OK)
    public void requestRefillDepositAccount() {
        requestService.refillRequest();
    }

    /**
     * Обрабатывает запрос пользователя для подтверждения пополнения вклада:
     * <div>
     *   <ul>
     *     <li>подтверждение заявки кодом из СМС сообщения</li>
     *     <li>перевод денежной суммы со счета на открытий вклад (при достаточном количестве денежной суммы)</li>
     *   </ul>
     * </div>
     *
     * @param refillDepositRequest Объект запроса с данными для пополнения вклада
     * @return Ответ на пополнение вклада
     */
    @PostMapping("/refill/confirm")
    public ResponseEntity<RequestResponse> refillDepositAccount(@RequestBody RefillDepositRequest refillDepositRequest) {
        RequestResponse response = confirmationService.confirmRefillDeposit(refillDepositRequest);

        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }

    /**
     * Обрабатывает запрос пользователя на закрытие вклада:
     * <div>
     *   <ul>
     *     <li>высылается СМС сообщение с кодом подтверждения (оно выводится в консоль)</li>
     *   </ul>
     * </div>
     */
    @GetMapping("/close")
    @ResponseStatus(HttpStatus.OK)
    public void applicationForClosingDepositAccount() {
        requestService.closeRequest();
    }

    /**
     * Обрабатывает запрос пользователя для закрытия вклада:
     * <div>
     *   <ul>
     *     <li>подтверждение заявки кодом из СМС сообщения</li>
     *     <li>перевод всей денежной суммы с вклада на банковский счет</li>
     *   </ul>
     * </div>
     *
     * @param closeDepositRequest Объект запроса с данными для закрытия вклада
     */
    @PostMapping("/close/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmCloseDeposit(@RequestBody CloseDepositRequest closeDepositRequest) {
        confirmationService.confirmCloseDeposit(closeDepositRequest);
    }
}
