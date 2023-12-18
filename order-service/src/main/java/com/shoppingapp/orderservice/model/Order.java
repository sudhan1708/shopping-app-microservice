package com.shoppingapp.orderservice.model;

import java.util.List;
import javax.persistence.*;
import lombok.*;

@Entity
@Table(name = "t_orders")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Order {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  private String orderNumber;

  @OneToMany(cascade = CascadeType.ALL)
  private List<OrderLineItems> orderLineItemsList;
}
