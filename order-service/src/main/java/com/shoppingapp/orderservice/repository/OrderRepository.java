package com.shoppingapp.orderservice.repository;

import com.shoppingapp.orderservice.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order,Long> {}
