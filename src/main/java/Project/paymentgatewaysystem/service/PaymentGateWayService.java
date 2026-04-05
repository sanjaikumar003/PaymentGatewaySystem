package Project.paymentgatewaysystem.service;

import Project.paymentgatewaysystem.constants.PaymentMethod;
import Project.paymentgatewaysystem.constants.PaymentStatus;

public class PaymentGateWayService {
    public PaymentStatus process(PaymentMethod method) {
        boolean success = Math.random() > 0.2;
        return success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
    }
}
