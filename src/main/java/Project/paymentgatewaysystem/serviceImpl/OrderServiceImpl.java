package Project.paymentgatewaysystem.serviceImpl;

import Project.paymentgatewaysystem.constants.OrderStatus;
import Project.paymentgatewaysystem.dto.OrderRequestDto;
import Project.paymentgatewaysystem.dto.OrderResponseDto;
import Project.paymentgatewaysystem.entity.Merchant;
import Project.paymentgatewaysystem.entity.Order;
import Project.paymentgatewaysystem.exception.ResourceNotFoundException;
import Project.paymentgatewaysystem.repository.MerchantRepository;
import Project.paymentgatewaysystem.repository.OrderRepository;
import Project.paymentgatewaysystem.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.hibernate.Hibernate.map;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final MerchantRepository merchantRepository;

    @Override
    @Transactional
    public OrderResponseDto createOrder(Long merchantId, OrderRequestDto request) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Merchant not found: " + merchantId));
        String ikey =(request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank())
                ? request.getIdempotencyKey()
                :UUID.randomUUID().toString();

        return orderRepository.findByIdempotencyKey(ikey)
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
    public OrderResponseDto getById (Long orderId){
        return toDto(orderRepository.findById(orderId)
                .orElseThrow(()-> new ResourceNotFoundException("Order not found: " +orderId)));
    }
    @Override
    public  List<OrderResponseDto> getByMerchant(Long merchantId){
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
