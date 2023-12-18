package com.shoppingapp.productservice;

import com.mongodb.assertions.Assertions;
import com.shoppingapp.productservice.dto.ProductRequest;
import com.shoppingapp.productservice.repository.ProductRepository;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
class ProductServiceApplicationTests {
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:4.4");
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    ProductRepository productRepository;
    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry dynamicPropertyRegistry) {
        dynamicPropertyRegistry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Test
    void shouldCreateProduct() throws Exception {
        ProductRequest productRequest = getProductRequest();
        String productRequestString = objectMapper.writeValueAsString(productRequest);
        mockMvc.perform(MockMvcRequestBuilders.post("/api/product").contentType(MediaType.APPLICATION_JSON).content(productRequestString)).andExpect(status().isCreated());
        Assertions.assertTrue((productRepository.findAll().size() == 1));
    }

    private ProductRequest getProductRequest() {
        return ProductRequest.builder().name("test product").description("testing product").price(BigDecimal.valueOf(10.0)).build();
    }

    @Test
    void shouldGetProduct() throws Exception {
        MvcResult mvcResult =
                mockMvc.perform(MockMvcRequestBuilders.get("/api/product").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();
        Assert.assertEquals("application/json",
                mvcResult.getResponse().getContentType());
    }

}
