package com.paulsoft.foodyeah.dtos.OrderDetailDto;

import com.paulsoft.foodyeah.entities.Order;
import com.paulsoft.foodyeah.entities.Product;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateOrderDetailDto {
    private Byte quantity;

    private Long orderId;
    private Long productId;
}
