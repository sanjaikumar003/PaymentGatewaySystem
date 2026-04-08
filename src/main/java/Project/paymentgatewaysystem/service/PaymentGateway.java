package Project.paymentgatewaysystem.service;

import Project.paymentgatewaysystem.constants.PaymentMethod;
import Project.paymentgatewaysystem.constants.PaymentStatus;
import Project.paymentgatewaysystem.entity.Payment;

import java.math.BigDecimal;

public interface PaymentGateway {
    PaymentStatus process(PaymentMethod method);
    boolean refund(Payment payment, BigDecimal amount);
}
