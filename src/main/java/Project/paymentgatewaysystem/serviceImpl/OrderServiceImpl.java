package Project.paymentgatewaysystem.serviceImpl;

import Project.paymentgatewaysystem.constants.OrderStatus;
import Project.paymentgatewaysystem.dto.OrderRequestDto;
import Project.paymentgatewaysystem.dto.OrderResponseDto;
import Project.paymentgatewaysystem.entity.Merchant;
import Project.paymentgatewaysystem.entity.MerchantUser;
import Project.paymentgatewaysystem.entity.Order;
import Project.paymentgatewaysystem.exception.ResourceNotFoundException;
import Project.paymentgatewaysystem.exception.UnauthorizedException;
import Project.paymentgatewaysystem.repository.MerchantUserRepository;
import Project.paymentgatewaysystem.repository.OrderRepository;
import Project.paymentgatewaysystem.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final MerchantUserRepository merchantUserRepository;
    private MerchantUser getUser(String email) {
        return merchantUserRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
    @Override
    @Transactional
    public OrderResponseDto createOrder(String email, OrderRequestDto request) {
        MerchantUser user = getUser(email);
        Merchant merchant=user.getMerchant();
        BigDecimal amount = Objects.requireNonNull(request.getAmount(), "Amount is required");
        if(amount.compareTo(BigDecimal.ZERO)<=0){
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        String currency = Objects.requireNonNull(
                request.getCurrency(), "Currency is required"
        ).toUpperCase();

        String ikey =(request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank())
                ? request.getIdempotencyKey()
                :UUID.randomUUID().toString();

        log.info("Creating order for merchant {}", merchant.getMerchantId());

        return orderRepository
                .findByIdempotencyKeyAndMerchant_MerchantId(ikey, merchant.getMerchantId())
                .map(this::toDto)
                .orElseGet(()->{
                    Order order = new Order();
                    order.setMerchant(merchant);
                    order.setAmount(amount);
                    order.setCurrency(currency);
                    order.setStatus(OrderStatus.CREATED);
                    order.setIdempotencyKey(ikey);
                    try {
                        Order saved = orderRepository.save(order);
                        log.info("Order created with ID {}", saved.getOrderId());
                        return toDto(saved);
                    }catch (Exception ex){
                        return orderRepository
                                .findByIdempotencyKeyAndMerchant_MerchantId(ikey,merchant.getMerchantId())
                                .map(this::toDto)
                                .orElseThrow();
                    }

                });
    }
    @Override
    public OrderResponseDto getById( String email,Long orderId) {

        MerchantUser user = getUser(email);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (!order.getMerchant().getMerchantId()
                .equals(user.getMerchant().getMerchantId())) {

            throw new UnauthorizedException("Access denied");
        }

        return toDto(order);
    }
    @Override
    public  List<OrderResponseDto> getByMerchant(String email){
       MerchantUser user = getUser(email);

        Long merchantId = user.getMerchant().getMerchantId();
        log.info("Fetching orders for merchant {}", merchantId);
        return orderRepository.findByMerchant_MerchantId(merchantId)
                .stream()
                .map(this::toDto)
                .toList();
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


