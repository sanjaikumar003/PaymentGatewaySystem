package Project.paymentgatewaysystem.serviceImpl;

import Project.paymentgatewaysystem.constants.PaymentMethod;
import Project.paymentgatewaysystem.constants.PaymentStatus;
import Project.paymentgatewaysystem.entity.Payment;
import Project.paymentgatewaysystem.service.PaymentGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentGatewayImpl implements PaymentGateway {
    @Override
    public PaymentStatus process (PaymentMethod method){
        boolean success = Math.random() > 0.2;
        return success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
    }
    @Override
    public boolean refund(Payment payment, BigDecimal amount){
        return true;
    }
}
