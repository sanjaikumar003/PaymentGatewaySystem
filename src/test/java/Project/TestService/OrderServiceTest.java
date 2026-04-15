package Project.TestService;

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
import Project.paymentgatewaysystem.serviceImpl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MerchantUserRepository merchantUserRepository;

    @InjectMocks
    private OrderServiceImpl orderServiceImpl;
    private MerchantUser merchantUser;
    private Merchant merchant;
    private Order order;
    private OrderRequestDto orderRequest;

    @BeforeEach
    void setUp(){
        merchant = new Merchant();
        merchant.setMerchantId(1L);
        merchant.setName("Test Merchant");

        merchantUser = new MerchantUser();
        merchantUser.setEmail("test@example.com");
        merchantUser.setMerchant(merchant);

        order = new Order();
        order.setOrderId(100L);
        order.setMerchant(merchant);
        order.setAmount(new BigDecimal("500.00"));
        order.setCurrency("INR");
        order.setStatus(OrderStatus.CREATED);
        order.setIdempotencyKey("ikey-123");
        order.setCreatedAt(LocalDateTime.now());

        orderRequest = new OrderRequestDto();
        orderRequest.setAmount(new BigDecimal("500.00"));
        orderRequest.setCurrency("INR");
        orderRequest.setIdempotencyKey("ikey-123");

    }
    @Test
    void createOrder_success(){
        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(merchantUser));
        when(orderRepository.findByIdempotencyKeyAndMerchant_MerchantId("ikey-123",1L)).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderResponseDto response = orderServiceImpl.createOrder("test@example.com",orderRequest);
        assertNotNull(response);
        assertEquals(100L,response.getOrderId());
        assertEquals(1L,response.getMerchantId());
        assertEquals(new BigDecimal("500.00"),response.getAmount());
        assertEquals("INR", response.getCurrency());
        assertEquals(OrderStatus.CREATED, response.getStatus());
        verify(orderRepository).save(any(Order.class));
    }
    @Test
    void createOrder_idempotency_existingOrder(){
        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(merchantUser));
        when(orderRepository.findByIdempotencyKeyAndMerchant_MerchantId("ikey-123",1L)).thenReturn(Optional.of(order));

        OrderResponseDto response = orderServiceImpl.createOrder("test@example.com",orderRequest);

        assertNotNull(response);
        assertEquals(100L,response.getOrderId());
        verify(orderRepository,never()).save(any());
    }
    @Test
    void createOrder_idempotencyKey_notProvided(){
        orderRequest.setIdempotencyKey(null);
        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(merchantUser));
        when(orderRepository.findByIdempotencyKeyAndMerchant_MerchantId(anyString(),anyLong())).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderResponseDto response = orderServiceImpl.createOrder("test@example.com",orderRequest);
        verify(orderRepository).save(any(Order.class));
    }
    @Test
    void createOrder_userNotfound(){
        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> {
            orderServiceImpl.createOrder("test@example.com",orderRequest);
        });
        verify(orderRepository, never()).save(any());
    }
    @Test
    void createorder_nullAmount(){
        orderRequest.setAmount(null);
        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(merchantUser));
        assertThrows(NullPointerException.class, () ->{
            orderServiceImpl.createOrder("test@example.com", orderRequest);
        });

    }
    @Test
    void createOrder_zeroAmount(){
        orderRequest.setAmount(BigDecimal.ZERO);
        when(merchantUserRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(merchantUser));

        assertThrows(IllegalArgumentException.class, () -> {
            orderServiceImpl.createOrder("test@example.com", orderRequest);
        });

    }
    @Test
    void createOrder_negativeAmount() {
        orderRequest.setAmount(new BigDecimal("-100.00"));
        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(merchantUser));

        assertThrows(IllegalArgumentException.class, () -> {
            orderServiceImpl.createOrder("test@example.com", orderRequest);
        });
    }
        @Test
        void createOrder_nullCurrency(){
            orderRequest.setCurrency(null);
            when(merchantUserRepository.findByEmail("test@example.com"))
                    .thenReturn(Optional.of(merchantUser));

            assertThrows(NullPointerException.class, () -> {
                orderServiceImpl.createOrder("test@example.com", orderRequest);
            });

        }
        @Test
        void getById_success(){
        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(merchantUser));
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        OrderResponseDto response = orderServiceImpl.getById("test@example.com",100L);

        assertNotNull(response);
        assertEquals(100L,response.getOrderId());
        assertEquals(new BigDecimal("500.00"),response.getAmount());
        assertEquals(OrderStatus.CREATED, response.getStatus());
        }
        @Test
        void getById_orderNotFound(){
        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(merchantUser)).thenReturn(Optional.of(merchantUser));
        when(orderRepository.findById(100L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,() ->{
            orderServiceImpl.getById("test@example.com",100L);
        });
        }
        @Test
        void getById_differentMerchant(){
        Merchant otherMerchant = new Merchant();
        otherMerchant.setMerchantId(99L);
        order.setMerchant(otherMerchant);
        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(merchantUser));
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
            assertThrows(UnauthorizedException.class, () -> {
                orderServiceImpl.getById("test@example.com", 100L);
            });
        }
        @Test
        void getByMerchant_success(){
            when(merchantUserRepository.findByEmail("test@example.com"))
                    .thenReturn(Optional.of(merchantUser));
            when(orderRepository.findByMerchant_MerchantId(1L))
                    .thenReturn(List.of(order));
            List<OrderResponseDto> response = orderServiceImpl.getByMerchant("test@example.com");
            assertNotNull(response);
            assertEquals(1, response.size());
            assertEquals(100L, response.get(0).getOrderId());

        }
    @Test
    void getByMerchant_noOrders_returnsEmptyList() {

        when(merchantUserRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(merchantUser));
        when(orderRepository.findByMerchant_MerchantId(1L))
                .thenReturn(List.of());


        List<OrderResponseDto> response = orderServiceImpl.getByMerchant("test@example.com");

        assertNotNull(response);
        assertEquals(0, response.size());
    }
    @Test
    void getByMerchant_userNotFound_throwsException() {
        when(merchantUserRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            orderServiceImpl.getByMerchant("test@example.com");
        });
    }

}
