package com.shoppingapp.orderservice.service;

import com.shoppingapp.orderservice.dto.InventoryResponse;
import com.shoppingapp.orderservice.dto.OrderLineItemsDto;
import com.shoppingapp.orderservice.dto.OrderRequest;
import com.shoppingapp.orderservice.event.OrderPlacedEvent;
import com.shoppingapp.orderservice.model.Order;
import com.shoppingapp.orderservice.model.OrderLineItems;
import com.shoppingapp.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.transaction.Transactional;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final Tracer tracer;
    private final KafkaTemplate<String,OrderPlacedEvent> kafkaTemplate;

    public String placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        List<OrderLineItems> orderLineItemsList =
                orderRequest.getOrderLineItemsDtoList().stream().map(this::mapDtoToOrderLineItems).toList();
        order.setOrderLineItemsList(orderLineItemsList);

        List<String> skuCodes = orderLineItemsList.stream().map(OrderLineItems::getSkuCode).toList();


        log.info("calling inventory service");

        Span inventorySpan = tracer.nextSpan().name("inventory-service");

        try(Tracer.SpanInScope spanInScope = tracer.withSpan(inventorySpan.start())){
            //call inventory service to check if the items are in stock
            InventoryResponse[] inventoryResponses = webClientBuilder.build().get().uri("http://inventory-service/api/inventory",
                            uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                    .retrieve().bodyToMono(InventoryResponse[].class).block();


            assert inventoryResponses != null;
            Boolean allProductsInStock = Arrays.stream(inventoryResponses).allMatch(InventoryResponse::getIsInStock);

            if (allProductsInStock) {
                orderRepository.save(order);
                try{
                    kafkaTemplate.send("notification_topic",new OrderPlacedEvent(order.getOrderNumber()));
                    log.info("order number {} sent to kafka topic notification_topic successfully",order.getOrderNumber());
                }catch (KafkaException e) {
                    log.error("Error sending message to Kafka: {}", e.getMessage(), e);
                }
                return "Order placed successfully";
            } else {
                throw new IllegalArgumentException("Item not in stock");
            }
        }finally {
            inventorySpan.end();
        }

    }

    private OrderLineItems mapDtoToOrderLineItems(OrderLineItemsDto orderLineItemsDto) {
        return OrderLineItems.builder()
                .skuCode(orderLineItemsDto.getSkuCode())
                .price(orderLineItemsDto.getPrice())
                .quantity(orderLineItemsDto.getQuantity())
                .build();
    }
}
