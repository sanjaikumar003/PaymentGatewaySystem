package Project.paymentgatewaysystem.serviceImpl;

import Project.paymentgatewaysystem.constants.OrderStatus;
import Project.paymentgatewaysystem.dto.OrderRequestDto;
import Project.paymentgatewaysystem.dto.OrderResponseDto;
import Project.paymentgatewaysystem.entity.Merchant;
import Project.paymentgatewaysystem.entity.MerchantUser;
import Project.paymentgatewaysystem.entity.Order;
import Project.paymentgatewaysystem.exception.ResourceNotFoundException;
import Project.paymentgatewaysystem.repository.MerchantUserRepository;
import Project.paymentgatewaysystem.repository.OrderRepository;
import Project.paymentgatewaysystem.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final MerchantUserRepository merchantUserRepository;

    @Override
    @Transactional
    public OrderResponseDto createOrder(String email, OrderRequestDto request) {
        MerchantUser user = merchantUserRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Merchant not found: " + email));
        Merchant merchant=user.getMerchant();
        String ikey =(request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank())
                ? request.getIdempotencyKey()
                :UUID.randomUUID().toString();

        return orderRepository
                .findByIdempotencyKeyAndMerchant_MerchantId(ikey, merchant.getMerchantId())
                .map(this::toDto)
                .orElseGet(()->{
                    Order order = new Order();
                    order.setMerchant(merchant);
                    order.setAmount(request.getAmount());
                    order.setCurrency(request.getCurrency().toUpperCase());
                    order.setStatus(OrderStatus.CREATED);
                    order.setIdempotencyKey(ikey);
                    return toDto(orderRepository.save(order));

                });
    }
    @Override
    public OrderResponseDto getById( String email,Long orderId) {

        MerchantUser user = merchantUserRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (!order.getMerchant().getMerchantId()
                .equals(user.getMerchant().getMerchantId())) {

            throw new RuntimeException("Access denied");
        }

        return toDto(order);
    }
    @Override
    public  List<OrderResponseDto> getByMerchant(String email){
        MerchantUser user = merchantUserRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Long merchantId = user.getMerchant().getMerchantId();
        return orderRepository.findByMerchant_MerchantId(merchantId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    private OrderResponseDto toDto(Order Id) {
        return new OrderResponseDto(
                Id.getOrderId(),
                Id.getMerchant().getMerchantId(),
                Id.getAmount(),
                Id.getCurrency(),
                Id.getStatus(),
                Id.getIdempotencyKey(),
                Id.getCreatedAt()
        );
    }
}
