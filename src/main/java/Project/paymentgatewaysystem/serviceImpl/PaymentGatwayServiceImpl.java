package Project.paymentgatewaysystem.serviceImpl;

import Project.paymentgatewaysystem.constants.PaymentMethod;
import Project.paymentgatewaysystem.constants.PaymentStatus;
import Project.paymentgatewaysystem.service.PaymentGatewayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentGatwayServiceImpl implements PaymentGatewayService {
    @Override
    public PaymentStatus process (PaymentMethod method){
        boolean success = Math.random() > 0.2;
        return success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
    }
}
