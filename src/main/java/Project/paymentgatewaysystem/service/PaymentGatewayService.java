package Project.paymentgatewaysystem.service;

import Project.paymentgatewaysystem.constants.PaymentMethod;
import Project.paymentgatewaysystem.constants.PaymentStatus;

public interface PaymentGatewayService {
    PaymentStatus process(PaymentMethod method);
}
